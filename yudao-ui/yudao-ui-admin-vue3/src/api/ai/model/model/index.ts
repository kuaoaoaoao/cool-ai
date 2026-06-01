import request from '@/config/axios'

// AI 模型 VO
export interface ModelVO {
  id: number // 编号
  keyId: number // API 秘钥编号
  name: string // 模型名字
  model: string // 模型标识
  platform: string // 模型平台
  type: number // 模型类型
  sort: number // 排序
  status: number // 状态
  temperature?: number // 温度参数
  maxTokens?: number // 单条回复的最大 Token 数量
  maxContexts?: number // 上下文的最大 Message 数量
  capabilities?: string[] // 能力标签
  inputModalities?: string[] // 输入模态
  outputModalities?: string[] // 输出模态
  options?: Record<string, any> // 模型默认参数
  limits?: Record<string, any> // 模型限制信息
  metadata?: Record<string, any> // 扩展元数据
  configTips?: string[] // 配置提示
}

// AI 模型连接测试 VO
export interface ModelConnectionTestRespVO {
  success: boolean // 是否成功
  target: string // 测试对象
  modelId?: number // 模型编号
  keyId?: number // API Key 编号
  platform?: string // 平台
  type?: number // 模型类型
  model?: string // 模型标识
  durationMillis?: number // 耗时
  message?: string // 提示
  errorCode?: string // 错误码
  detail?: string // 错误详情
}

// AI 模型 API
export const ModelApi = {
  // 查询模型分页
  getModelPage: async (params: any) => {
    return await request.get({ url: `/ai/model/page`, params })
  },

  // 获得模型列表
  getModelSimpleList: async (type?: number, platform?: string) => {
    return await request.get({
      url: `/ai/model/simple-list`,
      params: {
        type,
        platform
      }
    })
  },

  // 查询模型详情
  getModel: async (id: number) => {
    return await request.get({ url: `/ai/model/get?id=` + id })
  },

  // 新增模型
  createModel: async (data: ModelVO) => {
    return await request.post({ url: `/ai/model/create`, data })
  },

  // 修改模型
  updateModel: async (data: ModelVO) => {
    return await request.put({ url: `/ai/model/update`, data })
  },

  // 删除模型
  deleteModel: async (id: number) => {
    return await request.delete({ url: `/ai/model/delete?id=` + id })
  },

  // 测试模型连接
  testModelConnection: async (id: number) => {
    return await request.post({ url: `/ai/model/test-connection?id=` + id })
  }
}
