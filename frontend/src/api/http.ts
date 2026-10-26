import axios, { type AxiosRequestConfig } from 'axios'
import { ElMessage } from 'element-plus'

export interface ApiResponse<T> {
  code: number
  message: string
  data: T
}

const instance = axios.create({
  baseURL: '/api',
  timeout: 20000
})

instance.interceptors.request.use((config) => {
  const token = localStorage.getItem('cp_token')
  if (token) {
    config.headers.Authorization = `Bearer ${token}`
  }
  return config
})

instance.interceptors.response.use(
  (response) => response,
  (error) => {
    const status = error?.response?.status
    const message = error?.response?.data?.message || (status === 401
      ? '登录状态已失效，请重新登录'
      : error?.code === 'ECONNABORTED'
        ? '请求超时，请稍后重试'
        : '网络异常，请检查后端服务是否已启动')
    if (status === 401) {
      localStorage.removeItem('cp_token')
      localStorage.removeItem('cp_user')
      if (!location.pathname.startsWith('/login')) {
        location.href = `/login?redirect=${encodeURIComponent(location.pathname + location.search)}`
      }
    }
    ElMessage.error(message)
    return Promise.reject(new Error(message))
  }
)

export async function request<T>(config: AxiosRequestConfig): Promise<T> {
  const response = await instance.request<ApiResponse<T>>(config)
  const body = response.data
  if (body.code !== 200) {
    ElMessage.error(body.message || '操作失败')
    throw new Error(body.message || '操作失败')
  }
  return body.data
}

export async function download(path: string, filename: string) {
  const response = await instance.get(path, { responseType: 'blob' })
  const url = URL.createObjectURL(response.data as Blob)
  const link = document.createElement('a')
  link.href = url
  link.download = filename
  document.body.appendChild(link)
  link.click()
  document.body.removeChild(link)
  URL.revokeObjectURL(url)
}

export default instance
