<template>
  <Dialog :title="dialogTitle" v-model="dialogVisible">
    <el-form
      ref="formRef"
      :model="formData"
      :rules="formRules"
      label-width="120px"
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
      <el-form-item v-if="apiKeyConfigTips.length" label="配置提示">
        <el-alert type="info" :closable="false" show-icon>
          <template #default>
            <div v-for="tip in apiKeyConfigTips" :key="tip">{{ tip }}</div>
          </template>
        </el-alert>
      </el-form-item>
      <el-form-item label="名称" prop="name">
        <el-input v-model="formData.name" placeholder="请输入名称" />
      </el-form-item>
      <el-form-item label="密钥" prop="apiKey">
        <el-input v-model="formData.apiKey" :placeholder="apiKeyPlaceholder" show-password />
      </el-form-item>
      <el-form-item label="自定义 API URL" prop="url">
        <el-input v-model="formData.url" :placeholder="apiUrlPlaceholder" />
      </el-form-item>
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
    </el-form>
    <template #footer>
      <el-button @click="submitForm" type="primary" :disabled="formLoading">确 定</el-button>
      <el-button @click="dialogVisible = false">取 消</el-button>
    </template>
  </Dialog>
</template>
<script setup lang="ts">
import { getIntDictOptions, DICT_TYPE, getStrDictOptions } from '@/utils/dict'
import { ApiKeyApi, ApiKeyVO } from '@/api/ai/model/apiKey'
import { CommonStatusEnum } from '@/utils/constants'
import { AiPlatformEnum } from '@/views/ai/utils/constants'

/** AI API 密钥 表单 */
defineOptions({ name: 'ApiKeyForm' })

const { t } = useI18n() // 国际化
const message = useMessage() // 消息弹窗

const dialogVisible = ref(false) // 弹窗的是否展示
const dialogTitle = ref('') // 弹窗的标题
const formLoading = ref(false) // 表单的加载中：1）修改时的数据加载；2）提交的按钮禁用
const formType = ref('') // 表单的类型：create - 新增；update - 修改

type ApiKeyFormData = Partial<ApiKeyVO>

const formData = ref<ApiKeyFormData>({
  id: undefined,
  name: undefined,
  apiKey: undefined,
  platform: undefined,
  url: undefined,
  status: CommonStatusEnum.ENABLE
})
const formRules = reactive({
  name: [{ required: true, message: '名称不能为空', trigger: 'blur' }],
  apiKey: [
    {
      validator: (_rule, value, callback) => {
        if (formType.value === 'create' && !value?.trim()) {
          callback(new Error('密钥不能为空'))
          return
        }
        callback()
      },
      trigger: 'blur'
    }
  ],
  platform: [{ required: true, message: '平台不能为空', trigger: 'blur' }],
  status: [{ required: true, message: '状态不能为空', trigger: 'blur' }]
})
const formRef = ref() // 表单 Ref

const apiUrlPlaceholder = computed(() => {
  if (formData.value.platform === AiPlatformEnum.XIAOMI_MIMO) {
    return 'https://api.xiaomimimo.com/v1'
  }
  if (formData.value.platform === AiPlatformEnum.OPENAI) {
    return 'OpenAI 官方可留空；兼容服务商填写 https://xxx/v1'
  }
  if (formData.value.platform === AiPlatformEnum.ANTHROPIC) {
    return 'Anthropic 官方可留空；中转服务按服务商文档填写'
  }
  if (formData.value.platform === AiPlatformEnum.Ollama) {
    return 'http://127.0.0.1:11434'
  }
  if (formData.value.platform === AiPlatformEnum.PYTHON_WORKER) {
    return 'http://127.0.0.1:8099'
  }
  return '请输入自定义 API URL'
})

const apiKeyPlaceholder = computed(() =>
  formType.value === 'update' ? '留空表示不修改密钥' : '请输入密钥'
)

const apiKeyConfigTips = computed(() => {
  const tips: string[] = []
  if (formData.value.platform === AiPlatformEnum.XIAOMI_MIMO) {
    tips.push('小米 MiMo 使用 api-key 请求头，API URL 建议填写 https://api.xiaomimimo.com/v1')
    tips.push('聊天模型可配置 mimo-v2.5、mimo-v2.5-pro、mimo-v2-flash；TTS 可配置普通合成、音色克隆、文字设计音色三个模型')
  }
  if (formData.value.platform === AiPlatformEnum.OPENAI) {
    tips.push('OpenAI 官方 URL 可留空；OpenAI-compatible 服务商需要填写兼容地址，通常以 /v1 结尾')
  }
  if (formData.value.platform === AiPlatformEnum.ANTHROPIC) {
    tips.push('Anthropic 原生 Claude 使用 Messages 协议；不要和 OpenAI-compatible 地址混填')
  }
  if (formData.value.platform === AiPlatformEnum.Ollama) {
    tips.push('Ollama 本地服务通常不需要真实密钥，可填 ollama；URL 必须填写本地或内网地址')
  }
  if (formData.value.platform === AiPlatformEnum.PYTHON_WORKER) {
    tips.push('Python Worker 本地联调可填 worker-secret；实际调用使用后端 yudao.ai.media.worker 配置')
    tips.push('URL 建议填写 http://127.0.0.1:8099，需与 worker.py 启动端口一致')
  }
  return tips
})

watch(
  () => formData.value.platform,
  (platform) => {
    if (platform === AiPlatformEnum.XIAOMI_MIMO && !formData.value.url) {
      formData.value.url = 'https://api.xiaomimimo.com/v1'
    }
    if (platform === AiPlatformEnum.Ollama && !formData.value.url) {
      formData.value.url = 'http://127.0.0.1:11434'
    }
    if (platform === AiPlatformEnum.PYTHON_WORKER && !formData.value.url) {
      formData.value.url = 'http://127.0.0.1:8099'
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
      formData.value = await ApiKeyApi.getApiKey(id)
      formData.value.apiKey = undefined
    } finally {
      formLoading.value = false
    }
  }
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
      apiKey: formData.value.apiKey?.trim(),
      platform: formData.value.platform?.trim(),
      url: formData.value.url?.trim()
    } as ApiKeyVO
    if (formType.value === 'create') {
      await ApiKeyApi.createApiKey(data)
      message.success(t('common.createSuccess'))
    } else {
      await ApiKeyApi.updateApiKey(data)
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
  formData.value = {
    id: undefined,
    name: undefined,
    apiKey: undefined,
    platform: undefined,
    url: undefined,
    status: CommonStatusEnum.ENABLE
  }
  formRef.value?.resetFields()
}
</script>
