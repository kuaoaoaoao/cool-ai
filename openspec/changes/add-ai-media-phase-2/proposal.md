## Why

Phase 1 made the existing AI module usable and validated the unified `ai_media_task` foundation through TTS. Phase 2 should turn that foundation into a media generation platform that can safely run long-running, high-cost, and sensitive audio/video tasks such as image-to-video, text-to-video, Python Worker jobs, and person replacement.

## What Changes

- Add the first executable video generation flow as SiliconFlow `IMAGE_TO_VIDEO`, while reserving `TEXT_TO_VIDEO`.
- Extend media task execution from mostly synchronous TTS into asynchronous submission, queueing, polling/callback updates, retry, cancellation, and provider task mapping.
- Introduce a video Provider Adapter boundary so one initial third-party video provider can be integrated without leaking provider-specific details across controllers and services.
- Introduce a Python Worker contract and Java adapter for later local/custom model execution, starting with a skeleton that can submit tasks, report progress, and return result files.
- Add media asset management concepts for source files and generated results, including file metadata, task association, expiry, and cleanup readiness.
- Add safety and governance controls for sensitive media tasks, especially person replacement and local voice/person capabilities: feature switches, permissions, explicit confirmation, source ownership checks, audit logs, and watermark/trace hooks.
- Keep third-stage Agent/workflow orchestration as a direction only; Phase 2 exposes stable task APIs and tool-ready service boundaries but does not implement full creative workflow automation.

## Capabilities

### New Capabilities

- `ai-video-generation-phase-2`: Covers image-to-video and text-to-video task creation, provider submission, progress tracking, result saving, and user-facing video task history.
- `ai-media-async-execution`: Covers asynchronous media task lifecycle, queue/submission states, retry, cancellation, provider task IDs, callbacks, and idempotent state updates.
- `ai-python-worker-integration`: Covers the minimal Java-to-Python Worker contract, worker health checks, task submission, status/callback payloads, and result handoff.
- `ai-media-asset-governance`: Covers media asset records or views, source/result file metadata, ownership, expiry, cleanup readiness, quota/cost tracking, and sensitive-operation audit.
- `ai-sensitive-media-controls`: Covers person replacement and other sensitive media capability gating, confirmation, source ownership validation, audit logging, and watermark/trace requirements.

### Modified Capabilities

- None. There are no archived base specs yet; this change builds on the Phase 1 change artifacts and introduces new Phase 2 capability specs.

## Impact

- Affects `yudao-module-ai` services, controllers, VOs, enums, mappers, media provider adapters, and tests.
- May extend `ai_media_task` fields or use existing JSON fields for provider task IDs, execution mode, retry/cancel metadata, and callback data.
- May add new tables such as `ai_media_asset` and `ai_media_callback_log`.
- Extends admin UI with video generation, media task center enhancements, and optional media asset management.
- Adds optional Python Worker service integration through HTTP later in Phase 2, with MQ left as a later scaling option; the first MVP does not depend on a Python project.
- Requires provider configuration, API key/model entries, file service availability, quota/cost policy, and sensitive-operation permission design before production rollout.
