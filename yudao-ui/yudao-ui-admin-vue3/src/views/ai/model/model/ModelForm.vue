<template>
  <Dialog :title="dialogTitle" v-model="dialogVisible">
    <el-form
      ref="formRef"
      :model="formData"
      :rules="formRules"
      label-width="130px"
      v-loading="formLoading"
    >
      <el-form-item label="所属平台" prop="platform">
        <el-select v-model="formData.platform" placeholder="请输入平台" clearable>
          <el-option
            v-for="dict in getStrDictOptions(DICT_TYPE.AI_PLATFORM)"
            :key="dict.value"
            :label="dict.label"
            :value="dict.value"
          />
        </el-select>
      </el-form-item>
      <el-form-item label="模型类型" prop="type">
        <el-select
          v-model="formData.type"
          placeholder="请输入模型类型"
          clearable
          :disabled="!!formData.id"
        >
          <el-option
            v-for="dict in getIntDictOptions(DICT_TYPE.AI_MODEL_TYPE)"
            :key="dict.value"
            :label="dict.label"
            :value="dict.value"
          />
        </el-select>
      </el-form-item>
      <el-form-item label="API 秘钥" prop="keyId">
        <el-select
          v-model="formData.keyId"
          placeholder="请选择 API 秘钥"
          clearable
          :disabled="!formData.platform"
          class="!w-1/1"
        >
          <el-option
            v-for="apiKey in filteredApiKeyList"
            :key="apiKey.id"
            :label="buildApiKeyOptionLabel(apiKey)"
            :value="apiKey.id"
          />
        </el-select>
        <div v-if="formData.platform && !filteredApiKeyList.length" class="mt-5px text-12px text-orange">
          当前平台还没有启用的 API Key，请先在 API 密钥菜单配置。
        </div>
      </el-form-item>
      <el-form-item label="模型名字" prop="name">
        <el-input v-model="formData.name" placeholder="请输入模型名字" />
      </el-form-item>
      <el-form-item label="模型标识" prop="model">
        <el-autocomplete
          v-model="formData.model"
          :fetch-suggestions="queryModelSuggestions"
          placeholder="请输入模型标识"
          clearable
          class="!w-1/1"
          @select="handleModelSuggestionSelect"
        />
      </el-form-item>
      <el-form-item v-if="modelConfigTips.length" label="配置提示">
        <el-alert type="info" :closable="false" show-icon>
          <template #default>
            <div v-for="tip in modelConfigTips" :key="tip">{{ tip }}</div>
          </template>
        </el-alert>
      </el-form-item>
      <el-form-item label="能力标签" prop="capabilities">
        <el-select
          v-model="formData.capabilities"
          multiple
          filterable
          allow-create
          default-first-option
          placeholder="请选择或输入能力标签"
          class="!w-1/1"
        >
          <el-option
            v-for="capability in capabilityOptions"
            :key="capability"
            :label="capability"
            :value="capability"
          />
        </el-select>
      </el-form-item>
      <el-form-item label="输入模态" prop="inputModalities">
        <el-select
          v-model="formData.inputModalities"
          multiple
          filterable
          allow-create
          default-first-option
          placeholder="请选择输入模态"
          class="!w-1/1"
        >
          <el-option v-for="modality in modalityOptions" :key="modality" :label="modality" :value="modality" />
        </el-select>
      </el-form-item>
      <el-form-item label="输出模态" prop="outputModalities">
        <el-select
          v-model="formData.outputModalities"
          multiple
          filterable
          allow-create
          default-first-option
          placeholder="请选择输出模态"
          class="!w-1/1"
        >
          <el-option v-for="modality in outputModalityOptions" :key="modality" :label="modality" :value="modality" />
        </el-select>
      </el-form-item>
      <el-form-item label="扩展配置">
        <el-button @click="applyCapabilityPreset" :disabled="!formData.type">按类型填充能力</el-button>
      </el-form-item>
      <el-form-item label="默认参数 JSON">
        <el-input
          v-model="optionsText"
          type="textarea"
          :rows="4"
          placeholder='例如 {"duration":5,"resolution":"720p"}'
        />
      </el-form-item>
      <el-form-item label="限制 JSON">
        <el-input
          v-model="limitsText"
          type="textarea"
          :rows="3"
          placeholder='例如 {"maxDuration":10,"maxInputImageSizeMb":10}'
        />
      </el-form-item>
      <el-form-item label="元数据 JSON">
        <el-input
          v-model="metadataText"
          type="textarea"
          :rows="3"
          placeholder='例如 {"protocol":"dashscope-video"}'
        />
      </el-form-item>
      <el-form-item label="模型排序" prop="sort">
        <el-input-number v-model="formData.sort" placeholder="请输入模型排序" class="!w-1/1" />
      </el-form-item>
      <el-form-item label="开启状态" prop="status">
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
      <el-form-item
        label="温度参数"
        prop="temperature"
        v-if="formData.type === AiModelTypeEnum.CHAT"
      >
        <el-input-number
          v-model="formData.temperature"
          placeholder="请输入温度参数"
          :min="0"
          :max="2"
          :precision="2"
          class="!w-1/1"
        />
      </el-form-item>
      <el-form-item
        label="回复数 Token 数"
        prop="maxTokens"
        v-if="formData.type === AiModelTypeEnum.CHAT"
      >
        <el-input-number
          v-model="formData.maxTokens"
          placeholder="请输入回复数 Token 数"
          :min="0"
          :max="8192"
          class="!w-1/1"
        />
      </el-form-item>
      <el-form-item
        label="上下文数量"
        prop="maxContexts"
        v-if="formData.type === AiModelTypeEnum.CHAT"
      >
        <el-input-number
          v-model="formData.maxContexts"
          placeholder="请输入上下文数量"
          :min="0"
          :max="20"
          class="!w-1/1"
        />
      </el-form-item>
    </el-form>
    <template #footer>
      <el-button @click="submitForm" type="primary" :disabled="formLoading">确 定</el-button>
      <el-button @click="dialogVisible = false">取 消</el-button>
    </template>
  </Dialog>
</template>
<script setup lang="ts">
import { ModelApi, ModelVO } from '@/api/ai/model/model'
import { ApiKeyApi, ApiKeyVO } from '@/api/ai/model/apiKey'
import { CommonStatusEnum } from '@/utils/constants'
import { DICT_TYPE, getIntDictOptions, getStrDictOptions } from '@/utils/dict'
import { AiModelTypeEnum, AiPlatformEnum } from '@/views/ai/utils/constants'

/** API 模型的表单 */
defineOptions({ name: 'ModelForm' })

const { t } = useI18n() // 国际化
const message = useMessage() // 消息弹窗

const dialogVisible = ref(false) // 弹窗的是否展示
const dialogTitle = ref('') // 弹窗的标题
const formLoading = ref(false) // 表单的加载中：1）修改时的数据加载；2）提交的按钮禁用
const formType = ref('') // 表单的类型：create - 新增；update - 修改

type ModelFormData = Partial<ModelVO>

const formData = ref<ModelFormData>({
  id: undefined,
  keyId: undefined,
  name: undefined,
  model: undefined,
  platform: undefined,
  type: undefined,
  sort: undefined,
  status: CommonStatusEnum.ENABLE,
  capabilities: [],
  inputModalities: [],
  outputModalities: [],
  options: undefined,
  limits: undefined,
  metadata: undefined,
  temperature: undefined,
  maxTokens: undefined,
  maxContexts: undefined
})
const formRules = reactive({
  keyId: [{ required: true, message: 'API 秘钥不能为空', trigger: 'blur' }],
  name: [{ required: true, message: '模型名字不能为空', trigger: 'blur' }],
  model: [{ required: true, message: '模型标识不能为空', trigger: 'blur' }],
  platform: [{ required: true, message: '所属平台不能为空', trigger: 'blur' }],
  type: [{ required: true, message: '模型类型不能为空', trigger: 'blur' }],
  sort: [{ required: true, message: '排序不能为空', trigger: 'blur' }],
  status: [{ required: true, message: '状态不能为空', trigger: 'blur' }],
  temperature: [{ required: true, message: '温度参数不能为空', trigger: 'blur' }],
  maxTokens: [{ required: true, message: '回复数 Token 数不能为空', trigger: 'blur' }],
  maxContexts: [{ required: true, message: '上下文数量不能为空', trigger: 'blur' }]
})
const formRef = ref() // 表单 Ref
const apiKeyList = ref([] as ApiKeyVO[]) // API 密钥列表
const optionsText = ref('')
const limitsText = ref('')
const metadataText = ref('')

const capabilityOptions = [
  'chat',
  'text-generation',
  'image-generation',
  'text-to-video',
  'image-to-video',
  'vision',
  'audio',
  'video',
  'stream',
  'tool',
  'embedding',
  'rerank',
  'tts',
  'asr',
  'openai-compatible',
  'anthropic-compatible'
]
const modalityOptions = ['text', 'image', 'audio', 'video']
const outputModalityOptions = ['text', 'image', 'audio', 'video', 'vector', 'score']

const filteredApiKeyList = computed(() =>
  apiKeyList.value.filter(
    (apiKey) =>
      apiKey.status === CommonStatusEnum.ENABLE &&
      (!formData.value.platform || apiKey.platform === formData.value.platform)
  )
)

const modelPresets = [
  {
    platform: AiPlatformEnum.XIAOMI_MIMO,
    type: AiModelTypeEnum.CHAT,
    value: 'mimo-v2.5',
    name: 'MiMo Omni 图文多模态'
  },
  {
    platform: AiPlatformEnum.XIAOMI_MIMO,
    type: AiModelTypeEnum.CHAT,
    value: 'mimo-v2.5-pro',
    name: 'MiMo Pro 长上下文文本'
  },
  {
    platform: AiPlatformEnum.XIAOMI_MIMO,
    type: AiModelTypeEnum.CHAT,
    value: 'mimo-v2-flash',
    name: 'MiMo Flash 快速低成本'
  },
  {
    platform: AiPlatformEnum.XIAOMI_MIMO,
    type: AiModelTypeEnum.VOICE,
    value: 'mimo-v2.5-tts',
    name: 'MiMo TTS 普通语音合成'
  },
  {
    platform: AiPlatformEnum.XIAOMI_MIMO,
    type: AiModelTypeEnum.VOICE,
    value: 'mimo-v2.5-tts-voiceclone',
    name: 'MiMo TTS 音色克隆'
  },
  {
    platform: AiPlatformEnum.XIAOMI_MIMO,
    type: AiModelTypeEnum.VOICE,
    value: 'mimo-v2.5-tts-voicedesign',
    name: 'MiMo TTS 文字设计音色'
  },
  {
    platform: AiPlatformEnum.OPENAI,
    type: AiModelTypeEnum.CHAT,
    value: 'gpt-4o-mini',
    name: 'OpenAI 文本/图片'
  },
  {
    platform: AiPlatformEnum.OPENAI,
    type: AiModelTypeEnum.EMBEDDING,
    value: 'text-embedding-3-small',
    name: 'OpenAI Embedding'
  },
  {
    platform: AiPlatformEnum.DEEP_SEEK,
    type: AiModelTypeEnum.CHAT,
    value: 'deepseek-chat',
    name: 'DeepSeek Chat'
  },
  {
    platform: AiPlatformEnum.DEEP_SEEK,
    type: AiModelTypeEnum.CHAT,
    value: 'deepseek-reasoner',
    name: 'DeepSeek Reasoner'
  },
  {
    platform: AiPlatformEnum.TONG_YI,
    type: AiModelTypeEnum.EMBEDDING,
    value: 'text-embedding-v3',
    name: '通义 Embedding'
  },
  {
    platform: AiPlatformEnum.TONG_YI,
    type: AiModelTypeEnum.VIDEO,
    value: 'Wan-AI/Wan2.2-I2V-A1',
    name: '通义万相 Wan2.2 图生视频'
  },
  {
    platform: AiPlatformEnum.PYTHON_WORKER,
    type: AiModelTypeEnum.VIDEO,
    value: 'mock-i2v',
    name: 'Python Worker Mock 图生视频'
  }
]

const modelConfigTips = computed(() => {
  const tips: string[] = []
  if (formData.value.platform === AiPlatformEnum.XIAOMI_MIMO) {
    tips.push('API URL 建议在 API 密钥里填写：https://api.xiaomimimo.com/v1')
    if (formData.value.type === AiModelTypeEnum.CHAT) {
      tips.push('图文多模态用 mimo-v2.5；纯文本高质量用 mimo-v2.5-pro；快速低成本用 mimo-v2-flash')
    }
    if (formData.value.type === AiModelTypeEnum.VOICE) {
      tips.push('语音模型支持 mimo-v2.5-tts、mimo-v2.5-tts-voiceclone、mimo-v2.5-tts-voicedesign')
    }
  }
  if (formData.value.platform === AiPlatformEnum.OPENAI) {
    tips.push('可接 OpenAI 官方，也可接 OpenAI-compatible 服务商；兼容服务商需要在 API 密钥里填写自定义 URL')
  }
  if (formData.value.platform === AiPlatformEnum.ANTHROPIC) {
    tips.push('Anthropic 使用 Claude Messages 协议；OpenAI-compatible 中转请按实际协议选择 OpenAI 或 Anthropic 平台')
  }
  if (formData.value.platform === AiPlatformEnum.Ollama) {
    tips.push('Ollama 需要在 API 密钥里填写服务 URL，例如 http://127.0.0.1:11434')
  }
  if (formData.value.platform === AiPlatformEnum.PYTHON_WORKER) {
    tips.push('本地联调使用 VIDEO 类型，模型标识可填 mock-i2v，能力需包含 image-to-video')
    tips.push('后端需启用 yudao.ai.media.worker，并启动 worker.py 的 8099 服务')
  }
  if (formData.value.platform && formData.value.keyId) {
    const apiKey = apiKeyList.value.find((item) => item.id === formData.value.keyId)
    if (apiKey && apiKey.platform !== formData.value.platform) {
      tips.push('当前 API Key 平台和模型平台不一致，请重新选择同平台 API Key')
    }
  }
  return tips
})

const buildApiKeyOptionLabel = (apiKey: ApiKeyVO) => {
  const url = apiKey.url ? ` · ${apiKey.url}` : ''
  return `${apiKey.name}${url}`
}

const queryModelSuggestions = (queryString: string, cb: (suggestions: any[]) => void) => {
  const suggestions = modelPresets
    .filter((item) => !formData.value.platform || item.platform === formData.value.platform)
    .filter((item) => !formData.value.type || item.type === formData.value.type)
    .filter(
      (item) =>
        !queryString ||
        item.value.toLowerCase().includes(queryString.toLowerCase()) ||
        item.name.toLowerCase().includes(queryString.toLowerCase())
    )
    .map((item) => ({
      value: item.value,
      name: item.name
    }))
  cb(suggestions)
}

const handleModelSuggestionSelect = (item: any) => {
  if (!formData.value.name) {
    formData.value.name = item.name
  }
  applyCapabilityPreset()
}

watch(
  () => formData.value.platform,
  () => {
    if (
      formData.value.keyId &&
      !apiKeyList.value.some(
        (apiKey) => apiKey.id === formData.value.keyId && apiKey.platform === formData.value.platform
      )
    ) {
      formData.value.keyId = undefined
    }
  }
)

watch(
  () => [formData.value.platform, formData.value.type, formData.value.model],
  () => {
    if (formData.value.type !== AiModelTypeEnum.CHAT) {
      return
    }
    formData.value.temperature = formData.value.temperature ?? 0.7
    formData.value.maxContexts = formData.value.maxContexts ?? 10
    if (formData.value.platform === AiPlatformEnum.XIAOMI_MIMO) {
      const isLongContextModel = formData.value.model === 'mimo-v2.5' || formData.value.model === 'mimo-v2.5-pro'
      formData.value.maxTokens = formData.value.maxTokens ?? (isLongContextModel ? 8192 : 4096)
    } else {
      formData.value.maxTokens = formData.value.maxTokens ?? 4096
    }
  }
)

/** 打开弹窗 */
const open = async (type: string, id?: number) => {
  dialogVisible.value = true
  dialogTitle.value = t('action.' + type)
  formType.value = type
  resetForm()
  // 修改时，设置数据
  if (id) {
    formLoading.value = true
    try {
      formData.value = normalizeModelFormData(await ModelApi.getModel(id))
      syncJsonTextFromForm()
    } finally {
      formLoading.value = false
    }
  }
  // 获得下拉数据
  apiKeyList.value = await ApiKeyApi.getApiKeySimpleList()
}
defineExpose({ open }) // 提供 open 方法，用于打开弹窗

/** 提交表单 */
const emit = defineEmits(['success']) // 定义 success 事件，用于操作成功后的回调
const submitForm = async () => {
  // 校验表单
  await formRef.value.validate()
  // 提交请求
  formLoading.value = true
  try {
    const data = {
      ...formData.value,
      name: formData.value.name?.trim(),
      model: formData.value.model?.trim(),
      platform: formData.value.platform?.trim(),
      capabilities: normalizeStringArray(formData.value.capabilities),
      inputModalities: normalizeStringArray(formData.value.inputModalities),
      outputModalities: normalizeStringArray(formData.value.outputModalities),
      options: parseJsonObject(optionsText.value, '默认参数 JSON'),
      limits: parseJsonObject(limitsText.value, '限制 JSON'),
      metadata: parseJsonObject(metadataText.value, '元数据 JSON')
    } as ModelVO
    if (data.type !== AiModelTypeEnum.CHAT) {
      delete data.temperature
      delete data.maxTokens
      delete data.maxContexts
    }
    if (formType.value === 'create') {
      await ModelApi.createModel(data)
      message.success(t('common.createSuccess'))
    } else {
      await ModelApi.updateModel(data)
      message.success(t('common.updateSuccess'))
    }
    dialogVisible.value = false
    // 发送操作成功的事件
    emit('success')
  } finally {
    formLoading.value = false
  }
}

/** 重置表单 */
const resetForm = () => {
  formData.value = buildEmptyFormData()
  optionsText.value = ''
  limitsText.value = ''
  metadataText.value = ''
  formRef.value?.resetFields()
}

const buildEmptyFormData = (): ModelFormData => ({
    id: undefined,
    keyId: undefined,
    name: undefined,
    model: undefined,
    platform: undefined,
    type: undefined,
    sort: undefined,
    status: CommonStatusEnum.ENABLE,
    capabilities: [],
    inputModalities: [],
    outputModalities: [],
    options: undefined,
    limits: undefined,
    metadata: undefined,
    temperature: undefined,
    maxTokens: undefined,
    maxContexts: undefined
})

const normalizeModelFormData = (data: ModelVO): ModelFormData => ({
  ...buildEmptyFormData(),
  ...data,
  capabilities: data.capabilities || [],
  inputModalities: data.inputModalities || [],
  outputModalities: data.outputModalities || []
})

const normalizeStringArray = (values?: string[]) =>
  Array.from(new Set((values || []).map((item) => item?.trim()).filter((item): item is string => !!item)))

const parseJsonObject = (text: string, label: string) => {
  if (!text?.trim()) {
    return undefined
  }
  try {
    const value = JSON.parse(text)
    if (value === null || Array.isArray(value) || typeof value !== 'object') {
      throw new Error(`${label} 必须是 JSON 对象`)
    }
    return value
  } catch (error: any) {
    message.error(error?.message || `${label} 格式不正确`)
    throw error
  }
}

const stringifyJson = (value?: Record<string, any>) => {
  if (!value || !Object.keys(value).length) {
    return ''
  }
  return JSON.stringify(value, null, 2)
}

const syncJsonTextFromForm = () => {
  optionsText.value = stringifyJson(formData.value.options)
  limitsText.value = stringifyJson(formData.value.limits)
  metadataText.value = stringifyJson(formData.value.metadata)
}

const applyCapabilityPreset = () => {
  const type = formData.value.type
  const model = formData.value.model || ''
  if (type === AiModelTypeEnum.CHAT) {
    formData.value.capabilities = ['chat', 'text-generation', 'stream', 'tool']
    formData.value.inputModalities = ['text']
    formData.value.outputModalities = ['text']
  } else if (type === AiModelTypeEnum.IMAGE) {
    formData.value.capabilities = ['image-generation']
    formData.value.inputModalities = ['text']
    formData.value.outputModalities = ['image']
  } else if (type === AiModelTypeEnum.VOICE) {
    formData.value.capabilities = ['tts']
    formData.value.inputModalities = ['text']
    formData.value.outputModalities = ['audio']
  } else if (type === AiModelTypeEnum.VIDEO) {
    const isTextToVideo = /(^|[-_/])t2v($|[-_/])|text-?to-?video|text2video/i.test(model)
    formData.value.capabilities = isTextToVideo
      ? ['video', 'text-to-video']
      : ['video', 'image-to-video']
    formData.value.inputModalities = isTextToVideo ? ['text'] : ['image', 'text']
    formData.value.outputModalities = ['video']
  } else if (type === AiModelTypeEnum.EMBEDDING) {
    formData.value.capabilities = ['embedding']
    formData.value.inputModalities = ['text']
    formData.value.outputModalities = ['vector']
  } else if (type === AiModelTypeEnum.RERANK) {
    formData.value.capabilities = ['rerank']
    formData.value.inputModalities = ['text']
    formData.value.outputModalities = ['score']
  }
}
</script>
