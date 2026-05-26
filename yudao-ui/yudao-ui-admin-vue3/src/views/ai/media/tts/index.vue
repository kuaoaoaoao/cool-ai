<template>
  <div class="grid gap-16px lg:grid-cols-[420px_minmax(0,1fr)]">
    <ContentWrap>
      <template #header>
        <div>
          <div class="text-18px font-bold">AI 语音合成</div>
          <div class="mt-4px text-13px text-[var(--el-text-color-secondary)]">
            第一阶段接入小米 MiMo mimo-v2.5-tts，生成后可在右侧播放和下载。
          </div>
        </div>
      </template>

      <el-form ref="formRef" :model="formData" :rules="formRules" label-width="82px">
        <el-form-item label="语音模型" prop="modelId">
          <el-select v-model="formData.modelId" clearable placeholder="不选则使用默认语音模型">
            <el-option
              v-for="model in voiceModelList"
              :key="model.id"
              :label="`${model.name} / ${model.model}`"
              :value="model.id"
            />
          </el-select>
        </el-form-item>
        <el-form-item label="合成文本" prop="text">
          <el-input
            v-model="formData.text"
            type="textarea"
            :rows="8"
            maxlength="2000"
            show-word-limit
            placeholder="请输入要合成为语音的文本"
          />
        </el-form-item>
        <el-form-item label="风格提示" prop="stylePrompt">
          <el-input
            v-model="formData.stylePrompt"
            type="textarea"
            :rows="3"
            maxlength="300"
            show-word-limit
            placeholder="例如：温柔、自然、有亲和力"
          />
        </el-form-item>
        <el-form-item label="音色" prop="voice">
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
          <el-input v-else v-model="formData.voice" placeholder="请输入小米 MiMo 音色，如 male-qn-qingse" />
        </el-form-item>
        <el-form-item label="格式" prop="format">
          <el-select v-model="formData.format" placeholder="请选择音频格式">
            <el-option
              v-for="item in formatOptions"
              :key="item.value"
              :label="item.label"
              :value="item.value"
            />
          </el-select>
        </el-form-item>
        <el-form-item>
          <el-button type="primary" :loading="submitting" @click="handleCreate">
            <Icon icon="ep:microphone" class="mr-5px" />
            生成语音
          </el-button>
          <el-button @click="resetForm">重置</el-button>
        </el-form-item>
      </el-form>
    </ContentWrap>

    <ContentWrap>
      <template #header>
        <div class="flex w-full items-center justify-between">
          <div>
            <div class="text-18px font-bold">我的语音任务</div>
            <div class="mt-4px text-13px text-[var(--el-text-color-secondary)]">
              前端默认通过轮询查询任务状态，后续视频/声音克隆也会复用这套任务中心。
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
        <el-table-column label="文本" min-width="220">
          <template #default="{ row }">
            {{ row.inputJson?.text }}
          </template>
        </el-table-column>
        <el-table-column label="音色" align="center" width="140">
          <template #default="{ row }">
            {{ row.inputJson?.voice }}
          </template>
        </el-table-column>
        <el-table-column label="状态" align="center" width="110">
          <template #default="{ row }">
            <dict-tag :type="DICT_TYPE.AI_MEDIA_TASK_STATUS" :value="row.status" />
          </template>
        </el-table-column>
        <el-table-column label="进度" align="center" width="120">
          <template #default="{ row }">
            <el-progress :percentage="row.progress || 0" :stroke-width="8" />
          </template>
        </el-table-column>
        <el-table-column label="结果" min-width="260">
          <template #default="{ row }">
            <audio
              v-if="getAudioUrl(row)"
              :src="getAudioUrl(row)"
              controls
              class="h-32px max-w-240px"
            />
            <span v-else-if="row.errorMessage" class="text-[var(--el-color-danger)]">
              {{ row.errorMessage }}
            </span>
            <span v-else class="text-[var(--el-text-color-secondary)]">等待生成</span>
          </template>
        </el-table-column>
        <el-table-column label="创建时间" align="center" prop="createTime" :formatter="dateFormatter" width="180" />
        <el-table-column label="操作" align="center" width="90" fixed="right">
          <template #default="{ row }">
            <el-link v-if="getAudioUrl(row)" type="primary" :href="getAudioUrl(row)" target="_blank">
              下载
            </el-link>
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
  </div>
</template>

<script setup lang="ts">
import { DICT_TYPE, getStrDictOptions } from '@/utils/dict'
import { dateFormatter } from '@/utils/formatTime'
import { AiMediaTaskApi, AiMediaTaskVO, AiMediaTtsCreateReqVO } from '@/api/ai/media'
import { ModelApi, ModelVO } from '@/api/ai/model/model'
import { AiMediaTaskStatusEnum, AiModelTypeEnum } from '@/views/ai/utils/constants'

defineOptions({ name: 'AiMediaTts' })

const message = useMessage()

const formRef = ref()
const submitting = ref(false)
const loading = ref(false)
const list = ref<AiMediaTaskVO[]>([])
const total = ref(0)
const voiceModelList = ref<ModelVO[]>([])
const pollingTimer = ref<number>()

const formData = reactive<AiMediaTtsCreateReqVO>({
  modelId: undefined,
  text: '',
  stylePrompt: '',
  voice: '',
  format: 'mp3'
})
const formRules = reactive({
  text: [{ required: true, message: '合成文本不能为空', trigger: 'blur' }],
  voice: [{ required: true, message: '音色不能为空', trigger: 'blur' }],
  format: [{ required: true, message: '格式不能为空', trigger: 'change' }]
})
const queryParams = reactive({
  pageNo: 1,
  pageSize: 10,
  taskType: 1
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

const getList = async () => {
  loading.value = true
  try {
    const data = await AiMediaTaskApi.getMyTaskPage(queryParams)
    list.value = data.list
    total.value = data.total
  } finally {
    loading.value = false
  }
}

const handleCreate = async () => {
  await formRef.value.validate()
  submitting.value = true
  try {
    const id = await AiMediaTaskApi.createTtsTask(formData)
    message.success('语音任务已提交')
    queryParams.pageNo = 1
    await getList()
    startPolling(id)
  } finally {
    submitting.value = false
  }
}

const resetForm = () => {
  formRef.value?.resetFields()
  formData.format = 'mp3'
}

const getAudioUrl = (row: AiMediaTaskVO) => {
  return row.outputJson?.url || row.resultFileIds?.[0]
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

onMounted(async () => {
  voiceModelList.value = await ModelApi.getModelSimpleList(AiModelTypeEnum.VOICE)
  await getList()
})

onBeforeUnmount(stopPolling)
</script>
