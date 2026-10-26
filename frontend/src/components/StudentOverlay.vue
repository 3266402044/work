<script setup lang="ts">
import { computed, ref, watch } from 'vue'
import { ElMessage } from 'element-plus'
import { quizApi, raceApi, rollCallApi } from '@/api'
import { useAuthStore } from '@/stores/auth'
import { useRealtimeStore } from '@/stores/realtime'
import type { QuizVO } from '@/types'
import Countdown from './Countdown.vue'

const auth = useAuthStore()
const rt = useRealtimeStore()

const responding = ref(false)
const submitted = ref(false)
const quiz = ref<QuizVO | null>(null)
const answers = ref<number[]>([])
const expired = ref(false)
const buzzResult = ref<any>(null)
const buzzLoading = ref(false)

const rollTarget = computed(() => {
  if (!rt.rollCall?.drawnAt) return undefined
  const deadline = new Date(rt.rollCall.drawnAt).getTime() + (rt.rollCall.deadlineSeconds || 30) * 1000
  return new Date(deadline).toISOString()
})

const visible = computed(() => Boolean(rt.rollCall || quiz.value || rt.race))

watch(
  () => rt.quiz?.quizId,
  async (id) => {
    submitted.value = false
    answers.value = []
    quiz.value = null
    if (!id) return
    try {
      const detail = await quizApi.detail(id)
      if (detail.answered || detail.status !== 'RUNNING') {
        rt.setQuiz(null)
        return
      }
      quiz.value = detail
    } catch {
      rt.setQuiz(null)
    }
  },
  { immediate: true }
)

watch(
  () => rt.rollCall?.sessionId,
  () => {
    expired.value = false
  }
)

const respond = async () => {
  if (!rt.rollCall) return
  responding.value = true
  try {
    await rollCallApi.respond(rt.rollCall.sessionId)
    ElMessage.success('已确认，请在课堂上准备回答')
    rt.setRollCall(null)
  } finally {
    responding.value = false
  }
}

const toggleAnswer = (index: number) => {
  if (!quiz.value || submitted.value) return
  if (quiz.value.type === 'MULTIPLE') {
    answers.value = answers.value.includes(index)
      ? answers.value.filter((i) => i !== index)
      : [...answers.value, index]
  } else {
    answers.value = [index]
  }
}

const submitQuiz = async () => {
  if (!quiz.value) return
  if (!answers.value.length) {
    ElMessage.warning('请选择答案后再提交')
    return
  }
  await quizApi.submit(quiz.value.id, answers.value)
  submitted.value = true
  ElMessage.success('答案已提交')
}

const buzz = async () => {
  if (!rt.race) return
  buzzLoading.value = true
  try {
    buzzResult.value = await raceApi.buzz(rt.race.raceId)
  } finally {
    buzzLoading.value = false
  }
}

const dismiss = () => {
  if (rt.rollCall) rt.setRollCall(null)
  else if (quiz.value) rt.setQuiz(null)
  else if (rt.race) rt.setRace(null)
}

const optionLetter = (index: number) => String.fromCharCode(65 + index)

const isSelected = (index: number) => answers.value.includes(index)

const isCorrectOption = (index: number) => quiz.value?.correctAnswer?.includes(index) ?? false
</script>

<template>
  <transition name="overlay">
    <div v-if="visible" class="overlay">
      <div class="overlay-card" :class="{ wide: Boolean(quiz) }">
        <button class="close" type="button" aria-label="关闭" @click="dismiss">
          <el-icon><Close /></el-icon>
        </button>

        <!-- 随机点名 -->
        <template v-if="rt.rollCall">
          <div class="overlay-eyebrow"><i class="pulse-dot" /> 课堂随机点名</div>
          <div class="rc-course">{{ rt.rollCall.courseName || '当前课程' }}</div>
          <div class="rc-name">{{ rt.rollCall.studentName || auth.user?.realName }}</div>
          <div class="rc-title">你被点名了</div>
          <div class="rc-timer" :class="{ danger: expired }">
            <Countdown
              v-if="rollTarget"
              :target="rollTarget"
              size="lg"
              @end="expired = true"
            />
            <span v-else class="muted">等待教师确认</span>
            <span class="rc-timer-label">{{ expired ? '响应时间已结束' : '请在倒计时结束前确认响应' }}</span>
          </div>
          <el-button type="primary" size="large" class="rc-btn" :disabled="expired" :loading="responding" @click="respond">
            我在，准备回答
          </el-button>
          <div class="rc-foot">响应结果会实时显示在教师端 · 可稍后关闭此浮层</div>
        </template>

        <!-- 课堂小测 -->
        <template v-else-if="quiz">
          <div class="overlay-eyebrow"><i class="pulse-dot" /> 课堂小测 · {{ quiz.courseName }}</div>
          <div class="quiz-head">
            <div class="quiz-title">{{ quiz.title }}</div>
            <div class="quiz-timer">
              <Countdown :target="quiz.endTime" size="sm" @end="expired = true" />
            </div>
          </div>
          <div class="quiz-meta">
            <span class="chip cyan">{{ quiz.type === 'MULTIPLE' ? '多选题' : quiz.type === 'TRUE_FALSE' ? '判断题' : '单选题' }}</span>
            <span class="chip">{{ quiz.answerCount }} 人已提交</span>
            <span v-if="quiz.anonymous" class="chip">匿名</span>
          </div>

          <div v-if="!submitted" class="options">
            <button
              v-for="(option, index) in quiz.options"
              :key="index"
              type="button"
              class="option-item"
              :class="{ selected: isSelected(index) }"
              @click="toggleAnswer(index)"
            >
              <span class="option-letter">{{ optionLetter(index) }}</span>
              <span class="option-text">{{ option }}</span>
              <el-icon v-if="isSelected(index)" class="option-check"><Check /></el-icon>
            </button>
          </div>
          <div v-else class="quiz-submitted">
            <el-icon class="ok-icon"><CircleCheckFilled /></el-icon>
            <div class="ok-title">答案已提交</div>
            <div class="ok-desc">等待教师公布结果，可在课堂大屏查看实时分布</div>
          </div>

          <div v-if="!submitted" class="quiz-actions">
            <el-button type="primary" size="large" :disabled="expired || !answers.length" @click="submitQuiz">
              提交答案
            </el-button>
            <span class="quiz-hint">每个账号仅可提交一次</span>
          </div>
          <div v-else class="result-hint">
            <template v-if="quiz.showResult">
              <span :class="quiz.correct ? 'green' : 'coral'">
                {{ quiz.correct ? '回答正确' : '回答错误' }}
              </span>
              <span v-if="quiz.explanation" class="muted"> · {{ quiz.explanation }}</span>
            </template>
          </div>
        </template>

        <!-- 课堂抢答 -->
        <template v-else-if="rt.race">
          <div class="overlay-eyebrow"><i class="pulse-dot" /> 课堂抢答</div>
          <div class="rc-course">{{ rt.race.courseName || '当前课程' }}</div>
          <div class="rc-title">{{ rt.race.title }}</div>
          <div class="race-timer">
            <Countdown v-if="rt.race.endTime" :target="rt.race.endTime" size="lg" />
          </div>

          <template v-if="!buzzResult">
            <el-button type="primary" size="large" class="race-btn" :loading="buzzLoading" @click="buzz">
              立即抢答
            </el-button>
            <div class="rc-foot">服务端按收到请求的时间排序，结果公平可信</div>
          </template>
          <template v-else>
            <div class="race-rank">
              <span class="rank-num mono">{{ buzzResult.rank }}</span>
              <span class="rank-label">你的抢答名次</span>
            </div>
            <div class="top3">
              <div v-for="(item, index) in buzzResult.top3 || []" :key="item.studentNo" class="top3-item">
                <span class="top3-rank mono">{{ index + 1 }}</span>
                <span>{{ item.name }}</span>
                <span class="muted mono">{{ item.studentNo }}</span>
              </div>
            </div>
          </template>
        </template>
      </div>
    </div>
  </transition>
</template>

<style scoped>
.overlay {
  position: fixed;
  inset: 0;
  z-index: 2000;
  background: rgba(4, 8, 18, 0.78);
  backdrop-filter: blur(10px);
  display: flex;
  align-items: center;
  justify-content: center;
  padding: 20px;
}
.overlay-card {
  position: relative;
  width: 100%;
  max-width: 440px;
  background: linear-gradient(180deg, #16203a, #101728);
  border: 1px solid rgba(56, 221, 248, 0.28);
  border-radius: 18px;
  padding: 32px 28px;
  text-align: center;
  box-shadow: 0 30px 80px rgba(0, 0, 0, 0.5);
  animation: cardIn 0.28s cubic-bezier(0.2, 0.9, 0.3, 1);
}
.overlay-card.wide { max-width: 560px; text-align: left; }
@keyframes cardIn {
  from { opacity: 0; transform: translateY(14px) scale(0.98); }
  to { opacity: 1; transform: none; }
}
.close {
  position: absolute;
  top: 14px;
  right: 14px;
  width: 30px;
  height: 30px;
  border-radius: 8px;
  border: 1px solid transparent;
  background: transparent;
  color: var(--muted);
  cursor: pointer;
}
.close:hover { background: rgba(148, 163, 184, 0.1); color: var(--text); }

.overlay-eyebrow {
  display: inline-flex;
  align-items: center;
  gap: 8px;
  font-size: 11px;
  letter-spacing: 0.14em;
  color: var(--cyan);
  margin-bottom: 18px;
}
.rc-course { font-size: 12px; color: var(--muted); }
.rc-name {
  font-family: var(--mono);
  font-size: clamp(40px, 9vw, 66px);
  font-weight: 700;
  letter-spacing: 0.02em;
  line-height: 1.15;
  margin: 6px 0 2px;
  color: #f4fbff;
  text-shadow: 0 0 40px rgba(56, 221, 248, 0.3);
}
.rc-title { font-size: 18px; font-weight: 600; margin-bottom: 18px; }
.rc-timer { display: flex; flex-direction: column; align-items: center; gap: 4px; margin-bottom: 22px; }
.rc-timer-label { font-size: 11px; color: var(--muted); }
.rc-timer.danger .rc-timer-label { color: var(--coral); }
.rc-btn { width: 100%; height: 48px; font-size: 15px; }
.rc-foot { margin-top: 14px; font-size: 11px; color: var(--muted); }

.quiz-head { display: flex; align-items: flex-start; justify-content: space-between; gap: 14px; }
.quiz-title { font-size: 18px; font-weight: 620; line-height: 1.45; margin-top: 4px; }
.quiz-timer { flex-shrink: 0; }
.quiz-meta { display: flex; gap: 8px; margin: 12px 0 16px; flex-wrap: wrap; }
.options { display: flex; flex-direction: column; gap: 10px; }
.option-item {
  display: flex;
  align-items: center;
  gap: 12px;
  width: 100%;
  padding: 12px 14px;
  border-radius: 10px;
  border: 1px solid var(--line);
  background: rgba(148, 163, 184, 0.035);
  color: var(--text-2);
  cursor: pointer;
  text-align: left;
  font-size: 13.5px;
  transition: border-color 0.16s ease, background 0.16s ease;
}
.option-item:hover { border-color: var(--line-strong); }
.option-item.selected { border-color: rgba(56, 221, 248, 0.6); background: rgba(56, 221, 248, 0.09); color: var(--text); }
.option-letter {
  width: 26px; height: 26px; border-radius: 7px;
  display: inline-flex; align-items: center; justify-content: center;
  font-family: var(--mono); font-size: 12px;
  background: rgba(148, 163, 184, 0.1);
  flex-shrink: 0;
}
.option-item.selected .option-letter { background: var(--cyan); color: #04121b; }
.option-text { flex: 1; }
.option-check { color: var(--cyan); }
.quiz-actions { display: flex; align-items: center; gap: 14px; margin-top: 20px; }
.quiz-actions .el-button { min-width: 150px; height: 44px; }
.quiz-hint { font-size: 11px; color: var(--muted); }
.quiz-submitted { padding: 28px 0 6px; text-align: center; }
.ok-icon { font-size: 46px; color: var(--green); }
.ok-title { font-size: 18px; font-weight: 600; margin-top: 8px; }
.ok-desc { font-size: 12px; color: var(--muted); margin-top: 4px; }
.result-hint { margin-top: 16px; font-size: 12px; text-align: center; }

.race-timer { margin: 10px 0 22px; }
.race-btn { width: 100%; height: 62px; font-size: 20px; font-weight: 650; letter-spacing: 0.05em; }
.race-rank { display: flex; align-items: baseline; justify-content: center; gap: 8px; margin-bottom: 14px; }
.rank-num { font-size: 58px; font-weight: 700; color: var(--cyan); line-height: 1; }
.rank-label { font-size: 12px; color: var(--muted); }
.top3 { display: flex; flex-direction: column; gap: 6px; }
.top3-item {
  display: flex; align-items: center; gap: 10px;
  padding: 8px 12px; border-radius: 8px;
  background: rgba(148, 163, 184, 0.05);
  font-size: 12.5px;
}
.top3-rank { color: var(--amber); width: 18px; }

.overlay-enter-active, .overlay-leave-active { transition: opacity 0.2s ease; }
.overlay-enter-from, .overlay-leave-to { opacity: 0; }

@media (max-width: 480px) {
  .overlay-card { padding: 26px 18px; border-radius: 14px; }
  .quiz-title { font-size: 16px; }
}
</style>
