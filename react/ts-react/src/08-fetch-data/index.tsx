// [zod](https://zod.dev/)
// [React Query](https://tanstack.com/query/latest/docs/framework/react/overview)
// [Axios](https://axios-http.com/docs/intro)

import { useState, useEffect } from "react";

const url = "https://www.course-api.com/react-tours-project";

function Component() {
    const [isLoading, setIsLoading] = useState(false);
    const [isError, setIsError] = useState<string | null>(null);

    useEffect(() => {
        const fetchData = async () => {
            setIsLoading(true);
            try {
                const response = await fetch(url);
                if (!response.ok) {
                    throw new Error(`Failed to fetch tours...`);
                }
                const rawData = response.json();
                console.log(rawData);
            } catch (error) {
                const message =
                    error instanceof Error
                        ? error.message
                        : "There was an error...";
                setIsError(message);
            } finally {
                setIsLoading(false);
            }
        };
        fetchData();
    }, []);

    if (isLoading) {
        return <h3>Loading...</h3>;
    }

    if (isError) {
        return <h3>Error {isError}</h3>;
    }

    return (
        <div>
            <h2>React</h2>
            <h3>Fetch API</h3>
        </div>
    );
}

export default Component;
