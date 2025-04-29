<template>
  <div class="todo-list-wrapper">
    <ul class="todo-list" v-if="filteredTodos.length > 0">
      <TodoItem v-for="todo in filteredTodos" :key="todo.id" :todo="todo" />
    </ul>
    <div v-else class="empty-list">No tasks match the current filter!</div>
  </div>
</template>

<script>
import { computed } from "vue";
import { storeToRefs } from "pinia";
import { useTodoStore } from "../stores/todo";
import TodoItem from "./TodoItem.vue";

export default {
  name: "TodoList",

  components: {
    TodoItem,
  },

  setup() {
    const todoStore = useTodoStore();

    // Use storeRefs to maintain reactivity for store getters
    const { filteredTodos } = storeToRefs(todoStore);

    return {
      filteredTodos,
    };
  },
};
</script>

<style scoped>
.todo-list {
    list-style-type: none;
    padding: 0;
    margin: 0;
}

.empty-list {
    text-align: center;
    color: #888;
    padding: 20px 0;
    font-style: italic;
}
</style>
