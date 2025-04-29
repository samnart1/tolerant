<template>
  <div class="app-container">
    <img src="./assets/logo.png" alt="todo logo" class="logo">
    <HomeView />
  </div>
</template>

<script>
import HomeView from './view/HomeView.vue'

export default {
  name: 'App',
  components: {
    HomeView
  }
}
</script>

<style>
body {
  font-family: 'Arial', sans-serif;
  line-height: 1.6;
  color: #333;
  margin: 0;
  padding: 0;
  background-color: #f7f7f7;
}

.app-container {
  max-width: 600px;
  margin: 0 auto;
  padding: 20px;
}

.logo {
  display: block;
  width: 80px;
  margin: 0 auto 20px;
}
</style>