<template>
  <ContentWrap class="w-300px mb-[0!important] line-height-24px">
    <el-empty v-if="!currentSong.id" description="请选择音乐" />
    <template v-else>
      <el-image :src="currentSong.imageUrl || defaultCover" class="w-full" fit="cover" />
      <div class="mt-8px">{{ currentSong.title || '生成中的音乐' }}</div>
      <div class="text-[var(--el-text-color-secondary)] text-12px line-clamp-2">
        {{ description }}
      </div>
      <div class="text-[var(--el-text-color-secondary)] text-12px">
        {{ currentSong.createTime ? formatDate(currentSong.createTime) : '' }}
      </div>
      <div class="mt-8px whitespace-pre-wrap text-[var(--el-text-color-secondary)] text-12px">
        {{ currentSong.lyric || '暂无歌词' }}
      </div>
    </template>
  </ContentWrap>
</template>

<script lang="ts" setup>
import { MusicVO } from '@/api/ai/music'
import { formatDate } from '@/utils/formatTime'

defineOptions({ name: 'AiMusicSongInfo' })

const currentSong = inject<Ref<Partial<MusicVO>>>('currentSong', ref({}))
const defaultCover = 'https://dummyimage.com/300x300/e8eaf0/8b94a7&text=AI+Music'
const description = computed(() => {
  const song = currentSong.value
  const tags = Array.isArray(song.tags) ? song.tags.join(', ') : song.tags
  return song.description || song.gptDescriptionPrompt || tags || song.errorMessage || '暂无描述'
})
</script>
