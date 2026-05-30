#!/usr/bin/env python3
"""Minimal HTTP Python Worker skeleton for yudao AI media Phase 2."""

from __future__ import annotations

import argparse
import json
import time
import uuid
from http.server import BaseHTTPRequestHandler, ThreadingHTTPServer
from urllib import request


TASKS: dict[str, dict] = {}
AUTH_TOKEN = "worker-secret"
PUBLIC_BASE_URL = ""


def json_response(handler: BaseHTTPRequestHandler, status: int, payload: dict) -> None:
    data = json.dumps(payload, ensure_ascii=False).encode("utf-8")
    handler.send_response(status)
    handler.send_header("Content-Type", "application/json; charset=utf-8")
    handler.send_header("Content-Length", str(len(data)))
    handler.end_headers()
    handler.wfile.write(data)


def read_json(handler: BaseHTTPRequestHandler) -> dict:
    length = int(handler.headers.get("Content-Length", "0"))
    if length <= 0:
        return {}
    return json.loads(handler.rfile.read(length).decode("utf-8"))


def authorized(handler: BaseHTTPRequestHandler) -> bool:
    return handler.headers.get("Authorization") == f"Bearer {AUTH_TOKEN}"


def task_snapshot(worker_task_id: str, handler: BaseHTTPRequestHandler) -> dict:
    task = TASKS[worker_task_id]
    if task["cancelled"]:
        return {"workerTaskId": worker_task_id, "taskId": task["taskId"], "status": "cancelled", "progress": 100}
    elapsed = time.time() - task["createdAt"]
    prompt = str(task["input"].get("prompt", ""))
    if "fail" in prompt.lower():
        return {
            "workerTaskId": worker_task_id,
            "taskId": task["taskId"],
            "status": "failed",
            "progress": 100,
            "errorCode": "MOCK_FAIL",
            "errorMessage": "mock worker forced failure",
        }
    if elapsed < 1:
        return {"workerTaskId": worker_task_id, "taskId": task["taskId"], "status": "queued", "progress": 10}
    if elapsed < 3:
        return {
            "workerTaskId": worker_task_id,
            "taskId": task["taskId"],
            "status": "running",
            "progress": min(95, int(20 + elapsed * 25)),
            "stage": "mock_rendering",
        }
    base_url = PUBLIC_BASE_URL or f"http://{handler.headers.get('Host')}"
    return {
        "workerTaskId": worker_task_id,
        "taskId": task["taskId"],
        "status": "succeeded",
        "progress": 100,
        "outputs": [{"url": f"{base_url}/files/{worker_task_id}.mp4", "mimeType": "video/mp4"}],
    }


def send_callback(task: dict, payload: dict) -> None:
    callback_url = task.get("callbackUrl")
    if not callback_url:
        return
    body = json.dumps(payload).encode("utf-8")
    req = request.Request(callback_url, data=body, method="POST")
    req.add_header("Content-Type", "application/json")
    req.add_header("X-Worker-Token", AUTH_TOKEN)
    try:
        request.urlopen(req, timeout=3).read()
    except Exception as exc:
        print(f"callback failed: {exc}")


class WorkerHandler(BaseHTTPRequestHandler):

    def do_GET(self) -> None:
        if self.path == "/health":
            json_response(self, 200, {"ok": True, "status": "healthy"})
            return
        if self.path.startswith("/tasks/"):
            if not authorized(self):
                json_response(self, 401, {"error": "unauthorized"})
                return
            worker_task_id = self.path.removeprefix("/tasks/")
            if worker_task_id not in TASKS:
                json_response(self, 404, {"error": "task not found"})
                return
            json_response(self, 200, task_snapshot(worker_task_id, self))
            return
        if self.path.startswith("/files/"):
            data = b"\x00\x00\x00\x18ftypmp42\x00\x00\x00\x00mp42isom"
            self.send_response(200)
            self.send_header("Content-Type", "video/mp4")
            self.send_header("Content-Length", str(len(data)))
            self.end_headers()
            self.wfile.write(data)
            return
        json_response(self, 404, {"error": "not found"})

    def do_POST(self) -> None:
        if not authorized(self):
            json_response(self, 401, {"error": "unauthorized"})
            return
        if self.path == "/tasks":
            payload = read_json(self)
            worker_task_id = f"worker-{uuid.uuid4().hex[:12]}"
            TASKS[worker_task_id] = {
                "taskId": payload.get("taskId"),
                "input": payload.get("input") or {},
                "callbackUrl": payload.get("callbackUrl"),
                "createdAt": time.time(),
                "cancelled": False,
            }
            response = {"accepted": True, "workerTaskId": worker_task_id, "status": "submitted"}
            json_response(self, 200, response)
            send_callback(TASKS[worker_task_id], {
                "taskId": payload.get("taskId"),
                "workerTaskId": worker_task_id,
                "status": "submitted",
                "progress": 5,
            })
            return
        if self.path.startswith("/tasks/") and self.path.endswith("/cancel"):
            worker_task_id = self.path.removeprefix("/tasks/").removesuffix("/cancel")
            if worker_task_id not in TASKS:
                json_response(self, 404, {"error": "task not found"})
                return
            TASKS[worker_task_id]["cancelled"] = True
            json_response(self, 200, {"cancelled": True, "workerTaskId": worker_task_id})
            return
        json_response(self, 404, {"error": "not found"})

    def log_message(self, fmt: str, *args) -> None:
        print(f"{self.address_string()} - {fmt % args}")


def main() -> None:
    parser = argparse.ArgumentParser()
    parser.add_argument("--host", default="127.0.0.1")
    parser.add_argument("--port", type=int, default=8099)
    parser.add_argument("--token", default="worker-secret")
    parser.add_argument("--public-base-url", default="")
    args = parser.parse_args()
    global AUTH_TOKEN, PUBLIC_BASE_URL
    AUTH_TOKEN = args.token
    PUBLIC_BASE_URL = args.public_base_url.rstrip("/")
    server = ThreadingHTTPServer((args.host, args.port), WorkerHandler)
    print(f"Python Worker listening on http://{args.host}:{args.port}")
    server.serve_forever()


if __name__ == "__main__":
    main()
