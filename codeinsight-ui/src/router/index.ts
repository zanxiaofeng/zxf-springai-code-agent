import { createRouter, createWebHistory } from 'vue-router'
import { isTokenExpired } from '@/utils/token'

const router = createRouter({
  history: createWebHistory(),
  routes: [
    {
      path: '/login',
      component: () => import('@/layouts/AuthLayout.vue'),
      children: [
        { path: '', name: 'Login', component: () => import('@/views/LoginView.vue') },
      ],
    },
    {
      path: '/',
      component: () => import('@/layouts/DefaultLayout.vue'),
      meta: { requiresAuth: true },
      children: [
        { path: '', redirect: '/projects' },
        { path: 'projects', name: 'Projects', component: () => import('@/views/ProjectListView.vue') },
        { path: 'projects/:id', name: 'ProjectDetail', component: () => import('@/views/ProjectDetailView.vue') },
        { path: 'chat', name: 'Chat', component: () => import('@/views/ChatView.vue') },
        { path: 'chat/:projectId', name: 'ChatWithProject', component: () => import('@/views/ChatView.vue') },
      ],
    },
  ],
})

router.beforeEach((to) => {
  const token = localStorage.getItem('token')
  if (to.matched.some((r) => r.meta.requiresAuth) && (!token || isTokenExpired(token))) {
    localStorage.removeItem('token')
    localStorage.removeItem('username')
    localStorage.removeItem('role')
    return { path: '/login', query: { redirect: to.fullPath } }
  }
})

export default router
