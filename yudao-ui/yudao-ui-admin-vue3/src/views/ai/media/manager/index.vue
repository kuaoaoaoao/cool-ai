<template>
  <ContentWrap>
    <el-form
      ref="queryFormRef"
      class="-mb-15px"
      :model="queryParams"
      :inline="true"
      label-width="78px"
    >
      <el-form-item label="用户编号" prop="userId">
        <el-input v-model="queryParams.userId" clearable placeholder="请输入用户编号" class="!w-220px" />
      </el-form-item>
      <el-form-item label="任务类型" prop="taskType">
        <el-select v-model="queryParams.taskType" clearable placeholder="请选择任务类型" class="!w-220px">
          <el-option
            v-for="dict in getIntDictOptions(DICT_TYPE.AI_MEDIA_TASK_TYPE)"
            :key="dict.value"
            :label="dict.label"
            :value="dict.value"
          />
        </el-select>
      </el-form-item>
      <el-form-item label="任务状态" prop="status">
        <el-select v-model="queryParams.status" clearable placeholder="请选择任务状态" class="!w-220px">
          <el-option
            v-for="dict in getIntDictOptions(DICT_TYPE.AI_MEDIA_TASK_STATUS)"
            :key="dict.value"
            :label="dict.label"
            :value="dict.value"
          />
        </el-select>
      </el-form-item>
      <el-form-item label="平台" prop="platform">
        <el-input v-model="queryParams.platform" clearable placeholder="如 XiaomiMiMo" class="!w-220px" />
      </el-form-item>
      <el-form-item label="创建时间" prop="createTime">
        <el-date-picker
          v-model="queryParams.createTime"
          value-format="YYYY-MM-DD HH:mm:ss"
          type="daterange"
          start-placeholder="开始日期"
          end-placeholder="结束日期"
          :default-time="[new Date('1 00:00:00'), new Date('1 23:59:59')]"
          class="!w-240px"
        />
      </el-form-item>
      <el-form-item>
        <el-button @click="handleQuery"><Icon icon="ep:search" class="mr-5px" /> 搜索</el-button>
        <el-button @click="resetQuery"><Icon icon="ep:refresh" class="mr-5px" /> 重置</el-button>
      </el-form-item>
    </el-form>
  </ContentWrap>

  <ContentWrap>
    <el-table v-loading="loading" :data="list" :stripe="true" :show-overflow-tooltip="true">
      <el-table-column label="编号" align="center" prop="id" width="100" fixed="left" />
      <el-table-column label="用户编号" align="center" prop="userId" width="110" />
      <el-table-column label="类型" align="center" prop="taskType" width="110">
        <template #default="{ row }">
          <dict-tag :type="DICT_TYPE.AI_MEDIA_TASK_TYPE" :value="row.taskType" />
        </template>
      </el-table-column>
      <el-table-column label="状态" align="center" prop="status" width="110">
        <template #default="{ row }">
          <dict-tag :type="DICT_TYPE.AI_MEDIA_TASK_STATUS" :value="row.status" />
        </template>
      </el-table-column>
      <el-table-column label="进度" align="center" width="120">
        <template #default="{ row }">
          <el-progress :percentage="row.progress || 0" :stroke-width="8" />
        </template>
      </el-table-column>
      <el-table-column label="平台" align="center" prop="platform" width="130" />
      <el-table-column label="模型" align="center" prop="model" width="170" />
      <el-table-column label="输入摘要" min-width="220">
        <template #default="{ row }">
          {{ getInputSummary(row) }}
        </template>
      </el-table-column>
      <el-table-column label="结果" min-width="260">
        <template #default="{ row }">
          <audio v-if="getAudioUrl(row)" :src="getAudioUrl(row)" controls class="h-32px max-w-240px"></audio>
          <video
            v-else-if="getVideoUrl(row)"
            :src="getVideoUrl(row)"
            controls
            class="h-120px max-w-240px rounded-4px bg-black"
          ></video>
          <span v-else class="text-[var(--el-text-color-secondary)]">暂无</span>
        </template>
      </el-table-column>
      <el-table-column label="错误信息" align="center" prop="errorMessage" min-width="180" />
      <el-table-column label="创建时间" align="center" prop="createTime" :formatter="dateFormatter" width="180" />
      <el-table-column label="完成时间" align="center" prop="finishTime" :formatter="dateFormatter" width="180" />
    </el-table>
    <Pagination
      :total="total"
      v-model:page="queryParams.pageNo"
      v-model:limit="queryParams.pageSize"
      @pagination="getList"
    />
  </ContentWrap>
</template>

<script setup lang="ts">
import { DICT_TYPE, getIntDictOptions } from '@/utils/dict'
import { dateFormatter } from '@/utils/formatTime'
import { AiMediaTaskApi, AiMediaTaskVO } from '@/api/ai/media'
import { AiMediaTaskTypeEnum } from '@/views/ai/utils/constants'

defineOptions({ name: 'AiMediaManager' })

const loading = ref(true)
const list = ref<AiMediaTaskVO[]>([])
const total = ref(0)
const queryFormRef = ref()
const queryParams = reactive({
  pageNo: 1,
  pageSize: 10,
  userId: undefined,
  taskType: undefined,
  status: undefined,
  platform: undefined,
  createTime: []
})

const getList = async () => {
  loading.value = true
  try {
    const data = await AiMediaTaskApi.getTaskPage(queryParams)
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

const getAudioUrl = (row: AiMediaTaskVO) => {
  if (row.taskType !== AiMediaTaskTypeEnum.TTS) {
    return undefined
  }
  return row.outputJson?.url || row.resultFileIds?.[0]
}

const getVideoUrl = (row: AiMediaTaskVO) => {
  if (row.taskType !== AiMediaTaskTypeEnum.IMAGE_TO_VIDEO && row.taskType !== AiMediaTaskTypeEnum.TEXT_TO_VIDEO) {
    return undefined
  }
  return row.outputJson?.resultAsset?.url || row.outputJson?.url || row.resultFileIds?.[0]
}

const getInputSummary = (row: AiMediaTaskVO) => {
  if (!row.inputJson) {
    return ''
  }
  if (row.taskType === AiMediaTaskTypeEnum.IMAGE_TO_VIDEO) {
    return [row.inputJson.imageSize, row.inputJson.prompt].filter(Boolean).join('：')
  }
  if (row.taskType === AiMediaTaskTypeEnum.TEXT_TO_VIDEO) {
    return row.inputJson.prompt || JSON.stringify(row.inputJson)
  }
  if (row.inputJson.text) {
    const voice = row.inputJson.voiceProfileId
      ? `音色库 #${row.inputJson.voiceProfileId}`
      : row.inputJson.voice || row.inputJson.voiceMode || ''
    return [voice, row.inputJson.text].filter(Boolean).join('：')
  }
  return JSON.stringify(row.inputJson)
}

onMounted(getList)
</script>
