import { createApp } from 'vue'
import { createPinia } from 'pinia'
import ElementPlus from 'element-plus'
import zhCn from 'element-plus/es/locale/lang/zh-cn'
import * as ElementPlusIconsVue from '@element-plus/icons-vue'
import 'element-plus/dist/index.css'
import 'element-plus/theme-chalk/dark/css-vars.css'
import 'virtual:uno.css'
import './styles/main.css'
import App from './App.vue'
import router from './router'

// 挂载前先应用持久化主题，避免浅色用户先闪一下深色
const savedTheme = (localStorage.getItem('cp_theme') as 'dark' | 'light') || 'dark'
document.documentElement.classList.toggle('dark', savedTheme === 'dark')
document.documentElement.setAttribute('data-theme', savedTheme)

const app = createApp(App)
for (const [key, component] of Object.entries(ElementPlusIconsVue)) {
  app.component(key, component as never)
}
app.use(createPinia())
app.use(router)
app.use(ElementPlus, { locale: zhCn })
app.mount('#app')
