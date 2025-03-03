import { StrictMode } from "react";
import { createRoot } from "react-dom/client";
// import API from "./API";
import APIPost from "./APIPost"

createRoot(document.getElementById("root")!).render(
  <StrictMode>
    {/* <API /> */}
    <APIPost />
  </StrictMode>
);
