## ADDED Requirements

### Requirement: 视频任务创建
系统 SHALL 支持以统一媒体任务模型创建视频类任务，包括文生视频、图生视频和人物替换。

#### Scenario: 创建文生视频任务
- **WHEN** 用户提交文本提示词和视频参数
- **THEN** 系统 SHALL 创建 `TEXT_TO_VIDEO` 类型媒体任务

#### Scenario: 创建图生视频任务
- **WHEN** 用户提交源图片、提示词和视频参数
- **THEN** 系统 SHALL 创建 `IMAGE_TO_VIDEO` 类型媒体任务，并保存源图片文件引用

#### Scenario: 创建人物替换任务
- **WHEN** 用户提交源视频和目标人物素材
- **THEN** 系统 SHALL 创建 `PERSON_REPLACE` 类型媒体任务，并保存源视频和目标素材文件引用

### Requirement: 视频任务异步执行
系统 SHALL 将视频类任务作为异步任务执行，不得要求前端等待单个同步请求完成完整视频生成。

#### Scenario: 提交视频任务
- **WHEN** 用户提交视频生成请求
- **THEN** 系统 SHALL 立即返回任务 ID，并允许前端通过任务状态接口轮询进度

#### Scenario: 视频生成完成
- **WHEN** Provider 或 Python Worker 完成视频生成
- **THEN** 系统 SHALL 保存结果视频文件引用，并将任务状态更新为 `SUCCEEDED`

### Requirement: 视频素材校验
系统 MUST 校验视频任务所需素材的存在性、归属和基本媒体类型。

#### Scenario: 源文件不存在
- **WHEN** 用户提交的视频任务引用不存在的源文件
- **THEN** 系统 MUST 拒绝创建任务

#### Scenario: 源文件不属于当前用户
- **WHEN** 用户提交不属于自己的源视频或目标人物素材
- **THEN** 系统 MUST 拒绝创建任务

### Requirement: 人物替换安全控制
系统 MUST 将人物替换视为敏感能力，并要求功能开关、权限、配额、素材归属和确认状态校验。

#### Scenario: 未确认敏感操作
- **WHEN** 用户未完成敏感操作确认就提交人物替换任务
- **THEN** 系统 MUST 拒绝创建任务并要求用户确认

#### Scenario: 功能被禁用
- **WHEN** 租户或系统关闭人物替换能力
- **THEN** 系统 MUST 拒绝创建人物替换任务

### Requirement: 视频 MVP Provider 待配置
系统 SHALL 允许视频 Provider 在第一阶段保持未配置状态，但 MUST 保留 Provider 选择和能力扩展入口。

#### Scenario: 视频 Provider 未配置
- **WHEN** 用户提交需要未配置 Provider 的视频任务
- **THEN** 系统 SHALL 返回能力未启用或 Provider 未配置提示

#### Scenario: 第一阶段提交视频任务
- **WHEN** 第一阶段用户提交文生视频、图生视频或人物替换任务
- **THEN** 系统 SHALL 返回能力未启用提示，并保留后续 Provider 或 Python Worker 接入计划

#### Scenario: 后续接入视频 Provider
- **WHEN** 管理员配置可用视频 Provider
- **THEN** 系统 SHALL 能通过 Provider Adapter 执行对应视频任务，而不改变前端任务查询模型
