# Use public Trainer Codes for direct challenges

Status: superseded by ADR-0053

本决定原本通过额外的 Trainer Code 定位 Challenge 目标。ADR-0053 后续将全局唯一且不可修改的 displayName 确立为唯一外部 Trainer 标识，因此不再生成或公开 Trainer Code；内部关系仍只引用数据库 Identifier。
