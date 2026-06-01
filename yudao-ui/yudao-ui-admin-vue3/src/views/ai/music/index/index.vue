<template>
  <div class="flex h-full items-stretch">
    <!-- 模式 -->
    <Mode
      class="flex-none"
      :loading="generating"
      :model-options="musicModelOptions"
      :model-loading="modelLoading"
      @generate-music="generateMusic"
    />
    <!-- 音频列表 -->
    <List ref="listRef" class="flex-auto" />
  </div>
</template>

<script lang="ts" setup>
import Mode from './mode/index.vue'
import List from './list/index.vue'
import { MusicApi, MusicGenerateReqVO } from '@/api/ai/music'
import { ModelApi, ModelVO } from '@/api/ai/model/model'
import { AiModelTypeEnum, AiMusicGenerateModeEnum, AiPlatformEnum } from '@/views/ai/utils/constants'

defineOptions({ name: 'AiMusic' })

const message = useMessage()
const listRef = ref<Nullable<{ refresh: () => Promise<void>; startPolling: (ids: number[]) => void }>>(null)
const generating = ref(false)
const modelLoading = ref(false)
const musicModels = ref<ModelVO[]>([])

interface MusicModelOption {
  label: string
  value: string
  model: string
  platform: string
  id?: number
}

const musicModelOptions = computed<MusicModelOption[]>(() => {
  return musicModels.value
    .filter(isMusicModelSupported)
    .map((model) => ({
      label: `${model.platform} / ${model.name} / ${model.model}`,
      value: String(model.id),
      model: model.model,
      platform: model.platform,
      id: model.id
    }))
})

function isMusicModelSupported(model: ModelVO) {
  if (model.platform === AiPlatformEnum.SUNO) {
    return true
  }
  return model.platform === AiPlatformEnum.XIAOMI_MIMO && model.model === 'mimo-v2.5-tts'
}

/*
 *@Description: 拿到左侧配置信息调用右侧音乐生成的方法
 *@MethodAuthor: xiaohong
 *@Date: 2024-07-19 11:13:38
 */
async function generateMusic(args: { mode: 'desc' | 'lyric'; formData: Recordable }) {
  const req = buildGenerateReq(args)
  if (!req) {
    return
  }
  generating.value = true
  try {
    const ids = await MusicApi.generateMusic(req)
    message.success('音乐任务已提交')
    await unref(listRef)?.refresh()
    unref(listRef)?.startPolling(ids)
  } finally {
    generating.value = false
  }
}

function buildGenerateReq(args: { mode: 'desc' | 'lyric'; formData: Recordable }): MusicGenerateReqVO | undefined {
  const formData = args.formData || {}
  const selectedModel = musicModelOptions.value.find((item) => item.value === formData.version)
  if (!selectedModel?.id) {
    message.warning('请先在模型管理配置可用的音乐模型')
    return
  }
  const model = selectedModel.model
  const platform = selectedModel.platform
  if (platform !== AiPlatformEnum.SUNO) {
    return buildMediaGenerateReq(args, formData, selectedModel, model, platform)
  }
  if (args.mode === 'desc') {
    if (!formData.desc) {
      message.warning('请输入音乐描述')
      return
    }
    return {
      platform,
      modelId: selectedModel?.id,
      generateMode: AiMusicGenerateModeEnum.DESCRIPTION,
      prompt: formData.desc,
      makeInstrumental: formData.pure,
      model,
      stylePrompt: '请用自然、有音乐感的方式演绎这段内容',
      voiceMode: 'BUILTIN',
      voice: 'mimo_default',
      format: 'wav'
    }
  }
  if (!formData.lyric) {
    message.warning('请输入歌词')
    return
  }
  if (!formData.name) {
    message.warning('请输入音乐/歌曲名称')
    return
  }
  const customTags = (formData.style || '')
    .split(/[,，\n]/)
    .map((item: string) => item.trim())
    .filter(Boolean)
  const tags = [...(Array.isArray(formData.tags) ? formData.tags : []), ...customTags]
  if (tags.length === 0) {
    message.warning('请输入音乐风格')
    return
  }
  return {
    platform,
    modelId: selectedModel?.id,
    generateMode: AiMusicGenerateModeEnum.LYRIC,
    prompt: formData.lyric,
    model,
    tags,
    title: formData.name,
    stylePrompt: tags.length > 0 ? `请以 ${tags.join(', ')} 风格演唱这段歌词` : undefined,
    voiceMode: 'BUILTIN',
    voice: 'mimo_default',
    format: 'wav'
  }
}

function buildMediaGenerateReq(
  args: { mode: 'desc' | 'lyric'; formData: Recordable },
  formData: Recordable,
  selectedModel: MusicModelOption | undefined,
  model: string,
  platform: string
): MusicGenerateReqVO | undefined {
  if (!formData.text) {
    message.warning(args.mode === 'lyric' ? '请输入歌词' : '请输入演绎文本')
    return
  }
  if (!formData.name) {
    message.warning('请输入音乐/歌曲名称')
    return
  }
  return {
    platform,
    modelId: selectedModel?.id,
    generateMode:
      args.mode === 'lyric' ? AiMusicGenerateModeEnum.LYRIC : AiMusicGenerateModeEnum.DESCRIPTION,
    prompt: formData.text,
    model,
    title: formData.name,
    stylePrompt: formData.stylePrompt,
    voiceMode: formData.voiceMode || 'BUILTIN',
    voice: formData.voice || 'mimo_default',
    format: formData.format || 'wav'
  }
}

async function loadMusicModels() {
  modelLoading.value = true
  try {
    musicModels.value = await ModelApi.getModelSimpleList(AiModelTypeEnum.VOICE)
  } finally {
    modelLoading.value = false
  }
}

onMounted(loadMusicModels)
</script>
