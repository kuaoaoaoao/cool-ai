## ADDED Requirements

### Requirement: TTS 任务创建
系统 SHALL 支持创建文本转语音媒体任务，并在第一阶段优先支持小米 MiMo `mimo-v2.5-tts` Provider。

#### Scenario: 创建小米 TTS 任务
- **WHEN** 用户提交文本、音色、音频格式和可选风格提示
- **THEN** 系统 SHALL 创建 `TTS` 类型媒体任务，并选择 Xiaomi MiMo `mimo-v2.5-tts` 作为 Provider 模型

#### Scenario: TTS 参数缺失
- **WHEN** 用户提交 TTS 请求但缺少必需文本或音色参数
- **THEN** 系统 MUST 拒绝创建任务并返回参数错误

### Requirement: 小米 MiMo TTS 输入映射
系统 SHALL 将 TTS 文本、风格提示、音色和音频格式映射到小米 MiMo `mimo-v2.5-tts` 官方接口所需请求结构。

#### Scenario: 映射合成文本
- **WHEN** 系统调用小米 MiMo TTS Provider
- **THEN** 系统 SHALL 将待合成文本放入 `assistant` role 的消息中

#### Scenario: 映射风格提示
- **WHEN** 用户提供情绪、语气或说话风格提示
- **THEN** 系统 SHALL 将该提示放入 `user` role 的消息中

#### Scenario: 映射音频参数
- **WHEN** 用户选择音色和输出格式
- **THEN** 系统 SHALL 将 `voice` 和 `format` 作为音频生成选项传递给 Provider

#### Scenario: 使用 Provider 配置值
- **WHEN** 系统校验 TTS 音色和格式
- **THEN** 系统 SHALL 根据 Provider 配置或字典数据判断可用值，而不是在规格中固定可选值

### Requirement: TTS 结果保存
系统 SHALL 解码 Provider 返回的音频内容，并通过 Java 侧文件服务保存结果音频。

#### Scenario: Provider 返回 base64 音频
- **WHEN** 小米 MiMo TTS 返回 base64 音频载荷
- **THEN** 系统 SHALL 解码音频、保存到文件服务，并把结果 `fileId` 和 `url` 写入媒体任务输出

#### Scenario: Provider 返回失败
- **WHEN** 小米 MiMo TTS 返回错误响应
- **THEN** 系统 SHALL 将媒体任务标记为失败，并记录 Provider 错误信息

### Requirement: 声音克隆任务预留
系统 SHALL 为声音克隆保留任务类型和安全控制入口，但第一阶段不要求直接实现本地声音克隆模型推理。

#### Scenario: 创建声音克隆任务前置校验
- **WHEN** 用户尝试创建声音克隆任务
- **THEN** 系统 MUST 校验功能开关、用户权限、素材归属和确认状态

#### Scenario: 声音克隆 Provider 未配置
- **WHEN** 声音克隆 Provider 或 Python Worker 尚未配置
- **THEN** 系统 SHALL 拒绝创建可执行任务，并返回能力未启用提示

#### Scenario: 第一阶段提交声音克隆任务
- **WHEN** 第一阶段用户提交声音克隆任务
- **THEN** 系统 SHALL 返回能力未启用提示，并保留该能力在后续阶段接入 Python Worker 的计划入口

### Requirement: 音频任务结果管理
系统 SHALL 支持用户查询音频任务历史和结果文件，并按照任务归属进行访问控制。

#### Scenario: 查询音频任务列表
- **WHEN** 用户打开音频任务列表
- **THEN** 系统 SHALL 返回该用户可访问的音频任务状态、类型、创建时间和结果摘要

#### Scenario: 下载音频结果
- **WHEN** 用户请求下载已完成音频任务结果
- **THEN** 系统 MUST 校验任务归属后返回结果文件访问地址
