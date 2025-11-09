<template>
  <div style="padding: 2rem;">
    <h1>Vue MicroClient – Produkte</h1>
    <p v-if="products.length === 0">Keine Produkte gefunden.</p>
    <ul v-else>
      <li v-for="p in products" :key="p.id">
        {{ p.name }} – {{ (p.priceCents / 100).toFixed(2) }} €
      </li>
    </ul>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import axios from 'axios'
const products = ref([])
onMounted(async () => {
  const { data } = await axios.get('/api/products')
  products.value = data
})
</script>
