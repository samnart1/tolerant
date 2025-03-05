import { useSelector } from "react-redux";
import { RootState } from "./store/store";

const Display = () => {
  const count = useSelector((state: RootState) => state.counter.count);
  return <div>{count}</div>;
};

export default Display;
