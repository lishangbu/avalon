package playercharacter

import (
	"context"
	"sync"
	"sync/atomic"

	"github.com/lishangbu/avalon/internal/platform/snowflake"
)

// ActiveBindingHub 在单实例进程内向同账号的长连接投递最新活动绑定。
// 每个订阅者只保留最新事件，慢连接不会阻塞切换事务后的请求收尾。
type ActiveBindingHub struct {
	mu          sync.Mutex
	connection  atomic.Uint64
	subscribers map[snowflake.ID]map[uint64]chan ActiveBinding
}

// NewActiveBindingHub 创建账号级活动绑定事件 Hub。
func NewActiveBindingHub() *ActiveBindingHub {
	return &ActiveBindingHub{subscribers: make(map[snowflake.ID]map[uint64]chan ActiveBinding)}
}

// Subscribe 为一个已认证连接订阅所属账号的活动绑定变化。
// 调用方必须执行返回的 cancel，以便连接关闭时释放资源。
func (h *ActiveBindingHub) Subscribe(accountID snowflake.ID) (<-chan ActiveBinding, func()) {
	connectionID := h.connection.Add(1)
	updates := make(chan ActiveBinding, 1)
	h.mu.Lock()
	accountSubscribers := h.subscribers[accountID]
	if accountSubscribers == nil {
		accountSubscribers = make(map[uint64]chan ActiveBinding)
		h.subscribers[accountID] = accountSubscribers
	}
	accountSubscribers[connectionID] = updates
	h.mu.Unlock()
	return updates, func() {
		h.mu.Lock()
		defer h.mu.Unlock()
		accountSubscribers := h.subscribers[accountID]
		delete(accountSubscribers, connectionID)
		if len(accountSubscribers) == 0 {
			delete(h.subscribers, accountID)
		}
	}
}

// ActivePlayerCharacterChanged 向同账号全部订阅者投递最新持久绑定。
func (h *ActiveBindingHub) ActivePlayerCharacterChanged(_ context.Context, binding ActiveBinding) {
	h.mu.Lock()
	defer h.mu.Unlock()
	for _, updates := range h.subscribers[binding.AccountID] {
		select {
		case updates <- binding:
		default:
			// 丢弃尚未消费的旧值，再写入最新绑定，避免设备收到过期切换。
			<-updates
			updates <- binding
		}
	}
}
