<script setup>
import { RouterView } from 'vue-router'
import NavBar from './components/NavBar.vue'
</script>

<template>
  <div class="app">
    <!-- <NavBar v-if="$route.meta.showNav !== false" /> -->
    <RouterView />
  </div>
</template>

<style scoped>
.app {
  padding: 0px;
}
</style>
