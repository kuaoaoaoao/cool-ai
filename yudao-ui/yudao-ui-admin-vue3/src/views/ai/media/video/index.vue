<template>
  <div class="grid gap-16px lg:grid-cols-[420px_minmax(0,1fr)]">
    <ContentWrap>
      <template #header>
        <div class="flex w-full items-start justify-between gap-12px">
          <div>
            <div class="text-18px font-bold">AI 图生视频</div>
            <div class="mt-4px text-13px text-[var(--el-text-color-secondary)]">
              图生视频任务会先排队提交，再由后台同步 SiliconFlow 或 PythonWorker 的生成结果。
            </div>
          </div>
          <el-button :loading="checkingProvider" @click="handleCheckProvider">
            <Icon icon="ep:connection" class="mr-5px" />
            检测 Worker
          </el-button>
        </div>
      </template>

      <el-alert
        v-if="videoModelList.length === 0"
        type="warning"
        :closable="false"
        class="mb-16px"
        title="未找到可用的图生视频模型"
      />

      <el-alert
        v-if="providerHealth"
        :type="providerHealth.ok ? 'success' : 'error'"
        :closable="false"
        class="mb-16px"
      >
        <template #title>
          <div class="provider-health-title">
            <span>PythonWorker</span>
            <el-tag size="small" :type="providerHealth.ok ? 'success' : 'danger'" effect="plain">
              {{ providerHealth.ok ? '已连接' : '不可用' }}
            </el-tag>
            <el-tag v-if="providerHealth.mode" size="small" effect="plain">{{ providerHealth.mode }}</el-tag>
            <el-tag v-if="providerHealth.realGeneration === false" size="small" type="warning" effect="plain">
              mock
            </el-tag>
          </div>
        </template>
        <div class="provider-health-detail">
          <span>{{ providerHealth.baseUrl || '-' }}</span>
          <span v-if="providerHealth.errorMessage" class="text-[var(--el-color-danger)]">
            {{ providerHealth.errorMessage }}
          </span>
        </div>
      </el-alert>

      <el-form ref="formRef" :model="formData" :rules="formRules" label-width="82px">
        <el-form-item label="生成模式">
          <el-segmented v-model="generationMode" :options="generationModeOptions" />
        </el-form-item>
        <el-form-item label="视频模型" prop="modelId">
          <el-select v-model="formData.modelId" clearable placeholder="不选则使用默认视频模型">
            <el-option
              v-for="model in videoModelList"
              :key="model.id"
              :label="`${model.name} / ${model.platform} / ${model.model}`"
              :value="model.id"
            />
          </el-select>
        </el-form-item>
        <el-form-item label="源图片" prop="sourceImageUrl">
          <UploadImg
            v-model="formData.sourceImageUrl"
            directory="ai-media/source"
            width="100%"
            height="240px"
            :file-size="10"
            :file-type="['image/jpeg', 'image/png', 'image/webp']"
            borderradius="6px"
          >
            <template #empty>
              <div class="flex flex-col items-center gap-8px text-[var(--el-text-color-secondary)]">
                <Icon icon="ep:picture" class="text-28px" />
                <span>上传源图片</span>
              </div>
            </template>
            <template #tip>
              <span>支持 jpg/png/webp；本地联调可用 PythonWorker，公网 Provider 建议使用可访问的文件地址。</span>
            </template>
          </UploadImg>
        </el-form-item>
        <el-form-item label="图片地址" prop="sourceImageUrl">
          <el-input v-model="formData.sourceImageUrl" clearable placeholder="也可以直接粘贴图片 URL" />
        </el-form-item>
        <el-form-item label="提示词" prop="prompt">
          <el-input
            v-model="formData.prompt"
            type="textarea"
            :rows="5"
            maxlength="1000"
            show-word-limit
            placeholder="描述画面运动、镜头变化和氛围"
          />
        </el-form-item>
        <el-form-item label="负向提示" prop="negativePrompt">
          <el-input
            v-model="formData.negativePrompt"
            type="textarea"
            :rows="3"
            maxlength="500"
            show-word-limit
            placeholder="例如：模糊、畸变、低清晰度"
          />
        </el-form-item>
        <el-form-item label="视频尺寸" prop="imageSize">
          <el-segmented v-model="formData.imageSize" :options="imageSizeOptions" />
        </el-form-item>
        <el-form-item label="时长" prop="duration">
          <el-input-number v-model="formData.duration" :min="1" :max="10" controls-position="right" class="!w-full" />
        </el-form-item>
        <el-form-item label="质量" prop="quality">
          <el-select v-model="formData.quality" clearable placeholder="由模型默认决定">
            <el-option label="标准" value="standard" />
            <el-option label="高清" value="high" />
          </el-select>
        </el-form-item>
        <el-form-item label="随机种子" prop="seed">
          <el-input-number v-model="formData.seed" :min="0" :max="2147483647" controls-position="right" class="!w-full" />
        </el-form-item>
        <el-form-item>
          <el-button
            type="primary"
            :loading="submitting"
            :disabled="videoModelList.length === 0"
            @click="handleCreate"
            v-hasPermi="['ai:media:video:create']"
          >
            <Icon icon="ep:video-camera" class="mr-5px" />
            生成视频
          </el-button>
          <el-button @click="resetForm">重置</el-button>
        </el-form-item>
      </el-form>
    </ContentWrap>

    <ContentWrap>
      <template #header>
        <div class="flex w-full items-center justify-between">
          <div>
            <div class="text-18px font-bold">我的视频任务</div>
            <div class="mt-4px text-13px text-[var(--el-text-color-secondary)]">
              任务完成后展示项目文件服务中的视频结果。
            </div>
          </div>
          <el-button @click="getList">
            <Icon icon="ep:refresh" class="mr-5px" />
            刷新
          </el-button>
        </div>
      </template>

      <el-table v-loading="loading" :data="list" :stripe="true" :show-overflow-tooltip="true">
        <el-table-column label="编号" align="center" prop="id" width="100" />
        <el-table-column label="源图" width="96">
          <template #default="{ row }">
            <el-image
              v-if="getSourceImageUrl(row)"
              :src="getSourceImageUrl(row)"
              fit="cover"
              class="h-56px w-72px rounded-4px"
              :preview-src-list="[getSourceImageUrl(row)]"
              preview-teleported
            />
          </template>
        </el-table-column>
        <el-table-column label="提示词" min-width="220">
          <template #default="{ row }">
            {{ row.inputJson?.prompt }}
          </template>
        </el-table-column>
        <el-table-column label="状态" align="center" width="110">
          <template #default="{ row }">
            <dict-tag :type="DICT_TYPE.AI_MEDIA_TASK_STATUS" :value="row.status" />
          </template>
        </el-table-column>
        <el-table-column label="进度" align="center" width="130">
          <template #default="{ row }">
            <el-progress :percentage="row.progress || 0" :stroke-width="8" />
          </template>
        </el-table-column>
        <el-table-column label="阶段" align="center" prop="stage" width="150" />
        <el-table-column label="平台/模型" min-width="180">
          <template #default="{ row }">
            <div>{{ row.platform }}</div>
            <div class="text-12px text-[var(--el-text-color-secondary)]">{{ row.model }}</div>
          </template>
        </el-table-column>
        <el-table-column label="耗时" align="center" width="100">
          <template #default="{ row }">
            {{ getElapsedText(row) }}
          </template>
        </el-table-column>
        <el-table-column label="结果" min-width="280">
          <template #default="{ row }">
            <div v-if="getVideoUrl(row)" class="video-result">
              <button class="video-preview" type="button" @click="openVideoPreview(row)">
                <video :src="getVideoUrl(row)" muted preload="metadata"></video>
                <span class="video-preview__play">
                  <Icon icon="ep:video-play" />
                </span>
              </button>
              <div class="video-actions">
                <el-button link type="primary" @click="openVideoPreview(row)">
                  <Icon icon="ep:view" class="mr-3px" />
                  预览
                </el-button>
                <el-link type="primary" :href="getVideoUrl(row)" target="_blank">
                  <Icon icon="ep:top-right" class="mr-3px" />
                  打开
                </el-link>
                <el-link type="primary" :href="getVideoUrl(row)" target="_blank" download>
                  <Icon icon="ep:download" class="mr-3px" />
                  下载
                </el-link>
                <el-button link type="primary" @click="copyToClipboard(getVideoUrl(row)!)">
                  <Icon icon="ep:document-copy" class="mr-3px" />
                  复制
                </el-button>
              </div>
            </div>
            <div v-else-if="row.errorMessage" class="flex flex-col gap-4px">
              <el-tag type="danger" effect="plain" class="w-fit">{{ getFriendlyErrorTitle(row) }}</el-tag>
              <span class="text-[var(--el-color-danger)]">{{ row.errorMessage }}</span>
            </div>
            <span v-else class="text-[var(--el-text-color-secondary)]">等待生成</span>
          </template>
        </el-table-column>
        <el-table-column label="创建时间" align="center" prop="createTime" :formatter="dateFormatter" width="180" />
        <el-table-column label="操作" align="center" width="190" fixed="right">
          <template #default="{ row }">
            <div class="flex flex-wrap justify-center gap-8px">
              <el-button
                v-if="canRetry(row)"
                link
                type="primary"
                :loading="operatingTaskId === row.id"
                @click="handleRetry(row)"
              >
                <Icon icon="ep:refresh-right" class="mr-3px" />
                重试
              </el-button>
              <el-button
                v-if="canCancel(row)"
                link
                type="warning"
                :loading="operatingTaskId === row.id"
                @click="handleCancel(row)"
              >
                <Icon icon="ep:close" class="mr-3px" />
                取消
              </el-button>
              <el-button v-if="getVideoUrl(row)" link type="primary" @click="openVideoPreview(row)">
                <Icon icon="ep:view" class="mr-3px" />
                预览
              </el-button>
            </div>
          </template>
        </el-table-column>
      </el-table>
      <Pagination
        :total="total"
        v-model:page="queryParams.pageNo"
        v-model:limit="queryParams.pageSize"
        @pagination="getList"
      />
    </ContentWrap>

    <el-dialog
      v-model="videoPreviewVisible"
      title="视频预览"
      width="min(920px, 92vw)"
      append-to-body
      destroy-on-close
      class="ai-video-preview-dialog"
    >
      <div class="video-dialog-body">
        <video v-if="currentVideoUrl" :src="currentVideoUrl" controls autoplay class="video-dialog-player"></video>
      </div>
      <template #footer>
        <div class="flex justify-end gap-8px">
          <el-button v-if="currentVideoUrl" @click="copyToClipboard(currentVideoUrl)">
            <Icon icon="ep:document-copy" class="mr-5px" />
            复制链接
          </el-button>
          <el-button v-if="currentVideoUrl" type="primary" tag="a" :href="currentVideoUrl" target="_blank">
            <Icon icon="ep:top-right" class="mr-5px" />
            新窗口打开
          </el-button>
        </div>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { DICT_TYPE } from '@/utils/dict'
import { dateFormatter } from '@/utils/formatTime'
import { UploadImg } from '@/components/UploadFile'
import { useClipboard } from '@vueuse/core'
import {
  AiMediaImageToVideoCreateReqVO,
  AiMediaTaskApi,
  AiMediaTaskVO,
  AiMediaProviderHealthVO
} from '@/api/ai/media'
import { ModelApi, ModelVO } from '@/api/ai/model/model'
import { AiMediaTaskStatusEnum, AiMediaTaskTypeEnum, AiModelTypeEnum, AiPlatformEnum } from '@/views/ai/utils/constants'

defineOptions({ name: 'AiMediaVideo' })

const message = useMessage()

const formRef = ref()
const submitting = ref(false)
const loading = ref(false)
const operatingTaskId = ref<number>()
const checkingProvider = ref(false)
const list = ref<AiMediaTaskVO[]>([])
const total = ref(0)
const videoModelList = ref<ModelVO[]>([])
const providerHealth = ref<AiMediaProviderHealthVO['PythonWorker']>()
const pollingTimer = ref<number>()
const generationMode = ref(AiMediaTaskTypeEnum.IMAGE_TO_VIDEO)
const videoPreviewVisible = ref(false)
const currentVideoUrl = ref('')

const generationModeOptions = [
  { label: '图生视频', value: AiMediaTaskTypeEnum.IMAGE_TO_VIDEO },
  { label: '文生视频（暂未开放）', value: AiMediaTaskTypeEnum.TEXT_TO_VIDEO, disabled: true }
]

const IMAGE_TO_VIDEO_CAPABILITY = 'image-to-video'

const imageSizeOptions = [
  { label: '横屏 1280x720', value: '1280x720' },
  { label: '竖屏 720x1280', value: '720x1280' },
  { label: '方形 960x960', value: '960x960' }
]

const formData = reactive<AiMediaImageToVideoCreateReqVO>({
  modelId: undefined,
  sourceImageUrl: '',
  sourceMimeType: undefined,
  sourceSize: undefined,
  prompt: '',
  negativePrompt: '',
  imageSize: '1280x720',
  duration: undefined,
  quality: undefined,
  seed: undefined
})

const formRules = reactive({
  sourceImageUrl: [{ required: true, message: '源图片不能为空', trigger: 'blur' }],
  prompt: [{ required: true, message: '提示词不能为空', trigger: 'blur' }],
  imageSize: [{ required: true, message: '视频尺寸不能为空', trigger: 'change' }]
})

const queryParams = reactive({
  pageNo: 1,
  pageSize: 10,
  taskType: AiMediaTaskTypeEnum.IMAGE_TO_VIDEO
})

const getList = async () => {
  loading.value = true
  try {
    const data = await AiMediaTaskApi.getMyTaskPage(queryParams)
    list.value = data.list
    total.value = data.total
    resumeActivePolling()
  } finally {
    loading.value = false
  }
}

const handleCreate = async () => {
  await formRef.value.validate()
  submitting.value = true
  try {
    const id = await AiMediaTaskApi.createImageToVideoTask({ ...formData })
    message.success('视频任务已提交')
    queryParams.pageNo = 1
    await getList()
    startPolling(id)
  } catch (error: any) {
    showCreateError(error)
  } finally {
    submitting.value = false
  }
}

const handleCheckProvider = async () => {
  checkingProvider.value = true
  try {
    const data = await AiMediaTaskApi.getProviderHealth()
    providerHealth.value = data?.PythonWorker
    if (providerHealth.value?.ok) {
      message.success(providerHealth.value.realGeneration === false ? 'PythonWorker 已连接，当前为 mock 模式' : 'PythonWorker 已连接')
      return
    }
    message.error(providerHealth.value?.errorMessage || 'PythonWorker 不可用')
  } catch (error: any) {
    const errorMessage = error?.msg || error?.message || 'PythonWorker 检测失败'
    providerHealth.value = {
      ok: false,
      errorMessage
    }
    message.error(errorMessage)
  } finally {
    checkingProvider.value = false
  }
}

const resetForm = () => {
  formRef.value?.resetFields()
  formData.modelId = undefined
  formData.sourceImageUrl = ''
  formData.sourceMimeType = undefined
  formData.sourceSize = undefined
  formData.prompt = ''
  formData.negativePrompt = ''
  formData.imageSize = '1280x720'
  formData.duration = undefined
  formData.quality = undefined
  formData.seed = undefined
}

const getSourceImageUrl = (row: AiMediaTaskVO) => {
  return row.inputJson?.sourceImageUrl || row.inputJson?.sourceAsset?.url || row.sourceFileIds?.[0]
}

const getVideoUrl = (row: AiMediaTaskVO) => {
  return row.outputJson?.resultAsset?.url || row.outputJson?.url || row.resultFileIds?.[0]
}

const openVideoPreview = (row: AiMediaTaskVO) => {
  const videoUrl = getVideoUrl(row)
  if (!videoUrl) {
    return
  }
  currentVideoUrl.value = videoUrl
  videoPreviewVisible.value = true
}

const canRetry = (row: AiMediaTaskVO) => {
  return row.taskType === AiMediaTaskTypeEnum.IMAGE_TO_VIDEO && row.status === AiMediaTaskStatusEnum.FAILED
}

const canCancel = (row: AiMediaTaskVO) => {
  return (
    row.taskType === AiMediaTaskTypeEnum.IMAGE_TO_VIDEO &&
    ![AiMediaTaskStatusEnum.SUCCEEDED, AiMediaTaskStatusEnum.FAILED, AiMediaTaskStatusEnum.CANCELLED].includes(
      row.status
    )
  )
}

const isTerminalTask = (row: AiMediaTaskVO) =>
  [AiMediaTaskStatusEnum.SUCCEEDED, AiMediaTaskStatusEnum.FAILED, AiMediaTaskStatusEnum.CANCELLED].includes(row.status)

const resumeActivePolling = () => {
  if (pollingTimer.value) {
    return
  }
  const activeTask = list.value.find(
    (item) => item.taskType === AiMediaTaskTypeEnum.IMAGE_TO_VIDEO && !isTerminalTask(item)
  )
  if (activeTask?.id) {
    startPolling(activeTask.id)
  }
}

const getFriendlyErrorTitle = (row: AiMediaTaskVO) => {
  const errorCode = row.errorCode || ''
  const errorMessage = row.errorMessage || ''
  if (errorCode.includes('SOURCE') || errorMessage.includes('源图片')) {
    return '源文件不可用'
  }
  if (errorCode.includes('QUOTA') || errorMessage.includes('次数') || errorMessage.includes('上限')) {
    return '配额已用尽'
  }
  if (errorCode.includes('CONFIG') || errorCode.includes('API_KEY') || errorMessage.includes('配置')) {
    return 'Provider 未配置'
  }
  if (errorCode.includes('DISABLED') || errorMessage.includes('未开启')) {
    return '功能未开启'
  }
  return '生成失败'
}

const showCreateError = (error: any) => {
  const messageText = error?.msg || error?.message || ''
  if (messageText.includes('配额') || messageText.includes('次数') || messageText.includes('上限')) {
    message.error('视频生成配额已达上限')
    return
  }
  if (messageText.includes('源图片') || messageText.includes('源文件')) {
    message.error('源图片不可用，请重新上传或更换图片')
    return
  }
  if (messageText.includes('Provider') || messageText.includes('配置') || messageText.includes('模型')) {
    message.error('视频 Provider 或模型未配置完成')
    return
  }
  if (messageText.includes('未开启') || messageText.includes('暂未启用')) {
    message.error('视频生成功能暂未开启')
    return
  }
  message.error(messageText || '视频任务提交失败')
}

const getElapsedText = (row: AiMediaTaskVO) => {
  if (!row.createTime) {
    return '-'
  }
  const endTime = row.finishTime ? new Date(row.finishTime).getTime() : Date.now()
  const elapsedSeconds = Math.max(0, Math.floor((endTime - new Date(row.createTime).getTime()) / 1000))
  if (elapsedSeconds < 60) {
    return `${elapsedSeconds}s`
  }
  return `${Math.floor(elapsedSeconds / 60)}m ${elapsedSeconds % 60}s`
}

const handleRetry = async (row: AiMediaTaskVO) => {
  operatingTaskId.value = row.id
  try {
    await AiMediaTaskApi.retryMyTask(row.id)
    message.success('已重新提交任务')
    await getList()
    startPolling(row.id)
  } finally {
    operatingTaskId.value = undefined
  }
}

const handleCancel = async (row: AiMediaTaskVO) => {
  await message.confirm('确认取消该视频任务吗？')
  operatingTaskId.value = row.id
  try {
    await AiMediaTaskApi.cancelMyTask(row.id)
    message.success('已请求取消任务')
    await getList()
    if (row.providerTaskId) {
      startPolling(row.id)
    }
  } finally {
    operatingTaskId.value = undefined
  }
}

const startPolling = (id: number) => {
  stopPolling()
  pollingTimer.value = window.setInterval(async () => {
    const task = await AiMediaTaskApi.getMyTask(id)
    const index = list.value.findIndex((item) => item.id === task.id)
    if (index >= 0) {
      list.value[index] = task
    } else {
      await getList()
    }
    if (
      [AiMediaTaskStatusEnum.SUCCEEDED, AiMediaTaskStatusEnum.FAILED, AiMediaTaskStatusEnum.CANCELLED].includes(
        task.status
      )
    ) {
      stopPolling()
      await getList()
    }
  }, 3000)
}

const stopPolling = () => {
  if (pollingTimer.value) {
    window.clearInterval(pollingTimer.value)
    pollingTimer.value = undefined
  }
}

const copyToClipboard = async (text: string) => {
  const { copy, copied, isSupported } = useClipboard({ legacy: true, source: text })
  if (!isSupported) {
    message.error('当前浏览器不支持复制')
    return
  }
  await copy()
  if (unref(copied)) {
    message.success('复制成功')
  }
}

const isImageToVideoModel = (model: ModelVO) => {
  const capabilities = model.capabilities || []
  if (capabilities.includes(IMAGE_TO_VIDEO_CAPABILITY)) {
    return true
  }
  return /(^|[-_/])i2v($|[-_/])|image[-_]?to[-_]?video|image2video/i.test(model.model || '')
}

onMounted(async () => {
  const [siliconFlowModels, pythonWorkerModels] = await Promise.all([
    ModelApi.getModelSimpleList(AiModelTypeEnum.VIDEO, AiPlatformEnum.SiliconFlow),
    ModelApi.getModelSimpleList(AiModelTypeEnum.VIDEO, AiPlatformEnum.PYTHON_WORKER)
  ])
  videoModelList.value = [...siliconFlowModels, ...pythonWorkerModels].filter(isImageToVideoModel)
  await getList()
  handleCheckProvider()
})

onBeforeUnmount(stopPolling)
</script>

<style scoped>
.video-result {
  display: flex;
  flex-direction: column;
  gap: 8px;
  width: 240px;
}

.video-preview {
  position: relative;
  display: flex;
  width: 240px;
  height: 136px;
  align-items: center;
  justify-content: center;
  padding: 0;
  overflow: hidden;
  cursor: pointer;
  background: #111;
  border: 1px solid var(--el-border-color-lighter);
  border-radius: 6px;
}

.video-preview video {
  width: 100%;
  height: 100%;
  object-fit: contain;
}

.video-preview__play {
  position: absolute;
  display: flex;
  width: 34px;
  height: 34px;
  align-items: center;
  justify-content: center;
  font-size: 18px;
  color: #fff;
  background: rgb(0 0 0 / 58%);
  border-radius: 999px;
}

.video-actions {
  display: flex;
  min-height: 24px;
  align-items: center;
  gap: 10px;
  white-space: nowrap;
}

.video-dialog-body {
  display: flex;
  min-height: 320px;
  align-items: center;
  justify-content: center;
  background: #111;
  border-radius: 6px;
}

.video-dialog-player {
  width: 100%;
  max-height: min(72vh, 720px);
  object-fit: contain;
  background: #111;
  border-radius: 6px;
}

.provider-health-title {
  display: flex;
  flex-wrap: wrap;
  align-items: center;
  gap: 6px;
}

.provider-health-detail {
  display: flex;
  flex-direction: column;
  gap: 4px;
  font-size: 12px;
}
</style>
