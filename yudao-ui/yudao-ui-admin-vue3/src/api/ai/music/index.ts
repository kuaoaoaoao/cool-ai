import request from '@/config/axios'

// AI 音乐 VO
export interface MusicVO {
  id: number // 编号
  userId: number // 用户编号
  title: string // 音乐名称
  lyric: string // 歌词
  imageUrl: string // 图片地址
  audioUrl: string // 音频地址
  videoUrl: string // 视频地址
  status: number // 音乐状态
  description?: string // 描述词
  gptDescriptionPrompt: string // 描述词
  prompt: string // 提示词
  platform: string // 模型平台
  model: string // 模型
  generateMode: number // 生成模式
  tags: string[] // 音乐风格标签
  duration: number // 音乐时长
  publicStatus: boolean // 是否发布
  taskId: string // 任务id
  errorMessage: string // 错误信息
  createTime?: Date // 创建时间
}

export interface MusicGenerateReqVO {
  platform: string
  modelId?: number
  generateMode: number
  prompt: string
  makeInstrumental?: boolean
  model: string
  tags?: string[]
  title?: string
  stylePrompt?: string
  voice?: string
  voiceMode?: string
  format?: string
}

// AI 音乐 API
export const MusicApi = {
  // 获得【我的】音乐分页
  getMusicPageMy: async (params: any) => {
    return await request.get({ url: `/ai/music/my-page`, params })
  },

  // 获得【我的】音乐
  getMusicMy: async (id: number) => {
    return await request.get({ url: `/ai/music/get-my?id=${id}` })
  },

  // 生成音乐
  generateMusic: async (data: MusicGenerateReqVO) => {
    return await request.post({ url: `/ai/music/generate`, data })
  },

  // 删除【我的】音乐
  deleteMusicMy: async (id: number) => {
    return await request.delete({ url: `/ai/music/delete-my?id=${id}` })
  },

  // 查询音乐分页
  getMusicPage: async (params: any) => {
    return await request.get({ url: `/ai/music/page`, params })
  },

  // 更新音乐
  updateMusic: async (data: any) => {
    return await request.put({ url: '/ai/music/update', data })
  },

  // 删除音乐
  deleteMusic: async (id: number) => {
    return await request.delete({ url: `/ai/music/delete?id=` + id })
  }
}
