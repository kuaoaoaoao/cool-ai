import request from '@/config/axios'

export interface AiMediaTaskVO {
  id: number
  userId: number
  taskType: number
  platform: string
  modelId?: number
  model: string
  status: number
  progress: number
  stage?: string
  inputJson?: Record<string, any>
  outputJson?: Record<string, any>
  sourceFileIds?: string[]
  resultFileIds?: string[]
  providerTaskId?: string
  errorCode?: string
  errorMessage?: string
  finishTime?: Date
  createTime?: Date
}

export interface AiMediaTtsCreateReqVO {
  modelId?: number
  text: string
  stylePrompt?: string
  voice: string
  format: string
}

export const AiMediaTaskApi = {
  createTtsTask: async (data: AiMediaTtsCreateReqVO) => {
    return await request.post({ url: '/ai/media/task/tts', data })
  },

  getMyTask: async (id: number) => {
    return await request.get({ url: '/ai/media/task/get-my?id=' + id })
  },

  getMyTaskPage: async (params: any) => {
    return await request.get({ url: '/ai/media/task/my-page', params })
  },

  getTaskPage: async (params: any) => {
    return await request.get({ url: '/ai/media/task/page', params })
  }
}
