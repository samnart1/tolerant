import { useNavigate } from "react-router-dom";

const Home = () => {
  const navigate = useNavigate();

  const handleLogin = () => {
    navigate("/dashboard");
  };

  return (
    <div>
      <h1>HOME</h1>

      <button onClick={handleLogin}>Login</button>
    </div>
  );
};

export default Home;
