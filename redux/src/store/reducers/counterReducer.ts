const initialState = { count: 1900 };

const counterReducer = (state = initialState, action: { type: string }) => {
  switch (action.type) {
    case "INCREMENT":
      return { count: state.count + 1 };

    case "DECREMENT":
      return { count: state.count - 1 };

    default:
      return state;
  }
};

export default counterReducer;
