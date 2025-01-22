class Queue<T> {
  private items: T[] = [];

  enqueue(element: T): void {
    this.items.push(element);
  }

  dequeue(): T | undefined {
    return this.items.shift();
  }

  front(): T | undefined {
    return this.items[0];
  }

  isEmpty(): boolean {
    return this.items.length === 0;
  }

  size(): number {
    return this.items.length;
  }
}

// Examples
const queue = new Queue<string>();

queue.enqueue("Hedwig");
queue.enqueue("Crookshanks");
queue.enqueue("Scabbers");

console.log(queue.front());
console.log(queue.dequeue());
console.log(queue.size());
