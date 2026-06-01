# yudao-ai-worker

Minimal Python Worker skeleton for Phase 2 media task integration.

It intentionally uses only Python standard library so local Java integration can be tested without GPU/model dependencies.

## Run

```bash
python3 worker.py --host 127.0.0.1 --port 8099 --token worker-secret
```

Docker:

```bash
docker compose up -d
```

Java config example:

```yaml
yudao:
  ai:
    media:
      worker:
        enable: true
        base-url: http://127.0.0.1:8099
        auth-token: worker-secret
        callback-url: http://127.0.0.1:48080/admin-api/ai/media/task/callback/python-worker
```

## Contract

- `GET /health`
- `GET /capabilities`
- `POST /tasks`
- `GET /tasks/{workerTaskId}`
- `POST /tasks/{workerTaskId}/cancel`
- `GET /files/{workerTaskId}.mp4`

The skeleton simulates progress and returns a tiny playable mock MP4 byte stream. If the prompt contains `fail`, it reports a failed task.

This worker is still a contract/mock implementation. For real local video generation, replace the task execution section with a ComfyUI, Diffusers, or other model backend and keep the HTTP contract unchanged.
