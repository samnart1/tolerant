<template>
  <div class="input-group">
    <input
      type="text"
      v-model="newTodo"
      @keyup.enter="addTodo"
      placeholder="What needs to be done?"
      class="todo-input"
    />
    <button class="add-btn">Add</button>
  </div>
</template>

<script>
import { useTodoStore } from '../stores/todo'

export default {
    name: 'TodoInput',

    data() {
        return {
            newTodo: ''
        }
    },

    setup() {
        const todoStore = useTodoStore()
        return { todoStore }
    },

    methods: {
        addTodo() {
            this.todoStore.addTodo(this.newTodo)
            this.newTodo = ''
        }
    }
}
</script>

<style scoped>
.input-group {
  display: flex;
  margin-bottom: 20px;
}

.todo-input {
  flex: 1;
  padding: 10px;
  border: 1px solid #ddd;
  border-radius: 4px 0 0 4px;
  font-size: 16px;
}

.add-btn {
  background-color: #42b983;
  color: white;
  border: none;
  padding: 10px 15px;
  border-radius: 0 4px 4px 0;
  cursor: pointer;
  font-size: 16px;
  transition: background-color 0.3s;
}

.add-btn:hover {
  background-color: #3aa876;
}
</style>
