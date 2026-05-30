## Validation

Commands run:

```bash
JAVA_HOME=/Users/kuao/Library/Java/JavaVirtualMachines/ms-17.0.17/Contents/Home mvn -pl yudao-module-ai/yudao-module-ai-server -am -DskipTests compile
JAVA_HOME=/Users/kuao/Library/Java/JavaVirtualMachines/ms-17.0.17/Contents/Home mvn -pl yudao-module-ai/yudao-module-ai-server -am -Dtest=AiMediaTaskServiceImplTest,XiaomiMiMoTtsProviderAdapterTest,AiMediaTaskControllerTest -Dsurefire.failIfNoSpecifiedTests=false test
```

Result:

- Compile succeeded with JDK 17.
- AI media focused tests succeeded: 11 tests, 0 failures, 0 errors.

Latest code review follow-up:

```bash
JAVA_HOME=/Users/kuao/Library/Java/JavaVirtualMachines/ms-17.0.17/Contents/Home mvn -pl yudao-module-ai/yudao-module-ai-server -am -DskipTests compile
```

Result:

- Compile succeeded with JDK 17 after P1/P2 hardening.
- A later focused Mockito test run was blocked by the local Microsoft JDK 17 ByteBuddy self-attach limitation, not by a business assertion failure.
- Non-Mockito focused checks that had already run before the blocker included Xiaomi MiMo multimodal and remote resource service coverage.

First-phase hardening now reflected in code/docs:

- Default remote resource download blocks private/local URLs; local profiles may explicitly allow them for development.
- TTS quota lock is strict by default in production and can be relaxed in local/test profiles.
- Voice profile create/update clears fields that do not belong to the selected source type.
- Image model connection test validates model construction without sending a real image generation request.
- MiMo TTS supports built-in voice, cloud voice clone, and cloud voice design as TTS modes; local voice cloning remains a follow-up phase.

Manual setup required before real Xiaomi MiMo calls:

- Run `sql/mysql/add-ai-media-platform.sql`.
- Add an enabled `ai_api_key` row with platform `XiaomiMiMo`, API URL `https://api.xiaomimimo.com`, and the real Xiaomi MiMo API key.
- Add an enabled `ai_model` row with type `VOICE`, platform `XiaomiMiMo`, model `mimo-v2.5-tts`, and the API key id above.
- For cloud voice clone/design validation, add matching `VOICE` models for `mimo-v2.5-tts-voiceclone` and `mimo-v2.5-tts-voicedesign`.
- Bind permissions such as `ai:media:tts:create` and `ai:media:task:query` to the intended admin roles.
- For chat tool usage, create enabled `ai_tool` rows for `media_create_tts_task` and `media_get_task_status`, then bind them to the desired chat role.
