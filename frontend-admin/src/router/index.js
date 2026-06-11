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
        path: 'books/:id',
        name: 'BookDetail',
        component: () => import('@/views/BookDetail.vue'),
        meta: { title: '书籍详情', permission: 'book:view' }
      },
      {
        path: 'logs',
        name: 'Logs',
        component: () => import('@/views/LogList.vue'),
        meta: { title: '操作日志', permission: 'log:view' }
      },
      {
        path: 'download-logs',
        name: 'DownloadLogs',
        component: () => import('@/views/DownloadLogList.vue'),
        meta: { title: '文件下载审计', permission: 'download_log:view' }
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
      },
      {
        path: 'notifications',
        name: 'Notifications',
        component: () => import('@/views/AnnouncementList.vue'),
        meta: { title: '公告管理', permission: 'notification:view' }
      },
      {
        path: 'notification-templates',
        name: 'NotificationTemplates',
        component: () => import('@/views/NotificationTemplateList.vue'),
        meta: { title: '通知模板', permission: 'template:view' }
      },
      {
        path: 'backups',
        name: 'Backups',
        component: () => import('@/views/BackupList.vue'),
        meta: { title: '备份管理', permission: 'backup:view' }
      },
      {
        path: 'storage',
        name: 'Storage',
        component: () => import('@/views/StorageStats.vue'),
        meta: { title: '存储统计', permission: 'storage:view' }
      },
      {
        path: 'membership-plans',
        name: 'MembershipPlans',
        component: () => import('@/views/MembershipPlanList.vue'),
        meta: { title: '套餐配置', permission: 'plan:view' }
      },
      {
        path: 'orders',
        name: 'Orders',
        component: () => import('@/views/OrderList.vue'),
        meta: { title: '订单列表', permission: 'order:view' }
      },
      {
        path: 'members',
        name: 'Members',
        component: () => import('@/views/MemberUserList.vue'),
        meta: { title: '会员管理', permission: 'member:view' }
      },
      {
        path: 'convert-tasks',
        name: 'ConvertTasks',
        component: () => import('@/views/ConvertTaskList.vue'),
        meta: { title: '转图任务', permission: 'convert:view' }
      },
      {
        path: 'ai-summaries',
        name: 'AiSummaries',
        component: () => import('@/views/AiSummaryList.vue'),
        meta: { title: 'AI摘要', permission: 'ai_summary:view' }
      },
      {
        path: 'plaza/excerpts',
        name: 'PlazaExcerpts',
        component: () => import('@/views/PlazaExcerptList.vue'),
        meta: { title: '书摘管理', permission: 'plaza:view' }
      },
      {
        path: 'plaza/reports',
        name: 'PlazaReports',
        component: () => import('@/views/PlazaReportList.vue'),
        meta: { title: '举报管理', permission: 'plaza:report_view' }
      },
      {
        path: 'reading-plans',
        name: 'ReadingPlanStats',
        component: () => import('@/views/ReadingPlanStats.vue'),
        meta: { title: '阅读计划统计', permission: 'reading_plan:view' }
      },
      {
        path: 'search-index',
        name: 'SearchIndex',
        component: () => import('@/views/IndexList.vue'),
        meta: { title: '搜索索引管理', permission: 'search:view' }
      },
      {
        path: 'system-config',
        name: 'SystemConfig',
        component: () => import('@/views/SystemConfig.vue'),
        meta: { title: '系统配置', permission: 'config:view' }
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
