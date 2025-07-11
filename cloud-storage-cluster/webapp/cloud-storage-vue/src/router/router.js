import { createRouter, createWebHistory } from 'vue-router'
import HomeView from '@/views/HomeView.vue'

const routes = [
  {
    path: '/',
    name: 'home',
    component: HomeView
  },
  {
    path: '/auth',
    name: 'auth',
    component: () => import('@/views/AuthView.vue'),
    children: [
      {
        path: '/auth/signup',
        name:'signup',
        component: () => import('@/views/SignupView.vue')
      },
      {
        path: '/auth/login',
        name: 'login',
        component: () => import('@/views/LoginView.vue')
      },
      {
        path: '/auth/reset',
        name: 'reset',
        component: () => import('@/views/ResetView.vue')
      }
    ]
  },
  {
    path: '/main',
    name:'main',
    component: () => import('@/views/FileExplorer.vue')
  }
]

const router = createRouter({
  history: createWebHistory('/'),
  routes
})

export default router
