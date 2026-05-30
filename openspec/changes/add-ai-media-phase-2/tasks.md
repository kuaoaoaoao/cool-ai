## 1. Phase 2 Readiness

- [x] 1.1 Confirm SiliconFlow as the first video MVP provider and record `/v1/video/submit`, `/v1/video/status`, Bearer authentication, `requestId`, statuses, result URL TTL, supported image sizes, and pricing/limits.
- [x] 1.2 Confirm that the first SiliconFlow MVP uses scheduled polling as the status sync path and does not depend on Provider callback.
- [x] 1.3 Confirm that the first SiliconFlow MVP starts with standardized `inputJson`/`outputJson` asset metadata instead of creating `ai_media_asset`; keep `ai_media_asset` as a follow-up when asset browsing/cleanup is implemented.
- [x] 1.4 Confirm video generation feature switches, admin permissions, menu placement, and tenant/user quota defaults.
- [x] 1.5 Create or update SQL migration plan for new task fields, dictionaries, permissions, menus, and optional callback table; do not require a Python Worker project for the first MVP.

## 2. Async Media Task Foundation

- [x] 2.1 Extend media task status handling to include queued, submitted, running, retrying, cancelling, cancelled, succeeded, and failed transitions.
- [x] 2.2 Add explicit task metadata needed for async execution, such as execution mode, provider task ID, retry count, cancel request flag, submitted time, and callback status where not already covered.
- [x] 2.3 Implement guarded state transition helpers that prevent terminal states from regressing.
- [x] 2.4 Implement `AiMediaExecutionService` for enqueueing, submitting, refreshing, retrying, cancelling, and completing media tasks.
- [x] 2.5 Add unified task status response fields for progress, stage, provider, model, external task ID, source assets, result assets, retry count, and error details.
- [x] 2.6 Add user/tenant ownership checks for querying, retrying, and cancelling media tasks.

## 3. Polling Infrastructure

- [x] 3.1 Implement provider status polling job or service method for SiliconFlow `IMAGE_TO_VIDEO` tasks.
- [x] 3.2 Query only tasks with SiliconFlow platform, image-to-video task type, non-terminal status, and non-empty provider task ID.
- [x] 3.3 Map SiliconFlow `InQueue`, `InProgress`, `Succeed`, and `Failed` statuses to internal task states.
- [x] 3.4 Ensure terminal-state protection so polling cannot regress succeeded, failed, or cancelled tasks.
- [x] 3.5 Record raw SiliconFlow status response in task output or structured logs for debugging.
- [x] 3.6 Keep unified callback endpoint and callback log as a follow-up design item, not a blocker for the first SiliconFlow MVP.

## 4. Video Provider Adapter

- [x] 4.1 Define the Phase 2 video adapter contract with `supports`, `submit`, `query`, `cancel`, and `handleCallback` methods.
- [x] 4.2 Implement provider adapter resolution by task type, provider, model, and execution mode.
- [x] 4.3 Implement the SiliconFlow image-to-video provider adapter.
- [x] 4.4 Map internal image-to-video inputs to SiliconFlow request fields: `model`, `prompt`, `image_size`, `image`, optional `negative_prompt`, and optional `seed`.
- [x] 4.5 Map SiliconFlow status responses to internal task status, progress, stage, result URLs, and error codes.
- [x] 4.6 Implement SiliconFlow result URL download and transfer into the existing file service under an `ai-media/video` path or equivalent.
- [x] 4.7 Add provider error handling for missing credentials, unsupported parameters, quota exhaustion, rate limiting, timeout, invalid source file, and invalid response.
- [x] 4.8 Add provider adapter tests using mocked HTTP responses for accepted, running, succeeded, failed, timeout, and malformed response paths.

## 5. Image-To-Video Backend API

- [x] 5.1 Add image-to-video create request and response VOs with validation for source image, prompt, ratio, duration, quality, and provider/model selection.
- [x] 5.2 Add service method for creating `IMAGE_TO_VIDEO` media tasks with source image ownership and MIME type validation.
- [x] 5.3 Add admin API endpoint for creating image-to-video tasks.
- [x] 5.4 Add task detail/status API support for video-specific source and result metadata.
- [x] 5.5 Add task page query filters for video task type, provider, status, and create time.
- [x] 5.6 Add feature switch and permission checks before image-to-video task creation.
- [x] 5.7 Add quota checks for user daily count, tenant daily count, concurrent tasks, source file size, and max duration.
- [x] 5.8 Add backend tests for successful creation, missing image, unsupported image type, unauthorized source image, disabled feature, quota exceeded, and provider submission failure.

## 6. Text-To-Video Backend API

- [x] 6.1 Reserve text-to-video enums, VO names, and frontend/API placeholders only if needed for UI consistency.
- [x] 6.2 Ensure first MVP returns capability-not-enabled for `TEXT_TO_VIDEO` unless explicitly enabled later.
- [x] 6.3 Add tests for explicit `TEXT_TO_VIDEO` capability-not-enabled behavior.

## 7. Media Asset Governance

- [x] 7.1 Implement standardized task JSON metadata for source and result assets; defer `ai_media_asset` persistence until asset browsing, cleanup, or reuse is implemented.
- [x] 7.2 Record source image assets when image-to-video tasks are created.
- [x] 7.3 Record result video assets when video generation succeeds.
- [x] 7.4 Add asset ownership and tenant isolation checks.
- [x] 7.5 Add asset list or task asset aggregation API for source and result files.
- [x] 7.6 Add asset expiry, file size, duration, width, height, MIME type, and cleanup candidate metadata.
- [x] 7.7 Add quota/cost records for media task count, concurrency, file size, duration, provider, model, and estimated or actual cost.
- [x] 7.8 Add tests for asset creation, asset filtering, unauthorized access, and cleanup candidate selection.

## 8. Python Worker Skeleton

- [x] 8.1 Keep Python Worker out of the first SiliconFlow MVP implementation path.
- [x] 8.2 Document the future Worker contract after the SiliconFlow image-to-video flow is stable.
- [x] 8.3 Revisit whether to create `yudao-ai-worker` only after Java async task, polling, file transfer, and frontend video task flow are validated.

## 9. Sensitive Media Controls

- [x] 9.1 Add system-level and tenant-level feature switches for person replacement, local voice cloning, local voice conversion, and other sensitive media capabilities.
- [x] 9.2 Add permission checks for sensitive media task creation and result access.
- [x] 9.3 Add explicit confirmation fields and validation for sensitive task requests.
- [x] 9.4 Add source material ownership and MIME type validation for source video, target person image/video, and voice samples.
- [x] 9.5 Add sensitive task audit logging for create, submit, cancel, fail, succeed, and result access events.
- [x] 9.6 Add watermark or trace option fields to sensitive task requests and provider/worker payloads.
- [x] 9.7 Keep person replacement UI/API hidden or disabled by default until safety controls and an internal provider/worker path are ready.
- [x] 9.8 Add tests for disabled feature, missing permission, missing confirmation, unauthorized source material, and audit creation.

## 10. Frontend

- [x] 10.1 Add video generation API client methods for image-to-video, text-to-video if enabled, status query, retry, cancel, and task page query.
- [x] 10.2 Add a video generation page with mode switch, prompt input, source image upload/selection, ratio, duration, quality, and submit button.
- [x] 10.3 Add running task status display with progress, stage, provider, elapsed time, and error details.
- [x] 10.4 Add result video preview, download, copy link, and task detail actions.
- [x] 10.5 Extend media task manager to show TTS, image-to-video, text-to-video, and future media task types consistently.
- [x] 10.6 Add retry and cancel actions where backend status allows them.
- [x] 10.7 Add feature-disabled, provider-not-configured, quota-exceeded, and source-file-invalid UI states.
- [x] 10.8 Keep person replacement UI internal/hidden unless feature switch and permissions are enabled.

## 11. Chat Tool and Workflow Readiness

- [x] 11.1 Add video task creation tool metadata only after backend image-to-video API is stable.
- [x] 11.2 Ensure chat tools call media task services and never call providers directly.
- [x] 11.3 Add media task status tool support for video task summaries.
- [x] 11.4 Keep expensive or sensitive chat tools disabled unless role binding, permission, quota, and confirmation controls are available.
- [x] 11.5 Document third-stage workflow/Agent expansion points without implementing full orchestration in Phase 2.

## 12. Validation and Release

- [x] 12.1 Add service tests for async task state transitions, retry, cancel, ownership, tenant isolation, quota, and terminal-state protection.
- [x] 12.2 Add controller tests for image-to-video creation, status query, task page, retry, cancel, unauthorized access, and disabled feature.
- [x] 12.3 Add provider adapter tests for the selected video Provider.
- [x] 12.4 Add callback and polling tests.
- [x] 12.5 Add Python Worker adapter tests with mock Worker.
- [x] 12.6 Add frontend smoke checklist for image-to-video submit, polling, success preview, failure display, retry, and cancel.
- [x] 12.7 Run focused Maven tests for `yudao-module-ai/yudao-module-ai-server` and record environment blockers.
- [x] 12.8 Update setup documentation with provider configuration, callback URL, feature switches, permissions, quota defaults, and Worker optional setup.
