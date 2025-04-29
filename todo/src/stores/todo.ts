import { defineStore } from "pinia";

interface Todo {
  id: number;
  text: string;
  completed: boolean;
  createdAt: Date;
}

interface TodoState {
  todos: Todo[];
  filter: "all" | "active" | "completed";
  nextId: number;
}

export const useTodoStore = defineStore("todo", {
  state: (): TodoState => ({
    todos: [],
    filter: "all",
    nextId: 1,
  }),

  getters: {
    filteredTodos: (state: TodoState): Todo[] => {
      switch (state.filter) {
        case "active":
          return state.todos.filter((todo) => !todo.completed);
        case "completed":
          return state.todos.filter((todo) => todo.completed);
        default:
          return state.todos;
      }
    },

    remainingTodos: (state: TodoState): number => {
      return state.todos.filter((todo) => !todo.completed).length;
    },

    completedTodos: (state: TodoState): number => {
      return state.todos.filter((todo) => todo.completed).length;
    },
  },

  actions: {
    addTodo(text: string): void {
      if (text.trim()) {
        this.todos.push({
          id: this.nextId++,
          text: text,
          completed: false,
          createdAt: new Date(),
        });
      }
    },

    toggleTodo(id: number): void {
      const todo = this.todos.find((todo) => todo.id == id);
      if (todo) {
        todo.completed = !todo.completed;
      }
    },

    deleteTodo(id: number): void {
      this.todos = this.todos.filter((todo) => todo.id !== id);
    },

    setFilter(filter: "all" | "active" | "completed"): void {
      this.filter = filter;
    },
  },
});
