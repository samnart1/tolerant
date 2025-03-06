import { useDispatch, useSelector } from "react-redux";
import { RootState } from "./store/store";
import { useEffect } from "react";
import { fetchPosts } from "./store/actions/postAction";

const PostList = () => {
    const dispatch = useDispatch();
    const { post, loading, error } = useSelector(
        (state: RootState) => state.post
    );

    useEffect(() => {
        fetchPosts(dispatch);
    }, [dispatch]);

    if (loading) return <p>Loading...</p>;

    if (error) return <p>Error: {error}</p>;

    return (
        <div>
            <h2>Posts</h2>
            <ul>
                {post.map((post) => (
                    <li key={post.id}>
                        <strong>{post.title}</strong>
                        <p>{post.body}</p>
                    </li>
                ))}
            </ul>
        </div>
    );
};

export default PostList;
