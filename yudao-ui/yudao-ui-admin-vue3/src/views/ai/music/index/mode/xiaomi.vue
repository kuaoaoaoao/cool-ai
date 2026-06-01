<template>
  <div>
    <Title :title="generateMode === 'lyric' ? '歌词' : '演绎文本'">
      <el-input
        v-model="formData.text"
        :autosize="{ minRows: 6, maxRows: 6 }"
        resize="none"
        type="textarea"
        maxlength="1200"
        show-word-limit
        :placeholder="generateMode === 'lyric' ? '请输入要演唱的歌词' : '请输入要用音乐感演绎的文本'"
      />
    </Title>

    <Title title="音乐/歌曲名称">
      <el-input v-model="formData.name" placeholder="请输入音乐/歌曲名称" />
    </Title>

    <Title title="演唱风格">
      <el-input
        v-model="formData.stylePrompt"
        :autosize="{ minRows: 3, maxRows: 3 }"
        resize="none"
        type="textarea"
        maxlength="300"
        show-word-limit
        placeholder="例如：流行、温柔、自然、有旋律感"
      />
    </Title>

    <Title title="音色">
      <el-select
        v-if="voiceOptions.length > 0"
        v-model="formData.voice"
        filterable
        allow-create
        default-first-option
        placeholder="请选择或输入音色"
      >
        <el-option
          v-for="dict in voiceOptions"
          :key="dict.value"
          :label="dict.label"
          :value="dict.value"
        />
      </el-select>
      <el-input v-else v-model="formData.voice" placeholder="请输入音色，如 mimo_default、冰糖、Chloe" />
    </Title>

    <Title title="格式">
      <el-select v-model="formData.format" placeholder="请选择音频格式">
        <el-option
          v-for="item in formatOptions"
          :key="item.value"
          :label="item.label"
          :value="item.value"
        />
      </el-select>
    </Title>
  </div>
</template>

<script lang="ts" setup>
import Title from '../title/index.vue'
import { DICT_TYPE, getStrDictOptions } from '@/utils/dict'

defineOptions({ name: 'XiaomiMusicMode' })

defineProps({
  generateMode: {
    type: String,
    default: 'lyric'
  }
})

const voiceOptions = computed(() => getStrDictOptions(DICT_TYPE.AI_MEDIA_TTS_VOICE))
const formatOptions = computed(() => {
  const dictOptions = getStrDictOptions(DICT_TYPE.AI_MEDIA_TTS_FORMAT)
  return dictOptions.length > 0
    ? dictOptions
    : [
        { label: 'MP3', value: 'mp3' },
        { label: 'WAV', value: 'wav' },
        { label: 'PCM', value: 'pcm' }
      ]
})

const formData = reactive({
  text: '',
  name: '',
  stylePrompt: '',
  voice: 'mimo_default',
  voiceMode: 'BUILTIN',
  format: 'wav'
})

defineExpose({
  formData
})
</script>
