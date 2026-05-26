## Xiaomi MiMo TTS Spike

Source checked: https://platform.xiaomimimo.com/docs/zh-CN/usage-guide/speech-synthesis-v2.5

Confirmed first-phase contract:

- Endpoint: `POST https://api.xiaomimimo.com/v1/chat/completions`
- Authentication: `api-key` request header
- Model: `mimo-v2.5-tts`
- Text mapping: synthesis text is sent as an `assistant` role message
- Style mapping: optional emotion/style guidance is sent as a `user` role message
- Audio options: `voice` and `format` are sent in an `audio` object
- Response handling: adapter expects base64 audio under the OpenAI-compatible chat completion response shape and includes tolerant fallback paths for audio payload variants

Implementation note:

- The first version stores the generated binary through `FileApi.createFile(...)`.
- Current infra `FileApi` returns a file path/URL string rather than a numeric file id, so the media task output writes both `url` and `fileId` with that returned value. If infra later exposes a durable file id through API, only the output mapping needs to change.
