<template>
  <div class="system-container">
    <div class="page-header">
      <h2>系统管理</h2>
    </div>
    
    <div class="system-content">
      <!-- 子路由内容将在这里显示 -->
      <router-view v-slot="{ Component }">
        <transition name="fade" mode="out-in">
          <component :is="Component" />
        </transition>
      </router-view>
    </div>
  </div>
</template>

<script setup>
// 系统管理主视图组件
</script>

<style scoped>
.system-container {
  padding: 20px;
  background-color: rgba(0, 12, 30, 0.5);
  border-radius: 5px;
  border: 1px solid #0e5986;
  height: 100%;
  display: flex;
  flex-direction: column;
  overflow: hidden;
}

.page-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 20px;
}

.page-header h2 {
  color: #00ffff;
  font-size: 20px;
  margin: 0;
}

.system-content {
  flex: 1;
  overflow: auto;
}

/* 过渡动画 */
.fade-enter-active,
.fade-leave-active {
  transition: opacity 0.3s ease;
}

.fade-enter-from,
.fade-leave-to {
  opacity: 0;
}
</style> 