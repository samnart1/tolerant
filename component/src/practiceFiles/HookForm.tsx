import { useEffect } from "react";
import { useForm } from "react-hook-form";

const HookForm = () => {
  const {
    register,
    handleSubmit,
    watch,
    formState: { errors },
  } = useForm();

  const onSubmit = (data: any) => console.log(data);

  const watchedEmail = watch("email");
  const watchedName = watch("name");

  useEffect(() => {
    console.log(watchedName);
  }, [watchedName]);

  useEffect(() => {
    console.log(watchedEmail);
  }, [watchedEmail]);

  return (
    <div>
      <form onSubmit={handleSubmit(onSubmit)}>
        <label>
          Name:
          <input {...register("name", { required: true })} />
        </label>
        {errors.name && <span style={{color:"red"}}>Name is required!</span>}
          <br />

        <label htmlFor="email">
          Email:
          <input {...register("email", { required: true })} />
        </label>
        {errors.email && <span style={{color: "red"}}>Email is required!</span>}
          <br />

        <button type="submit">Submit Form</button>
      </form>
    </div>
  );
};

export default HookForm;
