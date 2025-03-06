import { Action } from "redux";

const initialState = {
  post: [] as any[],
  loading: false,
  error: null as string | null,
};

type PostState = typeof initialState;

interface PostAction extends Action {
  type: string;
  payload?: any;
}

const postReducer = (state = initialState, action: PostAction): PostState => {
  switch (action.type) {
    case "FETCH_POSTS_REQUEST":
      return {
        ...state,
        loading: true,
      };

    case "FETCH_POSTS_SUCCESS":
      return {
        ...state,
        loading: false,
        post: action.payload,
      };

    case "FETCH_POSTS_FAILURE":
      return {
        ...state,
        loading: false,
        error: action.payload,
      };

    default:
      return state;
  }
};

export default postReducer;
