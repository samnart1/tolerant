class NNode<T> {
  data: T;
  next: NNode<T> | null;

  constructor(data: T) {
    this.data = data;
    this.next = null;
  }
}

class LinkedList<T> {
  head: NNode<T> | null;

  constructor() {
    this.head = null;
  }

  append(data: T): void {
    const current = new NNode(data);

    if (!this.head) {
        this.head = current;
        return;
    }

    
  }
}
