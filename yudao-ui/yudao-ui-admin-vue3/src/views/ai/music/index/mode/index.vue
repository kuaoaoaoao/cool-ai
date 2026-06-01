<template>
  <ContentWrap class="w-300px h-full mb-[0!important]">
    <Title title="模型">
      <el-select v-model="selectedModelValue" filterable :loading="modelLoading" placeholder="请选择音乐模型">
        <el-option
          v-for="item in modelOptions"
          :key="item.value"
          :label="item.label"
          :value="item.value"
        />
      </el-select>
    </Title>

    <el-radio-group v-model="generateMode" class="mb-15px">
      <el-radio-button value="desc"> 描述模式 </el-radio-button>
      <el-radio-button value="lyric"> 歌词模式 </el-radio-button>
    </el-radio-group>

    <component
      :is="currentModeComponent"
      ref="modeRef"
      :generate-mode="generateMode"
    />

    <el-button
      type="primary"
      round
      class="w-full"
      :loading="loading"
      :disabled="modelLoading || !selectedModelValue"
      @click="generateMusic"
    >
      创作音乐
    </el-button>
  </ContentWrap>
</template>

<script lang="ts" setup>
import Title from '../title/index.vue'
import desc from './desc.vue'
import lyric from './lyric.vue'
import xiaomi from './xiaomi.vue'
import type { PropType } from 'vue'
import { AiPlatformEnum } from '@/views/ai/utils/constants'

defineOptions({ name: 'AiMusicMode' })

const emits = defineEmits(['generate-music'])

const props = defineProps({
  loading: {
    type: Boolean,
    default: false
  },
  modelOptions: {
    type: Array as PropType<Array<{ label: string; value: string; platform?: string; model?: string }>>,
    default: () => []
  },
  modelLoading: {
    type: Boolean,
    default: false
  }
})

const generateMode = ref('lyric')
const selectedModelValue = ref('')

const modeRef = ref<Nullable<{ formData: Recordable }>>(null)
const selectedModel = computed(() => props.modelOptions.find((item) => item.value === selectedModelValue.value))
const isSunoModel = computed(() => !selectedModel.value || selectedModel.value.platform === AiPlatformEnum.SUNO)
const currentModeComponent = computed(() => {
  if (!isSunoModel.value) {
    return xiaomi
  }
  return generateMode.value === 'desc' ? desc : lyric
})

watch(
  () => props.modelOptions,
  (options) => {
    if (!options.length) {
      selectedModelValue.value = ''
      return
    }
    if (!options.some((item) => item.value === selectedModelValue.value)) {
      selectedModelValue.value = options[0].value
    }
  },
  { immediate: true }
)

/*
 *@Description: 根据信息生成音乐
 *@MethodAuthor: xiaohong
 *@Date: 2024-06-27 16:40:16
 */
function generateMusic() {
  emits('generate-music', {
    mode: generateMode.value,
    formData: {
      ...(unref(modeRef)?.formData || {}),
      version: selectedModelValue.value
    }
  })
}
</script>
