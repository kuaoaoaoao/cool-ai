## Context

`yudao-module-ai` already contains Spring AI based chat, model configuration, API key management, knowledge retrieval, tool/function calling, MCP client/server integration, image generation, music generation, writing, mind map, and workflow concepts. The requested audio/video capabilities are heavier than normal chat/image calls: voice cloning, local video generation, and person replacement typically require Python, PyTorch, CUDA, FFmpeg, model weights, and long-running task orchestration.

The design therefore treats the Java application as the AI product and orchestration layer, while Python Workers and third-party providers perform model-specific inference.

```text
┌──────────────┐
│ Frontend/App │
└──────┬───────┘
       ▼
┌─────────────────────────────────────┐
│ yudao-module-ai                      │
│ task, auth, tenant, model, file, API │
└──────┬───────────────┬──────────────┘
       │               │
       ▼               ▼
┌──────────────┐   ┌──────────────────┐
│ Third-party  │   │ Python AI Worker  │
│ AI Provider  │   │ GPU / local model │
└──────┬───────┘   └────────┬─────────┘
       ▼                    ▼
┌─────────────────────────────────────┐
│ MySQL + Redis/MQ + File Storage      │
└─────────────────────────────────────┘
```

## Goals / Non-Goals

**Goals:**

- Provide a unified asynchronous task model for audio/video work that may take seconds or minutes.
- Let Java manage product concerns: permissions, tenant isolation, task records, file references, provider selection, retries, callbacks, and result queries.
- Support both fast MVP delivery through third-party AI APIs and later custom/local inference through Python Workers.
- Keep provider/model-specific inference logic outside the Java application when it depends on Python/GPU ecosystems.
- Make selected media capabilities callable from AI chat through the existing tool/function calling and MCP patterns.
- Enable phased delivery so the system can start with simple provider integrations and evolve toward workflow-based creative automation.

**Non-Goals:**

- Do not implement audio/video model inference directly inside Java.
- Do not train custom models as part of the first platform iteration.
- Do not replace existing chat, image, music, knowledge, model, or workflow modules.
- Do not require every media provider to support the same parameters; provider-specific options may live in structured input JSON.
- Do not build a full timeline video editor in the initial platform scope.

## Decisions

### 1. Use Java as the media orchestration layer

Java remains the public product backend for all audio/video features. New media endpoints, task records, provider selection, admin management, and chat tools should live under `yudao-module-ai`.

Alternatives considered:

- Put audio/video in a separate standalone product service: cleaner isolation, but loses reuse of existing auth, tenant, file, model, and AI chat infrastructure.
- Build everything in Python: convenient for models, but weak for enterprise backend features already solved by this project.

Rationale: this project is already a mature Java business platform, so the highest-leverage architecture is Java for orchestration and Python for inference.

### 2. Model audio/video as asynchronous media tasks

Audio and video generation should be represented as tasks with stable lifecycle states rather than synchronous request/response operations.

```text
CREATED
  ↓
QUEUED
  ↓
RUNNING ───────▶ CANCELLED
  ↓
SUCCEEDED
  ↓
result files available

RUNNING ───────▶ FAILED ───────▶ RETRYING ───────▶ QUEUED
```

The task record should capture task type, user, tenant, provider, model, input payload, source file references, output file references, status, progress, error information, callback data, and timing fields.

Use one primary `ai_media_task` table for the initial platform. The table should include a capability/task type field such as `TTS`, `VOICE_CLONE`, `MUSIC_GENERATE`, `TEXT_TO_VIDEO`, `IMAGE_TO_VIDEO`, or `PERSON_REPLACE`, plus structured input/output JSON fields for capability-specific parameters. Capability-specific typed request/response objects should validate the JSON at service boundaries.

This is an explicit first-phase decision. Dedicated tables should only be added when a capability needs its own durable domain entity, such as a reusable cloned voice profile, media asset catalog, or detailed callback audit log.

Alternatives considered:

- Separate unrelated task tables per feature from day one: gives stronger typing, but increases early duplication.
- Only use existing image/music task tables: fast, but video/audio/person replacement have broader inputs and lifecycle needs.

Rationale: a unified media task center creates one mental model for frontend, admin, chat tools, retries, and future workflow composition.

### 3. Use polling as the default progress reporting mechanism

Frontend clients should submit a task and poll a task status endpoint for progress and result updates. The task status response should include task ID, status, progress percentage, current stage/message, output references, and error details when failed.

```text
POST /ai/media/task
  ↓
taskId
  ↓
GET /ai/media/task/{id}
  ↓
status + progress + result
```

Alternatives considered:

- WebSocket/SSE progress streaming: better real-time experience, but adds connection lifecycle complexity before the task model is stable.
- Provider-specific progress endpoints exposed directly to frontend: fast for one provider, but leaks provider details into product UI.

Rationale: polling is simple, robust, and consistent with long-running provider and Python Worker tasks. WebSocket/SSE can be added later as an enhancement over the same task state.

This means the first frontend integration should not depend on MQ, WebSocket, or SSE. MQ remains a backend execution enhancement for later scaling, not a browser-facing progress mechanism.

### 4. Introduce provider adapters behind a stable service boundary

Java should talk to third-party AI APIs and Python Workers through provider adapters. Product services create tasks and delegate execution to an adapter selected by capability, provider, and model.

```text
AiAudioService / AiVideoService
        ↓
AiMediaTaskService
        ↓
AiMediaProviderAdapter
        ├── ThirdPartyHttpProviderAdapter
        └── PythonWorkerProviderAdapter
```

Alternatives considered:

- Call external APIs directly from controllers/services: fastest initially, but spreads provider-specific behavior across the module.
- Force all providers through MCP: elegant for agent tools, but awkward for task execution, callbacks, and file-heavy media workflows.

Rationale: adapters keep provider-specific details replaceable while preserving a consistent product API.

### 5. Define a minimal Python Worker API contract

Python Worker integration should start with a small HTTP contract that can later be backed by MQ without changing product APIs.

Submission request:

```json
{
  "taskId": "string",
  "capability": "VOICE_CLONE | TEXT_TO_VIDEO | PERSON_REPLACE",
  "model": "string",
  "input": {},
  "sourceFiles": [
    {
      "name": "string",
      "url": "string",
      "mimeType": "string"
    }
  ],
  "callbackUrl": "string",
  "tenantId": "string",
  "userId": "string",
  "traceId": "string"
}
```

Submission response:

```json
{
  "accepted": true,
  "workerTaskId": "string",
  "estimatedSeconds": 120,
  "message": "string"
}
```

Status/callback payload:

```json
{
  "taskId": "string",
  "workerTaskId": "string",
  "status": "RUNNING | SUCCEEDED | FAILED",
  "progress": 70,
  "stage": "rendering",
  "outputs": [
    {
      "name": "string",
      "url": "string",
      "mimeType": "string"
    }
  ],
  "errorCode": "string",
  "errorMessage": "string"
}
```

Timeouts should be handled at the adapter layer. Submission calls should fail fast when the Worker is unavailable, while long-running inference should continue asynchronously and report progress through polling or callbacks.

Alternatives considered:

- Design a full generic workflow protocol first: powerful, but too heavy for the first implementation.
- Make each Python capability define unrelated request formats: flexible, but hard for Java to standardize retries, status, and callbacks.

Rationale: a minimal common envelope gives Java a stable orchestration contract while leaving model-specific parameters inside `input`.

### 6. Prefer third-party APIs for MVP, Python Worker for local/custom inference

The first deliverable path should prioritize third-party APIs for capabilities such as TTS, music generation expansion, and text/image-to-video MVPs. Python Worker integration should follow for capabilities where local/custom control matters most: voice cloning, person replacement, and local video models.

Xiaomi MiMo `mimo-v2.5-tts` is the first provider integration target. Its API key should be configured outside source code, using the existing model/API key configuration path, Nacos/environment configuration, or a later admin-managed provider credential flow. The initial implementation may leave the key value empty for manual configuration.

The first implementation phase SHALL only execute `TTS` tasks. Other task types such as `VOICE_CLONE`, `TEXT_TO_VIDEO`, `IMAGE_TO_VIDEO`, and `PERSON_REPLACE` remain planned capabilities and should return a clear capability-not-enabled response until their providers and safety controls are implemented.

The initial Xiaomi adapter contract should follow the official MiMo speech synthesis API shape:

- Endpoint: `POST https://api.xiaomimimo.com/v1/chat/completions`
- Authentication header: `api-key: <configured-api-key>`
- Model: `mimo-v2.5-tts`
- Target synthesis text: `messages` item with role `assistant`
- Optional speaking style or emotional guidance: `messages` item with role `user`
- Audio options: include output `format` and `voice`
- Response handling: decode the returned base64 audio payload and store the generated audio through the Java-side file service before completing the media task.

The first TTS task input should therefore capture text, optional style instruction, voice, audio format, and provider/model selection. Supported voice and format values should come from provider configuration or dictionary data rather than being hard-coded in the specs. Provider-specific raw options can remain in `input_json` until a richer UI is needed.

Alternatives considered:

- Start with local Python models first: maximizes control, but likely slows product validation due to CUDA/model/deployment complexity.
- Use only third-party APIs permanently: fastest operations, but limits customization, cost control, and local/private model workflows.

Rationale: product workflow should be validated before the hardest model deployment work begins.

### 7. Keep Python Worker service-oriented

Python inference should be exposed as a service, initially via HTTP/FastAPI and later optionally through MQ workers for higher concurrency and reliability.

```text
Phase A:
Java ──HTTP──▶ Python FastAPI ──model/ffmpeg──▶ result

Phase B:
Java ──MQ task──▶ Python Worker ──callback/status──▶ Java
```

Alternatives considered:

- Java executes Python scripts directly: simple for a demo, but fragile for timeouts, concurrency, environments, logging, and GPU lifecycle.
- Embed Python inside JVM: high complexity and poor fit for modern AI model stacks.

Rationale: service boundaries isolate Python dependencies and make GPU capacity independently deployable.

### 8. Reuse the existing infra file service as canonical storage

Uploaded source media and generated audio/video results should be stored through the existing infra file service wherever possible. Task records should store file IDs/URLs and metadata, not binary blobs.

Alternatives considered:

- Store binary media directly in MySQL: easier transactionally, but poor for large video files and streaming.
- Let Python own result storage only: simple for Worker, but weakens Java-side permission and audit control.
- Introduce MinIO/OSS as a new dependency immediately: useful in some deployments, but unnecessary before confirming the existing infra file service is insufficient.

Rationale: reusing the existing file service avoids new infrastructure early and keeps permission, retention, and audit behavior aligned with the rest of the project.

### 9. Define one callback entry point for providers and workers

Third-party provider callbacks and Python Worker callbacks should be normalized into one Java callback endpoint shape at the provider adapter layer. The callback handler should verify task identity, validate provider signature or shared secret where supported, persist callback logs, update task status/progress/results, and tolerate duplicate callbacks.

```text
Provider/Python Worker
  ↓
POST /ai/media/callback/{provider}
  ↓
verify + normalize + persist log
  ↓
update ai_media_task
```

Callbacks should be retried by the sender when possible. Java-side callback handling must be idempotent so repeated `RUNNING` or `SUCCEEDED` notifications do not corrupt task state.

Alternatives considered:

- Create one callback endpoint per capability: clearer locally, but duplicates security, logging, and idempotency handling.
- Require Java to poll every provider only: avoids public callbacks, but delays updates and increases provider traffic.

Rationale: a unified callback normalization layer keeps provider-specific behavior inside adapters and gives the task center one update path.

### 10. Expose media operations as chat tools after task APIs exist

Chat tools should call the same media task services used by normal UI/API flows. Tools should create tasks, query status, and return result links, not bypass the task center.

```text
Chat Message
  ↓
Tool Calling / MCP
  ↓
createVideoTask / createVoiceTask / getMediaTaskStatus
  ↓
AiMediaTaskService
```

Alternatives considered:

- Let the LLM call providers directly: unsafe and hard to audit.
- Build an agent-only interface first: impressive demo, but brittle without stable task APIs.

Rationale: task APIs are the foundation; agent tooling should be a thin conversational layer over them.

### 11. Require confirmation and quota checks for expensive or sensitive chat tools

Chat-triggered media task creation should enforce the same permissions and tenant quotas as UI/API calls. Sensitive operations such as voice cloning and person replacement should require an explicit confirmation step before task creation. Expensive operations such as video generation should check tenant/user quotas before provider submission.

The first control set should include feature switches, permission checks, simple user/tenant usage limits, task audit logs, ownership checks for uploaded source media, and confirmation before chat tools create voice cloning or person replacement tasks. The first phase does not introduce a dedicated `WAITING_CONFIRMATION` task state; chat confirmation should happen before creating executable media tasks.

Alternatives considered:

- Let chat tools create tasks immediately for every request: smoother UX, but unsafe and potentially expensive.
- Disable sensitive media tools in chat entirely: safest early option, but blocks the intended Agent experience.

Rationale: confirmation and quota checks let the chat interface remain useful without letting the LLM silently trigger costly or sensitive operations.

### 12. Treat voice cloning and person replacement as higher-risk capabilities

Voice cloning and person replacement should include consent/audit hooks in the platform design, even if the first implementation uses simple policy checks.

Alternatives considered:

- Defer safety controls until later: faster, but risky because these features can impersonate real people.

Rationale: authorization, auditability, and result traceability are part of making these capabilities product-ready rather than just demo-ready.

## Risks / Trade-offs

- [Risk] Python model deployment becomes the bottleneck. → Mitigation: start with third-party API MVPs and integrate Python Worker after task APIs are stable.
- [Risk] One generic media task model becomes too loose. → Mitigation: keep common lifecycle fields generic, but allow typed request/response VO objects and capability-specific validation.
- [Risk] Provider APIs differ heavily in parameters and callbacks. → Mitigation: use provider adapters and structured provider-specific input/output JSON while maintaining a stable task lifecycle.
- [Risk] Long-running video tasks overload HTTP request flows. → Mitigation: require asynchronous submission and default status polling/callbacks; move to MQ-backed workers when concurrency grows.
- [Risk] Generated files become hard to secure or clean up. → Mitigation: store canonical file references in Java task records and apply existing file permission, retention, and cleanup policies.
- [Risk] Chat agent tool calls create expensive or unsafe media tasks unexpectedly. → Mitigation: require role-based tool enablement, explicit capability permissions, quotas, and task confirmation for sensitive actions.
- [Risk] Voice/person features create impersonation concerns. → Mitigation: add consent metadata, audit logs, task ownership checks, and review hooks before broad release.
- [Risk] Callback endpoints may receive duplicate, forged, or out-of-order events. → Mitigation: verify signatures/secrets where possible, persist callback logs, and make task updates idempotent with guarded state transitions.

## Migration Plan

1. Stabilize and enable the existing AI module in the chosen deployment mode.
2. Add media task contracts and persistence without changing existing image/music/chat behavior.
3. Add MVP provider adapters for third-party audio/video APIs and validate end-to-end task flow.
4. Add Python Worker integration for one bounded capability, preferably voice cloning or a small video operation.
5. Expose mature media task operations as chat tools through existing tool/function calling and MCP patterns.
6. Expand into multi-step workflow orchestration after individual media capabilities are reliable.

Rollback strategy: keep new media APIs and task tables additive. Existing AI chat, image, music, model, and knowledge features should remain operational if media providers or Python Workers are disabled.

## Open Questions

- Which provider should be used for the first video MVP after Xiaomi MiMo `mimo-v2.5-tts` validates the adapter path?
- Is MQ available in the intended deployment, or should the first implementation use scheduled jobs and provider callbacks only?
- What exact consent/audit policy is required before enabling voice cloning and person replacement for real users beyond the first control set?
- Should Python Worker be deployed as a single multi-capability service initially, or split into audio-worker and video-worker from the start?
