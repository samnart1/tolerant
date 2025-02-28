import { StrictMode } from "react";
import { createRoot } from "react-dom/client";
// import './index.css'
// import App from './App.tsx'
// import Counter from './Counter.tsx'
import Effect from "./Effect";

createRoot(document.getElementById("root")!).render(
  <StrictMode>
    {/* <App /> */}
    <Effect />
  </StrictMode>
);
