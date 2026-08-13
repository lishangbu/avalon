package battle

import (
	"context"
	"errors"
	"sync"

	"github.com/lishangbu/avalon/internal/platform/snowflake"
)

const defaultRealtimeQueueCapacity = 8

// ErrRealtimeUnavailable 表示实时视图所需的账本读取边界尚未正确装配。
var ErrRealtimeUnavailable = errors.New("对战实时视图不可用")

// DisclosureReader 读取某一真人 Participant 当前已获准知晓的披露账本视图。
//
// 实现不得返回完整权威状态，也不得让调用者自行传入对手的 Participant Side；账本读取边界必须
// 同时验证 Battle 和 PlayerCharacter 的参与关系。
type DisclosureReader interface {
	GetParticipantDisclosure(context.Context, snowflake.ID, snowflake.ID) (DisclosureView, error)
}

// RealtimeSubscription 是一个已完成初始重同步、可持续接收安全战斗视图的连接订阅。
//
// Views 的容量固定且有界。消费者未及时读取时，Hub 会主动关闭该订阅以保护 Battle Runtime 和其他
// 正常连接；客户端应重新鉴权并获取完整最新快照。
type RealtimeSubscription struct {
	// Views 按披露状态版本递增发送完整安全视图，不发送完整权威 Turn Record。
	Views <-chan DisclosureView
	close func()
}

// Close 主动解除订阅并释放关联的有界发送队列；可安全重复调用。
func (subscription *RealtimeSubscription) Close() {
	if subscription != nil && subscription.close != nil {
		subscription.close()
	}
}

// RealtimeHub 在进程内把已持久化的 Disclosure Ledger 变化扇出给实时传输适配器。
//
// 它不是权威状态、事件溯源或跨进程消息总线：每个发布都重新从数据库读取相应 Participant 的
// 账本，因此进程重启和连接重建不会丢失可见状态。多实例广播与可靠后台副作用由独立 Outbox
// 链路解决，不能用本 Hub 替代。
type RealtimeHub struct {
	reader        DisclosureReader
	queueCapacity int

	mu          sync.Mutex
	nextID      uint64
	subscribers map[snowflake.ID]map[uint64]*realtimeSubscriber
}

// NewRealtimeHub 使用明确的 Disclosure Ledger 读取边界创建实时视图 Hub。
//
// queueCapacity 小于 1 时采用保守的默认容量 8，防止某个慢连接无限累积内存或阻塞其他连接。
func NewRealtimeHub(reader DisclosureReader, queueCapacity int) *RealtimeHub {
	if queueCapacity < 1 {
		queueCapacity = defaultRealtimeQueueCapacity
	}
	return &RealtimeHub{
		reader: reader, queueCapacity: queueCapacity,
		subscribers: make(map[snowflake.ID]map[uint64]*realtimeSubscriber),
	}
}

// Subscribe 读取完整当前视图、登记指定真人 Participant，并二次读取填补登记期间的状态变化。
//
// 第二次读取与订阅登记交叠：若变化发生在登记前，二次读取会补齐；若发生在登记后，Publish 会
// 投递新视图。偶发的同版本重复快照对客户端无害，因 StateVersion 是单调且可去重的。
func (hub *RealtimeHub) Subscribe(
	ctx context.Context,
	battleID snowflake.ID,
	playerCharacterID snowflake.ID,
) (*RealtimeSubscription, error) {
	if hub == nil || hub.reader == nil || battleID == snowflake.ID(0) || playerCharacterID == snowflake.ID(0) {
		return nil, ErrRealtimeUnavailable
	}
	view, err := hub.reader.GetParticipantDisclosure(ctx, battleID, playerCharacterID)
	if err != nil {
		return nil, err
	}

	hub.mu.Lock()
	hub.nextID++
	subscriber := &realtimeSubscriber{
		id: hub.nextID, battleID: battleID, playerCharacterID: playerCharacterID,
		views: make(chan DisclosureView, hub.queueCapacity),
	}
	if hub.subscribers[battleID] == nil {
		hub.subscribers[battleID] = make(map[uint64]*realtimeSubscriber)
	}
	hub.subscribers[battleID][subscriber.id] = subscriber
	// 新订阅的队列为空，此处不会阻塞；保留 select 使未来实现保持同样的慢客户端语义。
	select {
	case subscriber.views <- view:
	default:
		hub.removeLocked(subscriber)
		hub.mu.Unlock()
		return nil, ErrRealtimeUnavailable
	}
	hub.mu.Unlock()

	latest, latestErr := hub.reader.GetParticipantDisclosure(ctx, battleID, playerCharacterID)
	if latestErr != nil {
		hub.remove(subscriber)
		return nil, latestErr
	}
	if latest.StateVersion > view.StateVersion {
		hub.deliver(subscriber, latest)
	}

	return &RealtimeSubscription{
		Views: subscriber.views,
		close: func() {
			hub.remove(subscriber)
		},
	}, nil
}

// Publish 从已提交的 Disclosure Ledger 为指定 Battle 的每个连接读取各自安全视图并扇出。
//
// 读取或单个连接失败不会影响其他订阅，也不会回滚已经提交的回合。队列已满的连接会被移除，
// 由实时传输层关闭底层连接；重新连接始终从账本执行完整重同步。
func (hub *RealtimeHub) Publish(ctx context.Context, battleID snowflake.ID) {
	if hub == nil || hub.reader == nil || battleID == snowflake.ID(0) {
		return
	}
	receivers := hub.receivers(battleID)
	for _, receiver := range receivers {
		view, err := hub.reader.GetParticipantDisclosure(ctx, battleID, receiver.playerCharacterID)
		if err != nil {
			continue
		}
		hub.deliver(receiver, view)
	}
}

// Close 解除当前进程持有的全部实时订阅；服务器优雅停止时调用它会立即唤醒所有写入协程。
func (hub *RealtimeHub) Close() {
	if hub == nil {
		return
	}
	hub.mu.Lock()
	defer hub.mu.Unlock()
	for _, byID := range hub.subscribers {
		for _, subscriber := range byID {
			subscriber.closeLocked()
		}
	}
	hub.subscribers = make(map[snowflake.ID]map[uint64]*realtimeSubscriber)
}

type realtimeSubscriber struct {
	id                uint64
	battleID          snowflake.ID
	playerCharacterID snowflake.ID
	views             chan DisclosureView
	closed            bool
}

func (hub *RealtimeHub) receivers(battleID snowflake.ID) []*realtimeSubscriber {
	hub.mu.Lock()
	defer hub.mu.Unlock()
	byID := hub.subscribers[battleID]
	receivers := make([]*realtimeSubscriber, 0, len(byID))
	for _, subscriber := range byID {
		if !subscriber.closed {
			receivers = append(receivers, subscriber)
		}
	}
	return receivers
}

func (hub *RealtimeHub) deliver(receiver *realtimeSubscriber, view DisclosureView) {
	hub.mu.Lock()
	defer hub.mu.Unlock()
	current, found := hub.subscribers[receiver.battleID][receiver.id]
	if !found || current != receiver || current.closed {
		return
	}
	select {
	case current.views <- view:
	default:
		hub.removeLocked(current)
	}
}

func (hub *RealtimeHub) remove(subscriber *realtimeSubscriber) {
	if hub == nil || subscriber == nil {
		return
	}
	hub.mu.Lock()
	defer hub.mu.Unlock()
	hub.removeLocked(subscriber)
}

func (hub *RealtimeHub) removeLocked(subscriber *realtimeSubscriber) {
	byID, found := hub.subscribers[subscriber.battleID]
	if !found || byID[subscriber.id] != subscriber {
		return
	}
	delete(byID, subscriber.id)
	if len(byID) == 0 {
		delete(hub.subscribers, subscriber.battleID)
	}
	subscriber.closeLocked()
}

func (subscriber *realtimeSubscriber) closeLocked() {
	if subscriber.closed {
		return
	}
	subscriber.closed = true
	close(subscriber.views)
}
