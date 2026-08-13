// Package ent 保存 Avalon 的 Ent 代码生成入口；生成代码不提交 Git。
package ent

//go:generate go tool ent generate --feature sql/upsert --feature sql/lock ./schema
