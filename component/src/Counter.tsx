import { useState } from "react";

export default function Counter() {
  const [counters, setCounters] = useState([{ id: 1, value: 0 }]);
  const [step, setStep] = useState(1);

  const addCounter = () => {
    setCounters([
      ...counters,
      {
        id: counters.length + 1,
        value: 0,
      },
    ]);
  };

  const increment = (id: number) => {
    setCounters(
      counters.map((counter) =>
        counter.id === id
          ? { ...counter, value: counter.value + counter.id }
          : counter
      )
    );
  };

  return (
    <div>
      <button onClick={addCounter}>Add Counter</button>
      <ul>
        {counters.map((counter) => (
          <li key={counter.id}>
            {/* Counter 1 : 0 Button */}
            Counter {counter.id} : {counter.value}
            <button
              onClick={() => {
                increment(counter.id);
              }}
            >
              Increment
            </button>
          </li>
        ))}
      </ul>
    </div>
  );
}
