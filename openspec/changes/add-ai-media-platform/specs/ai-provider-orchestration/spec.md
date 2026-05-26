## ADDED Requirements

### Requirement: Provider Adapter 边界
系统 SHALL 通过 Provider Adapter 调用第三方 AI API 或 Python Worker，不得在 Controller 中直接写 Provider 专属调用逻辑。

#### Scenario: 调用小米 TTS
- **WHEN** 系统执行 Xiaomi MiMo TTS 任务
- **THEN** 系统 SHALL 通过小米 Provider Adapter 构造请求、调用接口、解析响应并更新媒体任务

#### Scenario: 调用 Python Worker
- **WHEN** 系统执行需要本地模型推理的媒体任务
- **THEN** 系统 SHALL 通过 Python Worker Adapter 提交任务，而不是由 Java 直接运行 Python 脚本

### Requirement: Provider 凭据外部化
系统 MUST 将 Provider API Key 和密钥配置在源码之外。

#### Scenario: 小米 API Key 未配置
- **WHEN** 小米 MiMo TTS Provider 缺少 API Key
- **THEN** 系统 MUST 阻止调用 Provider，并返回配置缺失错误

#### Scenario: 使用手动配置的 API Key
- **WHEN** 管理员或部署环境提供小米 API Key
- **THEN** 系统 SHALL 在调用 Provider 时使用该配置值作为 `api-key` 请求头

### Requirement: 小米 MiMo TTS Provider 契约
系统 SHALL 支持小米 MiMo `mimo-v2.5-tts` 的官方调用契约。

#### Scenario: 请求小米 TTS 接口
- **WHEN** 系统执行小米 TTS 任务
- **THEN** 系统 SHALL 调用 `POST https://api.xiaomimimo.com/v1/chat/completions` 并设置 `api-key` 认证头

#### Scenario: 使用指定模型
- **WHEN** 系统构造小米 TTS 请求
- **THEN** 系统 SHALL 使用 `mimo-v2.5-tts` 作为模型标识

#### Scenario: 处理音频响应
- **WHEN** 小米 TTS 返回 base64 音频内容
- **THEN** 系统 SHALL 解码音频并交由文件服务保存

### Requirement: Python Worker HTTP 契约预留
系统 SHALL 在设计中保留 Python Worker 的统一提交请求、提交响应和状态回调载荷，但第一阶段不要求实现 Python Worker 调用。

#### Scenario: 提交 Worker 任务
- **WHEN** 后续阶段 Java 向 Python Worker 提交任务
- **THEN** 请求 SHALL 包含任务 ID、能力类型、模型、输入参数、源文件、回调地址、租户 ID、用户 ID 和 trace ID

#### Scenario: Worker 接受任务
- **WHEN** 后续阶段 Python Worker 接受任务
- **THEN** 响应 SHALL 返回 accepted、workerTaskId、预估耗时和消息

#### Scenario: Worker 上报状态
- **WHEN** 后续阶段 Python Worker 上报任务进度或结果
- **THEN** 载荷 SHALL 包含任务 ID、Worker 任务 ID、状态、进度、阶段、输出文件和错误信息

### Requirement: Callback 入口预留
系统 SHALL 在设计中保留第三方 Provider 和 Python Worker 的统一 Callback 入口，但第一阶段不要求实现 Callback 端点。第一阶段小米 MiMo TTS SHALL 允许同步完成任务而不依赖 Callback。

#### Scenario: 接收 Provider 回调
- **WHEN** 后续阶段 Provider 调用 `/ai/media/callback/{provider}`
- **THEN** 系统 SHALL 验证来源、记录回调日志、归一化载荷并更新媒体任务

#### Scenario: 小米 TTS 同步完成
- **WHEN** 小米 MiMo TTS 在同步响应中返回音频结果
- **THEN** 系统 SHALL 直接保存结果并更新任务状态，不要求 Provider 发送 Callback

#### Scenario: 重复回调
- **WHEN** 系统收到重复的成功或运行中回调
- **THEN** 系统 MUST 幂等处理，不得重复生成结果或破坏任务状态

#### Scenario: 非法回调
- **WHEN** 回调验签或任务身份校验失败
- **THEN** 系统 MUST 拒绝更新任务并记录异常回调

### Requirement: Provider 超时和失败处理
系统 SHALL 在 Adapter 层处理 Provider 调用超时、网络失败和响应格式错误。

#### Scenario: Provider 提交超时
- **WHEN** Provider 提交调用超时
- **THEN** 系统 SHALL 将任务标记为失败或保持可重试状态，并记录错误信息

#### Scenario: Provider 响应格式异常
- **WHEN** Provider 返回无法解析的响应
- **THEN** 系统 SHALL 记录原始错误摘要并将任务标记为失败
