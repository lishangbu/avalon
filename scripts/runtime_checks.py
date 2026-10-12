"""运行时负载检查共用的 HTTP 请求；不启动进程或操作数据库容器。"""

import json
import urllib.request


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
