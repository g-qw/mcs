<template>
  <div class="progress-bar-container">
    <div class="progress-bar">
      <div class="progress" :style="{ width: progress + '%' }">
        <span class="progress-text">{{ progress.toFixed(2) }}%</span>
      </div>
    </div>
  </div>
</template>

<script>
export default {
  props: {
    progress: {
      type: Number,
      default: 0.0
    }
  }
};
</script>

<style scoped>
.progress-bar-container {
  width: 100%;
  position: relative;
  display: flex;
  align-items: center;
  justify-content: center;
  margin: 0 5px;
}

.progress-bar {
  width: 100%;
  height: 10.5px;
  background-color: #5a6169;
  border-radius: 10px;
  overflow: hidden;
  position: relative;
}

.progress {
  height: 100%;
  background-color: rgb(50, 80, 50);
  transition: width 0.2s linear;
}

.progress-text {
  position: absolute;
  top: 50%;
  left: 50%;
  transform: translate(-50%, -50%);
  font-size: 0.5em;
  font-weight: bold;
  color: #8d96a0;
}
</style>