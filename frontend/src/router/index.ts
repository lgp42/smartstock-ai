import { createRouter, createWebHistory } from 'vue-router'

const router = createRouter({
  history: createWebHistory(),
  routes: [
    {
      path: '/login',
      name: 'Login',
      component: () => import('../views/LoginView.vue')
    },
    {
      path: '/register',
      name: 'Register',
      component: () => import('../views/RegisterView.vue')
    },
    {
      path: '/',
      name: 'Layout',
      component: () => import('../components/Layout.vue'),
      redirect: '/dashboard',
      children: [
        {
          path: 'dashboard',
          name: 'Dashboard',
          component: () => import('../views/DashboardView.vue'),
        },
        {
          path: 'terminal/:code?',
          name: 'Terminal',
          component: () => import('../views/TerminalView.vue'),
        },
        {
          path: 'copilot/:code?',
          name: 'Copilot',
          component: () => import('../views/CopilotView.vue'),
        },
        {
          path: 'screener',
          name: 'Screener',
          component: () => import('../views/ScreenerView.vue'),
        },
        {
          path: 'news',
          name: 'News',
          component: () => import('../views/NewsView.vue'),
        },
      ]
    }
  ]
})

const publicRoutes = ['Login', 'Register']

router.beforeEach((to, _from, next) => {
  const token = localStorage.getItem('token')
  if (!publicRoutes.includes(to.name as string) && !token) {
    next({ name: 'Login' })
  } else if (publicRoutes.includes(to.name as string) && token) {
    next({ name: 'Dashboard' })
  } else {
    next()
  }
})

export default router
