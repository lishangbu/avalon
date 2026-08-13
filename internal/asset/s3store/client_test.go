package s3store_test

import (
	"context"
	"net/url"
	"testing"

	"github.com/lishangbu/avalon/internal/asset/s3store"
)

// TestPublicURLDoesNotContainAuthentication 验证公开读取地址只由 Endpoint、Bucket 和对象键组成，
// 不携带会过期或泄露服务账号签名事实的查询参数。
func TestPublicURLDoesNotContainAuthentication(t *testing.T) {
	t.Parallel()

	client, err := s3store.New(context.Background(), s3store.Config{
		Endpoint: "https://objects.example", Region: "us-east-1", Bucket: "avalon-assets",
		AccessKeyID: "writer", SecretAccessKey: "secret", UsePathStyle: true,
	})
	if err != nil {
		t.Fatalf("New() error = %v", err)
	}
	location, err := client.PublicURL("assets/2026/08/asset.png")
	if err != nil {
		t.Fatalf("PublicURL() error = %v", err)
	}
	parsed, err := url.Parse(location)
	if err != nil {
		t.Fatalf("解析公开地址失败：%v", err)
	}
	if location != "https://objects.example/avalon-assets/assets/2026/08/asset.png" {
		t.Fatalf("PublicURL() = %q", location)
	}
	if parsed.RawQuery != "" || parsed.Fragment != "" || parsed.User != nil {
		t.Fatalf("公开读取地址包含认证或非对象定位信息：%q", location)
	}
}

// TestPublicURLAllowsAuthorizedSourceHierarchy 验证首套资料图片可以保留授权来源的
// pokedex/images/official 与 pokedex/images/items 原始目录，而不放宽到任意根路径。
func TestPublicURLAllowsAuthorizedSourceHierarchy(t *testing.T) {
	t.Parallel()

	client, err := s3store.New(context.Background(), s3store.Config{
		Endpoint: "https://objects.example", Region: "us-east-1", Bucket: "avalon-assets",
		AccessKeyID: "writer", SecretAccessKey: "secret", UsePathStyle: true,
	})
	if err != nil {
		t.Fatalf("New() error = %v", err)
	}
	location, err := client.PublicURL("pokedex/images/items/一边的耳环.webp")
	if err != nil {
		t.Fatalf("PublicURL() error = %v", err)
	}
	if location != "https://objects.example/avalon-assets/pokedex/images/items/%E4%B8%80%E8%BE%B9%E7%9A%84%E8%80%B3%E7%8E%AF.webp" {
		t.Fatalf("PublicURL() = %q", location)
	}
}

// TestPublicURLRejectsKeysOutsideAssetNamespace 验证公开地址构造不能被路径跳转、绝对路径或查询字符
// 逃逸出服务端生成的 Asset 对象命名空间。
func TestPublicURLRejectsKeysOutsideAssetNamespace(t *testing.T) {
	t.Parallel()

	client, err := s3store.New(context.Background(), s3store.Config{
		Endpoint: "https://objects.example", Region: "us-east-1", Bucket: "avalon-assets",
		AccessKeyID: "writer", SecretAccessKey: "secret", UsePathStyle: true,
	})
	if err != nil {
		t.Fatalf("New() error = %v", err)
	}
	for _, objectKey := range []string{"../secret", "/assets/absolute", "assets/key?credential=value", "assets/key#fragment"} {
		if location, publicErr := client.PublicURL(objectKey); publicErr == nil {
			t.Fatalf("PublicURL(%q) = %q，期望拒绝非法对象键", objectKey, location)
		}
	}
}
