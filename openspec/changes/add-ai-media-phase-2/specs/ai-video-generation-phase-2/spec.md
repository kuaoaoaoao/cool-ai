## ADDED Requirements

### Requirement: 图生视频任务创建
系统 SHALL 支持用户提交 SiliconFlow 图生视频任务，并使用统一媒体任务模型记录输入、源图、Provider、模型、状态和结果。

#### Scenario: 创建图生视频任务
- **WHEN** 用户提交源图片、提示词、视频比例、时长和质量参数
- **THEN** 系统 SHALL 创建 `IMAGE_TO_VIDEO` 类型媒体任务并保存源图片文件引用

#### Scenario: 创建 SiliconFlow 图生视频任务
- **WHEN** 用户选择 SiliconFlow `VIDEO` 模型创建图生视频任务
- **THEN** 系统 SHALL 使用 SiliconFlow Provider Adapter 提交 `/v1/video/submit` 请求并保存返回的 `requestId`

#### Scenario: 源图片缺失
- **WHEN** 用户提交图生视频任务但未提供源图片
- **THEN** 系统 MUST 拒绝创建任务并返回参数错误

#### Scenario: 源图片无权访问
- **WHEN** 用户提交不属于自己或当前租户不可访问的源图片
- **THEN** 系统 MUST 拒绝创建任务

### Requirement: 文生视频任务预留
系统 SHALL 预留文生视频任务创建能力，并在 Provider 支持时允许用户提交纯文本视频生成任务。

#### Scenario: 创建文生视频任务
- **WHEN** 用户提交提示词、视频比例、时长和质量参数
- **THEN** 系统 SHALL 创建 `TEXT_TO_VIDEO` 类型媒体任务

#### Scenario: 文生视频 Provider 未启用
- **WHEN** 文生视频 Provider 未配置或功能开关关闭
- **THEN** 系统 SHALL 返回能力未启用或 Provider 未配置提示

#### Scenario: SiliconFlow MVP 阶段提交文生视频
- **WHEN** 第一版 MVP 尚未启用 `TEXT_TO_VIDEO`
- **THEN** 系统 SHALL 返回能力未启用提示，而不是尝试提交 Provider

### Requirement: 视频 Provider 提交
系统 SHALL 通过视频 Provider Adapter 提交视频任务，而不是在 Controller 或页面直接调用第三方接口。

#### Scenario: 提交第三方视频任务
- **WHEN** 系统执行合法视频任务
- **THEN** 系统 SHALL 选择匹配的 Provider Adapter 并提交任务到第三方视频服务

#### Scenario: Provider 返回外部任务 ID
- **WHEN** 第三方视频服务接受任务并返回任务 ID
- **THEN** 系统 SHALL 保存外部任务 ID 并将媒体任务状态推进到已提交或运行中状态

#### Scenario: SiliconFlow 请求映射
- **WHEN** 系统向 SiliconFlow 提交图生视频任务
- **THEN** 请求 SHALL 包含 `model`、`prompt`、`image_size`、`image`、可选 `negative_prompt` 和可选 `seed`

#### Scenario: SiliconFlow 状态映射
- **WHEN** 系统查询 SiliconFlow 视频状态
- **THEN** 系统 SHALL 将 `InQueue` 映射为排队中、`InProgress` 映射为运行中、`Succeed` 映射为成功处理路径、`Failed` 映射为失败

### Requirement: 视频结果保存
系统 SHALL 将生成视频保存到文件服务，并将结果文件引用写入媒体任务输出。

#### Scenario: 视频生成成功
- **WHEN** Provider 返回视频结果 URL 或文件内容
- **THEN** 系统 SHALL 保存结果视频到文件服务，并在任务输出中记录 `fileId`、`url`、`mimeType`、时长和尺寸信息

#### Scenario: SiliconFlow 临时结果 URL
- **WHEN** SiliconFlow 返回 `results.videos[].url`
- **THEN** 系统 MUST 尽快下载并转存到项目文件服务，不得将 Provider 临时 URL 作为长期结果 URL

#### Scenario: 文件保存失败
- **WHEN** Provider 生成成功但系统保存结果文件失败
- **THEN** 系统 SHALL 将任务标记为失败并记录文件保存错误

### Requirement: 视频任务前端体验
系统 SHALL 提供视频生成入口和任务历史，使用户能够提交任务、查看进度、预览结果和查看失败原因。

#### Scenario: 用户查看运行中视频任务
- **WHEN** 用户打开视频生成页或任务中心
- **THEN** 系统 SHALL 展示运行中任务的状态、阶段、进度和 Provider 信息

#### Scenario: 用户查看已完成视频任务
- **WHEN** 视频任务已完成
- **THEN** 系统 SHALL 展示可播放的视频结果和下载入口
