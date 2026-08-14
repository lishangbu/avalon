// Package asset 管理 RustFS 认证写入、公开读取的不可变对象 Pending/Ready 生命周期。
package asset

import (
	"bytes"
	"context"
	"crypto/sha256"
	"encoding/hex"
	"errors"
	"fmt"
	"image"
	"image/color"
	_ "image/jpeg"
	_ "image/png"
	"io"
	"net/http"
	"strings"
	"time"

	"github.com/lishangbu/avalon/internal/platform/snowflake"

	_ "golang.org/x/image/webp"
)

const (
	maximumAssetBytes  = int64(10 * 1024 * 1024)
	maximumImagePixels = int64(16_000_000)
	maximumImageSide   = 8192
	// maximumDecodedImageBytes 显式约束完整解码后的像素缓冲预算。
	maximumDecodedImageBytes = int64(128 * 1024 * 1024)
	// imageDecodeTimeout 限制单次配置解析和完整解码共享的 CPU 时间窗口。
	imageDecodeTimeout = 2 * time.Second
	// maximumConcurrentImageDecodes 防止超时后尚未退出的标准库解码任务无限堆积。
	maximumConcurrentImageDecodes = 2
	uploadGrantTTL                = 10 * time.Minute
)

var (
	// ErrInvalidAsset 表示上传声明、版本或命令上下文无效。
	ErrInvalidAsset = errors.New("Asset 请求无效")
	// ErrAssetNotFound 表示调用账号不可访问指定 Asset。
	ErrAssetNotFound = errors.New("Asset 不存在")
	// ErrAssetConflict 表示 Asset 状态或乐观版本已经变化。
	ErrAssetConflict = errors.New("Asset 状态冲突")
	// ErrAssetContentInvalid 表示 RustFS 对象与声明或安全图片策略不一致。
	ErrAssetContentInvalid = errors.New("Asset 内容校验失败")
)

// Status 是 PostgreSQL 中受约束的 Asset 生命周期状态。
type Status string

const (
	// StatusPending 表示对象已保留但尚未完成后端字节校验。
	StatusPending Status = "pending"
	// StatusReady 表示对象通过完整校验，可供实时资料引用。
	StatusReady Status = "ready"
)

// Asset 是对象存储键与校验元数据的 PostgreSQL 权威记录。
type Asset struct {
	// ID 是 Asset 的稳定 Identifier。
	ID snowflake.ID
	// OwnerAccountID 是创建并管理该 Asset 的账号 Identifier。
	OwnerAccountID snowflake.ID
	// ObjectKey 是 RustFS Bucket 内不可变对象键。
	ObjectKey string
	// Status 是 Pending 或 Ready 生命周期状态。
	Status Status
	// MediaType 是经过规范化的图片 MIME 类型。
	MediaType string
	// ExpectedSize 是客户端声明且待服务端确认的字节数。
	ExpectedSize int64
	// ExpectedSHA256 是客户端声明且待服务端确认的 SHA-256 摘要。
	ExpectedSHA256 []byte
	// ActualSize 是确认成功后由服务端读取的真实字节数。
	ActualSize *int64
	// ActualSHA256 是确认成功后由服务端计算的 SHA-256 摘要。
	ActualSHA256 []byte
	// Width 是确认成功后完整解码得到的图片宽度。
	Width *int32
	// Height 是确认成功后完整解码得到的图片高度。
	Height *int32
	// Version 是 Asset 乐观锁版本。
	Version int64
	// CreatedAt 是 Pending 记录创建时间。
	CreatedAt time.Time
	// ReadyAt 是完成对象校验并进入 Ready 状态的时间。
	ReadyAt *time.Time
}

// ListQuery 是当前账号 Asset 管理列表的页码和可选状态条件。
type ListQuery struct {
	// Page 是从 1 开始的页码。
	Page int32
	// PageSize 是每页记录数，最大为 100。
	PageSize int32
	// Status 为空时返回全部状态，否则只允许 Pending 或 Ready。
	Status Status
}

// Page 是当前账号可管理 Asset 的分页结果。
type Page struct {
	// Items 按创建时间和 Identifier 倒序排列。
	Items []Asset
	// Page 是本次查询采用的页码。
	Page int32
	// PageSize 是本次查询采用的每页记录数。
	PageSize int32
	// Total 是筛选条件命中的总记录数。
	Total int64
}

// CommandContext 保存关键 Asset 写操作的操作者、幂等键和请求追踪标识。
type CommandContext struct {
	ActorAccountID snowflake.ID
	IdempotencyKey string
	RequestID      string
}

func (c CommandContext) normalize() CommandContext {
	c.IdempotencyKey = strings.TrimSpace(c.IdempotencyKey)
	c.RequestID = strings.TrimSpace(c.RequestID)
	return c
}

func (c CommandContext) valid() bool {
	return c.ActorAccountID != snowflake.ID(0) && c.IdempotencyKey != "" && len(c.IdempotencyKey) <= 128 &&
		c.RequestID != "" && len(c.RequestID) <= 128
}

// BeginUploadCommand 声明客户端即将上传的不可变图片字节。
type BeginUploadCommand struct {
	CommandContext
	MediaType      string
	ExpectedSize   int64
	ExpectedSHA256 string
}

// ConfirmCommand 请求校验对象并把 Pending Asset 原子转为 Ready。
type ConfirmCommand struct {
	CommandContext
	AssetID         snowflake.ID
	ExpectedVersion int64
}

// UploadGrant 是不持久化、不写日志的短期认证上传授权。
type UploadGrant struct {
	Asset     Asset
	URL       string
	Headers   map[string]string
	ExpiresAt time.Time
}

// DownloadGrant 是通过数据库归属检查后返回的稳定公开读取位置。
type DownloadGrant struct {
	// URL 是不携带签名和临时凭据、不会由 Avalon 主动设置过期时间的公开对象地址。
	URL string
}

// ReserveRecord 是 Pending Asset 事务写入所需的完整事实。
type ReserveRecord struct {
	CommandContext
	ID             snowflake.ID
	ObjectKey      string
	MediaType      string
	ExpectedSize   int64
	ExpectedSHA256 []byte
	CreatedAt      time.Time
}

// ReadyRecord 是对象完成外部校验后提交 PostgreSQL 的事实。
type ReadyRecord struct {
	CommandContext
	AssetID         snowflake.ID
	ExpectedVersion int64
	ActualSize      int64
	ActualSHA256    []byte
	Width           int32
	Height          int32
	ReadyAt         time.Time
}

// Writer 是 Asset 关键写事务内允许使用的最小 PostgreSQL 能力。
type Writer interface {
	Reserve(context.Context, ReserveRecord) (Asset, error)
	MarkReady(context.Context, ReadyRecord) (Asset, error)
}

// Repository 是 Asset 服务使用的关系型持久化端口。
type Repository interface {
	ListOwned(context.Context, snowflake.ID, ListQuery) (Page, error)
	GetOwned(context.Context, snowflake.ID, snowflake.ID) (Asset, error)
	WithinAsset(context.Context, func(Writer) error) error
}

// List 返回当前账号拥有的 Asset 管理分页，不暴露其他账号的对象记录。
func (s *Service) List(ctx context.Context, actorID snowflake.ID, query ListQuery) (Page, error) {
	if actorID == snowflake.ID(0) || query.Page < 1 || query.PageSize < 1 || query.PageSize > 100 ||
		(query.Status != "" && query.Status != StatusPending && query.Status != StatusReady) {
		return Page{}, ErrInvalidAsset
	}
	return s.repository.ListOwned(ctx, actorID, query)
}

// BlobObject 是后端使用服务账号从 RustFS 读取、等待完整校验的对象。
type BlobObject struct {
	Body      io.ReadCloser
	Size      int64
	MediaType string
}

// BlobStore 隔离 S3 兼容认证写、服务端校验读取和公开对象定位；实现不得记录上传签名。
type BlobStore interface {
	// PresignUpload 为一次不可变对象写入签发短期认证请求。
	PresignUpload(context.Context, string, string, int64, []byte, time.Duration) (string, map[string]string, error)
	// Get 使用后端服务账号读取对象，供确认流程完整校验字节。
	Get(context.Context, string) (BlobObject, error)
	// PublicURL 返回不带认证信息且不会过期的公开读取地址。
	PublicURL(string) (string, error)
}

// Service 在 PostgreSQL 事务之外编排 RustFS 调用，在事务内只提交权威状态。
type Service struct {
	repository  Repository
	blobs       BlobStore
	newID       snowflake.Source
	now         func() time.Time
	decode      func(io.Reader) (image.Image, string, error)
	decodeSlots chan struct{}
}

// NewService 使用显式依赖创建 Asset 生命周期服务。
func NewService(repository Repository, blobs BlobStore, newID snowflake.Source, now func() time.Time) *Service {
	return &Service{
		repository: repository, blobs: blobs, newID: newID, now: now,
		decode: image.Decode, decodeSlots: make(chan struct{}, maximumConcurrentImageDecodes),
	}
}

// BeginUpload 持久化 Pending 记录后签发只能创建一次对象的短期 PUT URL。
func (s *Service) BeginUpload(ctx context.Context, command BeginUploadCommand) (UploadGrant, error) {
	command.CommandContext = command.normalize()
	command.MediaType = strings.ToLower(strings.TrimSpace(command.MediaType))
	digest, err := decodeSHA256(command.ExpectedSHA256)
	if err != nil || !command.valid() || !allowedMediaType(command.MediaType) ||
		command.ExpectedSize <= 0 || command.ExpectedSize > maximumAssetBytes {
		return UploadGrant{}, ErrInvalidAsset
	}
	assetID, err := s.newID.Next(ctx)
	if err != nil {
		return UploadGrant{}, fmt.Errorf("生成 Asset 标识: %w", err)
	}
	objectID, err := s.newID.Next(ctx)
	if err != nil {
		return UploadGrant{}, fmt.Errorf("生成 Asset 对象标识: %w", err)
	}
	record := ReserveRecord{
		CommandContext: command.CommandContext, ID: assetID,
		ObjectKey: "assets/" + assetID.String() + "/" + objectID.String(), MediaType: command.MediaType,
		ExpectedSize: command.ExpectedSize, ExpectedSHA256: digest, CreatedAt: s.now().UTC(),
	}
	var reserved Asset
	if err := s.repository.WithinAsset(ctx, func(writer Writer) error {
		var reserveErr error
		reserved, reserveErr = writer.Reserve(ctx, record)
		return reserveErr
	}); err != nil {
		return UploadGrant{}, err
	}
	url, headers, err := s.blobs.PresignUpload(
		ctx, reserved.ObjectKey, reserved.MediaType, reserved.ExpectedSize, reserved.ExpectedSHA256, uploadGrantTTL,
	)
	if err != nil {
		return UploadGrant{}, err
	}
	return UploadGrant{Asset: reserved, URL: url, Headers: headers, ExpiresAt: s.now().UTC().Add(uploadGrantTTL)}, nil
}

// Confirm 在事务外完整读取和校验 RustFS 对象，然后以乐观版本提交 Ready 状态。
func (s *Service) Confirm(ctx context.Context, command ConfirmCommand) (Asset, error) {
	command.CommandContext = command.normalize()
	if !command.valid() || command.AssetID == snowflake.ID(0) || command.ExpectedVersion <= 0 {
		return Asset{}, ErrInvalidAsset
	}
	pending, err := s.repository.GetOwned(ctx, command.ActorAccountID, command.AssetID)
	if err != nil {
		return Asset{}, err
	}
	// 成功响应可能在到达客户端前丢失。相同确认命令重试时，复用 Ready 事实进入
	// 事务内幂等记录，不重复下载 RustFS 对象，也不把合法重试误报为版本冲突。
	if pending.Status == StatusReady && pending.Version == command.ExpectedVersion+1 &&
		pending.ActualSize != nil && len(pending.ActualSHA256) == sha256.Size &&
		pending.Width != nil && pending.Height != nil && pending.ReadyAt != nil {
		return s.commitReady(ctx, ReadyRecord{
			CommandContext: command.CommandContext, AssetID: command.AssetID,
			ExpectedVersion: command.ExpectedVersion, ActualSize: *pending.ActualSize,
			ActualSHA256: append([]byte(nil), pending.ActualSHA256...), Width: *pending.Width,
			Height: *pending.Height, ReadyAt: *pending.ReadyAt,
		})
	}
	if pending.Status != StatusPending || pending.Version != command.ExpectedVersion {
		return Asset{}, ErrAssetConflict
	}
	verified, err := s.verifyObject(ctx, pending)
	if err != nil {
		return Asset{}, err
	}
	record := ReadyRecord{
		CommandContext: command.CommandContext, AssetID: command.AssetID,
		ExpectedVersion: command.ExpectedVersion, ActualSize: verified.size, ActualSHA256: verified.digest,
		Width: verified.width, Height: verified.height, ReadyAt: s.now().UTC(),
	}
	return s.commitReady(ctx, record)
}

func (s *Service) commitReady(ctx context.Context, record ReadyRecord) (Asset, error) {
	var ready Asset
	if err := s.repository.WithinAsset(ctx, func(writer Writer) error {
		var readyErr error
		ready, readyErr = writer.MarkReady(ctx, record)
		return readyErr
	}); err != nil {
		return Asset{}, err
	}
	return ready, nil
}

// Download 对 PostgreSQL Ready 状态和对象归属执行检查后返回稳定公开读取地址。
func (s *Service) Download(ctx context.Context, actorID, assetID snowflake.ID) (DownloadGrant, error) {
	if actorID == snowflake.ID(0) || assetID == snowflake.ID(0) {
		return DownloadGrant{}, ErrInvalidAsset
	}
	value, err := s.repository.GetOwned(ctx, actorID, assetID)
	if err != nil {
		return DownloadGrant{}, err
	}
	if value.Status != StatusReady {
		return DownloadGrant{}, ErrAssetNotFound
	}
	url, err := s.blobs.PublicURL(value.ObjectKey)
	if err != nil {
		return DownloadGrant{}, err
	}
	return DownloadGrant{URL: url}, nil
}

type verifiedImage struct {
	size   int64
	digest []byte
	width  int32
	height int32
}

func (s *Service) verifyObject(ctx context.Context, expected Asset) (verifiedImage, error) {
	object, err := s.blobs.Get(ctx, expected.ObjectKey)
	if err != nil {
		return verifiedImage{}, err
	}
	defer func() { _ = object.Body.Close() }()
	if object.Size != expected.ExpectedSize || strings.ToLower(strings.TrimSpace(object.MediaType)) != expected.MediaType {
		return verifiedImage{}, ErrAssetContentInvalid
	}
	raw, err := io.ReadAll(io.LimitReader(object.Body, maximumAssetBytes+1))
	if err != nil || int64(len(raw)) != expected.ExpectedSize || int64(len(raw)) > maximumAssetBytes {
		return verifiedImage{}, ErrAssetContentInvalid
	}
	detected := strings.ToLower(strings.TrimSpace(strings.Split(http.DetectContentType(raw), ";")[0]))
	if detected != expected.MediaType {
		return verifiedImage{}, ErrAssetContentInvalid
	}
	digest := sha256.Sum256(raw)
	if !bytes.Equal(digest[:], expected.ExpectedSHA256) {
		return verifiedImage{}, ErrAssetContentInvalid
	}
	decodeCtx, cancelDecode := context.WithTimeout(ctx, imageDecodeTimeout)
	defer cancelDecode()
	configuration, format, err := image.DecodeConfig(contextReader{
		ctx: decodeCtx, reader: bytes.NewReader(raw),
	})
	if decodeCtx.Err() != nil {
		return verifiedImage{}, decodeCtx.Err()
	}
	if err != nil || mediaTypeForFormat(format) != expected.MediaType || configuration.Width <= 0 || configuration.Height <= 0 ||
		configuration.Width > maximumImageSide || configuration.Height > maximumImageSide ||
		int64(configuration.Width)*int64(configuration.Height) > maximumImagePixels || !allowedColorModel(configuration.ColorModel) ||
		decodedImageBytes(configuration.Width, configuration.Height, configuration.ColorModel) > maximumDecodedImageBytes {
		return verifiedImage{}, ErrAssetContentInvalid
	}
	decoded, fullFormat, err := s.decodeFullImage(decodeCtx, raw)
	if err != nil || fullFormat != format || decoded.Bounds().Dx() != configuration.Width || decoded.Bounds().Dy() != configuration.Height {
		if decodeCtx.Err() != nil {
			return verifiedImage{}, decodeCtx.Err()
		}
		return verifiedImage{}, ErrAssetContentInvalid
	}
	return verifiedImage{size: int64(len(raw)), digest: append([]byte(nil), digest[:]...), width: int32(configuration.Width), height: int32(configuration.Height)}, nil
}

type imageDecodeResult struct {
	image  image.Image
	format string
	err    error
}

func (s *Service) decodeFullImage(ctx context.Context, raw []byte) (image.Image, string, error) {
	select {
	case s.decodeSlots <- struct{}{}:
	case <-ctx.Done():
		return nil, "", ctx.Err()
	}
	result := make(chan imageDecodeResult, 1)
	go func() {
		defer func() { <-s.decodeSlots }()
		decoded, format, err := s.decode(contextReader{ctx: ctx, reader: bytes.NewReader(raw)})
		result <- imageDecodeResult{image: decoded, format: format, err: err}
	}()
	select {
	case decoded := <-result:
		return decoded.image, decoded.format, decoded.err
	case <-ctx.Done():
		// 标准图片解码器没有统一的 Cancel API。调用方按时返回，后台解码在退出前继续
		// 占用槽位，因此不合作的任务最多只有 maximumConcurrentImageDecodes 个。
		return nil, "", ctx.Err()
	}
}

// contextReader 让标准图片解码器的渐进读取响应请求取消和独立解码超时。
type contextReader struct {
	ctx    context.Context
	reader io.Reader
}

func (r contextReader) Read(buffer []byte) (int, error) {
	if err := r.ctx.Err(); err != nil {
		return 0, err
	}
	count, err := r.reader.Read(buffer)
	if contextErr := r.ctx.Err(); contextErr != nil {
		return count, contextErr
	}
	return count, err
}

func decodeSHA256(value string) ([]byte, error) {
	value = strings.TrimSpace(value)
	if len(value) != sha256.Size*2 {
		return nil, ErrInvalidAsset
	}
	decoded, err := hex.DecodeString(value)
	if err != nil || len(decoded) != sha256.Size {
		return nil, ErrInvalidAsset
	}
	return decoded, nil
}

func allowedMediaType(value string) bool {
	return value == "image/jpeg" || value == "image/png" || value == "image/webp"
}

func mediaTypeForFormat(format string) string {
	switch format {
	case "jpeg":
		return "image/jpeg"
	case "png":
		return "image/png"
	case "webp":
		return "image/webp"
	default:
		return ""
	}
}

func allowedColorModel(model color.Model) bool {
	if _, palette := model.(color.Palette); palette {
		return true
	}
	return model == color.GrayModel || model == color.Gray16Model || model == color.RGBAModel ||
		model == color.RGBA64Model || model == color.NRGBAModel || model == color.NRGBA64Model ||
		model == color.YCbCrModel || model == color.CMYKModel
}

func decodedImageBytes(width, height int, model color.Model) int64 {
	bytesPerPixel := int64(4)
	switch model {
	case color.GrayModel:
		bytesPerPixel = 1
	case color.Gray16Model:
		bytesPerPixel = 2
	case color.RGBA64Model, color.NRGBA64Model:
		bytesPerPixel = 8
	}
	if _, palette := model.(color.Palette); palette {
		bytesPerPixel = 1
	}
	return int64(width) * int64(height) * bytesPerPixel
}
