class Stack<T> {
  items: T[] = [];

  push(element: T): void {
    this.items.push(element);
  }

  pop(): T | any {
    return this.items.pop();
  }
}

// Example

const stack = new Stack<number>();

for (let i = 0; i < 100; i++) {
  stack.push(i);
}

console.log(stack.items);
// console.log(stack.pop());

