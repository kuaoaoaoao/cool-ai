<template>
  <div
    class="flex items-center justify-between px-2 h-72px bg-[var(--el-bg-color-overlay)] b-solid b-1 b-[var(--el-border-color)] b-l-none"
  >
    <div class="flex gap-[10px]">
      <el-image :src="currentSong.imageUrl || defaultCover" class="w-[45px] h-[45px]" fit="cover" />
      <div>
        <div>{{ currentSong.title || '未选择音乐' }}</div>
        <div class="text-[12px] text-gray-400">{{ currentSong.model || 'Suno' }}</div>
      </div>
    </div>

    <div class="flex gap-[12px] items-center">
      <Icon icon="majesticons:back-circle" :size="20" class="text-gray-300 cursor-pointer" />
      <Icon
        :icon="isPaused ? 'mdi:arrow-right-drop-circle' : 'solar:pause-circle-bold'"
        :size="30"
        :class="currentSong.audioUrl ? 'cursor-pointer' : 'cursor-not-allowed text-gray-300'"
        @click="togglePlay"
      />
      <Icon icon="majesticons:next-circle" :size="20" class="text-gray-300 cursor-pointer" />
      <div class="flex gap-[16px] items-center">
        <span>{{ currentTimeText }}</span>
        <el-slider
          v-model="currentTime"
          :max="duration || 0"
          color="#409eff"
          class="w-[160px!important]"
          @change="handleSeek"
        />
        <span>{{ durationText }}</span>
      </div>
      <audio
        ref="audioRef"
        :src="currentSong.audioUrl"
        class="hidden"
        @loadedmetadata="handleLoadedMetadata"
        @timeupdate="audioTimeUpdate"
        @ended="isPaused = true"
      ></audio>
    </div>

    <div class="flex gap-[16px] items-center">
      <Icon
        :icon="muted ? 'tabler:volume-off' : 'tabler:volume'"
        :size="20"
        class="cursor-pointer"
        @click="toggleMuted"
      />
      <el-slider v-model="volume" color="#409eff" class="w-[160px!important]" @change="handleVolumeChange" />
    </div>
  </div>
</template>

<script lang="ts" setup>
import { MusicVO } from '@/api/ai/music'

defineOptions({ name: 'AiMusicAudioBar' })

const currentSong = inject<Ref<Partial<MusicVO>>>('currentSong', ref({}))
const defaultCover = 'https://dummyimage.com/90x90/e8eaf0/8b94a7&text=AI'

const audioRef = ref<Nullable<HTMLAudioElement>>(null)
const isPaused = ref(true)
const currentTime = ref(0)
const duration = ref(0)
const muted = ref(false)
const volume = ref(50)

const currentTimeText = computed(() => formatSeconds(currentTime.value))
const durationText = computed(() => formatSeconds(duration.value))

watch(
  () => currentSong.value.audioUrl,
  async () => {
    isPaused.value = true
    currentTime.value = 0
    duration.value = 0
    await nextTick()
    audioRef.value?.load()
  }
)

function togglePlay() {
  if (!currentSong.value.audioUrl || !audioRef.value) {
    return
  }
  if (isPaused.value) {
    audioRef.value.play()
    isPaused.value = false
  } else {
    audioRef.value.pause()
    isPaused.value = true
  }
}

function toggleMuted() {
  muted.value = !muted.value
  if (audioRef.value) {
    audioRef.value.muted = muted.value
  }
}

function handleVolumeChange(value: number) {
  if (audioRef.value) {
    audioRef.value.volume = value / 100
  }
}

function handleSeek(value: number) {
  if (audioRef.value) {
    audioRef.value.currentTime = value
  }
}

function handleLoadedMetadata() {
  duration.value = audioRef.value?.duration || 0
  handleVolumeChange(volume.value)
  if (audioRef.value) {
    audioRef.value.muted = muted.value
  }
}

function audioTimeUpdate() {
  currentTime.value = audioRef.value?.currentTime || 0
}

function formatSeconds(value: number) {
  if (!Number.isFinite(value) || value <= 0) {
    return '00:00'
  }
  const minutes = Math.floor(value / 60)
  const seconds = Math.floor(value % 60)
  return `${String(minutes).padStart(2, '0')}:${String(seconds).padStart(2, '0')}`
}
</script>
