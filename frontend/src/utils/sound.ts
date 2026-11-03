// 轻量 Web Audio 提示音：无需任何音频资源文件，仅在用户交互允许时发声
let ctx: AudioContext | null = null

function audioCtx(): AudioContext | null {
  try {
    const AC: typeof AudioContext = window.AudioContext || (window as any).webkitAudioContext
    if (!AC) return null
    if (!ctx) ctx = new AC()
    if (ctx.state === 'suspended') ctx.resume()
    return ctx
  } catch {
    return null
  }
}

// 首次交互时恢复 AudioContext，规避浏览器自动播放策略拦截
if (typeof window !== 'undefined' && !(window as any).__cpAudioHooked) {
  ;(window as any).__cpAudioHooked = true
  const resume = () => audioCtx()
  window.addEventListener('pointerdown', resume, { once: true })
  window.addEventListener('keydown', resume, { once: true })
}

function tone(freqs: number[], volume: number, duration: number) {
  const c = audioCtx()
  if (!c) return
  freqs.forEach((freq, i) => {
    const osc = c.createOscillator()
    const gain = c.createGain()
    osc.type = 'sine'
    osc.frequency.value = freq
    const t = c.currentTime + i * 0.12
    gain.gain.setValueAtTime(0.0001, t)
    gain.gain.exponentialRampToValueAtTime(volume, t + 0.02)
    gain.gain.exponentialRampToValueAtTime(0.0001, t + duration)
    osc.connect(gain)
    gain.connect(c.destination)
    osc.start(t)
    osc.stop(t + duration + 0.05)
  })
}

/** 学生签到成功的清脆上行和弦 */
export function playSuccessChime() {
  if (window.matchMedia('(prefers-reduced-motion: reduce)').matches) return
  tone([523.25, 659.25, 783.99], 0.22, 0.42)
}

/** 教师监控页收到新签到时的短促提示音 */
export function playNoticeTone() {
  tone([880], 0.12, 0.16)
}
