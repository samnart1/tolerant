import { useState } from "react";
import "./App.css";

function App() {

  const [count, setCount] = useState(0);
  const [step, setStep] = useState(1);

  const incrementCount = () => {
    step ? setCount(count + 1 * step) : setCount(count + 1);
  }

  const decrementCount = () => {
    step ? setCount(count - 1 * step) : setCount(count - 1);
  }

  // const incrementStep = () => {
  //   setStep(step + 1);
  // }

  // const decrementStep = () => {
  //   setStep(step - 1);
  // }


  return (
    <div className="app-container">
      <h1>Counter Value: {count}</h1>
      <input 
        type="number"
        onChange={(e) => setStep(parseInt(e.target.value))}
        />
      <h1>Step: {step}</h1>
      <button onClick={incrementCount}>Increment</button>
      <button onClick={decrementCount}>Decrement</button>
      <button onClick={() => setCount(0)}>Reset</button>
      {/* <button onClick={incrementStep}>Increment Step</button>
      <button onClick={decrementStep}>Decrement Step</button> */}
    </div>
  );
} 

export default App;
