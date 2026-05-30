## Why

The project already has a strong Java/Spring AI foundation for chat, model management, knowledge retrieval, tools, MCP, image generation, and music generation, but the desired audio/video capabilities require a clearer platform boundary before implementation. This change defines an AI media platform architecture where Java owns product orchestration and Python/third-party providers own heavyweight AI media inference.

## What Changes

- Introduce a unified AI media task model for long-running audio and video jobs, including submission, status tracking, progress, failure handling, result files, and task history.
- Add first-class audio capabilities for text-to-speech, voice cloning, music generation expansion, and future audio processing such as denoising, separation, or voice conversion.
- Add first-class video capabilities for text-to-video, image-to-video, face/person replacement, and future video post-processing such as subtitles, enhancement, or composition.
- Define a provider integration boundary that supports both third-party AI APIs for early MVP delivery and Python Worker services for local/custom model inference.
- Use Xiaomi MiMo TTS models as the first third-party provider integration target for validating the provider adapter and media task flow, including built-in voice, cloud voice clone, and cloud voice design modes under the `TTS` task type.
- Expose selected media capabilities as chat-callable tools through the existing Spring AI tool/function calling and MCP-oriented chat role model, with confirmation and quota controls for sensitive or expensive operations.
- Establish a phased development path: AI module stabilization, media task center, third-party API MVPs, Python Worker integration, agent tool invocation, then multi-step creative workflows.
- No breaking changes are intended for existing chat, image, music, model, or knowledge features.

## Capabilities

### New Capabilities

- `ai-media-task-management`: Defines how audio/video jobs are created, tracked, cancelled, retried, completed, and queried as asynchronous media tasks backed by a unified task model.
- `ai-audio-generation`: Covers audio-facing product capabilities such as text-to-speech, voice cloning, music generation expansion, and audio result management.
- `ai-video-generation`: Covers video-facing product capabilities such as text-to-video, image-to-video, face/person replacement, and video result management.
- `ai-provider-orchestration`: Defines how Java orchestrates third-party AI APIs and Python Worker services without embedding model-specific inference logic in the Java application, including worker request/response and callback contracts.
- `ai-agent-media-tools`: Defines how chat roles, function calling, and MCP tools can trigger media tasks and query their status/results, including confirmation and tenant quota constraints for sensitive operations.

### Modified Capabilities

None. There are no existing OpenSpec capabilities yet.

## Impact

- Affects `yudao-module-ai` as the primary home for new media task, audio, video, provider, and agent-tool orchestration concepts.
- May extend existing AI model/platform enums, model configuration, API key/provider configuration, and admin UI surfaces for managing providers and tasks.
- Requires persistent storage for media tasks, task inputs/outputs, provider metadata, callback logs, and generated asset references.
- Requires file storage integration for uploaded source media and generated audio/video results, using the existing infrastructure file capabilities where possible.
- Introduces optional integration with Python AI Worker services for GPU/model-heavy workloads such as voice cloning, local video generation, and person replacement.
- May use Redis, scheduled jobs, callbacks, or a message queue for asynchronous task execution, retries, and progress updates.
- Uses existing Spring AI tool/function calling and MCP integration patterns to allow chat agents to create and inspect media tasks.

## Follow-up Phases

- Python audio Worker, local voice cloning, and voice conversion should be handled in a later change after first-phase MiMo cloud TTS modes are stable.
- Video Provider or Python video Worker integration should be handled in a later change after selecting the first video MVP provider.
- Agent-driven media workflows should be handled in a later change after task APIs and media tools are reliable.
