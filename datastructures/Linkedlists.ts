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

  append(data: T) {
    const newNode = new NNode(data);

    if (!this.head) {
      this.head = newNode;
      return;
    }
    
    let current = this.head;

    while (current.next) {
      current = current.next;
    }

    current.next = newNode;
  }

  printList() {
    if (!this.head) {
      console.log("THe list is empty.");
      return;
    }

    let current: NNode<T> | null = this.head;
    const elements: T[] = [];

    while (current) {
      elements.push(current.data);
      current = current.next;
    }

    console.log(elements.join(" -> "));
  }
}


const list = new LinkedList<number>();
list.append(10);
list.append(20);
list.append(30);
list.append(40);
list.append(50);
list.printList();
