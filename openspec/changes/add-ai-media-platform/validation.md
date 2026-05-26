## Validation

Commands run:

```bash
JAVA_HOME=/Users/kuao/Library/Java/JavaVirtualMachines/ms-17.0.17/Contents/Home mvn -pl yudao-module-ai/yudao-module-ai-server -am -DskipTests compile
JAVA_HOME=/Users/kuao/Library/Java/JavaVirtualMachines/ms-17.0.17/Contents/Home mvn -pl yudao-module-ai/yudao-module-ai-server -am -Dtest=AiMediaTaskServiceImplTest,XiaomiMiMoTtsProviderAdapterTest,AiMediaTaskControllerTest -Dsurefire.failIfNoSpecifiedTests=false test
```

Result:

- Compile succeeded with JDK 17.
- AI media focused tests succeeded: 11 tests, 0 failures, 0 errors.

Manual setup required before real Xiaomi MiMo calls:

- Run `sql/mysql/add-ai-media-platform.sql`.
- Add an enabled `ai_api_key` row with platform `XiaomiMiMo`, API URL `https://api.xiaomimimo.com`, and the real Xiaomi MiMo API key.
- Add an enabled `ai_model` row with type `VOICE`, platform `XiaomiMiMo`, model `mimo-v2.5-tts`, and the API key id above.
- Bind permissions such as `ai:media:tts:create` and `ai:media:task:query` to the intended admin roles.
- For chat tool usage, create enabled `ai_tool` rows for `media_create_tts_task` and `media_get_task_status`, then bind them to the desired chat role.
