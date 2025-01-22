// 1. DECLARING ARRAYS

// Array of numbers
let oddNumbers: number[] = [1, 3, 5, 7, 9];

// Array of strings
let cities: string[] = ["Amsterdam", "London", "Istanbul"];

// Generic array type
let names: Array<string> = ["Hermione", "Ron", "Harry"];

// Mixed typed array
let mixedArray: (string | number)[] = ["Dumbledore", 3, "Severus Snape", 7];

// ACCESS ELEMENTS

// Access the first odd number
let firstNumber: number = oddNumbers[0];

// Access the second city
let secondCity: string = cities[1];

// MODIFYING ELEMENTS

cities[0] = "Edinburgh"; // Changes the first city from "Amsterdam" to "Edinburgh";

console.log(oddNumbers);
console.log(cities);
console.log(names);
console.log(mixedArray);
console.log(firstNumber);
console.log(secondCity);

/*
ARRAY METHODS
.push(): Adds elements to the end of the array
.pop(): Removes the last element from the array
.length(): Returns the number of elements in the array
.map(): maps over each member of an array
.forEach(): goes over each member of an array but doesn't create an other array with each element like .map() does
filter(): creates another array with all members that passes a test
.reduce(callback, initialValue): executes a "reducer" callback function on each element of the array passing the result of the reducer on the next element for calculation until it goes through all the elements.
.find(callback): returns the first element in the array that satisfies the provided testing function.
splice(start, deleteCount, ...items): changes the contents of an array by removing or replacing existing elements and or adding new elements.

*/

// TUPLE
let coordinates: [number, number] = [5, 6];
console.log(coordinates);