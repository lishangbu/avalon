"""部署验证共用的 HTTP 请求与业务冒烟检查；不启动进程或操作数据库容器。"""

import json
import urllib.request
import uuid

READINESS_PATH = "/actuator/health/readiness"


def http(port, path, body=None, token=None):
    headers = {"Content-Type": "application/json"}
    if token:
        headers["Authorization"] = "Bearer " + token
    data = None if body is None else json.dumps(body).encode()
    request = urllib.request.Request(f"http://127.0.0.1:{port}{path}", data, headers)
    # 本地基准不经过用户配置的 HTTP 代理。
    with urllib.request.build_opener(urllib.request.ProxyHandler({})).open(request, timeout=2) as response:
        raw = response.read()
        return json.loads(raw) if raw else None

def smoke(port):
    """验证认证、创建角色、地图读取与雪花 ID 的 JSON 契约。"""
    name = "smoke_" + uuid.uuid4().hex[:10]
    tokens = http(port, "/api/auth/register", {
        "username": name, "password": "Smoke-test-1234", "device": "部署验收"})
    token = tokens["accessToken"]
    character = http(port, "/api/world/character", {
        "name": name, "profession": "WARRIOR"}, token)
    if not isinstance(character["id"], str):
        raise RuntimeError("雪花 ID 必须序列化为字符串")
    world = http(port, "/api/world", token=token)
    if world["character"]["id"] != character["id"]:
        raise RuntimeError("地图角色与新建角色不一致")
