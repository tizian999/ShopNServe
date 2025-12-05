import { createApp } from 'vue'
import Shop from './components/Shop.vue'

import 'vuetify/styles'
import { createVuetify } from 'vuetify'
import * as components from 'vuetify/components'
import * as directives from 'vuetify/directives'
import '@mdi/font/css/materialdesignicons.css'

// Create Vuetify instance
const vuetify = createVuetify({
    components,
    directives,
})

// Create and mount the app, making sure to use Vuetify
const app = createApp(Shop)
app.use(vuetify)
app.mount('#app')
