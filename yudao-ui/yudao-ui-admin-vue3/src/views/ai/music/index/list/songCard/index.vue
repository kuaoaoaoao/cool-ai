<template>
  <div class="flex bg-[var(--el-bg-color-overlay)] p-12px mb-12px rounded-1">
    <div class="relative" @click="playSong">
      <el-image :src="songInfo.imageUrl || defaultCover" class="flex-none w-80px h-80px" fit="cover" />
      <div
        class="bg-black bg-op-40 absolute top-0 left-0 w-full h-full flex items-center justify-center cursor-pointer"
      >
        <Icon
          :icon="
            currentSong.id === songInfo.id
              ? 'solar:pause-circle-bold'
              : 'mdi:arrow-right-drop-circle'
          "
          :size="30"
        />
      </div>
    </div>
    <div class="ml-8px min-w-0 flex-1">
      <div class="flex items-center gap-8px">
        <span>{{ songInfo.title || '生成中的音乐' }}</span>
        <dict-tag :type="DICT_TYPE.AI_MUSIC_STATUS" :value="songInfo.status" />
      </div>
      <div class="mt-8px text-12px text-[var(--el-text-color-secondary)] line-clamp-2">
        {{ description }}
      </div>
      <div v-if="songInfo.errorMessage" class="mt-8px text-12px text-[var(--el-color-danger)] line-clamp-1">
        {{ songInfo.errorMessage }}
      </div>
    </div>
  </div>
</template>

<script lang="ts" setup>
import { MusicVO } from '@/api/ai/music'
import { DICT_TYPE } from '@/utils/dict'

defineOptions({ name: 'AiMusicSongCard' })

const props = defineProps({
  songInfo: {
    type: Object,
    default: () => ({})
  }
})

const emits = defineEmits(['play'])

const currentSong = inject<Ref<Partial<MusicVO>>>('currentSong', ref({}))
const defaultCover = 'https://dummyimage.com/160x160/e8eaf0/8b94a7&text=AI+Music'

const description = computed(() => {
  const tags = Array.isArray(props.songInfo.tags) ? props.songInfo.tags.join(', ') : props.songInfo.tags
  return props.songInfo.description || props.songInfo.gptDescriptionPrompt || tags || props.songInfo.lyric || '等待 Suno 返回生成结果'
})

function playSong() {
  if (!props.songInfo.audioUrl) {
    return
  }
  emits('play')
}
</script>
