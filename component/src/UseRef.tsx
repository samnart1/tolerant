import { useRef } from "react";

export default function UseRef() {
  //   const [stateCount, setStateCount] = useState(0);
  //   const refCount = useRef(0);

  //   const incrementState = () => {
  //     setStateCount(stateCount + 1);
  //   };

  //   const incrementRef = () => {
  //     refCount.current += 1;
  //   };

  const inputRef = useRef<HTMLInputElement>(null);

  const focusInput = () => {
    // console.log(inputRef);
    if (inputRef.current) {
      inputRef.current.focus();
      inputRef.current.style.backgroundColor = "grey";
    }
  };

  return (
    <div>
      <h1>Learn React</h1>
      <input ref={inputRef} type="text" placeholder="Focus me" />
      <button onClick={focusInput}>Focus</button>
    </div>
  );
}
