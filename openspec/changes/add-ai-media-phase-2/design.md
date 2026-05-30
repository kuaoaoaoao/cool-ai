## Context

Phase 1 validated the AI module as the product orchestration layer and introduced the unified `ai_media_task` model through Xiaomi MiMo TTS. The next bottleneck is no longer simple provider invocation; video generation and person-related media tasks are long-running, cost-sensitive, file-heavy, and sometimes safety-sensitive.

The existing Java module should continue to own product concerns: auth, tenant isolation, model/API key configuration, task records, file references, permissions, quota, audit, and frontend/admin APIs. Third-party providers and Python Workers should own provider-specific or GPU/model-specific execution.

## Goals / Non-Goals

**Goals:**

- Deliver one usable video generation MVP, preferably image-to-video first, with text-to-video reserved or added second.
- Extend media tasks into asynchronous execution with submission, provider task IDs, polling/callback updates, retry, cancellation, and idempotent state transitions.
- Keep provider-specific video details behind adapters.
- Introduce a minimal Python Worker contract without depending on local GPU models for the first milestone.
- Add media asset governance for source and result files.
- Add safety controls for sensitive media capabilities such as person replacement and local voice/person model tasks.
- Leave stable service boundaries for third-stage Agent/workflow orchestration.

**Non-Goals:**

- Do not build a full video editor, timeline editor, multi-track composition tool, or advanced subtitle packaging in Phase 2.
- Do not integrate multiple video providers at once; one provider is enough for the first MVP.
- Do not train local models or run Python inference inside the JVM.
- Do not make real-time audio/video chat part of Phase 2.
- Do not expose person replacement broadly before feature switches, permission checks, confirmation, and audit logs exist.
- Do not implement full Agent creative workflows yet.

## Decisions

### 1. Start with image-to-video as the first video MVP

The first executable video flow should be SiliconFlow `IMAGE_TO_VIDEO`, with `TEXT_TO_VIDEO` reserved in the same API family or added immediately after.

Rationale: image-to-video reuses existing file upload, image generation, media task, and file result concepts. It also gives users a concrete source asset, reducing prompt ambiguity compared with pure text-to-video.

Alternatives considered:

- Start with text-to-video: simpler input but harder to validate quality and prompt consistency.
- Start with person replacement: highly visible but sensitive and more complex.

First MVP defaults:

- Platform: `SiliconFlow`.
- Model type: `VIDEO`.
- Recommended model: `Wan-AI/Wan2.2-I2V-A14B`, subject to real `/models` validation.
- Submit endpoint: `POST /v1/video/submit`.
- Status endpoint: `POST /v1/video/status`.
- Authentication: `Authorization: Bearer <api-key>`.
- Source image transport: prefer project file URL; use base64 data URL only if deployment cannot expose source files to SiliconFlow.
- Result handling: immediately transfer SiliconFlow result video URL into the project file service because provider result URLs are temporary.

### 2. Keep one unified media task model, but strengthen async fields

Phase 2 should continue using `ai_media_task` as the main task record. Add explicit fields when they are important for querying or state transitions, such as execution mode, provider task ID, retry count, cancel flag, and callback status. Provider-specific parameters can remain in JSON fields.

Rationale: a single task center keeps frontend, chat tools, and admin operations consistent. Explicit lifecycle fields prevent important execution state from being hidden in JSON.

Alternatives considered:

- Create separate video task tables immediately: stronger typing but duplicates task lifecycle logic.
- Store every new field in JSON: fast initially but weak for querying, retries, and operations.

### 3. Add a media execution service between task creation and adapters

Controllers and feature services should create tasks and delegate execution to an `AiMediaExecutionService`. The execution service handles state transitions, provider submission, retry/cancel logic, and callback updates.

```text
Controller / Tool
    ↓
AiMediaTaskService
    ↓
AiMediaExecutionService
    ↓
AiMediaProviderAdapter
```

Rationale: this avoids spreading async state logic across each capability service.

### 4. Use provider adapters for both third-party video APIs and Python Workers

Third-party video providers and Python Workers should share the same Java adapter interface where practical: `supports`, `submit`, `query`, `cancel`, and `handleCallback`.

Rationale: product APIs should not change when execution moves from third-party API to Python Worker.

Alternatives considered:

- Build Python Worker as a separate pathway: easier initially but creates two task execution models.
- Expose provider APIs directly to frontend: faster for one provider but leaks credentials and provider concepts.

### 5. Prefer polling plus optional callback for progress

Frontend should continue polling task status. For the SiliconFlow MVP, backend scheduled polling is the default status sync path. Provider/Worker callback should remain a later enhancement for providers that support it, but the UI should not depend on callback-only behavior.

Rationale: polling is robust across local development, private deployments, and providers that do not support callback. Callback improves freshness but requires public endpoints and verification.

For SiliconFlow:

```text
Frontend ──poll──▶ Java task detail
                    │
Scheduled job ──────┘
   │
   └── POST SiliconFlow /v1/video/status
```

User refreshes should not directly fan out to SiliconFlow on every request.

### 6. Introduce callback logs and idempotent updates

All provider/worker callbacks should be normalized, verified where possible, logged, and applied idempotently. Duplicate or out-of-order callbacks must not regress a terminal task state.

Rationale: long-running media providers commonly retry callbacks, and local workers may report progress out of order.

### 7. Add media assets as a first-class view or table

Phase 2 should at least standardize asset metadata in task input/output. If asset listing, cleanup, reuse, or audit is required, add `ai_media_asset`.

Rationale: video tasks create larger and more numerous files than TTS. Without asset metadata, result management, retention, and cost tracking become difficult.

For the first SiliconFlow MVP, asset metadata may be standardized in `input_json` and `output_json` first. Creating `ai_media_asset` can follow once the task center needs independent asset browsing, retention, or cleanup workflows.

### 8. Gate sensitive media operations before execution

Person replacement, local voice cloning, and similar capabilities must require feature switches, permissions, explicit user confirmation, source ownership checks, and audit logs before provider or worker submission.

Rationale: these capabilities can impersonate real people. Safety controls belong in product orchestration, not inside individual providers.

### 9. Use a minimal Python Worker HTTP contract first

The first Worker integration should support health check, task submit, task status, and callback. MQ can be added later once execution volume requires it.

Rationale: HTTP is enough to validate the orchestration contract and avoids introducing queue infrastructure before the task model is stable.

## Risks / Trade-offs

- [Risk] The chosen video provider may not support callbacks or stable progress. → Mitigation: require polling support and map unknown progress to coarse stages.
- [Risk] A generic media task becomes too loose. → Mitigation: validate each task type with typed request VOs and keep critical lifecycle fields explicit.
- [Risk] Provider callbacks may be forged or replayed. → Mitigation: use shared secrets/signatures where supported, persist callback logs, and require idempotent state transitions.
- [Risk] Video files increase storage cost quickly. → Mitigation: track asset metadata, size, retention, and cleanup readiness in Phase 2.
- [Risk] Python Worker deployment becomes a blocker. → Mitigation: start with a mock or lightweight FFmpeg worker before local model inference.
- [Risk] Sensitive media features create legal or trust risk. → Mitigation: default off, require permissions and confirmation, log source/result relationships, and add watermark/trace hooks.
- [Risk] Building too much frontend slows the MVP. → Mitigation: start with simple generation forms and a task center, not a full editor.

## Migration Plan

1. Keep Phase 1 TTS behavior working while adding new task types and async fields.
2. Add async execution service and callback logging without changing existing user-facing TTS APIs.
3. Add SiliconFlow image-to-video API and provider adapter behind feature switches.
4. Add frontend image-to-video form and task center updates.
5. Add Python Worker skeleton and adapter as an optional execution mode.
6. Add media asset metadata/table and cleanup-ready queries.
7. Add sensitive media controls and internal person replacement preview behind disabled-by-default switches.

Rollback strategy: all new task types and providers must be feature-switchable. If the video provider or Worker is disabled, existing chat, image, music, knowledge, and TTS flows should continue to work.

## Open Questions

- Which third-party video provider should be used for the first image-to-video MVP?
- Should Phase 2 persist `ai_media_asset` immediately, or start with standardized `output_json` and add the table when asset browsing is implemented?
- Is a public callback URL available in the target deployment, or must provider status rely on scheduled polling first?
- Should the first Python Worker be a single multi-capability service or split later into video-worker/audio-worker/person-worker?
- What watermark or trace policy is required before person replacement can be tested beyond internal users?
