export interface IDatabaseResource<T, S> {
  create(data: S): Promise<T>
  update(data: Partial<S>, id: string): Promise<T | null>
  get(id: string): Promise<T | null>
  find(data: Partial<T>): Promise<T | null>
  findAll(data: Partial<T>): Promise<T[]>
  delete(id: string): Promise<T | null>
}
