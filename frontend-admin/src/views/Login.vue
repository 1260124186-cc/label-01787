<template>
  <div class="login-wrapper">
    <div class="login-card">
      <div class="login-header">
        <div class="logo-icon">📚</div>
        <h1>小安的书店</h1>
        <p>管理后台</p>
      </div>
      <el-form ref="formRef" :model="form" :rules="rules" @keyup.enter="handleLogin">
        <el-form-item prop="username">
          <el-input v-model="form.username" placeholder="请输入用户名" size="large" prefix-icon="User" />
        </el-form-item>
        <el-form-item prop="password">
          <el-input v-model="form.password" type="password" placeholder="请输入密码" size="large"
            prefix-icon="Lock" show-password />
        </el-form-item>
        <el-form-item>
          <el-button type="primary" size="large" :loading="loading" class="login-btn" @click="handleLogin">
            登 录
          </el-button>
        </el-form-item>
      </el-form>
    </div>
  </div>
</template>

<script setup>
import { ref, reactive } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { login } from '@/api/admin'
import { useUserStore } from '@/store/user'

const router = useRouter()
const userStore = useUserStore()
const formRef = ref(null)
const loading = ref(false)

const form = reactive({ username: '', password: '' })
const rules = {
  username: [{ required: true, message: '请输入用户名', trigger: 'blur' }],
  password: [{ required: true, message: '请输入密码', trigger: 'blur' }]
}

const handleLogin = async () => {
  const valid = await formRef.value.validate().catch(() => false)
  if (!valid) return
  loading.value = true
  try {
    const loginData = {
      username: form.username,
      password: window.btoa(unescape(encodeURIComponent(form.password)))
    }
    const res = await login(loginData)
    userStore.setLogin(res.data)
    ElMessage.success('登录成功')
    router.push('/dashboard')
  } catch (e) {
    // 错误已在拦截器处理
  } finally {
    loading.value = false
  }
}
</script>

<style lang="scss" scoped>
.login-wrapper {
  min-height: 100vh;
  display: flex;
  align-items: center;
  justify-content: center;
  background: linear-gradient(135deg, #f5f0eb 0%, #e8ddd3 50%, #d4a574 100%);
}

.login-card {
  width: 400px;
  padding: 48px 40px;
  background: #fff;
  border-radius: 16px;
  box-shadow: 0 8px 40px rgba(107, 66, 38, 0.15);
}

.login-header {
  text-align: center;
  margin-bottom: 36px;

  .logo-icon {
    font-size: 48px;
    margin-bottom: 12px;
  }

  h1 {
    font-size: 24px;
    color: #6b4226;
    font-weight: 700;
    margin-bottom: 6px;
  }

  p {
    font-size: 14px;
    color: #999;
  }
}

.login-btn {
  width: 100%;
  height: 44px;
  font-size: 16px;
  border-radius: 8px;
  background: #6b4226;
  border-color: #6b4226;

  &:hover,
  &:focus {
    background: #52321d !important;
    border-color: #52321d !important;
    opacity: 1 !important;
  }

  &:active {
    background: #3d2516 !important;
    border-color: #3d2516 !important;
  }
}
</style>
