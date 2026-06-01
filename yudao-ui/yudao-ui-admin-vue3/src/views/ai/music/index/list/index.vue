<template>
  <div class="flex flex-col">
    <div class="flex-auto flex overflow-hidden">
      <el-tabs v-model="currentType" class="flex-auto px-[var(--app-content-padding)]">
        <!-- 我的创作 -->
        <el-tab-pane v-loading="loading" label="我的创作" name="mine">
          <el-row v-if="mySongList.length" :gutter="12">
            <el-col v-for="song in mySongList" :key="song.id" :span="24">
              <songCard :songInfo="song" @play="setCurrentSong(song)" />
            </el-col>
          </el-row>
          <el-empty v-else description="暂无音乐" />
        </el-tab-pane>

        <!-- 试听广场 -->
        <el-tab-pane v-loading="loading" label="试听广场" name="square">
          <el-row v-if="squareSongList.length" :gutter="12">
            <el-col v-for="song in squareSongList" :key="song.id" :span="24">
              <songCard :songInfo="song" @play="setCurrentSong(song)" />
            </el-col>
          </el-row>
          <el-empty v-else description="暂无音乐" />
        </el-tab-pane>
      </el-tabs>
      <!-- songInfo -->
      <songInfo class="flex-none" />
    </div>
    <audioBar class="flex-none" />
  </div>
</template>

<script lang="ts" setup>
import songCard from './songCard/index.vue'
import songInfo from './songInfo/index.vue'
import audioBar from './audioBar/index.vue'
import { MusicApi, MusicVO } from '@/api/ai/music'
import { AiMusicStatusEnum } from '@/views/ai/utils/constants'

defineOptions({ name: 'AiMusicList' })

const currentType = ref('mine')
// loading 状态
const loading = ref(false)
// 当前音乐
const currentSong = ref<Partial<MusicVO>>({})

const mySongList = ref<MusicVO[]>([])
const squareSongList = ref<MusicVO[]>([])
const pollingTimer = ref<number>()

provide('currentSong', currentSong)

async function getList() {
  loading.value = true
  try {
    const [mine, square] = await Promise.all([
      MusicApi.getMusicPageMy({ pageNo: 1, pageSize: 30, publicStatus: false }),
      MusicApi.getMusicPageMy({ pageNo: 1, pageSize: 30, publicStatus: true })
    ])
    mySongList.value = mine.list || []
    squareSongList.value = square.list || []
    if (!currentSong.value.id && mySongList.value.length > 0) {
      currentSong.value = mySongList.value[0]
    } else if (currentSong.value.id) {
      const latest = [...mySongList.value, ...squareSongList.value].find((song) => song.id === currentSong.value.id)
      if (latest) {
        currentSong.value = latest
      }
    }
  } finally {
    loading.value = false
  }
}

async function refresh() {
  await getList()
}

function startPolling(ids: number[]) {
  stopPolling()
  if (!ids?.length) {
    return
  }
  pollingTimer.value = window.setInterval(async () => {
    const tasks = await Promise.all(ids.map((id) => MusicApi.getMusicMy(id)))
    await getList()
    if (
      tasks
        .filter(Boolean)
        .every((task) => [AiMusicStatusEnum.SUCCESS, AiMusicStatusEnum.FAIL].includes(task.status))
    ) {
      stopPolling()
    }
  }, 5000)
}

function stopPolling() {
  if (pollingTimer.value) {
    window.clearInterval(pollingTimer.value)
    pollingTimer.value = undefined
  }
}

watch(currentType, async () => {
  await getList()
})

onMounted(async () => {
  await getList()
  if (mySongList.value.some((song) => song.status === AiMusicStatusEnum.IN_PROGRESS)) {
    startPolling(mySongList.value.filter((song) => song.status === AiMusicStatusEnum.IN_PROGRESS).map((song) => song.id))
  }
})

onBeforeUnmount(stopPolling)

function setCurrentSong(music: MusicVO) {
  if (!music.audioUrl && music.status !== AiMusicStatusEnum.SUCCESS) {
    return
  }
  currentSong.value = music
}

defineExpose({
  refresh,
  startPolling
})
</script>

<style lang="scss" scoped>
:deep(.el-tabs) {
  display: flex;
  flex-direction: column;

  .el-tabs__content {
    padding: 0 7px;
    overflow: auto;
  }
}
</style>
