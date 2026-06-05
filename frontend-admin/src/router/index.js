import { createRouter, createWebHistory } from 'vue-router'

const routes = [
  {
    path: '/login',
    name: 'Login',
    component: () => import('@/views/Login.vue'),
    meta: { title: '登录' }
  },
  {
    path: '/',
    component: () => import('@/views/Layout.vue'),
    redirect: '/dashboard',
    children: [
      {
        path: 'dashboard',
        name: 'Dashboard',
        component: () => import('@/views/Dashboard.vue'),
        meta: { title: '仪表盘', permission: 'dashboard:view' }
      },
      {
        path: 'users',
        name: 'Users',
        component: () => import('@/views/UserList.vue'),
        meta: { title: '用户管理', permission: 'user:view' }
      },
      {
        path: 'books',
        name: 'Books',
        component: () => import('@/views/BookList.vue'),
        meta: { title: '书籍管理', permission: 'book:view' }
      },
      {
        path: 'logs',
        name: 'Logs',
        component: () => import('@/views/LogList.vue'),
        meta: { title: '操作日志', permission: 'log:view' }
      },
      {
        path: 'admins',
        name: 'Admins',
        component: () => import('@/views/AdminList.vue'),
        meta: { title: '管理员管理', permission: 'admin:view' }
      },
      {
        path: 'roles',
        name: 'Roles',
        component: () => import('@/views/RoleList.vue'),
        meta: { title: '角色权限', permission: 'role:view' }
      },
      {
        path: 'complaints',
        name: 'Complaints',
        component: () => import('@/views/ComplaintList.vue'),
        meta: { title: '版权申诉', permission: 'complaint:view' }
      },
      {
        path: 'compliance',
        name: 'Compliance',
        component: () => import('@/views/ComplianceReport.vue'),
        meta: { title: '合规审计', permission: 'audit:report' }
      }
    ]
  }
]

const router = createRouter({
  history: createWebHistory(),
  routes
})

router.beforeEach((to, from, next) => {
  document.title = `${to.meta.title || ''} - 小安的书店`
  const token = localStorage.getItem('admin_token')
  if (to.path !== '/login' && !token) {
    next('/login')
    return
  }

  if (to.meta.permission && token) {
    const roleCode = localStorage.getItem('admin_role_code')
    if (roleCode === 'SUPER_ADMIN') {
      next()
      return
    }
    try {
      const permissions = JSON.parse(localStorage.getItem('admin_permissions') || '[]')
      if (!permissions.includes(to.meta.permission)) {
        next('/dashboard')
        return
      }
    } catch {
      next('/dashboard')
      return
    }
  }

  next()
})

export default router
