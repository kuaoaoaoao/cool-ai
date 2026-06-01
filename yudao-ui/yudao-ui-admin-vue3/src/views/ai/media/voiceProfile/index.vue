<template>
  <ContentWrap>
    <el-form
      ref="queryFormRef"
      class="-mb-15px"
      :model="queryParams"
      :inline="true"
      label-width="78px"
    >
      <el-form-item label="音色名称" prop="name">
        <el-input
          v-model="queryParams.name"
          clearable
          placeholder="请输入音色名称"
          class="!w-220px"
          @keyup.enter="handleQuery"
        />
      </el-form-item>
      <el-form-item label="来源类型" prop="sourceType">
        <el-select v-model="queryParams.sourceType" clearable placeholder="请选择来源类型" class="!w-180px">
          <el-option
            v-for="dict in sourceOptions"
            :key="dict.value"
            :label="dict.label"
            :value="dict.value"
          />
        </el-select>
      </el-form-item>
      <el-form-item label="状态" prop="status">
        <el-select v-model="queryParams.status" clearable placeholder="请选择状态" class="!w-160px">
          <el-option
            v-for="dict in getIntDictOptions(DICT_TYPE.COMMON_STATUS)"
            :key="dict.value"
            :label="dict.label"
            :value="dict.value"
          />
        </el-select>
      </el-form-item>
      <el-form-item>
        <el-button @click="handleQuery"><Icon icon="ep:search" class="mr-5px" /> 搜索</el-button>
        <el-button @click="resetQuery"><Icon icon="ep:refresh" class="mr-5px" /> 重置</el-button>
        <el-button
          type="primary"
          plain
          @click="openForm('create')"
          v-hasPermi="['ai:voice-profile:create']"
        >
          <Icon icon="ep:plus" class="mr-5px" /> 新增
        </el-button>
      </el-form-item>
    </el-form>
  </ContentWrap>

  <ContentWrap>
    <el-table v-loading="loading" :data="list" :stripe="true" :show-overflow-tooltip="true">
      <el-table-column label="编号" align="center" prop="id" width="100" fixed="left" />
      <el-table-column label="音色名称" prop="name" min-width="160" />
      <el-table-column label="来源" align="center" prop="sourceType" width="130">
        <template #default="{ row }">
          <dict-tag :type="DICT_TYPE.AI_VOICE_PROFILE_SOURCE_TYPE" :value="row.sourceType" />
        </template>
      </el-table-column>
      <el-table-column label="模型" align="center" prop="model" min-width="220" />
      <el-table-column label="配置" min-width="240">
        <template #default="{ row }">
          {{ getProfileConfigSummary(row) }}
        </template>
      </el-table-column>
      <el-table-column label="试听" min-width="260">
        <template #default="{ row }">
          <audio
            v-if="row.previewAudioUrl"
            :src="row.previewAudioUrl"
            controls
            class="h-32px max-w-240px"
          ></audio>
          <span v-else class="text-[var(--el-text-color-secondary)]">暂无</span>
        </template>
      </el-table-column>
      <el-table-column label="状态" align="center" prop="status" width="100">
        <template #default="{ row }">
          <dict-tag :type="DICT_TYPE.COMMON_STATUS" :value="row.status" />
        </template>
      </el-table-column>
      <el-table-column label="创建时间" align="center" prop="createTime" :formatter="dateFormatter" width="180" />
      <el-table-column label="操作" align="center" width="210" fixed="right">
        <template #default="{ row }">
          <el-button
            link
            type="primary"
            @click="openPreview(row)"
            v-hasPermi="['ai:voice-profile:preview']"
          >
            试听
          </el-button>
          <el-button
            link
            type="primary"
            @click="openForm('update', row.id)"
            v-hasPermi="['ai:voice-profile:update']"
          >
            编辑
          </el-button>
          <el-button
            link
            type="danger"
            @click="handleDelete(row.id)"
            v-hasPermi="['ai:voice-profile:delete']"
          >
            删除
          </el-button>
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

  <Dialog :title="dialogTitle" v-model="dialogVisible" width="760px">
    <el-form
      ref="formRef"
      :model="formData"
      :rules="formRules"
      label-width="118px"
      v-loading="formLoading"
    >
      <el-form-item label="音色名称" prop="name">
        <el-input v-model="formData.name" maxlength="64" show-word-limit placeholder="请输入音色名称" />
      </el-form-item>
      <el-form-item label="来源类型" prop="sourceType">
        <el-radio-group v-model="formData.sourceType" @change="handleSourceTypeChange">
          <el-radio-button
            v-for="dict in sourceOptions"
            :key="dict.value"
            :value="dict.value"
          >
            {{ dict.label }}
          </el-radio-button>
        </el-radio-group>
      </el-form-item>
      <el-form-item label="语音模型" prop="modelId">
        <el-select v-model="formData.modelId" filterable placeholder="请选择语音模型" class="!w-1/1">
          <el-option
            v-for="model in formModelOptions"
            :key="model.id"
            :label="`${model.name} / ${model.model}`"
            :value="model.id"
          />
        </el-select>
      </el-form-item>
      <el-form-item v-if="formData.sourceType === VoiceProfileSourceType.BUILTIN" label="预置音色" prop="builtinVoice">
        <el-select
          v-if="voiceOptions.length > 0"
          v-model="formData.builtinVoice"
          filterable
          allow-create
          default-first-option
          placeholder="请选择或输入小米 MiMo 音色"
          class="!w-1/1"
        >
          <el-option
            v-for="dict in voiceOptions"
            :key="dict.value"
            :label="dict.label"
            :value="dict.value"
          />
        </el-select>
        <el-input v-else v-model="formData.builtinVoice" placeholder="如 mimo_default、冰糖、Chloe" />
      </el-form-item>
      <el-form-item v-if="formData.sourceType === VoiceProfileSourceType.DESIGN" label="音色描述" prop="designPrompt">
        <el-input
          v-model="formData.designPrompt"
          type="textarea"
          :rows="5"
          maxlength="1000"
          show-word-limit
          placeholder="例如：年轻男性，声音清亮，语速自然，像科技播客主持人"
        />
      </el-form-item>
      <template v-if="formData.sourceType === VoiceProfileSourceType.CLONE">
        <el-form-item label="授权确认" prop="consentConfirmed">
          <el-checkbox v-model="formData.consentConfirmed">
            已获得声音本人授权并确认样本可用于音色克隆
          </el-checkbox>
        </el-form-item>
        <el-form-item label="样本文件" prop="sampleFileUrl">
          <el-upload
            v-model:file-list="sampleFileList"
            :action="uploadUrl"
            :http-request="handleSampleUpload"
            :before-upload="beforeSampleUpload"
            :limit="1"
            :on-remove="handleSampleRemove"
            accept=".mp3,.wav,audio/mpeg,audio/wav"
            class="w-full"
          >
            <el-button><Icon icon="ep:upload" class="mr-5px" /> 上传 mp3/wav</el-button>
            <template #tip>
              <div class="el-upload__tip">建议 7 MB 以内，后端调用时会重新做格式和大小校验。</div>
            </template>
          </el-upload>
          <div v-if="formData.sampleFileUrl" class="mt-6px text-12px text-[var(--el-text-color-secondary)]">
            {{ getSampleName(formData.sampleFileUrl) }} · {{ formatBytes(formData.sampleSize) }}
          </div>
        </el-form-item>
      </template>
      <el-form-item label="状态" prop="status">
        <el-radio-group v-model="formData.status">
          <el-radio
            v-for="dict in getIntDictOptions(DICT_TYPE.COMMON_STATUS)"
            :key="dict.value"
            :value="dict.value"
          >
            {{ dict.label }}
          </el-radio>
        </el-radio-group>
      </el-form-item>
      <el-form-item label="备注" prop="remark">
        <el-input v-model="formData.remark" type="textarea" :rows="2" maxlength="200" show-word-limit />
      </el-form-item>
    </el-form>
    <template #footer>
      <el-button type="primary" :disabled="formLoading" @click="submitForm">确 定</el-button>
      <el-button @click="dialogVisible = false">取 消</el-button>
    </template>
  </Dialog>

  <Dialog title="试听音色" v-model="previewDialogVisible" width="620px">
    <el-form ref="previewFormRef" :model="previewForm" :rules="previewRules" label-width="82px">
      <el-form-item label="试听文本" prop="text">
        <el-input
          v-model="previewForm.text"
          type="textarea"
          :rows="5"
          maxlength="500"
          show-word-limit
          placeholder="请输入试听文本"
        />
      </el-form-item>
      <el-form-item label="格式" prop="format">
        <el-select v-model="previewForm.format" placeholder="请选择音频格式">
          <el-option
            v-for="item in formatOptions"
            :key="item.value"
            :label="item.label"
            :value="item.value"
          />
        </el-select>
      </el-form-item>
      <el-form-item v-if="previewAudioUrl" label="结果">
        <audio :src="previewAudioUrl" controls class="h-32px max-w-360px"></audio>
      </el-form-item>
    </el-form>
    <template #footer>
      <el-button type="primary" :loading="previewLoading" @click="submitPreview">
        生成试听
      </el-button>
      <el-button @click="previewDialogVisible = false">关 闭</el-button>
    </template>
  </Dialog>
</template>

<script setup lang="ts">
import type { UploadProps, UploadRequestOptions, UploadUserFile } from 'element-plus'
import { useUpload } from '@/components/UploadFile/src/useUpload'
import {
  AiMediaTaskApi,
  AiMediaTaskVO,
  AiVoiceProfileApi,
  AiVoiceProfileVO
} from '@/api/ai/media'
import { ModelApi, ModelVO } from '@/api/ai/model/model'
import { CommonStatusEnum } from '@/utils/constants'
import { DICT_TYPE, getIntDictOptions, getStrDictOptions } from '@/utils/dict'
import { dateFormatter } from '@/utils/formatTime'
import { AiMediaTaskStatusEnum, AiModelTypeEnum, AiPlatformEnum } from '@/views/ai/utils/constants'

defineOptions({ name: 'AiVoiceProfile' })

const message = useMessage()
const { t } = useI18n()

const VoiceProfileSourceType = {
  BUILTIN: 'BUILTIN',
  CLONE: 'CLONE',
  DESIGN: 'DESIGN'
}

const modelBySourceType: Record<string, string> = {
  [VoiceProfileSourceType.BUILTIN]: 'mimo-v2.5-tts',
  [VoiceProfileSourceType.CLONE]: 'mimo-v2.5-tts-voiceclone',
  [VoiceProfileSourceType.DESIGN]: 'mimo-v2.5-tts-voicedesign'
}

const loading = ref(true)
const formLoading = ref(false)
const list = ref<AiVoiceProfileVO[]>([])
const total = ref(0)
const voiceModelList = ref<ModelVO[]>([])
const queryFormRef = ref()
const formRef = ref()
const previewFormRef = ref()
const dialogVisible = ref(false)
const dialogTitle = ref('')
const formType = ref<'create' | 'update'>('create')
const previewDialogVisible = ref(false)
const previewLoading = ref(false)
const previewAudioUrl = ref('')
const pollingTimer = ref<number>()
const sampleFileList = ref<UploadUserFile[]>([])

const { uploadUrl, httpRequest } = useUpload('ai-voice-profile')

const queryParams = reactive({
  pageNo: 1,
  pageSize: 10,
  name: undefined,
  sourceType: undefined,
  status: undefined
})

const formData = ref<AiVoiceProfileVO>({
  id: undefined,
  name: '',
  sourceType: VoiceProfileSourceType.BUILTIN,
  modelId: undefined,
  builtinVoice: 'mimo_default',
  designPrompt: '',
  sampleFileUrl: '',
  sampleMimeType: '',
  sampleSize: undefined,
  consentConfirmed: false,
  status: CommonStatusEnum.ENABLE,
  remark: ''
})

const previewForm = reactive({
  id: undefined as number | undefined,
  text: '你好，欢迎使用 AI 音色库。',
  format: 'wav'
})

const formRules = reactive({
  name: [{ required: true, message: '音色名称不能为空', trigger: 'blur' }],
  sourceType: [{ required: true, message: '来源类型不能为空', trigger: 'change' }],
  modelId: [{ required: true, message: '语音模型不能为空', trigger: 'change' }],
  builtinVoice: [{ required: true, message: '预置音色不能为空', trigger: 'change' }],
  designPrompt: [{ required: true, message: '音色描述不能为空', trigger: 'blur' }],
  sampleFileUrl: [{ required: true, message: '样本文件不能为空', trigger: 'change' }],
  consentConfirmed: [
    {
      validator: (_rule, value, callback) => {
        if (formData.value.sourceType === VoiceProfileSourceType.CLONE && !value) {
          callback(new Error('请确认已获得声音授权'))
          return
        }
        callback()
      },
      trigger: 'change'
    }
  ],
  status: [{ required: true, message: '状态不能为空', trigger: 'change' }]
})

const previewRules = reactive({
  text: [{ required: true, message: '试听文本不能为空', trigger: 'blur' }],
  format: [{ required: true, message: '格式不能为空', trigger: 'change' }]
})

const sourceOptions = computed(() => {
  const dictOptions = getStrDictOptions(DICT_TYPE.AI_VOICE_PROFILE_SOURCE_TYPE)
  return dictOptions.length > 0
    ? dictOptions
    : [
        { label: '预置音色', value: VoiceProfileSourceType.BUILTIN },
        { label: '音色克隆', value: VoiceProfileSourceType.CLONE },
        { label: '文字设计音色', value: VoiceProfileSourceType.DESIGN }
      ]
})
const voiceOptions = computed(() => getStrDictOptions(DICT_TYPE.AI_MEDIA_TTS_VOICE))
const formatOptions = computed(() => {
  const dictOptions = getStrDictOptions(DICT_TYPE.AI_MEDIA_TTS_FORMAT)
  return dictOptions.length > 0
    ? dictOptions
    : [
        { label: 'WAV', value: 'wav' },
        { label: 'MP3', value: 'mp3' },
        { label: 'PCM', value: 'pcm' }
      ]
})
const formModelOptions = computed(() =>
  voiceModelList.value.filter(
    (model) =>
      model.platform === AiPlatformEnum.XIAOMI_MIMO &&
      model.model === modelBySourceType[formData.value.sourceType]
  )
)

const getList = async () => {
  loading.value = true
  try {
    const data = await AiVoiceProfileApi.getVoiceProfilePage(queryParams)
    list.value = data.list
    total.value = data.total
  } finally {
    loading.value = false
  }
}

const handleQuery = () => {
  queryParams.pageNo = 1
  getList()
}

const resetQuery = () => {
  queryFormRef.value.resetFields()
  handleQuery()
}

const openForm = async (type: 'create' | 'update', id?: number) => {
  dialogVisible.value = true
  dialogTitle.value = type === 'create' ? '新增音色' : '编辑音色'
  formType.value = type
  resetForm()
  if (id) {
    formLoading.value = true
    try {
      formData.value = await AiVoiceProfileApi.getVoiceProfile(id)
      syncSampleFileList()
    } finally {
      formLoading.value = false
    }
  }
  nextTick(() => formRef.value?.clearValidate())
}

const resetForm = () => {
  formData.value = {
    id: undefined,
    name: '',
    sourceType: VoiceProfileSourceType.BUILTIN,
    modelId: undefined,
    builtinVoice: 'mimo_default',
    designPrompt: '',
    sampleFileUrl: '',
    sampleMimeType: '',
    sampleSize: undefined,
    consentConfirmed: false,
    status: CommonStatusEnum.ENABLE,
    remark: ''
  }
  sampleFileList.value = []
  setDefaultModel()
  formRef.value?.resetFields()
}

const handleSourceTypeChange = () => {
  formData.value.modelId = undefined
  formData.value.builtinVoice =
    formData.value.sourceType === VoiceProfileSourceType.BUILTIN ? 'mimo_default' : ''
  formData.value.designPrompt = ''
  formData.value.sampleFileUrl = ''
  formData.value.sampleMimeType = ''
  formData.value.sampleSize = undefined
  formData.value.consentConfirmed = false
  sampleFileList.value = []
  setDefaultModel()
  nextTick(() => formRef.value?.clearValidate())
}

const setDefaultModel = () => {
  const model = voiceModelList.value.find(
    (item) =>
      item.platform === AiPlatformEnum.XIAOMI_MIMO &&
      item.model === modelBySourceType[formData.value.sourceType]
  )
  formData.value.modelId = model?.id
}

const submitForm = async () => {
  await formRef.value.validate()
  formLoading.value = true
  try {
    const data = {
      ...formData.value,
      name: formData.value.name?.trim(),
      builtinVoice: formData.value.builtinVoice?.trim(),
      designPrompt: formData.value.designPrompt?.trim(),
      remark: formData.value.remark?.trim()
    }
    if (data.sourceType !== VoiceProfileSourceType.BUILTIN) {
      data.builtinVoice = undefined
    }
    if (data.sourceType !== VoiceProfileSourceType.DESIGN) {
      data.designPrompt = undefined
    }
    if (data.sourceType !== VoiceProfileSourceType.CLONE) {
      data.sampleFileUrl = undefined
      data.sampleMimeType = undefined
      data.sampleSize = undefined
      data.consentConfirmed = undefined
    }
    if (formType.value === 'create') {
      await AiVoiceProfileApi.createVoiceProfile(data)
      message.success(t('common.createSuccess'))
    } else {
      await AiVoiceProfileApi.updateVoiceProfile(data)
      message.success(t('common.updateSuccess'))
    }
    dialogVisible.value = false
    await getList()
  } finally {
    formLoading.value = false
  }
}

const handleDelete = async (id: number) => {
  try {
    await message.delConfirm()
    await AiVoiceProfileApi.deleteVoiceProfile(id)
    message.success(t('common.delSuccess'))
    await getList()
  } catch {}
}

const beforeSampleUpload: UploadProps['beforeUpload'] = (rawFile) => {
  const isAudio = ['audio/mpeg', 'audio/mp3', 'audio/wav', 'audio/wave', 'audio/x-wav'].includes(rawFile.type)
  const isExtMatched = /\.(mp3|wav)$/i.test(rawFile.name)
  if (!isAudio && !isExtMatched) {
    message.error('样本只支持 mp3/wav')
    return false
  }
  if (rawFile.size > 7.5 * 1024 * 1024) {
    message.error('样本文件建议控制在 7 MB 左右')
    return false
  }
  return true
}

const handleSampleUpload = async (options: UploadRequestOptions) => {
  const response = (await httpRequest(options)) as any
  const url = response?.data || response
  formData.value.sampleFileUrl = url
  formData.value.sampleMimeType = options.file.type || ''
  formData.value.sampleSize = options.file.size
  sampleFileList.value = [{ name: options.file.name, url }]
  formRef.value?.validateField('sampleFileUrl')
  return response
}

const handleSampleRemove = () => {
  formData.value.sampleFileUrl = ''
  formData.value.sampleMimeType = ''
  formData.value.sampleSize = undefined
}

const syncSampleFileList = () => {
  if (!formData.value.sampleFileUrl) {
    sampleFileList.value = []
    return
  }
  sampleFileList.value = [
    {
      name: getSampleName(formData.value.sampleFileUrl),
      url: formData.value.sampleFileUrl
    }
  ]
}

const openPreview = (row: AiVoiceProfileVO) => {
  previewForm.id = row.id
  previewForm.text = '你好，欢迎使用 AI 音色库。'
  previewForm.format = 'wav'
  previewAudioUrl.value = row.previewAudioUrl || ''
  previewDialogVisible.value = true
  nextTick(() => previewFormRef.value?.clearValidate())
}

const submitPreview = async () => {
  await previewFormRef.value.validate()
  if (!previewForm.id) {
    return
  }
  previewLoading.value = true
  previewAudioUrl.value = ''
  try {
    const taskId = await AiVoiceProfileApi.previewVoiceProfile({
      id: previewForm.id,
      text: previewForm.text,
      format: previewForm.format
    })
    message.success('试听任务已提交')
    startPolling(taskId)
  } finally {
    previewLoading.value = false
  }
}

const startPolling = (taskId: number) => {
  stopPolling()
  pollingTimer.value = window.setInterval(async () => {
    const task = await AiMediaTaskApi.getMyTask(taskId)
    if (task.status === AiMediaTaskStatusEnum.SUCCEEDED) {
      previewAudioUrl.value = getAudioUrl(task)
      stopPolling()
      await getList()
    }
    if ([AiMediaTaskStatusEnum.FAILED, AiMediaTaskStatusEnum.CANCELLED].includes(task.status)) {
      stopPolling()
      message.error(task.errorMessage || '试听生成失败')
    }
  }, 3000)
}

const stopPolling = () => {
  if (pollingTimer.value) {
    window.clearInterval(pollingTimer.value)
    pollingTimer.value = undefined
  }
}

const getAudioUrl = (row: AiMediaTaskVO) => row.outputJson?.url || row.resultFileIds?.[0] || ''

const getProfileConfigSummary = (row: AiVoiceProfileVO) => {
  if (row.sourceType === VoiceProfileSourceType.BUILTIN) {
    return row.builtinVoice || '-'
  }
  if (row.sourceType === VoiceProfileSourceType.DESIGN) {
    return row.designPrompt || '-'
  }
  if (row.sourceType === VoiceProfileSourceType.CLONE) {
    return [getSampleName(row.sampleFileUrl), formatBytes(row.sampleSize)].filter(Boolean).join(' · ') || '-'
  }
  return '-'
}

const getSampleName = (url?: string) => {
  if (!url) {
    return ''
  }
  return decodeURIComponent(url.split('?')[0].split('/').pop() || url)
}

const formatBytes = (size?: number) => {
  if (!size) {
    return ''
  }
  if (size < 1024 * 1024) {
    return `${(size / 1024).toFixed(1)} KB`
  }
  return `${(size / 1024 / 1024).toFixed(2)} MB`
}

onMounted(async () => {
  voiceModelList.value = await ModelApi.getModelSimpleList(AiModelTypeEnum.VOICE)
  setDefaultModel()
  await getList()
})

onBeforeUnmount(stopPolling)
</script>
