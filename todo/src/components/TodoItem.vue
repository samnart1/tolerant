<template>
  <li class="todo-item">
    <div class="todo-content">
      <input
        type="checkbox"
        :checked="todo.completed"
        @change="todoStore.toggleTodo(todo.id)"
        class="todo-checkbox"
      />
      <span class="todo-text" :class="{ completed: todo.completed }">
        {{ todo.text }}
      </span>
    </div>
    <div class="todo-actions">
      <span class="todo-date">
        {{ formatDate(todo.createdAt) }}
      </span>
      <button
        class="delete-btn"
        @click="todoStore.deleteTodo(todo.id)"
        aria-label="Delete task"
      >
        x
      </button>
    </div>
  </li>
</template>

<script>
import { useTodoStore } from "../stores/todo";

export default {
    name: 'TodoItem',

    props: {
        todo: {
            type: Object,
            required: true
        }
    },

    methods: {
        formatDate(date) {
            if (!date) return '';

            const d = new Date(date);
            return d.toLocaleDateString('en-US', {
                month: 'short',
                day: 'numeric'
            });
        }
    }
}
</script>

<style scoped>
.todo-item {
    display: flex;
    justify-content: space-between;
    align-items: center;
    padding: 12px;
    margin-bottom: 10px;
    background-color: white;
    border-radius: 4px;
    box-shadow: 0 1px 3px rgba(0, 0, 0, 0.1);
    transition: all 0.2s ease;
}

.todo-item:hover {
    box-shadow: 0 2px 5px rgba(0, 0, blue, 0.15);
}

.todo-content {
    display: flex;
    align-items: center;
    flex: 1;
}

.todo-checkbox {
    margin-right: 10px;
    cursor: pointer;
}

.todo-text {
    word-break: break-word;
}

.todo-text.completed {
    text-decoration: line-through;
    color: #888;
}

.todo-actions {
    display: flex;
    align-items: center;
}

.todo-date {
    font-size: 12px;
    color: #888;
    margin-right: 10px;
}

.delete-btn {
    background-color: transparent;
    border: none;
    color: #ff6b6b;
    font-size: 20px;
    cursor: pointer;
    padding: 0 5px;
    line-height: 1;
    transition: color 0.2s;
}

.delete-btn:hover {
    color: #ff5252;
}
</style>
