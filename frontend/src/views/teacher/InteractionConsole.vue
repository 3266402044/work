<script setup lang="ts">
import { computed, onBeforeUnmount, onMounted, reactive, ref, watch } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import dayjs from 'dayjs'
import { announcementApi, quizApi, raceApi, rollCallApi, teacherApi } from '@/api'
import { formatDateTime, formatTime, performanceLabel, scopeLabel } from '@/utils/format'
import type { Course, QuizVO } from '@/types'
import PageHeader from '@/components/PageHeader.vue'
import StatusTag from '@/components/StatusTag.vue'
import EmptyState from '@/components/EmptyState.vue'

const tab = ref('race')
const courses = ref<Course[]>([])
const courseId = ref<number | undefined>(undefined)
const loading = ref(false)

const races = ref<any[]>([])
const quizzes = ref<QuizVO[]>([])
const announcements = ref<any[]>([])
const rollHistory = ref<any[]>([])
const quizHistory = ref<any[]>([])
const raceHistory = ref<any[]>([])

const raceForm = reactive({ title: '', durationSeconds: 30 })
const raceDialog = ref(false)
const raceDetail = ref<any>(null)
let raceTimer: number | null = null

const quizForm = reactive({
  title: '',
  type: 'SINGLE',
  options: ['选项 A', '选项 B', '选项 C', '选项 D'],
  correct: [0] as number[],
  durationSeconds: 60,
  anonymous: false,
  explanation: ''
})
const quizResultDialog = ref(false)
const quizResult = ref<any>(null)
const quizResultLoading = ref(false)

const annForm = reactive({ content: '', pinned: false, expireAt: '' })

const quizTypeLabel = (type: string) =>
  type === 'MULTIPLE' ? '多选题' : type === 'TRUE_FALSE' ? '判断题' : '单选题'

const loadCourses = async () => {
  courses.value = await teacherApi.courses()
  if (courses.value.length) courseId.value = courses.value[0].id
}

const loadAll = async () => {
  if (!courseId.value) return
  loading.value = true
  try {
    const [raceList, quizList, annList, rollList, qHistory, rHistory] = await Promise.all([
      raceApi.history({ courseId: courseId.value }),
      quizApi.list({ courseId: courseId.value }),
      announcementApi.list(courseId.value),
      rollCallApi.history({ courseId: courseId.value }),
      quizApi.history({ courseId: courseId.value }),
      raceApi.history({ courseId: courseId.value })
    ])
    races.value = raceList
    quizzes.value = quizList
    announcements.value = annList
    rollHistory.value = rollList
    quizHistory.value = qHistory
    raceHistory.value = rHistory
  } finally {
    loading.value = false
  }
}

watch(courseId, loadAll)

const createRace = async () => {
  if (!raceForm.title.trim()) {
    ElMessage.warning('请输入抢答标题')
    return
  }
  await raceApi.create({
    courseId: courseId.value,
    title: raceForm.title,
    durationSeconds: raceForm.durationSeconds
  })
  ElMessage.success('课堂抢答已发布，学生端会立即弹出抢答按钮')
  raceForm.title = ''
  await loadAll()
}

const openRace = async (raceId: number) => {
  raceDialog.value = true
  const load = async () => {
    try {
      raceDetail.value = await raceApi.detail(raceId)
    } catch {
      raceDialog.value = false
    }
  }
  await load()
  if (raceTimer) window.clearInterval(raceTimer)
  raceTimer = window.setInterval(load, 3000)
}

const closeRaceDialog = () => {
  if (raceTimer) {
    window.clearInterval(raceTimer)
    raceTimer = null
  }
}

const addOption = () => {
  if (quizForm.options.length >= 6) {
    ElMessage.warning('最多支持 6 个选项')
    return
  }
  quizForm.options.push(`选项 ${String.fromCharCode(65 + quizForm.options.length)}`)
}

const removeOption = (index: number) => {
  if (quizForm.options.length <= 2) {
    ElMessage.warning('至少保留两个选项')
    return
  }
  quizForm.options.splice(index, 1)
  quizForm.correct = quizForm.correct.filter((i) => i !== index).map((i) => (i > index ? i - 1 : i))
}

const toggleCorrect = (index: number) => {
  if (quizForm.type === 'MULTIPLE') {
    quizForm.correct = quizForm.correct.includes(index)
      ? quizForm.correct.filter((i) => i !== index)
      : [...quizForm.correct, index]
  } else {
    quizForm.correct = [index]
  }
}

watch(
  () => quizForm.type,
  (type) => {
    if (type === 'TRUE_FALSE') {
      quizForm.options = ['正确', '错误']
      quizForm.correct = [0]
    }
    if (quizForm.correct.some((i) => i >= quizForm.options.length)) {
      quizForm.correct = [0]
    }
  }
)

const createQuiz = async () => {
  if (!quizForm.title.trim()) {
    ElMessage.warning('请输入题目')
    return
  }
  if (!quizForm.correct.length) {
    ElMessage.warning('请设置正确答案')
    return
  }
  await quizApi.create({
    courseId: courseId.value,
    title: quizForm.title,
    type: quizForm.type,
    options: quizForm.options,
    correctAnswer: quizForm.correct,
    durationSeconds: quizForm.durationSeconds,
    anonymous: quizForm.anonymous,
    explanation: quizForm.explanation
  })
  ElMessage.success('课堂小测已发布')
  quizForm.title = ''
  quizForm.explanation = ''
  await loadAll()
}

const openQuizResult = async (quizId: number) => {
  quizResultDialog.value = true
  quizResultLoading.value = true
  try {
    quizResult.value = await quizApi.results(quizId)
  } finally {
    quizResultLoading.value = false
  }
}

const distributionPercent = (count: number) => {
  const total = quizResult.value?.summary?.answerCount || 0
  return total ? Math.round((count * 100) / total) : 0
}

const createAnnouncement = async () => {
  if (!annForm.content.trim()) {
    ElMessage.warning('请输入公告内容')
    return
  }
  await announcementApi.create({
    courseId: courseId.value,
    content: annForm.content,
    pinned: annForm.pinned,
    expireAt: annForm.expireAt || null
  })
  ElMessage.success('课堂公告已发布')
  annForm.content = ''
  await loadAll()
}

const removeAnnouncement = async (id: number) => {
  try {
    await ElMessageBox.confirm('确认删除该公告？', '删除公告', {
      confirmButtonText: '删除',
      cancelButtonText: '取消',
      type: 'warning'
    })
  } catch {
    return
  }
  await announcementApi.remove(id)
  ElMessage.success('公告已删除')
  await loadAll()
}

const exportRollCall = async () => {
  await rollCallApi.exportHistory({ courseId: courseId.value })
  ElMessage.success('互动记录导出已开始')
}

onMounted(async () => {
  await loadCourses()
  await loadAll()
})

onBeforeUnmount(() => closeRaceDialog())
</script>

<template>
  <div class="page">
    <PageHeader title="互动控制台" subtitle="课堂抢答、即时小测、课堂公告与互动记录统一管理" eyebrow="INTERACTION CONSOLE">
      <template #actions>
        <el-select v-model="courseId" placeholder="选择课程" style="width: 190px">
          <el-option v-for="course in courses" :key="course.id" :label="course.name" :value="course.id" />
        </el-select>
        <el-button :loading="loading" @click="loadAll">刷新</el-button>
        <el-button @click="$router.push('/teacher/rollcall')"><el-icon><Aim /></el-icon> 点名大屏</el-button>
      </template>
    </PageHeader>

    <el-tabs v-model="tab" class="console-tabs">
      <!-- 抢答 -->
      <el-tab-pane label="课堂抢答" name="race">
        <div class="grid grid-main-side">
          <section class="panel">
            <div class="panel-head">
              <div class="panel-title">发布抢答</div>
              <span class="panel-sub">10 - 60 秒 · 服务端按请求时间排名</span>
            </div>
            <el-form label-position="top">
              <el-form-item label="抢答标题" required>
                <el-input v-model="raceForm.title" maxlength="128" placeholder="例如：课堂抢答：导数的定义" />
              </el-form-item>
              <el-form-item :label="`答题时长：${raceForm.durationSeconds} 秒`">
                <el-slider v-model="raceForm.durationSeconds" :min="10" :max="60" :step="5" />
              </el-form-item>
              <el-button type="primary" :disabled="!courseId" @click="createRace">
                <el-icon><Lightning /></el-icon> 立即发布
              </el-button>
            </el-form>
            <div class="divider" />
            <div class="hint-box">
              <el-icon><InfoFilled /></el-icon>
              <span>学生端会立即出现抢答浮层，前 3 名由后端排序返回，前端本地排序无效。</span>
            </div>
          </section>

          <section class="panel">
            <div class="panel-head">
              <div class="panel-title">抢答记录</div>
              <span class="panel-sub">{{ races.length }} 次</span>
            </div>
            <div v-if="!races.length">
              <EmptyState title="暂无抢答记录" icon="Lightning" />
            </div>
            <div v-else class="item-list">
              <div v-for="race in races" :key="race.id" class="list-item">
                <div class="row-between">
                  <span class="item-title">{{ race.title }}</span>
                  <StatusTag :status="race.status" />
                </div>
                <div class="item-meta">
                  <span class="mono">{{ formatDateTime(race.startTime) }}</span>
                  <span>{{ race.total }} 人参与</span>
                </div>
                <div class="row" style="gap: 6px; margin-top: 6px">
                  <el-button size="small" @click="openRace(race.id)">查看排名</el-button>
                </div>
              </div>
            </div>
          </section>
        </div>
      </el-tab-pane>

      <!-- 小测 -->
      <el-tab-pane label="课堂小测" name="quiz">
        <div class="grid grid-main-side">
          <section class="panel">
            <div class="panel-head">
              <div class="panel-title">创建小测 / 投票</div>
            </div>
            <el-form label-position="top">
              <el-form-item label="题型">
                <el-radio-group v-model="quizForm.type">
                  <el-radio-button value="SINGLE">单选</el-radio-button>
                  <el-radio-button value="MULTIPLE">多选</el-radio-button>
                  <el-radio-button value="TRUE_FALSE">判断</el-radio-button>
                </el-radio-group>
              </el-form-item>
              <el-form-item label="题目" required>
                <el-input v-model="quizForm.title" type="textarea" :rows="2" maxlength="255" placeholder="请输入题干" />
              </el-form-item>
              <el-form-item label="选项与正确答案">
                <div class="options-editor">
                  <div v-for="(option, index) in quizForm.options" :key="index" class="option-row">
                    <button
                      type="button"
                      class="correct-toggle"
                      :class="{ active: quizForm.correct.includes(index) }"
                      @click="toggleCorrect(index)"
                    >
                      {{ quizForm.type === 'MULTIPLE' ? '☑' : '◉' }}
                    </button>
                    <el-input v-model="quizForm.options[index]" :disabled="quizForm.type === 'TRUE_FALSE'" />
                    <el-button
                      v-if="quizForm.type !== 'TRUE_FALSE'"
                      text
                      type="danger"
                      size="small"
                      @click="removeOption(index)"
                    >删除</el-button>
                  </div>
                  <el-button v-if="quizForm.type !== 'TRUE_FALSE'" text type="primary" size="small" @click="addOption">
                    + 添加选项
                  </el-button>
                </div>
              </el-form-item>
              <div class="grid grid-2">
                <el-form-item :label="`答题时长：${quizForm.durationSeconds} 秒`">
                  <el-slider v-model="quizForm.durationSeconds" :min="10" :max="600" :step="10" />
                </el-form-item>
                <el-form-item label="匿名统计">
                  <el-switch v-model="quizForm.anonymous" />
                </el-form-item>
              </div>
              <el-form-item label="答案解析（可选）">
                <el-input v-model="quizForm.explanation" type="textarea" :rows="2" maxlength="1000" />
              </el-form-item>
              <el-button type="primary" :disabled="!courseId" @click="createQuiz">发布小测</el-button>
            </el-form>
          </section>

          <section class="panel">
            <div class="panel-head">
              <div class="panel-title">最近小测</div>
              <span class="panel-sub">{{ quizzes.length }} 个</span>
            </div>
            <div v-if="!quizzes.length">
              <EmptyState title="暂无课堂小测" icon="EditPen" />
            </div>
            <div v-else class="item-list">
              <div v-for="quiz in quizzes" :key="quiz.id" class="list-item">
                <div class="row-between">
                  <span class="item-title">{{ quiz.title }}</span>
                  <StatusTag :status="quiz.status" />
                </div>
                <div class="item-meta">
                  <span class="chip cyan">{{ quizTypeLabel(quiz.type) }}</span>
                  <span>{{ quiz.answerCount }} / {{ quiz.totalStudents }} 人提交</span>
                  <span class="mono">{{ formatTime(quiz.endTime) }} 截止</span>
                </div>
                <div class="row" style="gap: 6px; margin-top: 6px">
                  <el-button size="small" @click="openQuizResult(quiz.id)">实时结果</el-button>
                </div>
              </div>
            </div>
          </section>
        </div>
      </el-tab-pane>

      <!-- 公告 -->
      <el-tab-pane label="课堂公告" name="announcement">
        <div class="grid grid-main-side">
          <section class="panel">
            <div class="panel-head"><div class="panel-title">发布公告</div></div>
            <el-form label-position="top">
              <el-form-item label="公告内容" required>
                <el-input v-model="annForm.content" type="textarea" :rows="4" maxlength="1000" show-word-limit placeholder="请输入要推送给学生的课堂公告" />
              </el-form-item>
              <div class="grid grid-2">
                <el-form-item label="置顶">
                  <el-switch v-model="annForm.pinned" />
                </el-form-item>
                <el-form-item label="失效时间（可选）">
                  <el-date-picker v-model="annForm.expireAt" type="datetime" value-format="YYYY-MM-DDTHH:mm:ss" style="width: 100%" />
                </el-form-item>
              </div>
              <el-button type="primary" :disabled="!courseId" @click="createAnnouncement">发布公告</el-button>
            </el-form>
          </section>

          <section class="panel">
            <div class="panel-head">
              <div class="panel-title">课程公告</div>
              <span class="panel-sub">{{ announcements.length }} 条</span>
            </div>
            <div v-if="!announcements.length">
              <EmptyState title="暂无公告" icon="Bell" />
            </div>
            <div v-else class="ann-list">
              <div v-for="item in announcements" :key="item.id" class="ann-item">
                <div class="row-between">
                  <div class="row" style="gap: 6px">
                    <span v-if="item.pinned" class="chip amber">置顶</span>
                    <span class="mono muted" style="font-size: 10.5px">{{ formatDateTime(item.createTime) }}</span>
                  </div>
                  <el-button text type="danger" size="small" @click="removeAnnouncement(item.id)">删除</el-button>
                </div>
                <div class="ann-content">{{ item.content }}</div>
                <div v-if="item.expireAt" class="muted" style="font-size: 10.5px; margin-top: 4px">失效：{{ formatDateTime(item.expireAt) }}</div>
              </div>
            </div>
          </section>
        </div>
      </el-tab-pane>

      <!-- 互动记录 -->
      <el-tab-pane label="互动记录" name="history">
        <section class="panel">
          <div class="panel-head">
            <div class="panel-title">随机点名记录</div>
            <div class="row" style="gap: 8px">
              <span class="panel-sub">{{ rollHistory.length }} 次会话</span>
              <el-button size="small" @click="exportRollCall"><el-icon><Download /></el-icon> 导出 Excel</el-button>
            </div>
          </div>
          <div v-if="!rollHistory.length">
            <EmptyState title="暂无点名记录" icon="Aim" />
          </div>
          <div v-else class="history-list">
            <el-collapse>
              <el-collapse-item v-for="session in rollHistory" :key="session.id" :name="session.id">
                <template #title>
                  <div class="history-title">
                    <span class="mono">#{{ session.id }}</span>
                    <span>{{ session.courseName }}</span>
                    <span class="muted">{{ session.className || '全部班级' }}</span>
                    <span class="chip">{{ scopeLabel(session.candidateScope) }}</span>
                    <StatusTag :status="session.status" />
                    <span class="muted mono push-right">{{ formatDateTime(session.startedAt) }}</span>
                  </div>
                </template>
                <div class="history-summary">
                  <span>抽取 {{ session.summary.total }} 人</span>
                  <span class="green">已响应 {{ session.summary.responded }}</span>
                  <span class="coral">未响应 {{ session.summary.noResponse }}</span>
                  <span class="amber">待响应 {{ session.summary.pending }}</span>
                  <span>跳过 {{ session.summary.skipped }}</span>
                </div>
                <div class="record-grid">
                  <div v-for="record in session.records" :key="record.recordId" class="record-item">
                    <div class="row-between">
                      <span>{{ record.name }} <span class="muted mono">{{ record.studentNo }}</span></span>
                      <StatusTag :status="record.result" :label="record.resultLabel" />
                    </div>
                    <div class="record-meta">
                      <span class="mono">序号 {{ record.drawOrder }}</span>
                      <span v-if="record.performance">{{ performanceLabel(record.performance) }}</span>
                      <span v-if="record.respondedAt" class="mono">响应 {{ formatTime(record.respondedAt) }}</span>
                    </div>
                  </div>
                </div>
              </el-collapse-item>
            </el-collapse>
          </div>
        </section>

        <div class="grid grid-2 section-gap">
          <section class="panel">
            <div class="panel-head"><div class="panel-title">小测记录</div><span class="panel-sub">{{ quizHistory.length }} 个</span></div>
            <div v-if="!quizHistory.length"><EmptyState title="暂无小测记录" icon="EditPen" /></div>
            <el-table v-else :data="quizHistory" size="small" max-height="320">
              <el-table-column label="题目" min-width="160" prop="title" />
              <el-table-column label="类型" width="80">
                <template #default="{ row }">{{ quizTypeLabel(row.type) }}</template>
              </el-table-column>
              <el-table-column label="提交" width="70" prop="answerCount" />
              <el-table-column label="状态" width="90">
                <template #default="{ row }"><StatusTag :status="row.status" /></template>
              </el-table-column>
              <el-table-column label="操作" width="80">
                <template #default="{ row }"><el-button text type="primary" size="small" @click="openQuizResult(row.id)">结果</el-button></template>
              </el-table-column>
            </el-table>
          </section>

          <section class="panel">
            <div class="panel-head"><div class="panel-title">抢答记录</div><span class="panel-sub">{{ raceHistory.length }} 次</span></div>
            <div v-if="!raceHistory.length"><EmptyState title="暂无抢答记录" icon="Lightning" /></div>
            <el-table v-else :data="raceHistory" size="small" max-height="320">
              <el-table-column label="标题" min-width="160" prop="title" />
              <el-table-column label="开始" width="150">
                <template #default="{ row }"><span class="mono muted">{{ formatDateTime(row.startTime) }}</span></template>
              </el-table-column>
              <el-table-column label="参与" width="70" prop="total" />
              <el-table-column label="状态" width="90">
                <template #default="{ row }"><StatusTag :status="row.status" /></template>
              </el-table-column>
              <el-table-column label="操作" width="80">
                <template #default="{ row }"><el-button text type="primary" size="small" @click="openRace(row.id)">排名</el-button></template>
              </el-table-column>
            </el-table>
          </section>
        </div>
      </el-tab-pane>
    </el-tabs>

    <!-- 抢答实时 -->
    <el-dialog v-model="raceDialog" title="抢答实时排名" width="480px" @close="closeRaceDialog">
      <div v-if="raceDetail" class="race-detail">
        <div class="row-between">
          <div>
            <div class="detail-title">{{ raceDetail.title }}</div>
            <div class="muted" style="font-size: 11px">{{ raceDetail.courseName }}</div>
          </div>
          <div class="col" style="gap: 0; align-items: flex-end">
            <span class="label">剩余时间</span>
            <span class="mono cyan">{{ raceDetail.remainingSeconds }}s</span>
          </div>
        </div>
        <div class="top3-list">
          <div v-for="(item, index) in raceDetail.top3" :key="item.studentNo" class="top3-row">
            <span class="top3-medal mono">{{ index + 1 }}</span>
            <span class="top3-name">{{ item.name }}</span>
            <span class="muted mono">{{ item.studentNo }}</span>
          </div>
          <EmptyState v-if="!raceDetail.top3?.length" title="暂无学生抢答" icon="Lightning" />
        </div>
      </div>
    </el-dialog>

    <!-- 小测结果 -->
    <el-dialog v-model="quizResultDialog" title="小测实时结果" width="640px">
      <div v-if="quizResultLoading" class="skeleton" style="height: 260px" />
      <div v-else-if="quizResult" class="quiz-result">
        <div class="row-between">
          <div class="detail-title">{{ quizResult.quiz.title }}</div>
          <div class="row" style="gap: 10px">
            <span class="chip cyan">提交 {{ quizResult.summary.answerCount }}/{{ quizResult.summary.totalStudents }}</span>
            <span class="chip green">正确率 {{ quizResult.summary.correctRate }}%</span>
          </div>
        </div>
        <div class="dist-list">
          <div v-for="item in quizResult.distribution" :key="item.index" class="dist-item" :class="{ correct: item.correct }">
            <div class="row-between" style="margin-bottom: 4px">
              <span>{{ item.option }}</span>
              <span class="mono">{{ item.count }} 人 · {{ distributionPercent(item.count) }}%</span>
            </div>
            <div class="bar-track">
              <div class="bar-fill" :style="{ width: distributionPercent(item.count) + '%', background: item.correct ? 'var(--green)' : 'var(--cyan)' }" />
            </div>
          </div>
        </div>
        <div v-if="quizResult.participants?.length" class="participants">
          <div class="label" style="margin-bottom: 6px">参与学生</div>
          <el-table :data="quizResult.participants" size="small" max-height="200">
            <el-table-column label="学生" min-width="120">
              <template #default="{ row }">{{ row.name }} <span class="muted mono">{{ row.studentNo }}</span></template>
            </el-table-column>
            <el-table-column label="答案" width="100" prop="answer" />
            <el-table-column label="结果" width="90">
              <template #default="{ row }"><span :class="row.isCorrect ? 'green' : 'coral'">{{ row.isCorrect ? '正确' : '错误' }}</span></template>
            </el-table-column>
            <el-table-column label="提交时间" width="150">
              <template #default="{ row }"><span class="mono muted">{{ formatDateTime(row.answerTime) }}</span></template>
            </el-table-column>
          </el-table>
        </div>
        <div v-else class="muted" style="font-size: 11px; margin-top: 10px">匿名模式，不展示学生姓名</div>
      </div>
    </el-dialog>
  </div>
</template>

<style scoped>
.console-tabs :deep(.el-tabs__header) { margin-bottom: 18px; }
.item-list { display: flex; flex-direction: column; gap: 9px; max-height: 440px; overflow: auto; }
.list-item { border: 1px solid var(--line); border-radius: 8px; padding: 11px 12px; background: var(--panel-3); }
.item-title { font-size: 13px; color: var(--text-2); }
.item-meta { display: flex; gap: 10px; flex-wrap: wrap; font-size: 10.5px; color: var(--muted); margin-top: 5px; align-items: center; }
.hint-box { display: flex; gap: 8px; font-size: 11px; color: var(--muted); line-height: 1.7; }

.options-editor { display: flex; flex-direction: column; gap: 7px; width: 100%; }
.option-row { display: flex; align-items: center; gap: 8px; }
.correct-toggle {
  width: 30px; height: 30px; border-radius: 7px;
  border: 1px solid var(--line);
  background: transparent; color: var(--muted);
  cursor: pointer; font-size: 13px; flex-shrink: 0;
}
.correct-toggle.active { border-color: rgba(71, 209, 108, 0.5); color: var(--green); background: rgba(71, 209, 108, 0.08); }

.ann-list { display: flex; flex-direction: column; gap: 10px; max-height: 420px; overflow: auto; }
.ann-item { border: 1px solid var(--line); border-radius: 8px; padding: 11px 12px; background: var(--panel-3); }
.ann-content { font-size: 12.5px; color: var(--text-2); line-height: 1.75; margin-top: 5px; }

.history-title { display: flex; align-items: center; gap: 10px; font-size: 12px; width: 100%; flex-wrap: wrap; }
.push-right { margin-left: auto; }
.history-summary { display: flex; gap: 16px; flex-wrap: wrap; font-size: 11px; color: var(--muted); margin-bottom: 10px; }
.record-grid { display: grid; grid-template-columns: repeat(2, minmax(0, 1fr)); gap: 8px; }
.record-item { border: 1px solid var(--line); border-radius: 7px; padding: 8px 10px; font-size: 11.5px; background: rgba(148, 163, 184, 0.03); }
.record-meta { display: flex; gap: 10px; font-size: 10px; color: var(--muted); margin-top: 3px; }

.race-detail { display: flex; flex-direction: column; gap: 14px; }
.detail-title { font-size: 15px; font-weight: 600; }
.top3-list { display: flex; flex-direction: column; gap: 7px; }
.top3-row { display: flex; align-items: center; gap: 12px; padding: 10px 12px; border-radius: 8px; background: rgba(148, 163, 184, 0.05); }
.top3-medal { width: 22px; height: 22px; border-radius: 50%; display: flex; align-items: center; justify-content: center; background: rgba(245, 185, 66, 0.14); color: var(--amber); font-size: 11px; }
.top3-name { flex: 1; font-size: 13px; }

.quiz-result { display: flex; flex-direction: column; gap: 14px; }
.dist-list { display: flex; flex-direction: column; gap: 10px; }
.dist-item { font-size: 12px; color: var(--text-2); }
.dist-item.correct { color: var(--green); }

@media (max-width: 760px) {
  .record-grid { grid-template-columns: 1fr; }
  .history-title { gap: 6px; }
  .push-right { margin-left: 0; }
}
</style>
