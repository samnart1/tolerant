import axios from "axios";
import { useState } from "react";

interface DataType {
  title: string;
  body: string;
  userId: number;
}

axios.interceptors.request.use((request) => {
  console.log("Request ", request);
  return request;
});

axios.interceptors.response.use((response) => {
  console.log("Response: ", response);
  return response;
});

const APIPost = () => {
  const [data, setData] = useState<DataType[]>([]);

  const handleSubmit = (event: React.FormEvent) => {
    event.preventDefault();

    const newPost = {
      title: "foo",
      body: "bar",
      userId: 1,
    };
    axios
      .post("https://jsonplaceholder.typicode.com/posts", newPost)
      .then(function (response) {
        console.log("New Post Added: ", response.data);
        setData((prevData) => [response.data, ...data]);
      })
      .catch((error) => {
        console.error("Error adding new post: ", error);
      });
  };

  return (
    <div>
      <form action="submit" onSubmit={handleSubmit}>
        <button>Add Post</button>
      </form>
    </div>
  );
};

export default APIPost;
