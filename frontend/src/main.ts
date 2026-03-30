import { createApp } from 'vue'
import { createPinia } from 'pinia'
import App from './App.vue'
import router from './router'
import { useAuthStore } from './stores/auth'
import './style.css'

const app = createApp(App)
const pinia = createPinia()

app.use(pinia)

const bootstrap = async () => {
  const authStore = useAuthStore(pinia)
  if (authStore.token) {
    try {
      await authStore.fetchProfile()
    } catch {
      // fetchProfile already clears invalid local session
    }
  }

  app.use(router)
  app.mount('#app')
}

void bootstrap()
