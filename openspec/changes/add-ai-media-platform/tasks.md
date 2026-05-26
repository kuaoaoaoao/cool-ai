> Scope: This task list covers the first delivery phase only: unified `ai_media_task` foundation plus an end-to-end TTS flow using Xiaomi MiMo `mimo-v2.5-tts`. Voice cloning, video generation, person replacement, Python Workers, MQ execution, and advanced agent workflows are follow-up phases unless explicitly listed here as non-executable placeholders.

## 1. AI Module Readiness

- [x] 1.1 Confirm the intended deployment mode for `yudao-module-ai` and enable it in `yudao-server` or run it as an independent service.
- [x] 1.2 Verify existing AI module startup dependencies, database scripts, file service access, security context, and tenant context.
- [x] 1.3 Confirm the existing AI API key/model configuration pattern and decide where Xiaomi MiMo credentials will be read from without hardcoding secrets.

## 2. Media Task Data Model

- [x] 2.1 Add `ai_media_task` persistence model with task type, provider, model, status, progress, stage, input JSON, output JSON, error fields, user ID, tenant ID, and timing fields.
- [x] 2.2 Add mapper/query support for creating tasks, updating status/progress/results, querying by ID, and paging user-visible task history.
- [x] 2.3 Add media task status enum and first-phase task type handling for executable `TTS` plus an unsupported/unknown path for future task types.
- [x] 2.4 Add SQL migration and dictionary data needed for media task status, task type, provider, and first-phase TTS options.

## 3. Media Task Service

- [x] 3.1 Implement `AiMediaTaskService` for task creation, ownership checks, tenant isolation, status updates, failure updates, and result updates.
- [x] 3.2 Implement first-phase synchronous TTS execution flow: create task, call provider, save result, and update final status in one service flow.
- [x] 3.3 Implement first-phase capability gating so only `TTS` tasks execute and future task requests return capability-not-enabled errors from the service layer.
- [x] 3.4 Implement task status query response with task ID, status, progress, stage, output references, and error details for frontend polling.
- [x] 3.5 Implement minimal feature switch, permission check, and simple user/tenant usage limit hooks before task execution.

## 4. Provider Adapter Layer

- [x] 4.1 Define `AiMediaProviderAdapter` interface for submit/execute behavior and provider capability matching.
- [x] 4.2 Implement provider adapter resolution by task type, provider, and model.
- [x] 4.3 Add common provider error handling for missing credentials, timeout, network failure, invalid response, and unsupported capability.

## 5. Xiaomi MiMo TTS Adapter

- [x] 5.1 Run a MiMo TTS API spike against official documentation or a mock to confirm request shape, response shape, audio payload format, required fields, and error format.
- [x] 5.2 Implement Xiaomi MiMo `mimo-v2.5-tts` adapter using the confirmed endpoint and payload contract.
- [x] 5.3 Map TTS text and optional style/emotion guidance according to the confirmed MiMo request format.
- [x] 5.4 Pass configured `voice` and `format` values from provider configuration or dictionary data.
- [x] 5.5 Send configured API key through the `api-key` header and fail clearly when the key is missing.
- [x] 5.6 Decode or stream-process the returned audio according to the confirmed MiMo response format and convert successful provider response into media task output data.

## 6. File Result Handling

- [x] 6.1 Store generated TTS audio through the existing infra file service instead of saving binary data in `ai_media_task`.
- [x] 6.2 Write both `fileId` and `url` into media task output JSON after a successful file save.
- [x] 6.3 Ensure failed file saves mark the media task as failed with a useful error message.

## 7. Admin API Surface

- [x] 7.1 Add TTS task creation API under the AI module admin API surface.
- [x] 7.2 Add media task detail/status API for frontend polling.
- [x] 7.3 Add media task page API scoped to the current user and tenant.

## 8. Chat Tool Integration

- [x] 8.1 Investigate the existing AI tool registration mechanism, including annotation-based tools, `ToolCallbackResolver`, `AiToolDO`, and chat role tool binding.
- [x] 8.2 Add a TTS media task tool that calls `AiMediaTaskService` instead of calling Xiaomi MiMo directly.
- [x] 8.3 Register the TTS tool using the confirmed existing tool mechanism so it can be enabled through chat roles.
- [x] 8.4 Add media task status query tool for chat-driven progress checks.
- [x] 8.5 Add audit logging for chat-triggered media task creation and rejected tool calls.
- [x] 8.6 Ensure sensitive planned tools such as voice cloning and person replacement are not exposed until confirmation and safety controls are implemented.

## 9. Tests and Validation

- [x] 9.1 Add service tests for media task creation, ownership checks, tenant isolation, status updates, disabled capability handling, and output saving.
- [x] 9.2 Add adapter tests for Xiaomi MiMo request mapping, missing API key handling, provider failure handling, and base64 audio response handling.
- [x] 9.3 Add API tests for TTS creation, task status polling, task page query, and unauthorized task access.
- [x] 9.4 Add tests for duplicate task execution protection or idempotent provider result handling where applicable.
- [x] 9.5 Add tests for file storage failure and verify task status remains consistent.
- [x] 9.6 Add tenant isolation tests to ensure one user or tenant cannot access another user's media tasks.
- [x] 9.7 Add validation or smoke test notes for manual Xiaomi MiMo API key configuration and end-to-end TTS generation.
- [x] 9.8 Run the relevant Maven test/build command for `yudao-module-ai` and record any skipped checks or environment blockers.
