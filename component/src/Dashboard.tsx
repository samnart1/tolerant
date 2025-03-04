import { useNavigate } from "react-router-dom";

const Dashboard = () => {
  const navigate = useNavigate();
  const handleLogout = () => {
    navigate("/");
  };
  return (
    <>
      <h1>Welcome To The DASHBOARD</h1>
      <button onClick={handleLogout}>Logout</button>
    </>
  );
};

export default Dashboard;
