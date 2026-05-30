## ADDED Requirements

### Requirement: Python Worker 配置
系统 SHALL 支持将 Python Worker 作为媒体 Provider 的一种执行方式进行配置和健康检查。

#### Scenario: 配置 Worker Provider
- **WHEN** 管理员配置 Python Worker base URL、鉴权信息和支持能力
- **THEN** 系统 SHALL 能将匹配任务路由到 Python Worker Adapter

#### Scenario: Worker 健康检查
- **WHEN** 管理员测试 Python Worker 连接
- **THEN** 系统 SHALL 调用 Worker 健康检查接口并展示可用性结果

### Requirement: Worker 任务提交
系统 SHALL 使用统一请求信封向 Python Worker 提交任务，包含任务 ID、任务类型、模型、输入参数、源文件、回调地址、租户、用户和追踪 ID。

#### Scenario: 提交 Worker 任务
- **WHEN** 系统执行需要 Python Worker 的媒体任务
- **THEN** 系统 SHALL 向 Worker 发送统一任务提交请求

#### Scenario: Worker 接受任务
- **WHEN** Worker 返回已接受和 Worker 任务 ID
- **THEN** 系统 SHALL 保存 Worker 任务 ID 并更新媒体任务状态

#### Scenario: Worker 不可用
- **WHEN** Worker 连接失败、超时或返回不可用
- **THEN** 系统 SHALL 将任务标记为失败或保留在可重试状态，并记录 Worker 错误

### Requirement: Worker 状态同步
系统 SHALL 支持通过 Worker 状态查询或回调同步任务进度和结果。

#### Scenario: 查询 Worker 状态
- **WHEN** 系统需要刷新 Worker 任务进度
- **THEN** 系统 SHALL 调用 Worker 状态接口并映射为媒体任务状态

#### Scenario: Worker 回调运行进度
- **WHEN** Worker 回调任务运行进度
- **THEN** 系统 SHALL 更新任务进度和阶段

#### Scenario: Worker 回调完成结果
- **WHEN** Worker 回调任务完成并提供结果文件
- **THEN** 系统 SHALL 保存结果文件引用并将任务标记为成功

### Requirement: Worker 结果交付
系统 SHALL 支持 Worker 通过结果 URL 或上传后的文件引用交付结果。

#### Scenario: Worker 返回结果 URL
- **WHEN** Worker 返回可下载结果 URL
- **THEN** 系统 SHALL 下载或转存结果到项目文件服务，并记录项目侧文件引用

#### Scenario: Worker 返回项目文件引用
- **WHEN** Worker 已将结果上传到项目文件服务并返回文件引用
- **THEN** 系统 SHALL 校验文件引用并写入任务输出

### Requirement: Worker Skeleton 验收
系统 SHALL 提供一个可用于联调的最小 Worker skeleton 或 mock，验证提交、运行、成功和失败路径。

#### Scenario: Mock Worker 成功
- **WHEN** 系统向 mock Worker 提交测试任务
- **THEN** Worker SHALL 返回任务 ID、模拟进度并最终返回成功结果

#### Scenario: Mock Worker 失败
- **WHEN** 系统向 mock Worker 提交失败测试任务
- **THEN** Worker SHALL 返回失败状态和错误信息
