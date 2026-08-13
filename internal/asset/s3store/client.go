// Package s3store 使用 AWS SDK for Go v2 访问 RustFS 的认证写、公开读 S3 兼容接口。
package s3store

import (
	"context"
	"encoding/base64"
	"fmt"
	"io"
	"net/http"
	"net/url"
	"path"
	"strings"
	"time"

	"github.com/aws/aws-sdk-go-v2/aws"
	awsconfig "github.com/aws/aws-sdk-go-v2/config"
	"github.com/aws/aws-sdk-go-v2/credentials"
	"github.com/aws/aws-sdk-go-v2/service/s3"
	"github.com/lishangbu/avalon/internal/asset"
)

// Config 保存连接单个 RustFS 业务 Bucket 所需的非全局配置。
type Config struct {
	// Endpoint 是后端与公开读取客户端共同访问、不含凭据、查询参数和对象路径的 S3 API 地址。
	Endpoint string
	// Region 是服务端认证请求执行 SigV4 签名时使用的区域标识。
	Region string
	// Bucket 是允许匿名 GetObject、但所有写操作必须认证的唯一业务 Bucket。
	Bucket string
	// AccessKeyID 是后端上传、校验和删除对象使用的服务账号标识。
	AccessKeyID string
	// SecretAccessKey 是与 AccessKeyID 配对且只保存在受保护配置中的服务账号密钥。
	SecretAccessKey string
	// UsePathStyle 控制对象地址采用 endpoint/bucket/key 或 bucket.endpoint/key 形式。
	UsePathStyle bool
}

// Client 对写请求签发短期凭据、构造公开读取地址，并以服务端凭据校验待确认对象。
type Client struct {
	// bucket 是只允许匿名读取对象内容的唯一业务 Bucket 名称。
	bucket string
	// endpoint 是服务账号请求、readiness 探测与公开对象读取共同使用的 S3 API 根地址。
	endpoint *url.URL
	// usePathStyle 决定 Bucket 位于 URL 路径还是主机名前缀。
	usePathStyle bool
	// httpClient 执行 readiness 的匿名权限探测。
	httpClient *http.Client
	// client 使用服务账号执行 HeadBucket 和对象内容校验。
	client *s3.Client
	// presign 只用于认证写请求，公开读取地址绝不经过该客户端签名。
	presign *s3.PresignClient
}

// New 使用静态 RustFS 服务账号和自定义端点创建 S3 客户端，不执行网络调用。
func New(ctx context.Context, configuration Config) (*Client, error) {
	configuration.Endpoint = strings.TrimRight(strings.TrimSpace(configuration.Endpoint), "/")
	configuration.Region = strings.TrimSpace(configuration.Region)
	configuration.Bucket = strings.TrimSpace(configuration.Bucket)
	configuration.AccessKeyID = strings.TrimSpace(configuration.AccessKeyID)
	configuration.SecretAccessKey = strings.TrimSpace(configuration.SecretAccessKey)
	if configuration.Endpoint == "" || configuration.Region == "" || configuration.Bucket == "" ||
		configuration.AccessKeyID == "" || configuration.SecretAccessKey == "" {
		return nil, fmt.Errorf("RustFS 配置不完整")
	}
	endpoint, err := url.Parse(configuration.Endpoint)
	if err != nil || (endpoint.Scheme != "http" && endpoint.Scheme != "https") || endpoint.Host == "" ||
		endpoint.User != nil || endpoint.RawQuery != "" || endpoint.Fragment != "" {
		return nil, fmt.Errorf("RustFS Endpoint 必须是无凭据、查询和片段的 HTTP(S) URL")
	}
	loaded, err := awsconfig.LoadDefaultConfig(
		ctx,
		awsconfig.WithRegion(configuration.Region),
		awsconfig.WithCredentialsProvider(credentials.NewStaticCredentialsProvider(
			configuration.AccessKeyID, configuration.SecretAccessKey, "",
		)),
		awsconfig.WithBaseEndpoint(configuration.Endpoint),
	)
	if err != nil {
		return nil, fmt.Errorf("加载 RustFS S3 配置: %w", err)
	}
	client := s3.NewFromConfig(loaded, func(options *s3.Options) {
		options.UsePathStyle = configuration.UsePathStyle
	})
	return &Client{
		bucket: configuration.Bucket, endpoint: endpoint, usePathStyle: configuration.UsePathStyle,
		httpClient: &http.Client{Timeout: 5 * time.Second}, client: client, presign: s3.NewPresignClient(client),
	}, nil
}

// Ready 确认 Bucket 存在、服务账号可访问、匿名调用者可读取对象但不能列举 Bucket。
func (c *Client) Ready(ctx context.Context) error {
	if _, err := c.client.HeadBucket(ctx, &s3.HeadBucketInput{Bucket: aws.String(c.bucket)}); err != nil {
		return fmt.Errorf("RustFS Bucket 未就绪: %w", err)
	}
	if err := c.verifyAccessPolicy(ctx); err != nil {
		return err
	}
	return nil
}

// verifyAccessPolicy 使用匿名请求验证公开读取与私有列举边界。
//
// 写权限由只授予 GetObject 的 Bucket Policy 和服务账号 IAM 共同限定；真实 RustFS 集成测试另外验证
// 匿名 PUT 被拒绝，避免 readiness 通过写探测在启动期间制造对象。
func (c *Client) verifyAccessPolicy(ctx context.Context) error {
	bucketURL, err := c.bucketURL()
	if err != nil {
		return fmt.Errorf("构造 RustFS 权限探测请求: %w", err)
	}
	listURL := *bucketURL
	query := listURL.Query()
	query.Set("list-type", "2")
	query.Set("max-keys", "0")
	listURL.RawQuery = query.Encode()
	if err := c.expectAnonymousDenial(ctx, http.MethodGet, listURL.String()); err != nil {
		return fmt.Errorf("RustFS Bucket 允许匿名列举: %w", err)
	}
	objectURL, err := url.JoinPath(bucketURL.String(), "assets", ".avalon-private-readiness-probe")
	if err != nil {
		return fmt.Errorf("构造 RustFS 公开读取探测请求: %w", err)
	}
	if err := c.expectAnonymousNotFound(ctx, http.MethodHead, objectURL); err != nil {
		return fmt.Errorf("RustFS Bucket 未开放匿名对象读取: %w", err)
	}
	return nil
}

func (c *Client) bucketURL() (*url.URL, error) {
	return c.bucketURLFor(c.endpoint)
}

// bucketURLFor 按统一寻址风格把指定 Endpoint 与 Bucket 组合为根地址。
func (c *Client) bucketURLFor(endpoint *url.URL) (*url.URL, error) {
	if c.usePathStyle {
		joined, err := url.JoinPath(endpoint.String(), c.bucket)
		if err != nil {
			return nil, err
		}
		return url.Parse(joined)
	}
	result := *endpoint
	result.Host = c.bucket + "." + result.Host
	return &result, nil
}

func (c *Client) expectAnonymousDenial(ctx context.Context, method, target string) error {
	return c.expectAnonymousStatus(ctx, method, target, http.StatusUnauthorized, http.StatusForbidden)
}

// expectAnonymousNotFound 要求匿名调用已经通过读取授权，只因探测对象不存在而返回 404。
func (c *Client) expectAnonymousNotFound(ctx context.Context, method, target string) error {
	return c.expectAnonymousStatus(ctx, method, target, http.StatusNotFound)
}

// expectAnonymousStatus 执行不带任何认证 Header 的探测请求并限制允许的响应状态。
func (c *Client) expectAnonymousStatus(ctx context.Context, method, target string, allowed ...int) error {
	request, err := http.NewRequestWithContext(ctx, method, target, nil)
	if err != nil {
		return err
	}
	response, err := c.httpClient.Do(request)
	if err != nil {
		return err
	}
	defer func() { _ = response.Body.Close() }()
	_, _ = io.Copy(io.Discard, response.Body)
	for _, status := range allowed {
		if response.StatusCode == status {
			return nil
		}
	}
	return fmt.Errorf("匿名请求返回状态 %d", response.StatusCode)
}

// PresignUpload 签发带内容类型、长度、SHA-256 和只创建一次条件的 PUT 请求。
func (c *Client) PresignUpload(
	ctx context.Context,
	objectKey string,
	mediaType string,
	size int64,
	digest []byte,
	ttl time.Duration,
) (string, map[string]string, error) {
	checksum := base64.StdEncoding.EncodeToString(digest)
	result, err := c.presign.PresignPutObject(ctx, &s3.PutObjectInput{
		Bucket: aws.String(c.bucket), Key: aws.String(objectKey), ContentType: aws.String(mediaType),
		ContentLength: aws.Int64(size), ChecksumSHA256: aws.String(checksum), IfNoneMatch: aws.String("*"),
	}, func(options *s3.PresignOptions) { options.Expires = ttl })
	if err != nil {
		return "", nil, fmt.Errorf("签发 RustFS 上传请求: %w", err)
	}
	headers := make(map[string]string, len(result.SignedHeader))
	for name, values := range result.SignedHeader {
		if strings.EqualFold(name, "Host") {
			continue
		}
		headers[name] = strings.Join(values, ",")
	}
	return result.URL, headers, nil
}

// Get 使用服务端凭据读取对象，供 Asset 服务在事务外完整验证未确认字节。
func (c *Client) Get(ctx context.Context, objectKey string) (asset.BlobObject, error) {
	result, err := c.client.GetObject(ctx, &s3.GetObjectInput{Bucket: aws.String(c.bucket), Key: aws.String(objectKey)})
	if err != nil {
		return asset.BlobObject{}, fmt.Errorf("读取 RustFS 对象: %w", err)
	}
	return asset.BlobObject{
		Body: result.Body, Size: aws.ToInt64(result.ContentLength), MediaType: aws.ToString(result.ContentType),
	}, nil
}

// PublicURL 返回无需认证且不会过期的对象读取地址。
//
// Bucket 策略负责只开放 GetObject；本方法不会生成签名、查询参数或临时凭据，也不会执行网络请求。
func (c *Client) PublicURL(objectKey string) (string, error) {
	if !validPublicObjectKey(objectKey) {
		return "", fmt.Errorf("RustFS 公开对象键无效")
	}
	bucketURL, err := c.bucketURL()
	if err != nil {
		return "", fmt.Errorf("构造 RustFS 公开 Bucket 地址: %w", err)
	}
	result, err := url.JoinPath(bucketURL.String(), objectKey)
	if err != nil {
		return "", fmt.Errorf("构造 RustFS 公开对象地址: %w", err)
	}
	return result, nil
}

// validPublicObjectKey 限制公开地址只能定位服务端生成的 Asset 命名空间，并拒绝 URL 控制字符。
func validPublicObjectKey(objectKey string) bool {
	allowedNamespace := (strings.HasPrefix(objectKey, "assets/") && len(objectKey) > len("assets/")) ||
		(strings.HasPrefix(objectKey, "pokedex/images/official/") && len(objectKey) > len("pokedex/images/official/")) ||
		(strings.HasPrefix(objectKey, "pokedex/images/items/") && len(objectKey) > len("pokedex/images/items/"))
	return allowedNamespace && path.Clean(objectKey) == objectKey && !strings.ContainsAny(objectKey, "\\?#\r\n")
}
