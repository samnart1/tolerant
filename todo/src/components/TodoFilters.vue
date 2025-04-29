<template>
  <div class="filters-container">
    <div class="filters-buttons">
      <button
        v-for="filter in filters"
        :key="filter"
        class="filter-btn"
        :class="{ active: todoStore.filter === filter }"
        @click="todoStore.setFilter(filter)"
      >
        {{ formatFilterName(filter) }}
      </button>
    </div>

    <div class="todo-stats">
      <span>{{ todoStore.remainingTodos }} remaining</span>
      <span v-if="todoStore.completedTodos > 0">
        • {{ todoStore.completedTodos }} completed</span
      >
    </div>
  </div>
</template>

<script>
import { useTodoStore } from "../stores/todo";

export default {
    name: 'TodoFilters',

    data() {
        return {
            filters: ['all', 'active', 'completed']
        }
    },

    setup() {
        const todoStore = useTodoStore()
        return { todoStore }
    },

    methods: {
        formatFilterName(filter) {
            return filter.charAt(0).toUpperCase() + filter.slice(1)
        }
    }
}
</script>

<style scoped>
.filters-container {
    margin-top: 20px;
    padding-top: 15px;
    border-top: 1px solid #eee;
}

.filters-buttons {
    display: flex;
    justify-content: center;
    margin-bottom: 10px;
}

.filter-btn {
    background-color: transparent;
    border: 1px solid #ddd;
    border-radius: 4px;
    padding: 6px 12px;
    margin: 0 5px;
    cursor: pointer;
    font-size: 14px;
    transition: all 0.2s;
    color: #666;
}

.filter-btn:hover {
    border-color: #aaa;
}

.filter-btn.active {
    background-color: #42b983;
    color: white;
    border-color: #42b983;
}

.todo-stats {
    text-align: center;
    font-size: 14px;
    color: #666;
    margin-top: 10px;
}
</style>
