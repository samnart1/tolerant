import { StrictMode } from "react";
import { createRoot } from "react-dom/client";
// import API from "./API";
// import APIPost from "./APIPost"
import Form from "./Form";

createRoot(document.getElementById("root")!).render(
  <StrictMode>
    <Form />
  </StrictMode>
);
