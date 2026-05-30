# AI 第一阶段云 API 与现有功能完善设计与进度

> 目标：第一阶段不进入本地模型训练、视频生成、人物替换、Python Worker 等重型能力开发；先把当前代码里已经存在的 AI 对话、写作、思维导图、绘图、音乐、TTS、知识库、工作流、模型配置、任务框架完善到可配置、可测试、可运维、可验收的状态。

## 0. 当前进度快照

更新时间：2026-05-29

第一阶段整体状态：**基本完成，进入收尾验收和文档同步阶段**。

已经完成的核心闭环：

- AI 对话、写作、思维导图、绘图、音乐、TTS、知识库、模型配置等既有功能已完成主要可用性补齐，并经过人工功能测试。
- 模型/API Key 管理已补充测试连接能力；图片模型测试连接采用“构造模型 + 跳过真实绘图”的方式，避免后台连通性测试消耗额度。
- AI 对话附件下载已加入远程资源下载防护，默认禁止内网/本机地址下载，降低 SSRF 风险；本地调试可通过 `application-local.yaml` 放开。
- TTS 已形成 `ai_media_task` 统一任务闭环，支持 Xiaomi MiMo 预置音色、音色克隆、文字设计音色三种云 API 模式，并保存生成音频到文件服务。
- 音色库已支持预置音色、克隆样本、设计提示词的管理、预览和模式切换；保存/更新时会清理无关字段，避免脏数据影响执行。
- TTS 配额锁已补充严格模式，生产默认严格失败，本地/单测可降级为 count 校验。
- 后端编译验证通过：`yudao-module-ai/yudao-module-ai-server` 在 JDK 17 下 `-DskipTests compile` 成功。

仍建议作为上线前收尾处理的事项：

- 轮换并移除仓库配置中出现过的真实或疑似真实密钥，生产密钥统一放入 Nacos、环境变量、数据库或本地私有配置。
- 对用户已测通的功能补一份轻量验收记录，包括使用的 provider、模型、是否真实扣费、失败截图或日志摘要。
- 生产知识库不要使用默认内存向量库，建议明确 Redis Stack、Qdrant 或 Milvus 的部署方案。
- 第二阶段开始前，先确定视频生成/人物替换/本地 Worker 的第一个 MVP 能力，避免同时铺开过多高成本方向。

## 1. 阶段边界

### 1.1 第一阶段要做

- 完善现有云 API 接入链路：`ai_api_key -> ai_model -> AiModelService -> AiModelFactory -> 业务功能`。
- 让后台配置、前端入口、后端执行、任务同步、错误提示、日志、测试验证形成闭环。
- 优先保证现有功能真实可用：AI 对话、AI 写作、AI 思维导图、AI 绘图、AI 音乐、AI TTS、AI 知识库、AI 工作流基础测试。
- 为后续视频生成、人物替换、本地语音/视频模型和 Python Worker 预留边界。

### 1.2 第一阶段不做

- 不做本地音色克隆模型训练或私有推理服务。
- 不做视频生成、人物替换、数字人、换脸、口型同步。
- 不接 Python Worker。
- 不训练或部署本地大模型。
- 不重构整套 AI 架构，只补齐当前框架的可用性短板。

```text
第一阶段
  ├─ 文本云能力：对话 / 写作 / 导图 / 工作流基础
  ├─ 图片云能力：普通绘图 / Midjourney Proxy
  ├─ 音频云能力：Suno 音乐 / Xiaomi MiMo TTS / MiMo 云音色克隆与设计
  ├─ 知识库能力：Embedding / 检索 / 可选 Rerank
  └─ 平台能力：模型配置 / 密钥检测 / 任务状态 / 日志 / 测试

第二阶段以后
  ├─ 本地音色克隆 / 语音转换 / 语音资产生产化
  ├─ 视频生成
  ├─ 人物替换
  ├─ Python Worker
  └─ 本地/私有模型推理
```

## 2. 当前代码完善度总览

| 功能 | 当前状态 | 第一阶段判断 | 主要缺口 |
| --- | --- | --- | --- |
| 模型/API Key 管理 | 已有后台 CRUD、模型工厂和测试连接 | 第一阶段已补齐主要能力 | 仍需生产密钥治理、默认模型体验继续打磨 |
| AI 对话 | 云模型工厂已覆盖多平台，附件下载已加安全防护 | 第一阶段核心已可验收 | 继续补端到端验收记录和多模态模型能力标识 |
| AI 写作 | 复用 Chat 模型 | 第一阶段核心已可验收 | 继续确认模板与异常提示 |
| AI 思维导图 | 复用 Chat 模型流式生成 | 第一阶段核心已可验收 | 继续关注输出格式稳定性 |
| AI 绘图 | 支持多云图像模型 + Midjourney Proxy，连接测试避免真实绘图消耗 | 第一阶段核心已可验收 | Midjourney 回调/同步配置需按环境确认 |
| AI 音乐 | Suno Proxy 链路已存在，并保留 Xiaomi MiMo 音乐/语音模式扩展 | 第一阶段核心已可验收 | Proxy 稳定性和合规风险需上线前确认 |
| TTS 语音合成 | Xiaomi MiMo TTS 已接入，支持预置音色、克隆、设计三种云模式 | 第一阶段核心已可验收 | 需真实 Key 的最终冒烟和计费/配额记录 |
| 知识库 | Embedding + VectorStore 已有 | 第一阶段增强已可用 | 默认 SimpleVectorStore 不适合生产，Rerank 未纳入后台模型管理 |
| AI 工作流 | Tinyflow 已有基础 | 第一阶段保留最小可用 | 目前只映射 TongYi/Ollama，后续建议统一复用 Chat 云模型 |
| 视频/人物替换 | 只有枚举和规划痕迹 | 不进第一阶段 | 第二阶段单独设计 |
| 音色克隆 | MiMo 云 TTS voiceclone 已纳入 TTS 模式；本地克隆未接 | 第一阶段只覆盖云 API 模式 | 第二阶段再做本地模型、样本治理和资产生产化 |

## 3. 核心架构整理

### 3.1 现有云 API 调用链

```text
后台配置
  │
  ├─ ai_api_key：平台、密钥、URL、状态
  │
  └─ ai_model：模型名、模型类型、默认模型、keyId、状态
       │
       ▼
AiModelService
       │
       ├─ getChatModel(modelId)
       ├─ getImageModel(modelId)
       ├─ getMidjourneyApi(modelId)
       ├─ getSunoApi()
       └─ getOrCreateVectorStore(embeddingModelId)
       │
       ▼
AiModelFactoryImpl
       │
       ├─ Spring AI ChatModel
       ├─ Spring AI ImageModel
       ├─ Spring AI EmbeddingModel
       ├─ MidjourneyApi
       └─ SunoApi
```

第一阶段重点不是推翻这条链路，而是让这条链路可观测、可测试、可配置、可恢复。

### 3.2 推荐的第一阶段目标态

```text
管理员配置 API Key / Model
       │
       ▼
后台点击“测试连接”
       │
       ├─ 成功：可设为默认模型
       └─ 失败：明确提示平台、URL、鉴权、模型名、配额或网络错误
       │
       ▼
用户使用 AI 功能
       │
       ├─ 自动选择对应类型默认模型
       ├─ 执行云 API
       ├─ 记录请求结果和失败原因
       └─ 前端展示可理解的状态
```

## 4. 逐功能设计与开发计划

## 4.1 模型与 API Key 管理

### 现状

- 后端已有 `/ai/api-key` 和 `/ai/model` 管理接口。
- `AiModelFactoryImpl` 已支持多平台 Chat、Image、Embedding、Midjourney、Suno。
- 模型类型已有 `CHAT、IMAGE、VOICE、VIDEO、EMBEDDING、RERANK`。

### 问题

- 管理端能新增配置，但缺少“这个配置是否真实可用”的主动检测。
- 默认模型缺失时，用户侧功能会在执行时才报错。
- 平台、模型类型、模型名之间没有足够强的能力约束，例如把视频模型类型配置出来并不代表视频功能可用。
- 生产环境不应依赖 `application.yaml` 里的示例密钥。

### 第一阶段要完善

- 增加 API Key/Model 连通性测试能力：
  - Chat：发送最小 prompt，校验能返回文本。
  - Image：发送最小图片生成请求或轻量 dry-run，如果平台无 dry-run，则提供跳过真实消耗的提示。
  - Embedding：发送短文本，校验向量维度返回。
  - Suno/Midjourney/TTS：检测 base URL、鉴权方式、必要字段，不强制生成昂贵任务。
- 增加默认模型检查：
  - 每个已启用功能启动或进入页面时，提示是否配置对应默认模型。
  - `CHAT` 默认模型至少一个。
  - `IMAGE` 默认模型至少一个。
  - `VOICE` 默认模型必须是 `XiaomiMiMo / mimo-v2.5-tts`。
  - `EMBEDDING` 默认模型用于知识库引导。
- 后台模型表单增加能力提示：
  - `VIDEO`、`RERANK` 类型标注“当前第一阶段暂不可直接执行”。
  - `Suno` 标注为代理服务，不是普通 Spring AI 模型。
  - `Midjourney` 标注需要 Proxy 和 notify-url。
- 清理配置策略：
  - 示例密钥从配置文件迁移到本地示例或文档说明。
  - 生产只通过后台数据库/Nacos/环境变量配置真实密钥。

### 验收

- 管理员能在后台看到每个模型是否可用。
- 没有默认模型时，用户功能页给出明确配置提示。
- 错误信息能区分：密钥为空、密钥错误、URL 不通、模型名错误、平台不支持、配额不足。

## 4.2 AI 对话

### 现状

- Chat 云模型支持较完整：通义、文心、DeepSeek、豆包、混元、硅基流动、智谱、MiniMax、Moonshot、星火、百川、OpenAI、AzureOpenAI、Anthropic、Gemini、Grok。
- 前端已有聊天页面、角色、会话、消息管理。

### 问题

- 真实可用性依赖后台模型配置。
- 多平台流式输出、推理内容、工具调用、文件输入能力可能不一致。
- 失败时需要更清楚地告诉用户是模型配置问题还是云服务问题。

### 第一阶段要完善

- 增加聊天模型冒烟测试清单：
  - 普通对话。
  - 流式响应。
  - 多轮上下文。
  - 角色 prompt。
  - 工具调用开关关闭/开启。
  - 知识库关联关闭/开启。
- 统一聊天错误处理：
  - 捕获平台鉴权、超时、模型不存在、上下文过长。
  - 前端消息气泡展示失败状态和可重试按钮。
- 默认模型引导：
  - 首次进入聊天页，如果无启用 `CHAT` 默认模型，跳出配置指引。
- 聊天日志完善：
  - 记录 modelId、platform、model、耗时、失败码。
  - 避免记录完整 API Key 和敏感输入。

### 验收

- 至少一个国内云模型和一个 OpenAI 兼容模型完成端到端测试。
- 聊天页能稳定完成新建会话、发送、流式展示、失败重试。

## 4.3 AI 写作

### 现状

- 写作功能复用 Chat 模型。
- 前端已有写作页面和写作管理页。

### 问题

- 写作任务的模型依赖和聊天一致，但用户感知上是独立功能。
- 写作模板、语言、语气、长度等参数需要确认最终 prompt 稳定。

### 第一阶段要完善

- 写作模型选择策略：
  - 默认使用 `CHAT` 默认模型。
  - 允许管理员配置写作推荐模型。
- 写作结果稳定性：
  - 确认撰写、回复两类模式的 prompt 模板。
  - 统一输出长度和语言约束。
- 写作历史：
  - 确认生成记录保存成功。
  - 失败记录可查看错误原因。
- 前端体验：
  - 生成中禁用重复提交。
  - 失败可重新生成。

### 验收

- 中文、英文两种语言至少各跑通一次。
- 撰写、回复两种类型可正常生成并保存记录。

## 4.4 AI 思维导图

### 现状

- 思维导图通过 Chat 模型流式生成。
- 后端已有 `/ai/mind-map/generate-stream`。

### 问题

- 导图对输出格式敏感，云模型一旦输出跑偏，前端渲染会失败。
- 需要校验不同模型对格式指令的服从度。

### 第一阶段要完善

- 固定导图输出协议：
  - 明确 Markdown、JSON 或前端需要的格式。
  - 服务端增加格式校验和失败提示。
- 增加模型适配建议：
  - 推荐使用服从格式较稳定的 Chat 模型。
  - 不推荐用推理模型直接生成严格格式，除非做后处理。
- 前端失败兜底：
  - 格式解析失败时展示原始文本和重新生成入口。

### 验收

- 使用默认 Chat 模型生成导图能被前端稳定渲染。
- 模型输出异常时页面不白屏。

## 4.5 AI 绘图

### 现状

- 普通 ImageModel 支持通义、文心、智谱、OpenAI、硅基流动、StableDiffusion。
- Midjourney 通过 Proxy API 接入，已有 imagine、notify、action、sync job。
- 前端已有普通绘图、DALL-E、StableDiffusion、Midjourney 等组件。

### 问题

- 并不是每个 `IMAGE` 模型都支持同一套参数：尺寸、风格、质量、数量等需要分平台处理。
- Midjourney 依赖外部 Proxy、回调地址、同步任务，配置复杂。
- 图片生成通常有成本，测试需要避免误触发大量任务。

### 第一阶段要完善

- 图像模型能力矩阵：
  - 平台支持的尺寸。
  - 是否支持负面提示词。
  - 是否支持风格、质量、数量。
  - 是否同步返回图片，还是异步任务。
- 请求参数校验：
  - 按平台限制尺寸、数量、模型名。
  - 前端根据模型平台动态显示可用参数。
- Midjourney 配置向导：
  - 必填：Proxy base-url、api-key、notify-url。
  - 如果 notify-url 不可公网访问，必须开启 `aiMidjourneySyncJob` 兜底。
  - action 按钮只在任务成功且按钮存在时展示。
- 图片文件保存：
  - 普通图像和 Midjourney 结果都应保存到统一文件服务。
  - 失败时记录外部图片 URL 和上传失败原因。

### 验收

- 普通图像模型至少跑通一个。
- Midjourney 至少完成提交任务、同步状态、保存结果、二次操作其中的可用路径。
- 绘图失败能在记录中看到失败原因。

## 4.6 AI 音乐

### 现状

- 音乐通过 `SunoApi` 对接 Suno Proxy。
- 后端已有生成接口和 `aiSunoSyncJob`。
- 前端已有音乐生成和音乐管理页面。

### 问题

- 当前 `SunoApi` 构造只使用 URL，`apiKey` 没有真正进入客户端鉴权逻辑。
- `application.yaml` 默认 base-url 是 `http://127.0.0.1:3001`，真实环境必须部署 Suno Proxy。
- 任务结果依赖同步任务拉取。

### 第一阶段要完善

- 明确 Suno 接入形态：
  - 定义为“兼容 Suno Proxy 服务”，不是直接官方云 API。
  - 后台 API Key 配置中的 URL 是代理地址。
  - 如代理需要 token，补充 header 或配置字段策略。
- 增加代理健康检查：
  - 检查 base-url 是否可访问。
  - 检查生成接口和任务查询接口是否符合预期。
- 完善同步任务：
  - 文档说明必须配置 `aiSunoSyncJob`。
  - 后台任务页能看到同步失败日志。
- 音乐生成参数校验：
  - 歌词模式、描述模式分别校验必填字段。
  - 控制重复提交和并发数量。

### 验收

- Suno Proxy 可用时，歌词模式和描述模式都能生成任务。
- 同步任务能把任务状态更新为完成，并保存 audioUrl、videoUrl、imageUrl。
- Proxy 不可用时，前端提示清楚。

## 4.7 TTS 语音合成

### 现状

- 已有统一 `ai_media_task` 任务基础。
- 已支持 `XiaomiMiMo / mimo-v2.5-tts`、`mimo-v2.5-tts-voiceclone`、`mimo-v2.5-tts-voicedesign` 三类云 TTS 模型。
- 已有音色库、TTS 页面、媒体任务管理相关改动。
- 已支持生成结果保存到文件服务，并在媒体任务输出中记录文件引用。

### 问题

- MiMo 云音色克隆/设计已接入，但这仍然是云 TTS 能力，不等同于第二阶段要做的本地语音模型训练、声纹资产生产化或语音转换平台。
- `voice`、`format`、`voiceMode`、样本格式等选项需要继续保持字典或 provider 配置可维护。
- 上线前需要用真实云 API 记录最终验收：返回格式、错误格式、音频保存、额度消耗和限流。

### 第一阶段已完善

- TTS 配置向导：
  - 创建 API Key：平台 `XiaomiMiMo`，URL 可空、`https://api.xiaomimimo.com` 或 `https://api.xiaomimimo.com/v1`。
  - 创建模型：类型 `VOICE`，模型名按模式选择 `mimo-v2.5-tts`、`mimo-v2.5-tts-voiceclone`、`mimo-v2.5-tts-voicedesign`。
  - 配置权限：`ai:media:tts:create` 和任务查看权限。
- 前端 TTS 页面完善：
  - 文本输入、风格提示、音色模式、预置音色、克隆样本、设计提示词、格式。
  - 生成中状态、生成结果播放器、下载入口。
  - 历史任务列表。
- 服务端任务完善：
  - 创建任务、执行、保存文件、更新状态。
  - 明确失败状态：鉴权失败、响应无音频、文件保存失败、超时。
  - 音色克隆要求确认已获得声音授权，并限制样本类型与大小。
  - 生产默认开启 TTS 配额锁严格模式，避免并发绕过用量限制。
- 字典/配置：
  - 音色列表先配置为可维护数据，不硬编码在页面。
  - 格式限制为 provider 支持值。

### 验收

- 使用真实小米 MiMo Key 能生成一段音频。
- 任务成功后文件服务返回可播放 URL。
- 失败时 `ai_media_task` 保留错误码和错误消息。
- 使用预置音色、克隆样本、文字设计音色分别完成一次验收，且样本授权确认、样本格式、样本大小校验生效。

## 4.8 知识库

### 现状

- 知识库支持文档、分段、Embedding、检索。
- Embedding 支持通义、文心、智谱、MiniMax、OpenAI、AzureOpenAI、Ollama。
- VectorStore 代码支持 Simple、Qdrant、Redis、Milvus，但当前服务默认使用 `SimpleVectorStore`。
- Rerank 通过 Spring 可选 bean 注入，未走后台模型管理。

### 问题

- `SimpleVectorStore` 更适合本地开发，不适合生产多实例和长期数据。
- Rerank 不是统一的 `ai_model` 动态配置。
- 文档处理失败、向量写入失败、检索为空等状态需要更清楚。

### 第一阶段要完善

- 明确第一阶段知识库模式：
  - 开发环境可继续使用 `SimpleVectorStore`。
  - 生产建议优先 Redis/Qdrant/Milvus 之一，并写入配置向导。
- VectorStore 选择配置化：
  - 不要通过注释切换代码。
  - 增加配置项或后台项选择向量库实现。
- Embedding 模型测试：
  - 创建知识库前测试 embeddingModelId 是否可用。
  - 校验文本能成功生成向量。
- Rerank 策略：
  - 第一阶段可保留 Spring 配置方式。
  - 文档明确：Rerank 是可选增强，不是核心阻塞项。
  - 后续再纳入后台模型管理。
- 检索可观测：
  - 返回命中的段落、相似度、是否经过 rerank。
  - 管理端提供检索测试页。

### 验收

- 创建知识库、上传/新增文档、切片、写入向量、搜索段落全链路跑通。
- 关闭 Rerank 时检索可用。
- Embedding 模型配置错误时能明确提示。

## 4.9 AI 工作流

### 现状

- 工作流已有 CRUD 和测试接口。
- Tinyflow LLM Provider 当前只映射 `TongYi` 和 `Ollama`。

### 问题

- 其他 Chat 云模型虽然在聊天中可用，但工作流里不能通用。
- 工作流执行失败时，节点级错误和模型错误需要展示。

### 第一阶段要完善

- 最小目标：
  - 保证 TongYi 工作流节点可用。
  - 对不支持的平台给出明确提示，而不是静默无模型。
- 推荐目标：
  - 实现统一 `SpringAiLlm` 适配器，让 Tinyflow 复用 `AiModelService.getChatModel(modelId)`。
  - 这样所有已支持 ChatModel 的云平台都能用于工作流。
- 前端设计器：
  - 模型下拉只展示工作流当前可执行的平台。
  - 测试运行展示节点输出和失败节点。

### 验收

- 使用 TongYi 创建一个简单工作流并测试成功。
- 如果完成统一适配器，则 DeepSeek/OpenAI 兼容模型也能跑通简单工作流。
- 不支持的平台有明确错误提示。

## 4.10 任务、日志与运维

### 现状

- Midjourney、Suno 有独立同步 Job。
- TTS 使用 `ai_media_task`。
- 聊天、绘图、音乐、写作分别有各自记录表。

### 问题

- 各功能任务状态模型不完全一致。
- 外部云 API 错误不一定能直接定位。
- 定时任务是否配置，当前依赖运维知道。

### 第一阶段要完善

- 统一外部调用日志字段：
  - platform、model、modelId、apiKeyId、requestId、耗时、状态、错误码。
  - 不记录密钥原文。
- 同步任务向导：
  - `aiMidjourneySyncJob`：Midjourney 回调不可用时必须启用。
  - `aiSunoSyncJob`：Suno 任务状态更新必须启用。
- 管理端健康检查：
  - 模型配置健康。
  - Proxy 服务健康。
  - 文件服务健康。
  - 定时任务最近执行时间。
- 用户侧失败提示：
  - “模型未配置”
  - “云服务鉴权失败”
  - “云服务超时”
  - “文件保存失败”
  - “能力暂未开放”

## 5. 配置向导

### 5.1 最小可用配置顺序

1. 启用 AI 模块，确认 `yudao-server` 已引入 `yudao-module-ai-server`。
2. 执行 AI 相关 SQL，确认字典、菜单、权限、AI 表存在。
3. 配置文件服务，保证图片、音频结果能保存。
4. 后台新增 API Key。
5. 后台新增 Model，并设置对应类型默认模型。
6. 对每个模型执行连接测试。
7. 配置必要定时任务。
8. 在用户功能页做端到端冒烟。

### 5.2 推荐默认模型配置

| 类型 | 用途 | 推荐第一阶段配置 |
| --- | --- | --- |
| CHAT | 对话、写作、导图 | 选择一个稳定低成本云模型作为默认 |
| IMAGE | 普通绘图 | 选择一个普通图像云模型作为默认 |
| VOICE | TTS | `XiaomiMiMo / mimo-v2.5-tts` |
| EMBEDDING | 知识库 | 选择一个 embedding 云模型 |
| RERANK | 知识库增强 | 第一阶段可选，不作为阻塞 |
| VIDEO | 视频 | 不配置或标注暂不可用 |

### 5.3 Proxy 服务配置

| 服务 | 配置项 | 要点 |
| --- | --- | --- |
| Midjourney Proxy | base-url、api-key、notify-url | notify-url 需要公网可访问；否则启用同步 Job |
| Suno Proxy | base-url、可选鉴权 | 默认本地 `127.0.0.1:3001` 只适合开发 |

## 6. 第一阶段实施计划

### 里程碑 A：配置与健康检查

- API Key/Model 连通性测试。
- 默认模型检查。
- 后台配置向导文案。
- 示例密钥治理。

验收：管理员能知道哪些模型可用，用户不会因缺默认模型而踩空。

### 里程碑 B：文本功能稳定

- 对话端到端测试。
- 写作端到端测试。
- 思维导图格式校验。
- 统一错误提示。

验收：对话、写作、导图稳定使用同一个 Chat 默认模型。

### 里程碑 C：图片与音乐稳定

- 图片能力矩阵和参数校验。
- Midjourney 回调/同步向导。
- Suno Proxy 健康检查和同步任务验证。

验收：普通绘图、Midjourney、Suno 至少各跑通一条完整路径。

### 里程碑 D：TTS 与媒体任务稳定

- 小米 MiMo TTS 真实 API 测试。
- TTS 页面与任务管理完善。
- 音色/格式配置化。
- 文件保存和错误状态验证。

验收：能生成可播放音频，失败能定位原因。

### 里程碑 E：知识库与工作流补齐

- Embedding 模型测试。
- VectorStore 选择配置化方案。
- 知识库检索测试页。
- 工作流至少 TongYi 可用，推荐统一到 Spring AI ChatModel。

验收：知识库从文档到检索全链路可用；工作流测试有清楚成功/失败结果。

## 7. 测试清单

### 7.1 后端测试

- `AiModelFactoryImpl`：各平台模型创建参数。
- `AiModelServiceImpl`：模型、Key、默认模型校验。
- Chat：普通响应、流式响应、异常响应。
- Image：普通绘图、Midjourney 提交、同步、回调。
- Music：Suno 生成、同步、失败。
- Media TTS：任务创建、Xiaomi 请求、音频解析、文件保存失败。
- Knowledge：Embedding、向量写入、搜索、Rerank 开关。
- Workflow：支持平台、非支持平台、节点失败。

### 7.2 前端/接口冒烟

- 模型配置页：新增 Key、新增 Model、设默认、测试连接。
- 聊天页：新会话、发送、流式、失败重试。
- 写作页：撰写、回复、记录。
- 导图页：生成、解析失败兜底。
- 绘图页：普通模型、Midjourney、历史记录。
- 音乐页：歌词模式、描述模式、同步完成展示。
- TTS 页：生成、播放、历史任务。
- 知识库页：创建、上传、切片、搜索。
- 工作流页：创建、测试、失败展示。

## 8. 风险与处理

| 风险 | 影响 | 第一阶段处理 |
| --- | --- | --- |
| 云 API 密钥过期或额度不足 | 功能不可用 | 增加连接测试和错误分类 |
| 不同平台参数差异大 | 前端请求失败 | 建立能力矩阵，动态参数表单 |
| Midjourney/Suno Proxy 不稳定 | 异步任务卡住 | 健康检查 + 同步 Job + 超时失败 |
| SimpleVectorStore 不适合生产 | 知识库数据丢失或多实例不一致 | 文档明确生产配置 Redis/Qdrant/Milvus，后续配置化 |
| 工作流只支持少量模型 | 用户误以为所有模型都可跑 | 第一阶段先限制可选模型，推荐实现统一适配 |
| 视频/本地音色模型需求混入 | 阶段失焦 | 页面和文档标注“第二阶段能力”；MiMo 云 TTS 音色克隆/设计作为第一阶段 TTS 模式管理 |

## 9. 第一阶段完成定义

第一阶段完成时，应满足：

- 管理员能在后台完成云 API Key、模型、默认模型配置，并能测试连接。
- 用户能真实使用 AI 对话、写作、导图、绘图、音乐、TTS。
- 知识库能完成文档导入、向量化和检索。
- 工作流至少有一个云模型路径可测试成功。
- Midjourney 和 Suno 的代理、回调、同步任务有明确配置说明。
- 每个功能失败时都有可读错误，不再只表现为页面无响应或任务卡住。
- 视频生成、人物替换、本地语音模型和 Python Worker 被明确标记为第二阶段，不影响第一阶段验收。

## 10. 文档与当前实现偏差校准

本节用于把规划和当前代码实现对齐，避免把“已存在”“半存在”“还没做”混在一起。

| 项目 | 文档判断 | 当前代码事实 | 偏差/修正 |
| --- | --- | --- | --- |
| API Key 管理 | 需要连通性测试 | 只有 CRUD 表单，暂无测试连接按钮 | 文档准确，需要实现 |
| 模型默认值 | 需要默认模型检查 | 后端已有 `getRequiredDefaultModel(type)`，按排序取首个启用模型 | 不是显式“设默认”，需要后台标识或清晰排序规则 |
| 模型能力约束 | 需要能力提示 | 模型表单允许任意平台 + 任意类型组合 | 文档准确，需要限制或提示 |
| Chat 云模型 | 支持多平台 | `AiModelFactoryImpl` 已覆盖大量 Chat 平台 | 文档准确，重点是配置和冒烟 |
| 写作/导图 | 复用 Chat | 代码确实使用 `CHAT` 默认模型或指定模型 | 文档准确 |
| 图片生成 | 支持多云模型 | 已支持 TongYi、YiYan、ZhiPu、OpenAI、SiliconFlow、StableDiffusion、Midjourney Proxy | 文档准确，但 OpenAI 图像模型要注意新旧模型兼容 |
| OpenAI 图片 | 可配置 OpenAI 图片模型 | 当前种子数据偏向 `dall-e-2/dall-e-3`，前端也有 DALL-E 组件 | 需新增“GPT Image 兼容性评估/迁移”任务 |
| 音乐 | Suno Proxy | 代码只对接 Suno Proxy，非官方 Suno 云 API | 文档准确，但需要强调合规和稳定性风险 |
| Suno API Key | 需要 API Key 策略 | `getOrCreateSunoApi(apiKey, url)` 接收 key，但 `SunoApi` 构造只用 URL | 需要修正代码或文档明确“当前 key 不生效” |
| TTS | Xiaomi MiMo | 代码支持 `mimo-v2.5-tts`、`mimo-v2.5-tts-voiceclone`、`mimo-v2.5-tts-voicedesign` | 文档已修正为云 TTS 三模式 |
| 音色克隆 | 本地模型不进第一阶段 | MiMo 云 voiceclone 已通过 TTS/音色库链路接入；本地模型、语音资产生产化未接 | 第一阶段边界修正为“只做云 API 模式，不做本地推理/训练” |
| 知识库向量库 | 需要生产化 | 代码硬编码返回 `SimpleVectorStore`，其他向量库代码被注释 | 文档准确，需要配置化 |
| Rerank | 可选增强 | 不是后台模型管理，依赖 Spring 配置注入 | 文档准确 |
| 工作流 | 半接入 | Tinyflow 只支持 TongYi/Ollama | 文档准确，需要至少限制可选模型或做统一适配 |
| 视频 | 不进第一阶段 | 只有模型类型和规划痕迹，无执行链路 | 文档准确 |
| 明文密钥 | 文档提到清理 | `application.yaml` 中存在多处示例/疑似真实密钥 | 优先级上调为 P0：轮换、清理、改示例配置 |

## 11. 第一阶段云服务商与模型选择清单

### 11.1 最小采购组合

如果目标是尽快把第一阶段跑通，建议先不要每个平台都买。最小组合如下：

| 用途 | 推荐服务 | 为什么 | 你需要准备 |
| --- | --- | --- | --- |
| 对话/写作/导图 | DeepSeek 或阿里百炼 DashScope | 成本较低，代码已支持，适合中文 | 开通账号、创建 API Key、充值小额额度 |
| 知识库 Embedding | 阿里百炼 DashScope `text-embedding-v3` 或 `text-embedding-v4` | 代码已有 TongYi Embedding；中文 RAG 够用 | 开通百炼，确认 embedding 模型可调用 |
| 普通绘图 | 阿里百炼 Wanx 或 Stability AI | 代码已有 TongYi/Stability 图像入口 | 开通图像生成模型，充值测试额度 |
| TTS 语音合成 | Xiaomi MiMo TTS 系列 | 当前已实现 TTS Provider 和音色库链路 | 申请 MiMo API Key，按需开通普通 TTS、voiceclone、voicedesign |
| 文件存储 | 项目现有 infra 文件服务 | 图片/音频结果必须保存 | 确认本地/OSS/S3 配置可访问 |

最小闭环可以先不买 Midjourney、Suno、OpenAI、Claude、Gemini。第一阶段先保证“能用”，再扩服务商。

### 11.2 推荐服务商分层

| 层级 | 服务商/服务 | 覆盖功能 | 当前代码接入程度 | 是否建议第一阶段购买 |
| --- | --- | --- | --- | --- |
| P0 必需 | 一个 Chat 服务：DeepSeek 或 DashScope | 对话、写作、导图 | 已接 | 必需 |
| P0 必需 | 一个 Embedding 服务：DashScope 或 OpenAI | 知识库 | 已接 | 必需，如果要做知识库 |
| P0 必需 | Xiaomi MiMo | TTS | 已接 `mimo-v2.5-tts`、`mimo-v2.5-tts-voiceclone`、`mimo-v2.5-tts-voicedesign` | 必需，如果要做语音合成 |
| P1 建议 | DashScope Wanx / 智谱 CogView / Stability AI | 普通绘图 | 已接部分模型 | 建议选一个 |
| P1 建议 | Redis/Qdrant/Milvus | 生产知识库向量库 | 代码有但未启用 | 生产环境建议 |
| P2 可选 | Suno + Suno Proxy | 音乐生成 | 只接 Proxy | 可选，需接受非官方代理风险 |
| P2 可选 | Midjourney Proxy | 高质量绘图 | 只接 Proxy | 可选，配置复杂 |
| P2 可选 | OpenAI / Azure OpenAI | Chat、Embedding、Image | 已接 | 海外或高质量场景再买 |
| P3 暂不买 | 视频模型 API | 视频生成 | 无执行链路 | 第二阶段 |
| P3 暂不买 | 本地音色克隆模型/语音转换模型 | 本地语音推理 | 无 Python Worker 和本地模型执行链路 | 第二阶段 |

### 11.3 按功能可选服务商

| 功能 | 当前代码可选平台 | 第一阶段推荐 |
| --- | --- | --- |
| AI 对话 | TongYi、YiYan、DeepSeek、ZhiPu、XingHuo、DouBao、HunYuan、SiliconFlow、MiniMax、Moonshot、BaiChuan、OpenAI、AzureOpenAI、Anthropic、Gemini、Grok、Ollama | 先选 DeepSeek 或 TongYi；后续再扩 |
| AI 写作 | 复用 Chat 平台 | 同 Chat |
| AI 思维导图 | 复用 Chat 平台 | 同 Chat，优先选格式服从好的模型 |
| AI 绘图 | TongYi、YiYan、ZhiPu、OpenAI、SiliconFlow、StableDiffusion、Midjourney Proxy | 先选 TongYi Wanx 或 Stability；Midjourney 先作为可选 |
| AI 音乐 | Suno Proxy | 可选，需部署/购买 Proxy 能力 |
| TTS | XiaomiMiMo | 普通合成选 `mimo-v2.5-tts`；需要云音色克隆/设计时再开 `mimo-v2.5-tts-voiceclone`、`mimo-v2.5-tts-voicedesign` |
| 知识库 Embedding | TongYi、YiYan、ZhiPu、MiniMax、OpenAI、AzureOpenAI、Ollama | 先选 TongYi `text-embedding-v3/v4` |
| Rerank | Spring 配置注入，偏 DashScope | 可选，先不阻塞 |
| 工作流 | 当前只 TongYi、Ollama | 第一阶段先用 TongYi |
| 视频/人物替换 | 无可执行 Provider | 暂不买 |
| 音色克隆 | XiaomiMiMo 云 voiceclone 已可走 TTS；本地模型无执行 Provider | 云能力按需开通；本地模型第二阶段再做 |

## 12. 你需要跟进申请/购买的清单

### 12.1 必须先做

- 申请或确认一个主力 Chat API Key：
  - 推荐一：DeepSeek，模型先用 `deepseek-chat`。
  - 推荐二：阿里百炼 DashScope，模型先用 `qwen-plus`、`qwen-turbo` 或项目已配置过的 Qwen 模型。
- 申请阿里百炼 DashScope API Key：
  - 用于 Embedding：优先 `text-embedding-v3`，新项目可评估 `text-embedding-v4`。
  - 用于绘图：优先 Wanx 系列，先确认当前 Spring AI/代码支持的模型 ID。
- 申请 Xiaomi MiMo API Key：
  - 普通 TTS 至少开通 `mimo-v2.5-tts`。
  - 如要验收音色库里的克隆/设计模式，再开通 `mimo-v2.5-tts-voiceclone`、`mimo-v2.5-tts-voicedesign`。
- 准备文件存储：
  - 本地开发可用本地文件服务。
  - 演示/生产建议 OSS/S3/MinIO，确保生成图片和音频能公网或内网访问。
- 轮换当前仓库配置里出现过的疑似真实密钥：
  - 包括 `yudao-server/src/main/resources/application.yaml`。
  - 包括 `yudao-module-ai/yudao-module-ai-server/src/main/resources/application.yaml`。
  - 轮换后把真实 Key 放到本地配置/Nacos/环境变量/数据库，不提交仓库。

### 12.2 根据是否要上线对应功能再做

- 如果要上线音乐：
  - 准备 Suno 账号。
  - 部署 `gcui-art/suno-api` 兼容代理，配置 `SUNO_COOKIE`。
  - 给 Java 后台配置 `Suno` 平台 API Key 记录，其中 URL 指向代理地址。
  - 接受它是非官方 API 包装器，稳定性和合规性需要你确认。
- 如果要上线 Midjourney：
  - 准备 Midjourney 可用账号或第三方 Proxy 服务。
  - 配置 Proxy base-url、api-key、notify-url。
  - 如果没有公网回调地址，必须启用 `aiMidjourneySyncJob`。
- 如果要上线生产知识库：
  - 选择 Redis Stack、Qdrant 或 Milvus。
  - 第一阶段建议先 Redis Stack 或 Qdrant，部署成本低于 Milvus。

### 12.3 暂时不要做

- 不要购买视频生成 API 套餐，当前没有代码执行入口。
- 不要购买人物替换/换脸 API 套餐，当前没有代码执行入口。
- 不要为本地音色克隆/语音转换购买模型或 GPU 资源，当前还没有 Python Worker 和本地推理执行链路；MiMo 云 voiceclone/voicedesign 可按第一阶段功能验收需要开通。
- 不要同时买太多 Chat 服务商，先跑通一个主力模型，避免调试变量过多。

## 13. 第一阶段新增开发任务补充

基于本次校准，需要在原实施计划上新增以下任务。

### P0：密钥治理

- 删除或替换配置文件中的明文示例/疑似真实 Key。
- 为本地开发提供 `application-local.example.yaml` 或文档示例。
- 已暴露 Key 必须轮换。
- 后台列表默认脱敏显示 Key。

### P0：模型连接测试

- 后端新增模型测试接口。
- 前端 API Key/Model 页面增加“测试连接”按钮。
- 测试结果记录平台、模型、耗时、错误码。

### P0：能力与默认模型引导

- 模型表单按平台和类型给出能力提示。
- 用户功能页进入时检查默认模型。
- 对暂不可执行类型：`VIDEO`、`RERANK`、人物替换、本地音色模型，给出“第一阶段暂未开放”提示。

### P1：OpenAI 图片模型兼容性评估

- 当前代码/种子数据偏 `dall-e-2/dall-e-3`。
- OpenAI 官方文档显示 GPT Image 是新一代图像模型，DALL-E 2/3 已处于弃用状态。
- 第一阶段如果继续用 OpenAI 绘图，需要评估 Spring AI 当前版本是否支持 `gpt-image-1/gpt-image-1.5`。
- 如果不改代码，第一阶段优先使用 DashScope/Stability 图像能力。

### P1：Suno/Midjourney 风险标注

- 前端配置页明确标注二者是 Proxy 服务。
- Suno 增加代理鉴权策略，否则后台 Key 配置没有意义。
- Midjourney 增加 notify-url 可访问性检查或同步 Job 提示。

## 14. 第二阶段主要方向

第二阶段建议从“继续补 provider”转向“媒体生成平台化”。第一阶段已经验证了统一媒体任务、Provider Adapter、文件保存、任务轮询和聊天工具入口，第二阶段应把这些基础扩展到更长耗时、更高成本、更敏感的音视频能力。

### 14.1 建议主线

1. 视频生成 MVP：
   - 先选一个第三方视频 Provider，做 `TEXT_TO_VIDEO` 或 `IMAGE_TO_VIDEO` 的最小闭环。
   - 复用 `ai_media_task`：提交任务、轮询进度、保存结果视频、失败重试、配额控制。
   - 前端先做轻量任务页，不做完整时间线剪辑器。

2. Python Worker 边界：
   - 建一个最小 FastAPI Worker 契约，先支持健康检查、任务提交、状态回调、结果上传。
   - Java 只管任务、鉴权、租户、文件、回调、审计；模型推理、FFmpeg、CUDA 环境放在 Worker。
   - 后续并发上来后，再从 HTTP 提交演进到 MQ Worker。

3. 人物替换/换脸/虚拟试穿：
   - 作为敏感能力单独设计权限、确认、素材归属、审计日志、水印和开关。
   - MVP 可以先只支持一个固定 Provider 或一个 Worker 模型，不要一开始做多模型编排。

4. 语音能力生产化：
   - MiMo 云音色克隆/设计已能作为第一阶段 TTS 模式使用。
   - 第二阶段重点应放在音色资产管理、授权证明、样本审核、复用范围、结果追踪，以及可选的本地 voice clone/voice conversion Worker。

5. 媒体资产与治理：
   - 建立统一媒体资产视图：源文件、结果文件、任务、Provider、用户、租户、使用次数、过期时间。
   - 增加内容安全、敏感操作确认、结果水印、成本统计、配额面板和清理策略。

### 14.2 第二阶段优先级建议

| 优先级 | 方向 | 原因 |
| --- | --- | --- |
| P0 | 视频生成 MVP Provider | 用户感知强，能最快证明第二阶段价值 |
| P0 | 媒体任务异步化增强 | 视频任务耗时长，必须补排队、回调、超时、重试 |
| P1 | Python Worker 最小契约 | 为本地模型、人物替换、视频后处理打基础 |
| P1 | 敏感能力安全控制 | 人物替换和声音克隆必须有授权、确认、审计 |
| P1 | 媒体资产管理 | 否则任务结果多了之后难查询、难清理、难控成本 |
| P2 | 工作流/Agent 媒体编排 | 等单项能力稳定后，再做多步骤创作链路 |

### 14.3 第二阶段暂缓项

- 暂缓完整视频剪辑器、时间线编辑器和复杂字幕包装。
- 暂缓同时接入多个视频 Provider，先跑通一个可控 MVP。
- 暂缓大规模本地模型训练，先以推理 Worker 和第三方 API 为主。
- 暂缓实时音视频对话，先完成离线生成任务闭环。

## 15. 参考资料

- [OpenAI Images 文档](https://platform.openai.com/docs/guides/image-generation?lang=javascript)：GPT Image 模型、DALL-E 兼容与弃用信息。
- [OpenAI Models 文档](https://platform.openai.com/docs/models)：当前 OpenAI Chat、Image、Embedding、Audio 模型列表。
- [阿里云百炼模型列表](https://help.aliyun.com/zh/model-studio/model)：Qwen、Embedding、图像等模型规格与计费。
- [阿里云百炼向量与重排序](https://help.aliyun.com/zh/model-studio/embedding-rerank-model)：Embedding/Rerank 模型选择。
- [Xiaomi MiMo TTS 文档](https://platform.xiaomimimo.com/docs/usage-guide/speech-synthesis-v2.5)：`mimo-v2.5-tts`、voice clone/design 能力边界。
- [Xiaomi MiMo Pricing](https://platform.xiaomimimo.com/docs/pricing)：MiMo TTS 系列计费和限流。
- [Stability AI Stable Image 文档](https://platform.stability.ai/docs/getting-started/stable-image)：Stability 图像 API 能力。
- [Stability AI Getting Started](https://platform.stability.ai/docs/getting-started)：账号、API Key、Credits。
- [gcui-art/suno-api](https://github.com/gcui-art/suno-api)：当前项目音乐功能所依赖的 Suno Proxy 接口。

## 16. OpenAI 协议与 Anthropic 协议兼容策略

### 16.1 为什么要关注“协议”

现在很多大模型服务商不只提供自己的 SDK，还会提供“OpenAI 兼容接口”或“Anthropic/Claude 兼容接口”。这意味着接入一个新服务商时，不一定要为它写完整新 Adapter：

- 如果它兼容 OpenAI Chat Completions，可以复用 OpenAI 客户端。
- 如果它兼容 Anthropic Messages API，可以复用 Anthropic 客户端。
- 如果它只支持自家私有 API，才需要单独写 Provider。

当前系统的 `AiPlatformEnum` 更偏“服务商枚举”，但未来更应该区分：

```text
服务商 provider：OpenAI / Anthropic / DeepSeek / Moonshot / 第三方聚合平台 / 自建网关
协议 protocol：OPENAI_CHAT_COMPLETIONS / OPENAI_RESPONSES / ANTHROPIC_MESSAGES / PROVIDER_NATIVE
能力 capability：CHAT / IMAGE / EMBEDDING / RERANK / TTS / MUSIC
```

### 16.2 两种协议的核心区别

| 项目 | OpenAI Chat Completions 协议 | Anthropic Messages 协议 |
| --- | --- | --- |
| 常见端点 | `/v1/chat/completions` | `/v1/messages` |
| 鉴权 | `Authorization: Bearer <api-key>` | `x-api-key: <api-key>` 或 Bearer token，且必须带 `anthropic-version` |
| 请求主体 | `model` + `messages[]` | `model` + `max_tokens` + `messages[]`，`system` 通常是顶层字段 |
| 消息角色 | `system/user/assistant/tool` 等 | 主要是 `user/assistant`，system 不作为普通 messages 角色 |
| 响应结构 | `choices[0].message.content` | `content[]` block，`stop_reason`，`usage.input_tokens/output_tokens` |
| 工具调用 | `tools` + `tool_calls` | content block 中的 `tool_use` / `tool_result` 模型 |
| 流式输出 | SSE chunk 风格 | Anthropic 自己的 message/content block 事件 |
| 兼容生态 | 最广，国产与聚合平台常优先支持 | Claude 原生能力更完整 |
| 典型优势 | 接入成本低，第三方兼容多 | Claude 的长上下文、工具、PDF、缓存、扩展思考等能力更完整 |

另外，OpenAI 自己现在也在推动 `Responses API`。但行业里说“OpenAI 兼容协议”时，大多数仍然指 `/v1/chat/completions` 这套 Chat Completions 形态。第一阶段建议先兼容 Chat Completions，不急着把 Responses API 作为主协议。

### 16.3 当前系统已经支持到什么程度

| 协议/兼容方式 | 当前代码现状 | 可用方式 |
| --- | --- | --- |
| OpenAI 官方 | 已有 `OPENAI` 平台和 `buildOpenAiChatModel(apiKey, url)` | 平台选 `OpenAI`，URL 默认 OpenAI 或填兼容 base-url |
| OpenAI-compatible | 部分已支持 | 很多服务商可直接用 `OpenAI` 平台 + 自定义 URL + 模型名 |
| Anthropic 官方 | 已有 `ANTHROPIC` 平台和 `buildAnthropicChatModel(apiKey, url)` | 平台选 `Anthropic`，URL 可空或填 Anthropic 兼容 base-url |
| Anthropic-compatible | 理论可支持 | 如果第三方严格兼容 Anthropic Messages API，可用 `Anthropic` 平台 + URL |
| Anthropic 的 OpenAI SDK 兼容层 | 可通过 `OpenAI` 平台测试 Claude | Anthropic 官方说明该兼容层主要用于测试比较，不建议作为大多数生产场景长期方案 |
| OpenAI Responses API | 当前未单独支持 | 第一阶段不作为必要项 |

当前代码里已经有很多“复用 OpenAI 客户端”的实现，例如 Gemini、Grok、豆包、混元、硅基流动、百川等都有 OpenAI 兼容复用痕迹。这说明系统方向是对的，但后台配置还没有把“协议兼容”作为一等概念。

### 16.4 第一阶段是否需要兼容

需要，但第一阶段建议采用“轻改造”：

- 保留当前 `platform` 枚举，不大改表结构。
- 后台文案和配置向导明确：
  - OpenAI 兼容服务商：可以先选 `OpenAI` 平台，填写自定义 URL 和模型名。
  - Anthropic 兼容服务商：可以先选 `Anthropic` 平台，填写自定义 URL 和 Claude/兼容模型名。
- 新增模型测试时必须按协议测试，而不是只按平台名称测试。
- 日志里记录真实 provider 名称或备注，避免所有兼容服务都显示成 `OpenAI` 而难以排查。

这能最快支持第三方聚合平台、自建 OpenAI-compatible 网关、Claude-compatible 网关。

### 16.5 推荐改进方案

#### 方案 A：第一阶段轻量兼容

不新增数据库字段，只改后台体验和校验逻辑。

- API Key 表单增加说明：
  - `OpenAI` 可用于 OpenAI-compatible 服务。
  - `Anthropic` 可用于 Claude/Anthropic-compatible 服务。
- Model 表单增加“协议提示”：
  - 平台选 OpenAI 时，提示 URL 应是 OpenAI Chat Completions 兼容 base URL。
  - 平台选 Anthropic 时，提示 URL 应是 Anthropic Messages 兼容 base URL。
- 新增模型连接测试：
  - OpenAI-compatible：调用最小 Chat Completions。
  - Anthropic-compatible：调用最小 Messages API。
- 增加备注字段或复用 name 规则：
  - 例如 API Key 名称写 `OpenAI兼容-OpenRouter`、`OpenAI兼容-自建网关`、`Anthropic兼容-Claude代理`。

优点：改动少，最快能用。

缺点：统计、权限、能力矩阵仍然把协议和服务商混在一起。

#### 方案 B：第一阶段后半段或第二阶段做协议化

新增协议字段，把服务商和协议拆开。

建议字段：

| 表 | 字段 | 含义 |
| --- | --- | --- |
| `ai_api_key` | `provider` | 真实服务商，例如 OpenAI、Anthropic、OpenRouter、SiliconFlow、自建网关 |
| `ai_api_key` | `protocol` | `OPENAI_CHAT_COMPLETIONS`、`ANTHROPIC_MESSAGES`、`PROVIDER_NATIVE` |
| `ai_api_key` | `url` | 兼容协议 base URL |
| `ai_model` | `capabilities` | JSON，标注 chat/image/embedding/tool/vision/stream 等能力 |
| `ai_model` | `protocol_options` | JSON，放 provider 特定选项 |

目标架构：

```text
AiApiKey(provider, protocol, url, key)
        │
        ▼
AiProtocolAdapterFactory
        ├─ OpenAiChatCompletionsAdapter
        ├─ AnthropicMessagesAdapter
        ├─ OpenAiResponsesAdapter     # 后续
        └─ ProviderNativeAdapter      # 特殊服务商
```

优点：未来接新服务商不用频繁加 enum 和 switch。

缺点：涉及 DB、前端表单、服务层和迁移，第一阶段不是必须。

### 16.6 第一阶段新增任务

把以下任务加入第一阶段 P1：

- 增加“协议兼容配置说明”：
  - OpenAI-compatible 如何配置。
  - Anthropic-compatible 如何配置。
  - 哪些能力第一阶段只支持 Chat，不支持图片/Embedding/工具扩展。
- 模型测试接口按协议分流：
  - `OPENAI_CHAT_COMPLETIONS`：最小 chat 测试。
  - `ANTHROPIC_MESSAGES`：最小 messages 测试。
- 后台配置页增加提示文案：
  - `OpenAI` 不等于只能接 OpenAI 官方，也可接兼容接口。
  - `Anthropic` 不等于只能接 Claude 官方，也可接兼容接口。
- 日志增加 protocol/provider 识别：
  - 第一阶段可先从 API Key 名称解析或增加备注字段。
- 不建议第一阶段默认使用 Anthropic 的 OpenAI SDK 兼容层跑生产 Claude：
  - 官方说明它更适合测试比较。
  - 生产要完整 Claude 能力，优先使用 Anthropic 原生 Messages API。

### 16.7 采购与申请清单修正

新增一条采购原则：

- 优先选择提供 OpenAI-compatible 接口的服务商或网关，因为当前系统接入成本最低。
- 如果购买 Claude/Anthropic，优先确认是否需要 Claude 原生能力；如果需要 PDF、缓存、扩展思考、Claude 原生工具能力，走 `Anthropic` 平台，不要只走 OpenAI 兼容层。
- 第三方聚合平台如果同时提供 OpenAI 协议和 Anthropic 协议，第一阶段优先接 OpenAI Chat Completions 协议，除非明确要 Claude 原生能力。

### 16.8 参考资料补充

- [OpenAI API Overview](https://developers.openai.com/api/reference/overview)：OpenAI API 鉴权、请求 ID、REST/streaming 说明。
- [OpenAI Chat Completions](https://platform.openai.com/docs/api-reference/chat/create)：OpenAI Chat Completions 请求与响应结构。
- [OpenAI Responses API 迁移说明](https://developers.openai.com/api/docs/guides/migrate-to-responses)：Chat Completions 与 Responses 的差异。
- [Anthropic API Overview](https://platform.claude.com/docs/en/api/overview)：Anthropic 鉴权、`anthropic-version` 和 Claude API 说明。
- [Anthropic Messages API](https://platform.claude.com/docs/en/build-with-claude/working-with-messages)：Claude Messages API 请求/响应结构。
- [Anthropic OpenAI SDK Compatibility](https://platform.claude.com/docs/en/api/openai-sdk)：Anthropic 对 OpenAI SDK 的兼容层及限制。

## 17. AI 对话多模态支持规划

### 17.1 当前实现程度

当前聊天页面已经有附件上传入口，前端可以上传图片、PDF、Office、文本等文件，并把附件 URL 作为 `attachmentUrls` 传给后端。

后端 `AiChatMessageServiceImpl` 也已经读取 `attachmentUrls`：

- 图片：下载后转成 Base64 字符串。
- 非图片：通过知识库文档读取逻辑抽取文本。
- 最终：统一拼成 `<Attachment>` 文本，再追加为一个普通 `UserMessage`。

这说明当前系统是“附件文本化增强”，不是严格意义上的多模态 Vision 调用。图片并没有作为 Spring AI 的 `Media` 传给模型，而是作为一大段 base64 文本塞进 prompt。普通文本模型通常不能真正理解这张图片，反而会浪费上下文。

### 17.2 结论

当前 AI 对话：

- 支持上传附件。
- 支持读取文档类附件文本。
- 不算真正支持图片多模态理解。
- 暂未看到音频、视频输入理解链路。

如果你的小米多模态模型支持图文输入，并且 API 兼容 OpenAI Chat Completions 或 Spring AI 可适配的消息格式，那么第一阶段可以支持“图片 + 文本对话”。这属于对话功能完善，可以纳入第一阶段。

如果小米多模态模型是私有协议，或者需要视频/音频理解，则建议先做接口 spike，再决定是否放第一阶段。

### 17.3 第一阶段支持范围

第一阶段建议只做：

- 图片理解：用户上传 1 到 5 张图片，然后提问。
- 文档理解：继续沿用当前文本抽取逻辑。
- 多模态模型能力标识：只有支持 vision 的模型才允许图片以多模态方式发送。

第一阶段暂不做：

- 音频输入理解。
- 视频输入理解。
- 图片编辑/圈选/局部标注。
- 多轮历史图片完整回放。
- 大文件多模态分析。

### 17.4 推荐技术方案

当前 Spring AI 1.1.5 的 `UserMessage` 已支持 `media`：

```java
UserMessage.builder()
    .text("请描述图片内容")
    .media(mediaList)
    .build();
```

第一阶段改造思路：

```text
前端上传图片
    │
    ▼
attachmentUrls
    │
    ▼
后端识别附件类型
    │
    ├─ image/*：构建 Media，放入 UserMessage.media
    └─ 非图片：读取文本，仍拼接为文本附件
    │
    ▼
ChatModel.call/stream
```

需要注意：不是所有 `ChatModel` 都支持 media。必须先通过模型能力配置判断。

### 17.5 模型能力标识设计

短期不改大表结构时，可以先在后台模型名称/备注/配置说明中约定；更稳妥的做法是在 `ai_model` 增加能力字段。

建议后续增加：

| 字段 | 类型 | 示例 | 用途 |
| --- | --- | --- | --- |
| `capabilities` | JSON | `["chat","vision","stream","tool"]` | 表示模型能力 |
| `input_modalities` | JSON | `["text","image"]` | 可输入模态 |
| `output_modalities` | JSON | `["text"]` | 可输出模态 |

第一阶段轻量方案：

- 不新增字段也可以先做常量白名单。
- 例如把小米多模态模型 ID、OpenAI vision 模型、Claude vision 模型、Gemini vision 模型配置到服务端能力表。
- 前端根据 `modelId` 查能力，决定是否允许上传图片。

### 17.6 小米多模态模型接入判断

需要先确认你的小米多模态模型属于哪种协议：

| 情况 | 接入方式 | 第一阶段难度 |
| --- | --- | --- |
| OpenAI-compatible 图文输入 | 平台可先复用 `OpenAI` 或新增 `XiaomiMiMo` Chat 构建 | 低 |
| Anthropic-compatible 图文输入 | 可复用 `Anthropic` 消息结构 | 中 |
| 小米私有 Chat Completions 变体 | 新增 Xiaomi Multimodal Chat Adapter | 中 |
| 只能用专用 SDK 或特殊文件上传流程 | 先做 spike | 高 |
| 支持视频/音频输入 | 不建议第一阶段做 | 高 |

你需要提供或确认：

- 模型名称，例如具体 model id。
- API base-url。
- 鉴权方式，是 `Authorization: Bearer`、`api-key`，还是其他 header。
- 图片输入格式：
  - URL。
  - base64 data URL。
  - multipart 文件上传。
  - 先上传文件再引用 fileId。
- 是否支持流式输出。
- 单次最多图片数量、单张图片大小、支持格式。
- 是否兼容 OpenAI Chat Completions 的 `image_url` content part。

### 17.7 后端改造任务

加入第一阶段 P1：

- 新增附件分类逻辑：
  - 图片附件：构建 Spring AI `Media`。
  - 文档附件：保留文本抽取。
  - 不支持的文件：发送前提示。
- 改造 `buildPrompt`：
  - 当前用户消息如果含图片，并且模型支持 vision，则构建 `UserMessage.builder().text(...).media(...).build()`。
  - 历史消息中的图片默认不重新携带，避免上下文爆炸。
  - 文档附件继续拼接文本引用。
- 增加模型能力判断：
  - `supportsVision(modelId)`。
  - 不支持 vision 的模型收到图片时，返回明确错误：“当前模型不支持图片理解，请切换多模态模型”。
- 增加大小限制：
  - 单图最大 5-10MB。
  - 单次最多 5 张。
  - 只允许 `jpg/jpeg/png/webp`，GIF 先按不支持处理。
- 增加日志：
  - 图片数量、总大小、mime type、模型 ID、是否使用 vision。
  - 不记录图片 base64 内容。

### 17.8 前端改造任务

- 聊天页面根据模型能力控制附件类型：
  - 普通文本模型：允许文档附件，禁止图片多模态或提示会转文本不可理解。
  - Vision 模型：允许图片上传。
- 发送前校验：
  - 图片大小。
  - 图片数量。
  - 文件类型。
- 消息展示：
  - 图片显示缩略图。
  - 文档显示文件卡片。
- 模型选择页增加能力标签：
  - `文本`
  - `图片理解`
  - `联网`
  - `工具调用`

### 17.9 测试与验收

验收用例：

- 使用普通 Chat 模型上传图片，前端或后端明确提示不支持。
- 使用小米多模态模型上传图片并提问：“这张图里有什么？”
- 使用两张图片提问：“比较这两张图片的区别。”
- 上传 PDF 或 txt，仍走文本附件逻辑。
- 图片过大、格式不支持、URL 下载失败都有明确错误。
- 流式输出正常。

### 17.10 第一阶段采购/申请修正

如果要在第一阶段验证小米多模态：

- 需要申请小米多模态模型 API Key。
- 确认是否和 `mimo-v2.5-tts` 使用同一个平台账号/Key。
- 确认是否兼容 OpenAI 图文协议。
- 只先购买/开通图文理解额度，不购买视频理解或音频理解额度。

### 17.11 多模态和音色克隆/视频的边界

多模态对话中的“图片理解”可以进入第一阶段，因为它是现有聊天附件能力的自然完善。

但下面这些仍属于第二阶段：

- 视频理解。
- 视频生成。
- 人物替换。
- 本地音色克隆、语音转换和语音资产生产化。
- 语音输入对话。
- 实时音视频对话。

## 18. Xiaomi MiMo 模型接入补充

你提供的小米 MiMo 文档说明了一个很关键的点：MiMo 的文本、多模态理解、TTS、音色设计、音色克隆都统一使用类似 OpenAI Chat Completions 的接口形态：

- Endpoint：`POST https://api.xiaomimimo.com/v1/chat/completions`
- Header：`api-key: <MIMO_API_KEY>`
- Header：`Content-Type: application/json`
- Base URL：`https://api.xiaomimimo.com/v1`

请求体接近 OpenAI Chat Completions，但鉴权不是标准 `Authorization: Bearer <key>`，而是 `api-key`。因此第一阶段不要假设直接配置为 `OpenAI` 平台就一定能跑通。更稳妥的做法是新增或完善 `XiaomiMiMo` Chat/Multimodal Adapter。

### 18.1 MiMo 模型选型

| 系列 | 模型 ID | 能力 | 第一阶段建议 |
| --- | --- | --- | --- |
| Pro | `mimo-v2.5-pro` | 文本生成、深度思考、流式输出、函数调用、结构化输出、联网搜索；上下文 1M，最大输出 128K | 可作为高质量长上下文文本模型 |
| Pro | `mimo-v2-pro` | 文本生成、深度思考、流式输出、函数调用、结构化输出、联网搜索 | 可选，优先用 `mimo-v2.5-pro` |
| Omni | `mimo-v2.5` | 文本生成、全模态理解、深度思考、流式输出、函数调用、结构化输出、联网搜索；上下文 1M，最大输出 128K | 第一阶段推荐用于“文字 + 图片理解” |
| Omni | `mimo-v2-omni` | 全模态理解；上下文 256K，最大输出 128K | 可作为多模态备选 |
| Flash | `mimo-v2-flash` | 文本生成、深度思考、流式输出、函数调用、结构化输出、联网搜索；上下文 256K，最大输出 64K | 可作为低成本快速文本模型 |
| TTS | `mimo-v2.5-tts` | 预置音色语音合成，支持唱歌模式，不支持音色设计和音色复刻 | 第一阶段保留并完善 |
| TTS Voice Design | `mimo-v2.5-tts-voicedesign` | 文本描述生成音色，不支持唱歌、预置音色、音色复刻 | 已作为第一阶段 TTS 音色库模式接入 |
| TTS Voice Clone | `mimo-v2.5-tts-voiceclone` | 基于音频样本复刻音色，不支持唱歌、预置音色、音色设计 | 已作为第一阶段 TTS 音色库模式接入；需保留授权确认和样本限制 |

官方快速选型里，复杂推理/长文档推荐 `mimo-v2.5-pro`，图片/音频/视频内容理解推荐 `mimo-v2.5` 或 `mimo-v2-omni`，高并发低成本推荐 `mimo-v2-flash`，普通 TTS 推荐 `mimo-v2.5-tts`。

### 18.2 对话与图片理解调用形态

图片理解使用 `content` 数组，图片可以传 URL 或 Base64 data URL：

```json
{
  "model": "mimo-v2.5",
  "messages": [
    {
      "role": "user",
      "content": [
        {
          "type": "image_url",
          "image_url": {
            "url": "https://example.com/image.png"
          }
        },
        {
          "type": "text",
          "text": "请描述这张图片"
        }
      ]
    }
  ],
  "max_completion_tokens": 1024
}
```

第一阶段建议把“图片理解”作为 AI 对话的一部分实现，而不是另起一个图片分析功能。原因是系统现在已经有聊天附件能力，只是当前实现把图片 base64 当文本塞进 prompt，并不是真正的多模态。

### 18.3 音频与视频理解调用形态

音频理解同样走 Chat Completions，并使用 `input_audio` content part：

```json
{
  "type": "input_audio",
  "input_audio": {
    "data": "https://example.com/audio.wav"
  }
}
```

也可以传 `data:{MIME_TYPE};base64,<BASE64_AUDIO>`。

视频理解使用 `video_url` content part，并可指定 `fps` 和 `media_resolution`：

```json
{
  "type": "video_url",
  "video_url": {
    "url": "https://example.com/video.mp4"
  },
  "fps": 2,
  "media_resolution": "default"
}
```

第一阶段不建议把音频理解、视频理解直接做成正式功能。可以只在设计里预留：

- 附件类型枚举支持 `image/audio/video/document`。
- 模型能力支持 `vision/audio/video`。
- MiMo Adapter 的 content part 构造方法可扩展。

真正上线时再处理大文件、上传链路、费用控制、异步任务、超时、内容安全和前端播放体验。

### 18.4 TTS、唱歌、音色设计、音色克隆边界

当前代码已经有 `XiaomiMiMo` TTS Provider，并支持普通 TTS、voiceclone、voicedesign 三种云模式，因此第一阶段语音合成和音色库可以继续使用它做闭环。

普通 TTS 调用大致形态：

```json
{
  "model": "mimo-v2.5-tts",
  "messages": [
    {
      "role": "user",
      "content": "用明快、轻松的语气朗读"
    },
    {
      "role": "assistant",
      "content": "这里是要合成语音的正文。"
    }
  ],
  "audio": {
    "format": "wav",
    "voice": "..."
  }
}
```

音色克隆调用 `mimo-v2.5-tts-voiceclone`，把样本音频放到 `audio.voice`：

```json
{
  "model": "mimo-v2.5-tts-voiceclone",
  "messages": [
    {
      "role": "user",
      "content": ""
    },
    {
      "role": "assistant",
      "content": "这里是要用克隆音色合成的文本。"
    }
  ],
  "audio": {
    "format": "wav",
    "voice": "data:audio/mpeg;base64,<BASE64_AUDIO>"
  }
}
```

官方文档说明音色克隆样本当前支持 `mp3` 和 `wav`，Base64 编码后的字符串大小不能超过 10MB。当前代码已经把 MiMo 云 voiceclone 接入为 TTS 模式；第二阶段需要继续补的是更完整的样本审核、授权证明、音色资产复用范围、审计追踪，以及可选的本地 voice clone/voice conversion Worker。

### 18.5 第一阶段 MiMo 改造任务

当前建议保留为验收和后续增强清单：

- 后端新增 `XiaomiMiMo` Chat Adapter：
  - 支持 `mimo-v2.5-pro`、`mimo-v2.5`、`mimo-v2-omni`、`mimo-v2-flash`。
  - 使用 `api-key` header。
  - 使用 `/v1/chat/completions`。
  - 支持普通文本对话和流式输出。
- 后端新增 MiMo 图片理解：
  - 只对 `mimo-v2.5`、`mimo-v2-omni` 开启图片输入。
  - 将图片附件构造成 `image_url` content part。
  - 普通文本模型收到图片时给出明确错误。
- 后端完善 MiMo TTS：
  - 保留 `mimo-v2.5-tts`。
  - 补齐音频格式、预置音色、唱歌模式的配置与验收。
  - 已接入 `voiceclone` 和 `voicedesign`，继续补真实 Key 验收、授权提示、计费/限流记录。
- 前端模型能力展示：
  - `文本`
  - `图片理解`
  - `长上下文`
  - `流式`
  - `TTS`
- 配置向导：
  - 平台选择 `XiaomiMiMo`。
  - API URL 填 `https://api.xiaomimimo.com/v1`。
  - Chat 模型优先配置 `mimo-v2.5` 和 `mimo-v2.5-pro`。
  - TTS 模型配置 `mimo-v2.5-tts`。

### 18.6 第一阶段验收用例

MiMo 相关验收建议：

- `mimo-v2.5-pro` 可以完成普通文本对话，支持流式输出。
- `mimo-v2-flash` 可以完成低成本快速文本对话。
- `mimo-v2.5` 可以上传一张图片并回答“这张图里有什么？”。
- `mimo-v2.5` 可以上传两张图片并回答“比较这两张图的区别”。
- 选择 `mimo-v2.5-pro` 或 `mimo-v2-flash` 上传图片时，系统提示“当前模型不支持图片理解，请切换多模态模型”。
- `mimo-v2.5-tts` 可以生成一段 `wav` 或 `mp3` 音频，并保存到系统文件服务。
- `mimo-v2.5-tts-voiceclone` 可以在确认声音授权后使用 `mp3/wav` 样本合成音频。
- `mimo-v2.5-tts-voicedesign` 可以通过文字描述生成音色并合成音频。
- 未配置 API Key、Key 无效、额度不足、429 限流、文件过大都有清晰错误提示。

### 18.7 你需要准备的 MiMo 事项

第一阶段需要你跟进：

- 注册/登录小米 MiMo 开放平台。
- 创建并保存 `MIMO_API_KEY`。
- 开通或确认可用模型：
  - `mimo-v2.5-pro`：高质量文本/长上下文。
  - `mimo-v2.5`：多模态图片理解。
  - `mimo-v2-flash`：低成本快速文本。
  - `mimo-v2.5-tts`：普通语音合成。
  - `mimo-v2.5-tts-voiceclone`。
  - `mimo-v2.5-tts-voicedesign`。
- 暂不必购买或正式启用：
  - 视频理解的大规模额度。
  - 音频理解的大规模额度。

如果你希望第一阶段就验证“音频理解/视频理解能不能跑通”，可以做一个内部测试开关和最小接口 spike，但不建议把它作为第一阶段正式上线范围。

### 18.8 参考文档

- [Xiaomi MiMo 模型与限速](https://platform.xiaomimimo.com/docs/zh-CN/quick-start/model)
- [Xiaomi MiMo 图片理解](https://platform.xiaomimimo.com/docs/zh-CN/usage-guide/multimodal-understanding/image-understanding)
- [Xiaomi MiMo 音频理解](https://platform.xiaomimimo.com/docs/zh-CN/usage-guide/multimodal-understanding/audio-understanding)
- [Xiaomi MiMo 视频理解](https://platform.xiaomimimo.com/docs/zh-CN/usage-guide/multimodal-understanding/video-understanding)
- [Xiaomi MiMo 语音合成 V2.5](https://platform.xiaomimimo.com/docs/zh-CN/usage-guide/speech-synthesis-v2.5)
