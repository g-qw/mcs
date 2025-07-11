import { createApp } from 'vue'
import App from './App.vue'
import router from './router/router'
import { createPinia } from 'pinia'
import SvgIcon from '@/components/SvgIcon.vue'
import 'virtual:svg-icons-register'; // 引入插件注册svg图标
import Popup from "@/components/Popup.vue"

const app = createApp(App)
    .component('SvgIcon', SvgIcon)
    .component('Popup', Popup)
    .use(router)
    .use(new createPinia())
    .mount('#app');