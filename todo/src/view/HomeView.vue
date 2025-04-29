<template>
  <div class="todo-container">
    <h1>Vue To-Do List</h1>
    <TodoInput />
    <TodoList />
    <TodoFilters />
  </div>
</template>

<script>
import TodoInput from "../components/TodoInput.vue";
import TodoList from "../components/TodoList.vue";
import TodoFilters from "../components/TodoFilters.vue";

export default {
  name: "HomeView",
  components: {
    TodoInput,
    TodoList,
    TodoFilters,
  },
};
</script>

<style scoped>
.todo-container {
  background-color: #fff;
  border-radius: 8px;
  padding: 20px;
  box-shadow: 0 2px 10px rgba(0, 0, 0, 0.1);
}

h1 {
  text-align: center;
  color: #42b983;
  margin-top: 0;
}
</style>
