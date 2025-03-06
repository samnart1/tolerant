import { Dispatch } from "redux";

export const fetchPostRequest = () => ({
  type: "FETCH_POSTS_REQUEST",
});

export const fetchPostSuccess = (posts: any) => ({
  type: "FETCH_POSTS_SUCCESS",
  payload: posts,
});

export const fetchPostFailure = (error: any) => ({
  type: "FETCH_POSTS_FAILURE",
  payload: error,
});

export const fetchPosts = async (dispatch: Dispatch) => {
  dispatch(fetchPostRequest());
  try {
    const response = await fetch("https://jsonplaceholder.typicode.com/posts");
    const data = await response.json();
    dispatch(fetchPostSuccess(data));
  } catch (error: any) {
    let errorMessage = "An unknown error occurred";

    if (error instanceof Error)
      errorMessage = error.message

    dispatch(fetchPostFailure(errorMessage));
  }
  
};
