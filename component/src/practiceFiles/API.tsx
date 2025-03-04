import axios from "axios";

import { useEffect, useState } from "react";

interface Todo {
  title: string;
  userId: number;
  id: number;
  completed: boolean;
}

const API = () => {
  const [data, setData] = useState<Todo | null>(null);
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState();

  useEffect(() => {
    setLoading(true);

    axios
      .all([axios.get("https://jsonplaceholder.typicode.com/todos/1")])
      .then(function (response) {
        console.log(response);
        // setData(response.data);
        setLoading(false);
      })
      .catch(function (error) {
        console.log(error);
        setError(error);
        setLoading(false);
      })
      .finally(function () {});

    // fetch("https://jsonplaceholder.typicode.com/todos/1")
    //   .then((response) => response.json())
    //   .then((json) => {
    //     // console.log(json);
    //     setData(json);
    //     setLoading(false);
    //   })
    //   .catch((error) => {
    //     console.log(`There was an error: `, error);
    //     setError(error);
    //     setLoading(false);
    //   });
  }, []);

  if (loading) {
    return <p>Loading...</p>;
  }

  if (error) {
    return <p>There was an error!!!</p>;
  }

  return (
    <div>
      <h2>API's</h2>
      <p>{data?.title ?? "Loading..."}</p>
      <p>{data?.userId ? "User ID: " + data.userId : "Loading..."}</p>
      <p>{data?.id ? "ID: " + data.id : "Loading..."}</p>
      <p>{data?.completed ? "Completed" : "Not completed!"}</p>
    </div>
  );
};

export default API;
