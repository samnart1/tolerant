import { useState, type PropsWithChildren } from 'react'

// type ComponentProps = { name?: string; id?: number; children?: React.ReactNode }

// type ComponentProps = PropsWithChildren<{
//   name?: string
//   id?: number
// }>

// function Component({ name, id, children }: ComponentProps) {
//   // return (
//   //   <div>
//   //     <h1>Name : {name}</h1>
//   //     <h1>ID : {id}</h1>
//   //     {children}
//   //   </div>
//   // )
// }

type navLinks = {
  id: number
  url: string
  text: string
}

const navLinks = [
  {
    id: 1,
    url: 'some url',
    text: 'some text',
  },
  {
    id: 2,
    url: 'some url',
    text: 'some text',
  },
  {
    id: 3,
    url: 'some url',
    text: 'some text',
  },
]

function Component() {
  const [text, setText] = useState('shake and bake')

  const [links, setLinks] = useState<navLinks[]>(navLinks)

  return (
    <>
      <div>React State</div>
      <button
        onClick={() => {
          setText(text)
          setLinks([...links, { id: 4, url: 'haha', text: 'wohoa' }])
        }}
      >
        Click Me
      </button>
    </>
  )
}

export default Component
