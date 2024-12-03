// one
export type Email = `${string}@${string}.${string}`

export interface DBEntity {
  id: string
  createdAT: Date
  updatedAt: Date
}

export interface DBUser extends DBEntity {
  name: string
  email: Email
  password: string
}

export interface DBChat extends DBEntity {
  ownerId: DBUser['id']
  name: string
}

export type MessageType = 'system' | 'user'

export interface DBMessage extends DBEntity {
  chatId: DBChat['id']
  type: MessageType
  message: string
}

export type DBCreateUser = Pick<DBUser, 'email' | 'password' | 'name'>
export type DBCreateChat = Pick<DBChat, 'ownerId' | 'name'>
export type DBCreateMessage = Pick<DBMessage, 'chatId' | 'type' | 'message'>
