## ADDED Requirements

### Requirement: 统一媒体任务模型
系统 SHALL 使用统一的 AI 媒体任务模型承载音频和视频类长任务，并在第一阶段以 `ai_media_task` 作为主任务表。

#### Scenario: 创建 TTS 媒体任务
- **WHEN** 用户提交 TTS 生成请求
- **THEN** 系统 SHALL 创建一条 `ai_media_task` 记录，并记录任务类型、用户、租户、Provider、模型、输入参数、状态和创建时间

#### Scenario: 创建视频媒体任务
- **WHEN** 用户提交文生视频、图生视频或人物替换请求
- **THEN** 系统 SHALL 使用同一套媒体任务模型记录该请求，而不是为每种任务建立完全独立的主流程

### Requirement: 任务类型和扩展参数
系统 SHALL 使用任务类型字段区分媒体任务能力，并使用结构化 JSON 字段保存能力专属输入和输出。第一阶段 MUST 支持可执行 `TTS` 类型，并 MUST 对未知或未启用任务类型提供清晰拒绝路径。

#### Scenario: 保存能力专属参数
- **WHEN** TTS 任务需要保存文本、音色、格式和风格提示
- **THEN** 系统 SHALL 将通用字段保存到任务主字段，并将能力专属参数保存到输入 JSON

#### Scenario: 遇到未来任务类型
- **WHEN** 系统收到第一阶段尚未启用的任务类型
- **THEN** 系统 SHALL 走未启用或未知类型处理路径，而不是执行 Provider 调用

#### Scenario: 校验能力专属参数
- **WHEN** 服务层接收媒体任务创建请求
- **THEN** 系统 MUST 使用对应任务类型的请求对象校验输入 JSON，而不是直接信任前端传入的任意 JSON

### Requirement: 任务生命周期
系统 SHALL 为媒体任务提供稳定生命周期，至少支持 `CREATED`、`QUEUED`、`RUNNING`、`SUCCEEDED`、`FAILED`、`CANCELLED` 和 `RETRYING` 状态。

#### Scenario: 成功完成任务
- **WHEN** Provider 或 Python Worker 返回任务成功和结果文件
- **THEN** 系统 SHALL 将任务状态更新为 `SUCCEEDED`，并保存结果文件引用和完成时间

#### Scenario: 任务失败
- **WHEN** Provider 或 Python Worker 返回失败结果
- **THEN** 系统 SHALL 将任务状态更新为 `FAILED`，并保存错误码、错误信息和失败时间

#### Scenario: 任务取消
- **WHEN** 用户取消仍可取消的媒体任务
- **THEN** 系统 SHALL 将任务状态更新为 `CANCELLED`，并阻止后续重复执行该任务

### Requirement: 前端默认轮询任务状态
系统 SHALL 提供任务状态查询接口作为第一阶段默认进度反馈机制，前端 SHALL 通过轮询获取状态、进度、阶段信息和结果。

#### Scenario: 查询运行中任务
- **WHEN** 前端请求任务状态
- **THEN** 系统 SHALL 返回任务 ID、状态、进度百分比、当前阶段、结果文件引用和错误信息

#### Scenario: 查询已完成任务
- **WHEN** 前端查询已完成任务
- **THEN** 系统 SHALL 返回 `SUCCEEDED` 状态和可展示或下载的结果文件引用

### Requirement: 任务归属和租户隔离
系统 MUST 校验媒体任务的用户归属和租户归属，防止用户读取、取消或重试不属于自己的任务。

#### Scenario: 用户访问他人任务
- **WHEN** 用户请求查询不属于自己的媒体任务
- **THEN** 系统 MUST 拒绝访问

#### Scenario: 跨租户访问任务
- **WHEN** 请求上下文租户与任务租户不一致
- **THEN** 系统 MUST 拒绝访问或按现有租户隔离规则过滤该任务

### Requirement: 媒体文件引用
系统 SHALL 在媒体任务中保存上传源文件和生成结果文件的文件 ID、URL 和元数据引用，而不是将音视频二进制内容保存到任务表。

#### Scenario: 保存生成音频
- **WHEN** TTS Provider 返回音频数据
- **THEN** 系统 SHALL 将音频保存到文件服务，并在任务输出中记录 `fileId` 和 `url`

#### Scenario: 保存源视频
- **WHEN** 用户上传人物替换源视频
- **THEN** 系统 SHALL 保存源视频文件引用，并在任务输入中关联该引用

### Requirement: 第一阶段能力范围
系统 SHALL 在第一阶段只执行 `TTS` 类型媒体任务，其它任务类型 SHALL 作为后续计划能力保留入口但返回未启用提示。`TTS` 类型内部 MAY 支持 Provider 自身的预置音色、云音色克隆和文字设计音色模式。

#### Scenario: 执行 TTS 任务
- **WHEN** 用户提交合法 TTS 请求且 Provider 已配置
- **THEN** 系统 SHALL 创建并执行 TTS 媒体任务

#### Scenario: 执行 MiMo 云音色克隆 TTS
- **WHEN** 用户提交合法 TTS 请求、选择 MiMo 云音色克隆模式并完成授权确认
- **THEN** 系统 SHALL 仍创建并执行 `TTS` 媒体任务，而不是创建独立 `VOICE_CLONE` 任务

#### Scenario: 提交未启用任务类型
- **WHEN** 用户提交独立 `VOICE_CLONE`、`TEXT_TO_VIDEO`、`IMAGE_TO_VIDEO` 或 `PERSON_REPLACE` 任务
- **THEN** 系统 SHALL 拒绝执行任务并返回能力未启用提示
