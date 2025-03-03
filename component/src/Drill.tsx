import { createContext, useContext, useEffect, useState } from "react";

type ThemeType = "light" | "dark";

const ThemeContext = createContext<ThemeType>("light");

export default function Drill() {
  //   const theme: string = "light";

  const [theme, setTheme] = useState<ThemeType>(() => {
    return window.matchMedia("(prefers-color-schema: dark)").matches
      ? "dark"
      : "light";
  });

  useEffect(() => {
    document.documentElement.classList.toggle("dark", theme === "dark");
  }, [theme]);

  const toggleTheme = () => {
    setTheme((prevTheme) => (prevTheme === "light" ? "dark" : "light"));
  };

  return (
    <ThemeContext.Provider value={theme}>
      <div>
        <ComponentA />
        <button onClick={toggleTheme}>Toggle Theme</button>
      </div>
    </ThemeContext.Provider>
  );
}

function ComponentA() {
  return <ComponentB />;
}

function ComponentB() {
  return <ComponentC />;
}

function ComponentC() {
  return <ThemedComponent />;
}

function ThemedComponent() {
  const theme = useContext(ThemeContext);
  return <h1>The current UI of the system is : {theme}</h1>;
}
