import { useEffect, useState } from "react";

export default function Effect() {
  const [count, setCount] = useState(0);

  useEffect(() => {
    document.title = `Title: ${count}`;
  }, [count]);

  const incrementCount = () => {
    setCount(count + 1);
  };

  return (
    <div>
      <h1>USE EFFECT</h1>
      <button onClick={incrementCount}>Increment</button>
    </div>
  );
}
