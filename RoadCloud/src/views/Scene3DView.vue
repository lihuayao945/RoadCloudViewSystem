<template>
  <div class="scene-3d-container">
    <SceneViewer />
  </div>
</template>

<script setup>
import SceneViewer from '../components/SceneViewer.vue'
</script>

<style scoped>
.scene-3d-container {
  width: 100vw;
  height: 100vh;
  position: fixed;
  top: 0;
  left: 0;
  right: 0;
  bottom: 0;
  background-color: #000;
  margin: 0;
  padding: 0;
  overflow: hidden;
  z-index: 9999;
}
</style> 