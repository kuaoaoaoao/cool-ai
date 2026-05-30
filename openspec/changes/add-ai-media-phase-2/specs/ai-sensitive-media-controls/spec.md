## ADDED Requirements

### Requirement: 敏感媒体功能开关
系统 SHALL 为人物替换、本地声音克隆、本地语音转换和类似高风险媒体能力提供系统级和租户级功能开关。

#### Scenario: 系统级关闭敏感能力
- **WHEN** 系统级开关关闭人物替换能力
- **THEN** 系统 MUST 拒绝创建人物替换任务

#### Scenario: 租户级关闭敏感能力
- **WHEN** 租户未启用某个敏感媒体能力
- **THEN** 系统 MUST 拒绝该租户用户创建对应任务

### Requirement: 敏感媒体权限校验
系统 MUST 在创建敏感媒体任务前校验用户权限。

#### Scenario: 用户缺少权限
- **WHEN** 用户没有人物替换任务创建权限
- **THEN** 系统 MUST 拒绝创建任务

#### Scenario: 用户具备权限
- **WHEN** 用户具备权限且功能开关启用
- **THEN** 系统 SHALL 继续执行后续素材和确认校验

### Requirement: 显式确认
系统 MUST 要求用户在创建人物替换、本地声音克隆或类似敏感任务前完成显式确认。

#### Scenario: 未确认敏感操作
- **WHEN** 用户未确认已获得素材授权或理解敏感能力风险
- **THEN** 系统 MUST 拒绝创建敏感媒体任务

#### Scenario: 已确认敏感操作
- **WHEN** 用户提交确认标记和合法素材
- **THEN** 系统 SHALL 在任务输入或审计记录中保存确认信息

### Requirement: 素材归属校验
系统 MUST 校验敏感媒体任务中源视频、目标人物素材、声音样本等源文件的存在性、类型和访问权限。

#### Scenario: 素材不存在
- **WHEN** 用户提交不存在的源文件引用
- **THEN** 系统 MUST 拒绝创建任务

#### Scenario: 素材类型不支持
- **WHEN** 用户提交不符合任务要求的文件类型
- **THEN** 系统 MUST 拒绝创建任务并返回类型错误

#### Scenario: 素材不属于当前用户
- **WHEN** 用户提交无权访问的素材
- **THEN** 系统 MUST 拒绝创建任务

### Requirement: 敏感任务审计
系统 SHALL 记录敏感媒体任务的创建、提交、取消、失败、完成和结果访问审计。

#### Scenario: 创建敏感任务
- **WHEN** 用户创建人物替换或本地声音克隆任务
- **THEN** 系统 SHALL 记录用户、租户、素材引用、确认信息、Provider、模型和时间

#### Scenario: 访问敏感结果
- **WHEN** 用户访问敏感媒体任务结果
- **THEN** 系统 SHALL 记录访问审计或确保现有文件访问审计覆盖该行为

### Requirement: 水印与追踪标记
系统 SHALL 为人物替换等敏感结果预留水印或追踪标记能力。

#### Scenario: Provider 支持水印
- **WHEN** Provider 或 Worker 支持结果水印
- **THEN** 系统 SHALL 在敏感任务提交时启用水印或追踪标记选项

#### Scenario: Provider 不支持水印
- **WHEN** Provider 不支持水印
- **THEN** 系统 SHALL 在任务输出和审计中记录无法加水印的事实，并允许通过功能开关禁止该 Provider 用于敏感任务
