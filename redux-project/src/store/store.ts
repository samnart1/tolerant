import { configureStore } from "@reduxjs/toolkit";
import postReducer from "./reducers/postReducer";

const initialState = {
  post: {
    post: [],
    loading: false,
    error: null,
  },
};

export const store = configureStore({
  reducer: {
    post: postReducer,
  },
  preloadedState: initialState
});

export type RootState = ReturnType<typeof store.getState>;
export type AppDispatch = typeof store.dispatch;

export default store;
