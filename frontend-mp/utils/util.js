/**
 * 格式化文件大小
 */
function formatSize(bytes) {
  if (!bytes) return '0 B'
  if (bytes < 1024) return bytes + ' B'
  if (bytes < 1024 * 1024) return (bytes / 1024).toFixed(1) + ' KB'
  return (bytes / (1024 * 1024)).toFixed(1) + ' MB'
}

/**
 * 格式化阅读时长
 */
function formatDuration(seconds) {
  if (!seconds || seconds < 60) return (seconds || 0) + '秒'
  const hours = Math.floor(seconds / 3600)
  const minutes = Math.floor((seconds % 3600) / 60)
  if (hours > 0) return `${hours}小时${minutes}分钟`
  return `${minutes}分钟`
}

/**
 * 格式化日期
 */
function formatDate(dateStr) {
  if (!dateStr) return ''
  const d = new Date(dateStr)
  const y = d.getFullYear()
  const m = String(d.getMonth() + 1).padStart(2, '0')
  const day = String(d.getDate()).padStart(2, '0')
  return `${y}-${m}-${day}`
}

/**
 * 获取主题样式
 */
function getThemeClass() {
  const app = getApp()
  return `theme-${app.globalData.theme || 'white'}`
}

/**
 * 主题配置
 */
const themes = {
  white: { name: '白色', bg: '#FFFFFF', text: '#333333', cardBg: '#FFFFFF' },
  green: { name: '护眼绿', bg: '#C7EDCC', text: '#2D4A2D', cardBg: '#D8F5DC' },
  dark: { name: '夜空黑', bg: '#1A1A2E', text: '#E0E0E0', cardBg: '#252542' }
}

module.exports = { formatSize, formatDuration, formatDate, getThemeClass, themes }
