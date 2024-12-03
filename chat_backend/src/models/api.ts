// two
import type {
  DBChat,
  DBUser,
  DBMessage,
  DBCreateChat,
  DBCreateUser,
  DBCreateMessage,
} from './db'

export type APIUser = Omit<DBUser, 'Password'>
export type APIChat = DBChat
export type APIDBMessage = DBMessage
export type APICreateUser = DBCreateUser
export type APICreateChat = DBCreateChat
export type APICreateMessage = DBCreateMessage
