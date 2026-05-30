# AI 第二阶段媒体生成平台规划

## 1. 阶段定位

> 进度更新（2026-05-30）：SiliconFlow `IMAGE_TO_VIDEO` MVP 已完成后端任务创建、异步提交、后台轮询、Provider 状态映射、临时结果 URL 下载转存、base64 源图保底、retry/cancel、基础配额/开关、资产聚合视图、usageRecord 成本记录、SQL 菜单权限迁移草案、后端测试，以及后台图生视频页面和媒体任务中心的视频展示。Python Worker 已补齐 Java Adapter、回调入口、轮询 Job、mock Worker skeleton 和测试；敏感媒体能力已补齐默认关闭的开关、租户白名单、权限、确认、素材归属/MIME、审计和水印/追踪治理层。独立资产表仍可在资产清理/复用需求明确后推进。

第二阶段的核心目标不是继续零散接入更多模型，而是把第一阶段已经验证过的 `ai_media_task`、Provider Adapter、文件服务、权限、配额和任务查询能力扩展成可承载视频、人物替换、Python Worker 和媒体资产治理的平台。

第一阶段解决“现有 AI 功能能不能稳定用”。第二阶段解决“长耗时、高成本、敏感媒体任务能不能产品化、可控地运行”。

## 2. 第二阶段目标

- 建立视频生成 MVP：优先跑通 `IMAGE_TO_VIDEO`，同时预留 `TEXT_TO_VIDEO`。
- 将媒体任务从同步 TTS 扩展为真正的异步执行模型：排队、运行、回调、轮询、失败、重试、取消。
- 建立第三方视频 Provider Adapter，先接一个可控 Provider，避免同时接多个视频平台。
- 建立 Python Worker 最小契约，为本地视频模型、人物替换、语音转换等后续能力打基础。
- 建立媒体资产视图，统一管理源文件、生成结果、任务、Provider、用户、租户、过期和清理。
- 建立敏感媒体能力安全控制：权限、开关、授权确认、素材归属、审计、水印或追踪标记。

## 3. 第二阶段不做

- 不做完整视频剪辑器、时间线编辑器、多轨道编辑和复杂字幕包装。
- 不同时接入多个视频 Provider；先用一个 Provider 跑通平台闭环。
- 不训练本地大模型，不把 Python 模型推理嵌入 Java 进程。
- 不做实时音视频对话。
- 不默认开放人物替换/换脸给所有用户；该能力必须有开关、权限、确认和审计。
- 不把 Agent 多步骤自动创作作为第二阶段主线，只保留工具入口和后续扩展点。

## 4. 推荐 MVP 顺序

### 4.1 MVP-A：图生视频

优先级最高，并确定第一版 Provider 为 SiliconFlow。

第一版范围：

- 任务类型：`IMAGE_TO_VIDEO`。
- 平台：`SiliconFlow`。
- 推荐模型：`Wan-AI/Wan2.2-I2V-A14B`。
- 提交接口：`POST https://api.siliconflow.cn/v1/video/submit`。
- 状态接口：`POST https://api.siliconflow.cn/v1/video/status`。
- 鉴权方式：`Authorization: Bearer <api-key>`。
- 图片传入：优先传项目文件服务可公网访问的 URL；如果部署环境无法被 SiliconFlow 访问，再考虑 base64 data URL 兜底。
- 结果处理：SiliconFlow 返回的视频 URL 有有效期，Java 后端必须尽快下载并转存到项目文件服务，不能把临时 URL 当长期结果。
- 后端 API：`POST /admin-api/ai/media/task/image-to-video`。
- 后台 Job：`aiSiliconFlowVideoSyncJob`，建议 30-60 秒调度一次。
- 状态刷新：生产建议配置 `aiSiliconFlowVideoSyncJob`；本地或未接 XXL-Job 时，前端轮询 `get-my` 任务详情会触发单个 SiliconFlow 任务状态刷新，避免提交后长期停留在 5%。
- 结果目录：`ai-media/video`。
- SQL：`sql/mysql/add-ai-media-phase-2-siliconflow-video.sql`。
- 前端页面：`yudao-ui/yudao-ui-admin-vue3/src/views/ai/media/video/index.vue`。
- 配置前缀：`yudao.ai.media.video.*`。

优先做图生视频的原因：

- 和现有图片生成、文件服务、媒体任务最容易衔接。
- 用户输入相对可控：一张源图、提示词、时长、比例、运动强度。
- 比人物替换敏感度低，比完整文本生成视频更容易做出稳定验收。

最小链路：

```text
用户上传/选择图片
    ↓
提交图生视频任务
    ↓
Java 创建 ai_media_task
    ↓
SiliconFlow Provider Adapter 提交任务
    ↓
保存 requestId 到 providerTaskId
    ↓
定时轮询 SiliconFlow status
    ↓
下载临时视频 URL 并保存视频到文件服务
    ↓
任务 SUCCEEDED，前端播放/下载
```

第一版请求建议：

```json
{
  "modelId": 1001,
  "sourceImageUrl": "https://example.com/source.png",
  "prompt": "让画面中的人物缓慢转身，镜头轻微推进",
  "negativePrompt": "blur, low quality",
  "imageSize": "1280x720",
  "seed": 123
}
```

SiliconFlow 请求映射：

```json
{
  "model": "Wan-AI/Wan2.2-I2V-A14B",
  "prompt": "让画面中的人物缓慢转身，镜头轻微推进",
  "image_size": "1280x720",
  "negative_prompt": "blur, low quality",
  "image": "https://example.com/source.png",
  "seed": 123
}
```

SiliconFlow 状态映射：

| SiliconFlow | 系统状态 | 说明 |
| --- | --- | --- |
| `InQueue` | `QUEUED` | 已提交，等待执行 |
| `InProgress` | `RUNNING` | 生成中 |
| `Succeed` | `SUCCEEDED` | 下载并保存结果成功后才置为成功 |
| `Failed` | `FAILED` | 记录 `reason` 和原始响应 |

第一版先采用后台定时轮询，不依赖 callback。前端轮询我们自己的任务状态接口，避免把用户刷新频率直接放大到 SiliconFlow。

当前后端 MVP 状态：

- 已新增 `IMAGE_TO_VIDEO` 和预留 `TEXT_TO_VIDEO` 任务类型。
- 已补充 `SUBMITTED`、`CANCELLING` 状态和终态保护判断。
- 已新增 SiliconFlow 视频 Provider Adapter，支持 `submit` / `query`，`cancel` / `handleCallback` 先保留为未支持。
- 已复用 `input_json` / `output_json` 记录源图片资产、Provider 原始提交/状态响应和结果视频资产。
- 已复用 `provider_task_id` 保存 SiliconFlow `requestId`。
- 已复用 `AiRemoteResourceService` 下载 SiliconFlow 临时结果 URL，并通过 `FileApi` 保存项目侧结果。
- 已增加视频开关、用户日限额、用户并发限制、源图大小限制、结果大小限制和轮询批量限制。
- 已增加后台图生视频页面：支持 SiliconFlow 视频模型选择、源图片上传/URL、提示词、负向提示词、尺寸、时长、质量、seed、任务轮询、进度/阶段/耗时展示、视频预览、下载和复制链接。
- 已扩展媒体任务中心：可统一展示 TTS 音频结果、图生视频/文生视频任务类型和视频结果。
- 已补充 SQL 菜单：`AI 视频生成` 页面和 `ai:media:video:create` 按钮权限。
- 已收紧图生视频模型选择：前端只展示 `image-to-video` 能力模型，后端也会拒绝文生视频模型被误用于图生视频。
- 已增加聚焦测试样例；当前本机 Microsoft JDK 17 因 Mockito inline ByteBuddy self-attach 失败，测试运行无法进入业务断言，但 `compile` 和 `test-compile` 均通过。

前端冒烟 checklist：

- 管理员拥有 `ai:media:video:create` 权限后，可以看到“AI 视频生成”菜单和提交按钮。
- SiliconFlow 视频模型启用后，视频模型下拉可以加载 `VIDEO + SiliconFlow` 模型。
- 上传或粘贴公网可访问图片 URL，填写提示词后，可以提交 `IMAGE_TO_VIDEO` 任务。
- 提交成功后，“我的视频任务”列表可以展示任务状态、进度、阶段、平台、模型和耗时。
- 定时轮询任务详情时，`QUEUED` / `SUBMITTED` / `RUNNING` / `SUCCEEDED` / `FAILED` 状态可以正常刷新。
- 任务成功后，可以在页面预览视频、下载视频、复制项目文件服务中的结果 URL。
- 任务失败后，可以展示后端返回的 `errorMessage`。
- 关闭视频功能、未配置 Provider、超出配额、源图片格式或大小非法时，页面应展示后端错误提示，不产生重复任务。
- 当前 retry/cancel 尚未开放，任务操作区不展示重试和取消入口。

### 4.2 MVP-B：文生视频

作为第二个视频能力。文生视频不依赖源图，但 prompt 稳定性更依赖模型能力和参数约束。

最小输入：

- 提示词。
- 负面提示词，可选。
- 时长。
- 比例。
- 清晰度或质量档位。
- 风格，可选。

### 4.3 MVP-C：Python Worker 最小样板

在 SiliconFlow 图生视频 MVP 跑通之后再做。不要求一开始承载复杂模型，可以先做一个可运行的 Worker skeleton：

- `/health` 健康检查。
- `/tasks` 提交任务。
- `/tasks/{workerTaskId}` 查询状态。
- 回调 Java `/ai/media/callback/{provider}`。
- 从 Java 提供的源文件 URL 下载文件。
- 将结果上传到 Java 文件服务或返回结果 URL。

这个 Worker 可以先用 mock 或轻量 FFmpeg 操作验证链路，后续再替换为真实模型。

### 4.4 MVP-D：人物替换预研

人物替换不建议作为第二阶段第一个可公开能力，但应在第二阶段完成设计和内部开关。

最小预研内容：

- 源视频素材校验。
- 目标人物素材校验。
- 授权确认。
- 敏感能力权限。
- 任务审计。
- 结果水印或追踪标记。
- Provider/Worker 选择。

## 5. 系统架构

```text
前端媒体页 / 聊天工具
        ↓
AI Media API
        ↓
AiMediaTaskService
        ↓
任务持久化 ai_media_task
        ↓
AiMediaExecutionService
        ↓
Provider Adapter
    ├── ThirdPartyVideoProviderAdapter
    └── PythonWorkerProviderAdapter
        ↓
第三方 API / Python Worker
        ↓
统一回调或状态轮询
        ↓
文件服务保存结果
        ↓
媒体资产与任务历史
```

### 5.1 Java 负责

- 用户、租户、权限、菜单和角色。
- 任务创建、状态机、重试、取消。
- Provider 选择、模型配置、API Key 读取。
- 输入校验、素材归属校验、敏感操作确认。
- 回调验签、幂等更新、任务审计。
- 文件服务集成和结果引用。
- 前端 API、聊天工具、后续工作流节点。

### 5.2 Provider 负责

- 第三方视频 API 提交。
- 第三方任务 ID 映射。
- 第三方状态查询。
- 第三方回调解析。
- Provider 特定参数转换。

### 5.3 Python Worker 负责

- FFmpeg、PyTorch、CUDA、本地模型权重和推理环境。
- 本地视频生成、人物替换、语音转换等模型任务。
- Worker 内部任务执行和进度上报。
- 结果文件上传或结果 URL 返回。

## 6. 数据模型规划

### 6.1 扩展 `ai_media_task`

继续复用第一阶段主任务表，新增或确认以下字段：

- `task_type`：`IMAGE_TO_VIDEO`、`TEXT_TO_VIDEO`、`PERSON_REPLACE`、`VOICE_CONVERSION` 等。
- `execution_mode`：`SYNC`、`ASYNC_PROVIDER`、`PYTHON_WORKER`。
- `provider_task_id`：第三方或 Worker 任务 ID。
- `callback_url`：提交给 Provider/Worker 的回调地址。
- `callback_status`：回调处理状态。
- `retry_count`：重试次数。
- `cancel_requested`：用户是否请求取消。
- `cost_units`：消耗额度或估算成本。

如现有字段已能容纳，不强制新增字段；可先放入 `input_json` / `output_json`，但状态机、Provider 任务 ID、重试次数建议成为明确字段。

### 6.2 新增媒体资产视图

建议新增 `ai_media_asset`，或者先以视图/查询聚合实现。

核心字段：

- `id`
- `task_id`
- `asset_type`：`SOURCE_IMAGE`、`SOURCE_VIDEO`、`RESULT_VIDEO`、`RESULT_AUDIO`、`WATERMARKED_RESULT`
- `file_id`
- `url`
- `mime_type`
- `duration`
- `width`
- `height`
- `size`
- `expire_time`
- `user_id`
- `tenant_id`

如果第二阶段想控制改动，可以先不建表，先把这些元数据规范写入 `input_json` / `output_json`；但一旦要做资产列表、清理、审计和复用，建议建表。

### 6.3 新增回调日志

建议新增 `ai_media_callback_log`。

用途：

- 记录 Provider/Worker 原始回调。
- 支持幂等排查。
- 追踪伪造、重复、乱序回调。
- 保存验签结果和处理结果。

## 7. 状态机

第二阶段建议统一状态：

```text
CREATED
  ↓
QUEUED
  ↓
SUBMITTED
  ↓
RUNNING
  ├── CANCELLING → CANCELLED
  ├── FAILED
  └── SUCCEEDED

FAILED → RETRYING → QUEUED
```

状态含义：

- `CREATED`：任务记录已创建，尚未提交。
- `QUEUED`：等待执行。
- `SUBMITTED`：已提交给 Provider/Worker，拿到外部任务 ID。
- `RUNNING`：Provider/Worker 已开始处理。
- `SUCCEEDED`：结果已保存到文件服务。
- `FAILED`：任务失败，保留错误码和错误消息。
- `RETRYING`：系统或用户触发重试。
- `CANCELLING`：已请求取消，等待外部确认。
- `CANCELLED`：任务已取消。

## 8. Provider Adapter 设计

### 8.1 通用接口

```java
public interface AiMediaProviderAdapter {

    boolean supports(String taskType, String provider, String model);

    AiMediaSubmitResult submit(AiMediaTaskDO task);

    AiMediaQueryResult query(AiMediaTaskDO task);

    AiMediaCancelResult cancel(AiMediaTaskDO task);

    AiMediaCallbackResult handleCallback(String provider, String payload, Map<String, String> headers);

}
```

第一阶段 TTS 可继续同步执行；第二阶段视频 Provider 必须至少支持 `submit` + `query`，如果 Provider 支持回调，再实现 `handleCallback`。

当前代码为了兼容 Phase 1 TTS，没有立即拆出全新的 Provider 层级；而是在现有 `AiMediaProviderAdapter` 上增加了默认异步方法：

- `submit(AiMediaTaskDO task)`
- `query(AiMediaTaskDO task)`
- `cancel(AiMediaTaskDO task)`
- `handleCallback(AiMediaTaskDO task, Object payload)`

这样 TTS 同步 `execute` 不受影响，视频 Provider 可以先使用异步方法。等 retry/cancel/callback 和 Python Worker 都进入实现后，再评估是否抽出独立 `AiMediaExecutionService` 和更强类型的结果对象。

### 8.2 Provider 选择原则

第一批只选一个视频 Provider，选择标准：

- 支持图生视频。
- 支持任务查询，最好支持回调。
- 支持 URL 输入或文件上传。
- API 文档稳定。
- 有可控费用和限流说明。
- 支持常见比例和短视频时长。

Provider 未最终确定前，代码层应通过 Adapter 接口隔离，不要把特定 Provider 字段散落到 controller/service。

## 9. Python Worker 契约

当前实现结论：Python Worker 不阻塞 SiliconFlow MVP，但 Phase 2 已在主仓加入最小 `yudao-ai-worker` skeleton，用于联调 Java 的 Worker Adapter、回调、轮询和结果交付。该 skeleton 只依赖 Python 标准库，适合作为 mock/轻量 Worker；真实本地模型或 FFmpeg 后处理仍建议在内测时替换实现。

Java 配置默认关闭：

```yaml
yudao:
  ai:
    media:
      worker:
        enable: false
        base-url:
        auth-token:
        callback-url:
        request-timeout-millis: 30000
        poll-limit: 20
```

本地 mock Worker 启动：

```bash
python3 yudao-ai-worker/worker.py --host 127.0.0.1 --port 8099 --token worker-secret
```

推荐 XXL-Job：`aiPythonWorkerMediaSyncJob`，按 30-60 秒间隔调度。

### 9.1 提交任务

```json
{
  "taskId": "10001",
  "taskType": "IMAGE_TO_VIDEO",
  "model": "worker-demo",
  "input": {
    "prompt": "camera slowly moves forward",
    "duration": 5,
    "ratio": "16:9"
  },
  "sourceFiles": [
    {
      "fileId": "123",
      "url": "https://example.com/source.png",
      "mimeType": "image/png"
    }
  ],
  "callbackUrl": "https://java.example.com/admin-api/ai/media/task/callback/python-worker",
  "tenantId": "1",
  "userId": "100",
  "traceId": "abc"
}
```

### 9.2 提交响应

```json
{
  "accepted": true,
  "workerTaskId": "worker-789",
  "estimatedSeconds": 120,
  "message": "accepted"
}
```

### 9.3 状态/回调

```json
{
  "taskId": "10001",
  "workerTaskId": "worker-789",
  "status": "RUNNING",
  "progress": 60,
  "stage": "rendering",
  "outputs": [],
  "errorCode": null,
  "errorMessage": null
}
```

完成时：

```json
{
  "taskId": "10001",
  "workerTaskId": "worker-789",
  "status": "SUCCEEDED",
  "progress": 100,
  "stage": "done",
  "outputs": [
    {
      "name": "result.mp4",
      "url": "https://example.com/result.mp4",
      "mimeType": "video/mp4"
    }
  ]
}
```

## 10. 前端规划

### 10.1 视频生成页

第一版页面建议简单：

- 模式切换：图生视频 / 文生视频。
- Prompt 输入。
- 源图上传或选择已有图片。
- 参数：比例、时长、质量档位、运动强度。
- 生成按钮。
- 当前任务状态。
- 视频预览。
- 历史任务列表。

### 10.2 媒体任务中心

统一展示：

- 任务类型。
- 状态。
- Provider / 模型。
- 创建时间。
- 耗时。
- 错误原因。
- 结果预览。
- 重试/取消/下载。

### 10.3 媒体资产页

当前第二阶段先提供任务资产聚合 API，不立刻新增 `ai_media_asset` 表。后续如果需要独立资产页，则提供：

- 源图、源视频、生成结果筛选。
- 按任务、用户、Provider、类型查询。
- 预览、下载、复制链接。
- 删除或过期提示。

## 11. 安全与合规

第二阶段必须把敏感能力前置设计好。

### 11.1 通用安全

- 上传文件类型白名单。
- 文件大小限制。
- 源文件归属校验。
- 私有 URL 下载防护。
- 结果文件访问权限。
- API Key 脱敏和隔离。
- Provider 回调验签或共享密钥。
- 回调幂等处理。

### 11.2 人物替换安全

- 系统级功能开关。
- 租户级功能开关。
- 角色权限。
- 用户显式确认。
- 素材归属校验。
- 任务审计。
- 结果水印或追踪标识。
- 默认不开放给普通用户。

当前已落地通用治理服务 `AiSensitiveMediaControlService`，人物替换、本地声音克隆、本地语音转换均默认关闭：

```yaml
yudao:
  ai:
    media:
      sensitive:
        person-replacement-enable: false
        local-voice-clone-enable: false
        local-voice-conversion-enable: false
        person-replacement-tenant-ids:
        local-voice-clone-tenant-ids:
        local-voice-conversion-tenant-ids:
        require-watermark: true
        audit-enable: true
```

该治理层统一检查权限、显式确认、项目文件归属、MIME 白名单，并向 Worker payload 注入 `watermark.enabled` 和 `watermark.traceId`。

### 11.3 语音资产安全

- 音色授权确认。
- 样本来源记录。
- 音色复用范围。
- 删除/禁用音色后禁止继续使用。
- 生成结果可追踪到音色和任务。

## 12. 配额与成本

第二阶段建议加入最小配额模型：

- 用户每日任务数。
- 租户每日任务数。
- 并发任务数。
- 单任务最大时长。
- 单文件最大大小。
- Provider 每日预算或额度。
- 失败任务是否返还额度。
- 管理端成本统计。

第一版可以只做硬限制和计数，不必立刻做复杂计费。

## 13. 里程碑

### 里程碑 A：异步任务底座

- 扩展媒体任务状态机。
- 增加提交、查询、取消、重试服务。
- 增加回调入口和回调日志。
- 增加任务执行器。
- 前端任务中心支持运行中任务轮询。

### 里程碑 B：图生视频 MVP

- 定义图生视频请求/响应 VO。（后端已完成）
- 接入一个视频 Provider。（SiliconFlow 后端已完成）
- 支持源图上传/选择。（前端通过上传组件获得项目文件 URL，后端校验项目文件归属并在必要时对本地/内网文件 URL 使用 base64 保底）
- 支持提交任务、查询进度、保存结果。（后端已完成）
- 前端视频生成页完成图生视频闭环。（已完成基础闭环：提交、轮询、成功预览、失败展示、重试、取消）

### 里程碑 C：文生视频

- 定义文生视频请求/响应 VO。
- 复用视频 Provider Adapter。
- 补充 prompt 参数校验和默认参数。
- 前端开放文生视频模式。

### 里程碑 D：Python Worker Skeleton

- 建立 Worker HTTP 契约。
- Java 增加 PythonWorkerProviderAdapter。
- Worker 支持 mock/轻量任务。
- 完成任务提交、回调、结果保存闭环。

### 里程碑 E：媒体资产与治理

- 建立媒体资产记录或聚合视图。
- 增加资产列表、预览、下载、清理。
- 增加配额统计和成本记录。
- 增加敏感能力审计。

### 里程碑 F：人物替换预研

- 完成 API 和前端内部页。
- 默认关闭功能。
- 完成素材校验、授权确认、审计。
- 可通过 Provider 或 Worker 跑内部验证。

## 14. 验收标准

第二阶段完成时，应满足：

- 用户可以提交图生视频任务，并通过任务中心看到进度、结果和失败原因。
- 至少一个视频 Provider 完成真实 API 或稳定 mock 验收。
- 视频结果保存到文件服务，任务输出记录 `fileId` 和 `url`。
- 任务支持失败记录、重试和取消。
- Provider/Worker 回调具备幂等处理和日志。
- Python Worker skeleton 能跑通提交、进度、完成、失败四种路径。
- 媒体任务中心可统一查看 TTS、视频和后续媒体任务。
- 人物替换等敏感能力默认关闭，有权限、确认和审计设计。

## 15. 第三阶段方向

第三阶段建议做“创作编排和 Agent 化”，不要在第二阶段过早展开。

可能方向：

- Chat Agent 调用视频、TTS、图片、音乐工具。
- 工作流节点支持媒体任务：生成脚本、生成图片、图生视频、生成配音、合成字幕。
- 多步骤创作模板：短视频、广告、课程片段、商品展示。
- 任务编排：串行、并行、失败回滚、人工确认节点。
- MCP 工具开放给外部 Agent。
- 成本预算、租户套餐、内容审核流。
- 更完整的媒体资产库和素材复用。

第三阶段开始的前提：至少一个视频能力、媒体资产治理和任务状态机已经稳定；Worker 能力可先是 mock/skeleton，但不能让 Agent 直接调用 Provider 或 Worker。Agent/Workflow 只能调用 Java 媒体任务服务，并沿用权限、租户、配额、确认、审计和状态查询边界。
