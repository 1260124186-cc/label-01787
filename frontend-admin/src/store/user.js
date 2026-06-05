import { defineStore } from 'pinia'
import { ref } from 'vue'

// 安全地从 localStorage 读取 nickname（兼容旧数据）
function getSavedNickname() {
  const saved = localStorage.getItem('admin_nickname')
  if (!saved) return '管理员'
  try {
    // 尝试解码（新格式）
    const decoded = decodeURIComponent(saved)
    return decoded || '管理员'
  } catch {
    // 旧格式或已是明文
    return saved || '管理员'
  }
}

export const useUserStore = defineStore('user', () => {
  const token = ref(localStorage.getItem('admin_token') || '')
  const nickname = ref(getSavedNickname())

  function setLogin(data) {
    token.value = data.token
    // 如果 nickname 为空，使用 username 作为备用显示
    nickname.value = data.nickname || data.username || '管理员'
    localStorage.setItem('admin_token', data.token)
    // 对中文进行 URI 编码后存储，避免乱码
    localStorage.setItem('admin_nickname', encodeURIComponent(nickname.value))
  }

  function logout() {
    token.value = ''
    nickname.value = ''
    localStorage.removeItem('admin_token')
    localStorage.removeItem('admin_nickname')
  }

  const isLoggedIn = () => !!token.value

  return { token, nickname, setLogin, logout, isLoggedIn }
})
