import { createRouter, createWebHistory } from 'vue-router'

const router = createRouter({
  history: createWebHistory(),
  routes: [
    { path: '/', redirect: '/login' },
    { path: '/login', component: () => import('../views/Login.vue') },
    {
      path: '/student', component: () => import('../views/student/Layout.vue'), children: [
        { path: '', component: () => import('../views/student/Dashboard.vue') },
        { path: 'rooms', component: () => import('../views/student/RoomList.vue') },
        { path: 'rooms/:roomId', component: () => import('../views/student/SeatSelect.vue') },
        { path: 'reservations', component: () => import('../views/student/MyReservations.vue') },
        { path: 'records', component: () => import('../views/student/MyRecords.vue') }
      ]
    },
    {
      path: '/admin', component: () => import('../views/admin/Layout.vue'), children: [
        { path: '', component: () => import('../views/admin/Dashboard.vue') },
        { path: 'rooms', component: () => import('../views/admin/RoomManage.vue') },
        { path: 'reservations', component: () => import('../views/admin/ReservationManage.vue') },
        { path: 'violations', component: () => import('../views/admin/ViolationManage.vue') },
        { path: 'ai', component: () => import('../views/admin/AiAnalysis.vue') }
      ]
    }
  ]
})

// 路由守卫：未登录跳回登录页
router.beforeEach((to, from, next) => {
  const token = localStorage.getItem('token')
  if (to.path !== '/login' && !token) {
    next('/login')
  } else if (to.path === '/login' && token) {
    const role = localStorage.getItem('role')
    next(role === 'ADMIN' ? '/admin' : '/student')
  } else {
    next()
  }
})

export default router