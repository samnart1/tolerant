import { Outlet } from "react-router-dom";

const About = () => {
    return (<>
        <h1>ABOUT</h1>
        <div>
            <Outlet />
        </div>
    </>
        
    )
}

export default About;