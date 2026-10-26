import { defineConfig, presetUno } from 'unocss'

export default defineConfig({
  presets: [presetUno()],
  theme: {
    colors: {
      ink: '#0B1020',
      panel: '#151C2D',
      panel2: '#1A2236',
      line: 'rgba(148,163,184,.14)',
      cyan: '#38DDF8',
      teal: '#2DD4BF',
      ok: '#47D16C',
      warn: '#F5B942',
      coral: '#FF6B6B',
      muted: '#8B9AB5'
    }
  },
  shortcuts: {
    'flex-center': 'flex items-center justify-center',
    'flex-between': 'flex items-center justify-between',
    'text-muted': 'text-[#8B9AB5]',
    'text-num': 'font-mono tabular-nums tracking-tight'
  }
})
