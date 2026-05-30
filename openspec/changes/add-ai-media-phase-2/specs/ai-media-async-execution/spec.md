## ADDED Requirements

### Requirement: 异步媒体任务生命周期
系统 SHALL 为第二阶段媒体任务提供异步生命周期，至少覆盖创建、排队、提交、运行、成功、失败、重试、取消中和已取消状态。

#### Scenario: 创建后进入队列
- **WHEN** 用户创建需要异步执行的媒体任务
- **THEN** 系统 SHALL 创建任务记录并将任务置为可执行队列状态

#### Scenario: 提交到外部执行器
- **WHEN** 执行器将任务提交到 Provider 或 Python Worker
- **THEN** 系统 SHALL 保存执行模式、外部任务 ID 和提交时间

#### Scenario: 任务完成
- **WHEN** Provider 或 Worker 返回成功结果
- **THEN** 系统 SHALL 保存结果并将任务状态更新为成功

### Requirement: 任务状态查询
系统 SHALL 提供统一任务状态查询接口，供前端、聊天工具和工作流节点查询媒体任务状态。

#### Scenario: 查询运行中任务
- **WHEN** 调用方查询运行中媒体任务
- **THEN** 系统 SHALL 返回任务状态、进度、阶段、Provider、模型、错误信息和结果摘要

#### Scenario: 查询终态任务
- **WHEN** 调用方查询成功、失败或已取消任务
- **THEN** 系统 SHALL 返回终态信息且不得再回退到非终态

### Requirement: Provider 回调处理
系统 SHALL 支持 Provider 或 Worker 通过统一回调入口更新任务状态，并记录回调日志。

#### Scenario: 接收合法回调
- **WHEN** Provider 或 Worker 发送合法任务回调
- **THEN** 系统 SHALL 校验回调、记录原始载荷、规范化状态并更新任务

#### Scenario: 接收重复回调
- **WHEN** 系统收到已处理过的重复回调
- **THEN** 系统 SHALL 保持任务状态幂等且记录重复回调信息

#### Scenario: 接收乱序回调
- **WHEN** 系统收到会使成功或失败任务回退状态的旧回调
- **THEN** 系统 MUST 拒绝状态回退并保留审计记录

### Requirement: 任务重试
系统 SHALL 支持对可重试失败任务进行重试，并限制最大重试次数。

#### Scenario: 重试失败任务
- **WHEN** 用户或系统重试可重试的失败任务
- **THEN** 系统 SHALL 增加重试次数并重新进入队列或提交状态

#### Scenario: 超过重试限制
- **WHEN** 任务重试次数达到上限
- **THEN** 系统 MUST 拒绝继续重试并返回明确错误

### Requirement: 任务取消
系统 SHALL 支持用户取消尚未完成且 Provider/Worker 支持取消的媒体任务。

#### Scenario: 取消排队任务
- **WHEN** 用户取消尚未提交到外部执行器的任务
- **THEN** 系统 SHALL 直接将任务标记为已取消

#### Scenario: 取消已提交任务
- **WHEN** 用户取消已提交到 Provider 或 Worker 的任务
- **THEN** 系统 SHALL 调用 Adapter 取消接口或标记取消请求，并最终更新为已取消或取消失败
