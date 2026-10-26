<script setup lang="ts">
import { computed, onMounted, reactive, ref, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import dayjs from 'dayjs'
import { ElMessage, ElMessageBox } from 'element-plus'
import { taskApi, teacherApi } from '@/api'
import { formatDateTime, modeLabel } from '@/utils/format'
import type { ClassGroup, Course } from '@/types'
import PageHeader from '@/components/PageHeader.vue'

const router = useRouter()
const route = useRoute()
const step = ref(0)
const loading = ref(true)
const publishing = ref(false)
const courses = ref<Course[]>([])
const classes = ref<ClassGroup[]>([])
const passcodeVisible = ref(false)
const createdTask = ref<any>(null)

const now = dayjs()
const form = reactive({
  title: '',
  courseId: undefined as number | undefined,
  classId: undefined as number | undefined,
  startTime: now.add(5, 'minute').format('YYYY-MM-DDTHH:mm:ss'),
  endTime: now.add(25, 'minute').format('YYYY-MM-DDTHH:mm:ss'),
  lateTime: now.add(10, 'minute').format('YYYY-MM-DDTHH:mm:ss'),
  signModes: ['CLICK'] as string[],
  locationName: '',
  longitude: undefined as number | undefined,
  latitude: undefined as number | undefined,
  radiusMeters: 150
})

const modeOptions = [
  { value: 'CLICK', label: '普通签到', desc: '学生点击即可完成，适合线下小班', icon: 'Pointer' },
  { value: 'QR', label: '动态二维码', desc: '30 秒短期令牌，学生扫码进入任务', icon: 'Grid' },
  { value: 'LOCATION', label: '定位签到', desc: '校验学生与课堂坐标距离', icon: 'Location' },
  { value: 'PASSCODE', label: '口令签到', desc: '教师公布 6 位口令，仅保存哈希', icon: 'Key' }
]

const selectedCourse = computed(() => courses.value.find((course) => course.id === form.courseId))
const selectedClass = computed(() => classes.value.find((group) => group.id === form.classId))

const loadCourses = async () => {
  loading.value = true
  try {
    courses.value = await teacherApi.courses()
    const presetCourse = Number(route.query.courseId)
    if (presetCourse) {
      form.courseId = presetCourse
      await loadClasses()
    }
    const copyId = Number(route.query.copy)
    if (copyId) {
      const task = await taskApi.detail(copyId)
      form.title = task.title
      form.courseId = task.courseId
      form.classId = task.classId
      form.signModes = [...task.signModes]
      form.locationName = task.locationName || ''
      form.longitude = task.longitude
      form.latitude = task.latitude
      form.radiusMeters = task.radiusMeters || 150
      await loadClasses()
    }
  } finally {
    loading.value = false
  }
}

const loadClasses = async () => {
  if (!form.courseId) {
    classes.value = []
    return
  }
  classes.value = await teacherApi.classes({ courseId: form.courseId })
  if (!classes.value.some((group) => group.id === form.classId)) {
    form.classId = classes.value[0]?.id
  }
}

watch(
  () => form.courseId,
  async (value, old) => {
    if (old !== undefined) {
      form.classId = undefined
      await loadClasses()
    }
  }
)

const applyCourseLocation = () => {
  const course = selectedCourse.value
  if (!course) return
  form.locationName = course.classroom || form.locationName
  form.longitude = course.longitude
  form.latitude = course.latitude
  form.radiusMeters = course.defaultRadius || 150
  ElMessage.success('已使用课程默认签到地点')
}

const validateStep = (index: number) => {
  if (index === 0) {
    if (!form.courseId) return '请选择课程'
    if (!form.classId) return '请选择班级'
  }
  if (index === 1) {
    if (!form.title.trim()) return '请输入任务标题'
    if (!form.startTime || !form.endTime) return '请选择开始与结束时间'
    if (!dayjs(form.startTime).isBefore(dayjs(form.endTime))) return '结束时间必须晚于开始时间'
    if (dayjs(form.startTime).isBefore(dayjs().subtract(1, 'minute'))) return '开始时间不能早于当前时间'
    if (form.lateTime) {
      if (dayjs(form.lateTime).isBefore(dayjs(form.startTime)) || dayjs(form.lateTime).isAfter(dayjs(form.endTime))) {
        return '迟到截止时间必须位于开始与结束时间之间'
      }
    }
  }
  if (index === 2) {
    if (!form.signModes.length) return '至少选择一种签到方式'
    if (form.signModes.includes('LOCATION')) {
      if (form.longitude === undefined || form.latitude === undefined) return '定位签到需要填写经纬度'
      if (form.radiusMeters < 20 || form.radiusMeters > 5000) return '定位半径需在 20-5000 米之间'
    }
  }
  return ''
}

const next = async () => {
  const message = validateStep(step.value)
  if (message) {
    ElMessage.warning(message)
    return
  }
  step.value = Math.min(3, step.value + 1)
}

const prev = () => {
  step.value = Math.max(0, step.value - 1)
}

const publish = async () => {
  for (let index = 0; index <= 2; index++) {
    const message = validateStep(index)
    if (message) {
      step.value = index
      ElMessage.warning(message)
      return
    }
  }
  publishing.value = true
  try {
    const result = await taskApi.create({
      title: form.title,
      courseId: form.courseId,
      classId: form.classId,
      startTime: form.startTime,
      endTime: form.endTime,
      lateTime: form.lateTime || null,
      signModes: form.signModes,
      locationName: form.locationName || null,
      longitude: form.signModes.includes('LOCATION') ? form.longitude : null,
      latitude: form.signModes.includes('LOCATION') ? form.latitude : null,
      radiusMeters: form.signModes.includes('LOCATION') ? form.radiusMeters : null
    })
    createdTask.value = result
    ElMessage.success('签到任务已发布')
    if (result.passcode) {
      passcodeVisible.value = true
    } else {
      router.push(`/teacher/tasks/${result.id}/monitor`)
    }
  } finally {
    publishing.value = false
  }
}

const goMonitor = () => {
  passcodeVisible.value = false
  if (createdTask.value) router.push(`/teacher/tasks/${createdTask.value.id}/monitor`)
}

const copyPasscode = async () => {
  try {
    await navigator.clipboard.writeText(createdTask.value?.passcode || '')
    ElMessage.success('口令已复制')
  } catch {
    ElMessage.warning('请手动记录口令')
  }
}

onMounted(loadCourses)
</script>

<template>
  <div class="page">
    <PageHeader title="创建签到任务" subtitle="按步骤配置课程、时间、签到方式与地点，发布后立即生效" eyebrow="CREATE TASK">
      <template #actions><el-button @click="router.push('/teacher/tasks')">返回任务列表</el-button></template>
    </PageHeader>

    <div v-if="loading" class="skeleton" style="height: 400px" />

    <div v-else class="create-grid">
      <section class="panel step-panel">
        <div class="steps">
          <div v-for="(label, index) in ['对象与课程', '时间与标题', '签到方式', '预览发布']" :key="label" class="step-item" :class="{ active: step === index, done: step > index }">
            <div class="step-num mono">{{ index + 1 }}</div>
            <div class="step-label">{{ label }}</div>
          </div>
        </div>

        <div class="step-body">
          <template v-if="step === 0">
            <div class="step-title">选择签到对象</div>
            <p class="step-desc">任务只能发布到你授课的课程与班级，后端会再次校验授课关系。</p>
            <el-form label-position="top">
              <el-form-item label="课程" required>
                <el-select v-model="form.courseId" placeholder="选择课程" style="width: 100%" @change="loadClasses">
                  <el-option v-for="course in courses" :key="course.id" :label="`${course.name}（${course.courseNo}）`" :value="course.id" />
                </el-select>
              </el-form-item>
              <el-form-item label="面向班级" required>
                <el-select v-model="form.classId" placeholder="选择班级" style="width: 100%" :disabled="!form.courseId">
                  <el-option v-for="group in classes" :key="group.id" :label="group.name" :value="group.id" />
                </el-select>
                <div v-if="form.courseId && !classes.length" class="muted hint">该课程尚未关联班级，请联系管理员配置授课关系</div>
              </el-form-item>
            </el-form>
          </template>

          <template v-else-if="step === 1">
            <div class="step-title">设置任务标题与时间</div>
            <p class="step-desc">开始时间必须早于结束时间；迟到截止时间可留空，超过该时间签到会记为迟到。</p>
            <el-form label-position="top">
              <el-form-item label="任务标题" required>
                <el-input v-model="form.title" maxlength="128" show-word-limit placeholder="例如：高等数学 · 第 3 次签到" />
              </el-form-item>
              <div class="grid grid-2">
                <el-form-item label="开始时间" required>
                  <el-date-picker v-model="form.startTime" type="datetime" value-format="YYYY-MM-DDTHH:mm:ss" style="width: 100%" />
                </el-form-item>
                <el-form-item label="结束时间" required>
                  <el-date-picker v-model="form.endTime" type="datetime" value-format="YYYY-MM-DDTHH:mm:ss" style="width: 100%" />
                </el-form-item>
              </div>
              <el-form-item label="迟到截止时间">
                <el-date-picker v-model="form.lateTime" type="datetime" value-format="YYYY-MM-DDTHH:mm:ss" style="width: 100%" />
              </el-form-item>
            </el-form>
          </template>

          <template v-else-if="step === 2">
            <div class="step-title">选择签到方式与地点</div>
            <p class="step-desc">至少开启一种方式，可组合使用。开启定位签到时必须配置课堂坐标与半径。</p>
            <div class="mode-grid">
              <button
                v-for="mode in modeOptions"
                :key="mode.value"
                type="button"
                class="mode-card"
                :class="{ active: form.signModes.includes(mode.value) }"
                @click="form.signModes.includes(mode.value) ? (form.signModes = form.signModes.filter((m) => m !== mode.value)) : form.signModes.push(mode.value)"
              >
                <el-icon class="mode-icon"><component :is="mode.icon" /></el-icon>
                <div>
                  <div class="mode-name">{{ mode.label }}</div>
                  <div class="mode-desc">{{ mode.desc }}</div>
                </div>
                <el-icon v-if="form.signModes.includes(mode.value)" class="mode-check"><Check /></el-icon>
              </button>
            </div>

            <div v-if="form.signModes.includes('LOCATION')" class="location-box">
              <div class="row-between" style="margin-bottom: 10px">
                <span class="label">定位签到配置</span>
                <el-button text type="primary" size="small" @click="applyCourseLocation">使用课程教室坐标</el-button>
              </div>
              <el-form label-position="top">
                <el-form-item label="签到地点名称">
                  <el-input v-model="form.locationName" placeholder="例如：教学楼 A-302" />
                </el-form-item>
                <div class="grid grid-2">
                  <el-form-item label="经度（longitude）">
                    <el-input-number v-model="form.longitude" :precision="7" :step="0.0001" :controls="false" style="width: 100%" placeholder="118.1234567" />
                  </el-form-item>
                  <el-form-item label="纬度（latitude）">
                    <el-input-number v-model="form.latitude" :precision="7" :step="0.0001" :controls="false" style="width: 100%" placeholder="32.1234567" />
                  </el-form-item>
                </div>
                <el-form-item :label="`允许半径：${form.radiusMeters} 米`">
                  <el-slider v-model="form.radiusMeters" :min="20" :max="1000" :step="10" style="width: 100%" />
                </el-form-item>
              </el-form>
            </div>

            <div v-if="form.signModes.includes('PASSCODE')" class="hint-box">
              <el-icon><Key /></el-icon>
              发布后系统会生成 6 位随机口令，只保存 BCrypt 哈希，请在大屏展示或口述给学生。
            </div>
            <div v-if="form.signModes.includes('QR')" class="hint-box">
              <el-icon><Grid /></el-icon>
              动态二维码在任务进行中生成，有效期 30 秒，过期后可在监控页刷新。
            </div>
          </template>

          <template v-else>
            <div class="step-title">确认并发布</div>
            <p class="step-desc">发布后任务立即进入时间轴，学生端将在签到窗口内看到任务。</p>
            <div class="preview">
              <div class="preview-title">{{ form.title || '未命名签到任务' }}</div>
              <div class="preview-meta">
                <span class="chip cyan">{{ selectedCourse?.name || '未选择课程' }}</span>
                <span class="chip">{{ selectedClass?.name || '未选择班级' }}</span>
                <span class="chip">{{ form.signModes.map(modeLabel).join(' / ') }}</span>
              </div>
              <div class="preview-grid">
                <div><span>开始时间</span><strong class="mono">{{ formatDateTime(form.startTime) }}</strong></div>
                <div><span>结束时间</span><strong class="mono">{{ formatDateTime(form.endTime) }}</strong></div>
                <div><span>迟到截止</span><strong class="mono">{{ form.lateTime ? formatDateTime(form.lateTime) : '未设置' }}</strong></div>
                <div><span>签到地点</span><strong>{{ form.locationName || '未设置' }}</strong></div>
                <div v-if="form.signModes.includes('LOCATION')"><span>允许半径</span><strong class="mono">{{ form.radiusMeters }} m</strong></div>
                <div><span>签到方式</span><strong>{{ form.signModes.length }} 种</strong></div>
              </div>
            </div>
            <el-alert
              type="info"
              :closable="false"
              show-icon
              title="发布后仍可编辑未结束任务，也可提前关闭；已结束的任务不可修改。"
              style="margin-top: 14px"
            />
          </template>
        </div>

        <div class="step-foot">
          <el-button v-if="step > 0" @click="prev">上一步</el-button>
          <div class="grow" />
          <el-button v-if="step < 3" type="primary" @click="next">下一步</el-button>
          <el-button v-else type="primary" :loading="publishing" @click="publish">发布签到任务</el-button>
        </div>
      </section>

      <aside class="panel summary-panel">
        <div class="panel-head"><div class="panel-title">实时预览</div></div>
        <div class="sum-course" :style="{ '--c': selectedCourse?.coverColor || 'var(--cyan)' }">
          <span>{{ selectedCourse?.name || '选择课程' }}</span>
          <strong>{{ form.title || '签到任务标题' }}</strong>
          <div class="sum-meta">{{ selectedClass?.name || '未选择班级' }}</div>
        </div>
        <div class="divider" />
        <div class="sum-list">
          <div><span>开始</span><strong class="mono">{{ form.startTime ? formatDateTime(form.startTime) : '—' }}</strong></div>
          <div><span>结束</span><strong class="mono">{{ form.endTime ? formatDateTime(form.endTime) : '—' }}</strong></div>
          <div><span>迟到截止</span><strong class="mono">{{ form.lateTime ? formatDateTime(form.lateTime) : '—' }}</strong></div>
          <div><span>方式</span><strong>{{ form.signModes.map(modeLabel).join(' / ') || '—' }}</strong></div>
          <div><span>地点</span><strong>{{ form.locationName || '—' }}</strong></div>
        </div>
        <div class="divider" />
        <div class="tip">
          <el-icon><InfoFilled /></el-icon>
          <span>任务状态由服务端时间实时计算，前端不自行判断是否可签到。</span>
        </div>
      </aside>
    </div>

    <el-dialog v-model="passcodeVisible" title="口令签到已生成" width="400px">
      <div class="passcode-block">
        <div class="muted">请在课堂大屏展示以下 6 位口令</div>
        <div class="passcode-value mono">{{ createdTask?.passcode }}</div>
        <el-button type="primary" plain @click="copyPasscode">复制口令</el-button>
      </div>
      <template #footer>
        <el-button @click="goMonitor">前往实时监控</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<style scoped>
.create-grid { display: grid; grid-template-columns: minmax(0, 1.55fr) minmax(280px, 1fr); gap: 16px; align-items: start; }
.step-panel { padding: 0; overflow: hidden; }

.steps { display: flex; border-bottom: 1px solid var(--line); }
.step-item { flex: 1; display: flex; align-items: center; gap: 9px; padding: 16px 18px; position: relative; }
.step-item::after { content: ""; position: absolute; right: 0; top: 18px; bottom: 18px; width: 1px; background: var(--line); }
.step-item:last-child::after { display: none; }
.step-num {
  width: 24px; height: 24px; border-radius: 50%;
  display: flex; align-items: center; justify-content: center;
  font-size: 11px;
  border: 1px solid var(--line-strong);
  color: var(--muted);
  flex-shrink: 0;
}
.step-label { font-size: 12px; color: var(--muted); }
.step-item.active .step-num { background: var(--cyan); border-color: var(--cyan); color: #04121b; }
.step-item.active .step-label { color: var(--text); }
.step-item.done .step-num { border-color: var(--teal); color: var(--teal); }
.step-item.done .step-label { color: var(--text-2); }

.step-body { padding: 24px; min-height: 380px; }
.step-title { font-size: 17px; font-weight: 650; margin-bottom: 4px; }
.step-desc { font-size: 12px; color: var(--muted); margin: 0 0 20px; line-height: 1.8; }
.step-foot { display: flex; align-items: center; gap: 10px; padding: 16px 24px; border-top: 1px solid var(--line); }
.hint { font-size: 11px; margin-top: 6px; }

.mode-grid { display: grid; grid-template-columns: repeat(2, minmax(0, 1fr)); gap: 10px; margin-bottom: 18px; }
.mode-card {
  display: flex;
  align-items: flex-start;
  gap: 11px;
  padding: 13px;
  border-radius: 9px;
  border: 1px solid var(--line);
  background: var(--panel-3);
  color: inherit;
  text-align: left;
  cursor: pointer;
  position: relative;
  transition: border-color 0.16s ease, background 0.16s ease;
}
.mode-card:hover { border-color: var(--line-strong); }
.mode-card.active { border-color: rgba(56, 221, 248, 0.5); background: rgba(56, 221, 248, 0.06); }
.mode-icon { font-size: 19px; color: var(--muted); margin-top: 1px; }
.mode-card.active .mode-icon { color: var(--cyan); }
.mode-name { font-size: 13px; color: var(--text); }
.mode-desc { font-size: 10.5px; color: var(--muted); margin-top: 2px; line-height: 1.5; }
.mode-check { position: absolute; right: 10px; top: 10px; color: var(--cyan); font-size: 13px; }

.location-box { border: 1px solid var(--line); border-radius: 9px; padding: 14px; background: var(--panel-3); margin-bottom: 14px; }
.hint-box {
  display: flex; align-items: center; gap: 8px;
  font-size: 11.5px; color: var(--text-2);
  border: 1px dashed var(--line-strong);
  border-radius: 8px;
  padding: 10px 12px;
  margin-top: 10px;
}
.preview { border: 1px solid var(--line); border-radius: 10px; padding: 18px; background: var(--panel-3); }
.preview-title { font-size: 18px; font-weight: 650; }
.preview-meta { display: flex; gap: 8px; flex-wrap: wrap; margin: 10px 0 16px; }
.preview-grid { display: grid; grid-template-columns: repeat(2, minmax(0, 1fr)); gap: 10px; }
.preview-grid > div { display: flex; justify-content: space-between; gap: 10px; font-size: 12px; padding: 8px 10px; border-radius: 6px; background: rgba(148, 163, 184, 0.04); }
.preview-grid span { color: var(--muted); }

.summary-panel { position: sticky; top: 84px; }
.sum-course { display: flex; flex-direction: column; gap: 5px; padding: 14px; border-radius: 8px; border: 1px solid var(--line); border-left: 3px solid var(--c); background: var(--panel-3); }
.sum-course span { font-size: 11px; color: var(--muted); }
.sum-course strong { font-size: 14px; color: var(--text); }
.sum-meta { font-size: 11px; color: var(--text-2); }
.sum-list { display: flex; flex-direction: column; gap: 10px; }
.sum-list div { display: flex; justify-content: space-between; gap: 12px; font-size: 11.5px; }
.sum-list span { color: var(--muted); }
.sum-list strong { font-weight: 500; color: var(--text-2); text-align: right; }
.tip { display: flex; gap: 8px; font-size: 11px; color: var(--muted); line-height: 1.7; }
.passcode-block { display: flex; flex-direction: column; align-items: center; gap: 14px; padding: 8px 0 4px; font-size: 12px; }
.passcode-value { font-size: 42px; font-weight: 700; letter-spacing: 0.28em; color: var(--cyan); }

@media (max-width: 1100px) {
  .create-grid { grid-template-columns: 1fr; }
  .summary-panel { position: static; }
}
@media (max-width: 760px) {
  .steps { overflow-x: auto; }
  .step-item { min-width: 120px; padding: 12px; }
  .step-body { padding: 18px; }
  .mode-grid { grid-template-columns: 1fr; }
  .preview-grid { grid-template-columns: 1fr; }
}
</style>
