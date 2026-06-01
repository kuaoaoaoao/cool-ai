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
  executionMode?: string
  retryCount?: number
  cancelRequested?: boolean
  submittedTime?: string
  callbackStatus?: string
  sourceAssets?: any
  resultAssets?: any
  errorCode?: string
  errorMessage?: string
  finishTime?: Date
  createTime?: Date
}

export interface AiMediaTtsCreateReqVO {
  modelId?: number
  voiceProfileId?: number
  voiceMode?: string
  text: string
  stylePrompt?: string
  voice?: string
  designPrompt?: string
  sampleFileUrl?: string
  sampleMimeType?: string
  sampleSize?: number
  consentConfirmed?: boolean
  format: string
}

export interface AiMediaImageToVideoCreateReqVO {
  modelId?: number
  sourceImageUrl: string
  sourceMimeType?: string
  sourceSize?: number
  prompt: string
  negativePrompt?: string
  imageSize?: string
  seed?: number
  duration?: number
  quality?: string
}

export interface AiMediaProviderHealthVO {
  PythonWorker?: {
    ok?: boolean
    enabled?: boolean
    baseUrl?: string
    status?: string
    mode?: string
    realGeneration?: boolean
    playableMockVideo?: boolean
    errorCode?: string
    errorMessage?: string
  }
  xxlJobHandlers?: string[]
}

export interface AiVoiceProfileVO {
  id?: number
  userId?: number
  name: string
  sourceType: string
  platform?: string
  modelId?: number
  model?: string
  builtinVoice?: string
  designPrompt?: string
  sampleFileUrl?: string
  sampleMimeType?: string
  sampleSize?: number
  consentConfirmed?: boolean
  previewTaskId?: number
  previewAudioUrl?: string
  status: number
  remark?: string
  createTime?: Date
}

export interface AiVoiceProfilePreviewReqVO {
  id: number
  text: string
  format?: string
}

export const AiMediaTaskApi = {
  createTtsTask: async (data: AiMediaTtsCreateReqVO) => {
    return await request.post({ url: '/ai/media/task/tts', data })
  },

  createImageToVideoTask: async (data: AiMediaImageToVideoCreateReqVO) => {
    return await request.post({ url: '/ai/media/task/image-to-video', data })
  },

  getMyTask: async (id: number) => {
    return await request.get({ url: '/ai/media/task/get-my?id=' + id })
  },

  getMyTaskPage: async (params: any) => {
    return await request.get({ url: '/ai/media/task/my-page', params })
  },

  getTaskPage: async (params: any) => {
    return await request.get({ url: '/ai/media/task/page', params })
  },

  retryMyTask: async (id: number) => {
    return await request.post({ url: '/ai/media/task/retry-my?id=' + id })
  },

  cancelMyTask: async (id: number) => {
    return await request.post({ url: '/ai/media/task/cancel-my?id=' + id })
  },

  getProviderHealth: async () => {
    return await request.get({ url: '/ai/media/task/provider-health' })
  }
}

export const AiVoiceProfileApi = {
  createVoiceProfile: async (data: AiVoiceProfileVO) => {
    return await request.post({ url: '/ai/voice-profile/create', data })
  },

  updateVoiceProfile: async (data: AiVoiceProfileVO) => {
    return await request.put({ url: '/ai/voice-profile/update', data })
  },

  deleteVoiceProfile: async (id: number) => {
    return await request.delete({ url: '/ai/voice-profile/delete?id=' + id })
  },

  getVoiceProfile: async (id: number) => {
    return await request.get({ url: '/ai/voice-profile/get?id=' + id })
  },

  getVoiceProfilePage: async (params: any) => {
    return await request.get({ url: '/ai/voice-profile/my-page', params })
  },

  getVoiceProfileSimpleList: async (status?: number) => {
    return await request.get({ url: '/ai/voice-profile/simple-list', params: { status } })
  },

  previewVoiceProfile: async (data: AiVoiceProfilePreviewReqVO) => {
    return await request.post({ url: '/ai/voice-profile/preview', data })
  }
}
