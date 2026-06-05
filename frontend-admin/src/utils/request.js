import axios from 'axios'
import { ElMessage, ElMessageBox } from 'element-plus'
import router from '@/router'
import { requestConfirmToken } from '@/api/admin'

const request = axios.create({
  baseURL: '/api',
  timeout: 30000,
  headers: { 'Content-Type': 'application/json' }
})

request.interceptors.request.use(
  config => {
    const token = localStorage.getItem('admin_token')
    if (token) {
      config.headers['Authorization'] = `Bearer ${token}`
    }
    return config
  },
  error => Promise.reject(error)
)

async function handleSensitiveOperation(error) {
  const res = error.response?.data
  if (res && res.code === 403 && res.message && res.message.includes('二次确认')) {
    try {
      await ElMessageBox.confirm(
        '该操作为敏感操作，需要二次确认。是否继续？',
        '二次确认',
        { confirmButtonText: '确认执行', cancelButtonText: '取消', type: 'warning' }
      )
      const operation = extractOperation(error.config)
      const tokenRes = await requestConfirmToken(operation)
      const confirmToken = tokenRes.data?.confirmToken
      if (confirmToken) {
        error.config.headers['X-Confirm-Token'] = confirmToken
        return axios(error.config)
      }
    } catch {
      return Promise.reject(new Error('用户取消操作'))
    }
  }
  return null
}

function extractOperation(config) {
  const op = config.headers?.['X-Sensitive-Operation']
  if (op) return op
  const method = config.method?.toUpperCase()
  const url = config.url || ''
  return `${method}:${url}`
}

request.interceptors.response.use(
  response => {
    const res = response.data
    if (res.code !== 200) {
      ElMessage.error(res.message || '请求失败')
      if (res.code === 401) {
        localStorage.removeItem('admin_token')
        localStorage.removeItem('admin_role_code')
        localStorage.removeItem('admin_permissions')
        router.push('/login')
      }
      return Promise.reject(new Error(res.message))
    }
    return res
  },
  async error => {
    const retryResult = await handleSensitiveOperation(error)
    if (retryResult) {
      const res = retryResult.data
      if (res.code !== 200) {
        ElMessage.error(res.message || '请求失败')
        return Promise.reject(new Error(res.message))
      }
      return res
    }

    const msg = error.response?.data?.message || error.message || '网络错误'
    if (error.response?.data?.code === 403) {
      ElMessage.error(msg)
    } else {
      ElMessage.error(msg)
    }
    if (error.response?.status === 401 || error.response?.data?.code === 401) {
      localStorage.removeItem('admin_token')
      localStorage.removeItem('admin_role_code')
      localStorage.removeItem('admin_permissions')
      router.push('/login')
    }
    return Promise.reject(error)
  }
)

export default request
