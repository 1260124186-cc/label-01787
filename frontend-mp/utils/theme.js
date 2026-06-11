// 统一主题常量配置 - 三端共享
const THEMES = {
  white: {
    key: 'white',
    name: '白色',
    bgColor: '#FFFFFF',
    textColor: '#333333',
    cardBg: '#FFFFFF',
    borderColor: '#EEEEEE',
    placeholderColor: '#CCCCCC'
  },
  green: {
    key: 'green',
    name: '护眼绿',
    bgColor: '#EBF9ED',
    textColor: '#2D4A2D',
    cardBg: '#F4FBF5',
    borderColor: '#C7E6CC',
    placeholderColor: '#A8D5AF'
  },
  dark: {
    key: 'dark',
    name: '夜空黑',
    bgColor: '#1A1A2E',
    textColor: '#E0E0E0',
    cardBg: '#252542',
    borderColor: '#3A3A5C',
    placeholderColor: '#5A5A7C'
  },
  sepia: {
    key: 'sepia',
    name: '羊皮纸',
    bgColor: '#F5F0EB',
    textColor: '#5D4037',
    cardBg: '#FAF6F1',
    borderColor: '#E8DCC8',
    placeholderColor: '#C9B89E'
  }
}

const THEME_LIST = [
  THEMES.white,
  THEMES.green,
  THEMES.sepia,
  THEMES.dark
]

const SYSTEM_DARK_MAP = {
  dark: 'dark',
  light: 'white'
}

function getSystemTheme() {
  try {
    const sysInfo = wx.getSystemInfoSync()
    return SYSTEM_DARK_MAP[sysInfo.theme] || 'white'
  } catch (e) {
    return 'white'
  }
}

function isValidTheme(theme) {
  return !!THEMES[theme]
}

function getThemeConfig(theme) {
  return THEMES[theme] || THEMES.white
}

module.exports = {
  THEMES,
  THEME_LIST,
  getSystemTheme,
  isValidTheme,
  getThemeConfig
}
