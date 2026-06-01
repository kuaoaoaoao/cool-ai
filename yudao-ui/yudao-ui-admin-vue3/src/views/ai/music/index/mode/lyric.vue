<template>
  <div class="">
    <Title title="歌词" desc="自己编写歌词或使用Ai生成歌词，两节/8行效果最佳">
      <el-input
        v-model="formData.lyric"
        :autosize="{ minRows: 6, maxRows: 6 }"
        resize="none"
        type="textarea"
        maxlength="1200"
        show-word-limit
        placeholder="请输入您自己的歌词"
      />
    </Title>

    <Title title="音乐风格">
      <el-space class="flex-wrap">
        <el-tag
          v-for="tag in tags"
          :key="tag"
          round
          class="mb-8px cursor-pointer"
          :type="formData.tags.includes(tag) ? 'primary' : 'info'"
          @click="toggleTag(tag)"
        >
          {{ tag }}
        </el-tag>
      </el-space>

      <el-button
        :type="showCustom ? 'primary' : 'default'"
        round
        size="small"
        class="mb-6px"
        @click="showCustom = !showCustom"
      >
        自定义风格
      </el-button>
    </Title>

    <Title
      v-show="showCustom"
      desc="描述您想要的音乐风格，Suno无法识别艺术家的名字，但可以理解流派和氛围"
      class="-mt-12px"
    >
      <el-input
        v-model="formData.style"
        :autosize="{ minRows: 4, maxRows: 4 }"
        resize="none"
        type="textarea"
        maxlength="256"
        show-word-limit
        placeholder="输入音乐风格(英文)"
      />
    </Title>

    <Title title="音乐/歌曲名称">
      <el-input v-model="formData.name" placeholder="请输入音乐/歌曲名称" />
    </Title>

  </div>
</template>

<script lang="ts" setup>
import Title from '../title/index.vue'
defineOptions({ name: 'Lyric' })

const tags = ['rock', 'punk', 'jazz', 'soul', 'country', 'kidsmusic', 'pop']

const showCustom = ref(false)

const formData = reactive({
  lyric: '',
  style: '',
  name: '',
  tags: ['pop']
})

const toggleTag = (tag: string) => {
  const index = formData.tags.indexOf(tag)
  if (index >= 0) {
    formData.tags.splice(index, 1)
    return
  }
  formData.tags.push(tag)
}

defineExpose({
  formData
})
</script>
