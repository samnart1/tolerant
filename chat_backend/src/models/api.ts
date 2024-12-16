import {
  DBUser,
  DBChat,
  DBMessage,
  DBCreateUser,
  DBCreateChat,
  DBCreateMessage,
} from './db'

export type APIUser = Omit<DBUser, 'password'>
export type APICreateUser = DBCreateUser

export type APIChat = DBChat
export type APICreateChat = DBCreateChat

export type APIMessage = DBMessage
export type APICreateMessage = DBCreateMessage
