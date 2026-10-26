import { defineStore } from 'pinia'
import { ref } from 'vue'

export interface RollCallPrompt {
  sessionId: number
  recordId?: number
  courseName?: string
  teacherName?: string
  deadlineSeconds?: number
  drawnAt?: string
  studentName?: string
}

export interface QuizPrompt {
  quizId: number
  title?: string
}

export interface RacePrompt {
  raceId: number
  title?: string
  courseName?: string
  endTime?: string
}

export const useRealtimeStore = defineStore('realtime', () => {
  const rollCall = ref<RollCallPrompt | null>(null)
  const quiz = ref<QuizPrompt | null>(null)
  const race = ref<RacePrompt | null>(null)
  const taskTick = ref(0)

  function setRollCall(prompt: RollCallPrompt | null) {
    rollCall.value = prompt
  }
  function setQuiz(prompt: QuizPrompt | null) {
    quiz.value = prompt
  }
  function setRace(prompt: RacePrompt | null) {
    race.value = prompt
  }
  function bumpTask() {
    taskTick.value += 1
  }

  return { rollCall, quiz, race, taskTick, setRollCall, setQuiz, setRace, bumpTask }
})
