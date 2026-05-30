# yudao-ai-worker

Minimal Python Worker skeleton for Phase 2 media task integration.

It intentionally uses only Python standard library so local Java integration can be tested without GPU/model dependencies.

## Run

```bash
python3 worker.py --host 127.0.0.1 --port 8099 --token worker-secret
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
- `POST /tasks`
- `GET /tasks/{workerTaskId}`
- `POST /tasks/{workerTaskId}/cancel`
- `GET /files/{workerTaskId}.mp4`

The skeleton simulates progress and returns a tiny placeholder MP4 byte stream. If the prompt contains `fail`, it reports a failed task.
