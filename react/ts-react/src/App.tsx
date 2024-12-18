import './App.css'
import Component from './01-return'

function App() {
  return (
    <>
      <Component >
        <h1>Hello World</h1>
      </Component>
      <Component name="Sam" id={1234} />
    </>
  )
}

export default App
