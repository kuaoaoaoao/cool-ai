## ADDED Requirements

### Requirement: 媒体资产记录
系统 SHALL 记录媒体任务源文件和生成结果的资产元数据，至少包括任务、资产类型、文件引用、媒体类型、大小、用户和租户。

第一版 SiliconFlow MVP SHALL 允许先将标准资产元数据保存在 `input_json` 和 `output_json` 中；独立 `ai_media_asset` 表 MAY 在资产浏览、清理或复用需求明确后再落地。

#### Scenario: 记录源图片资产
- **WHEN** 用户提交图生视频任务
- **THEN** 系统 SHALL 记录源图片资产或在任务输入中保存等价资产元数据

#### Scenario: 记录结果视频资产
- **WHEN** 视频任务生成结果并保存到文件服务
- **THEN** 系统 SHALL 记录结果视频资产或在任务输出中保存等价资产元数据

### Requirement: 媒体资产访问控制
系统 MUST 按用户归属和租户隔离控制媒体资产访问。

#### Scenario: 用户访问自己的资产
- **WHEN** 用户查询自己有权访问的媒体资产
- **THEN** 系统 SHALL 返回资产元数据和可访问链接

#### Scenario: 用户访问他人资产
- **WHEN** 用户查询不属于自己且无权限访问的媒体资产
- **THEN** 系统 MUST 拒绝访问或过滤该资产

### Requirement: 媒体资产列表
系统 SHALL 提供媒体资产列表或任务结果聚合视图，支持按类型、任务、状态、用户、租户和创建时间查询。

#### Scenario: 查询结果视频
- **WHEN** 管理员或用户按结果视频类型查询资产
- **THEN** 系统 SHALL 返回匹配的视频资产列表

#### Scenario: 查询任务关联资产
- **WHEN** 调用方查看某个媒体任务详情
- **THEN** 系统 SHALL 返回该任务关联的源文件和结果文件

### Requirement: 资产过期与清理准备
系统 SHALL 为媒体资产保留过期时间、大小和清理状态信息，以便后续执行清理策略。

#### Scenario: 资产达到过期时间
- **WHEN** 媒体资产超过过期时间
- **THEN** 系统 SHALL 能识别该资产为可清理候选

#### Scenario: 清理仍被任务引用的资产
- **WHEN** 资产仍被未完成任务或保留策略引用
- **THEN** 系统 MUST 避免误清理该资产

### Requirement: 配额与成本记录
系统 SHALL 记录媒体任务和资产相关的基本配额与成本信息，包括任务数、并发数、文件大小和 Provider 消耗。

#### Scenario: 创建任务前检查配额
- **WHEN** 用户创建视频或敏感媒体任务
- **THEN** 系统 SHALL 检查用户和租户配额

#### Scenario: 任务完成后记录成本
- **WHEN** 媒体任务完成或失败
- **THEN** 系统 SHALL 记录任务耗时、Provider、模型和消耗额度或估算成本
