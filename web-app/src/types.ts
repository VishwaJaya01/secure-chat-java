export type MessageType = 'msg' | 'system';

export interface ChatMessage {
  id: number;
  author: string;
  content: string;
  createdAt: string;
  mine?: boolean;
  type?: MessageType;
}

export interface Announcement {
  id: number;
  author: string;
  title: string;
  content: string;
  createdAt: string;
}

export interface Task {
  id: number;
  title: string;
  description?: string;
  status: string;
  assignee?: string;
  createdBy: string;
  createdAt: string;
  updatedAt: string;
}

export interface File {
  fileId: string;
  filename: string;
  fileSize: number;
  owner: string;
  tcpHost: string;
  tcpPort: number;
  createdAt: string;
}

export interface User {
  id: number;
  userId: string;
  displayName?: string;
  lastSeen?: string;
  isAdmin: boolean;
  status?: 'online' | 'idle' | 'offline';
}

export interface LinkPreview {
  id: number;
  url: string;
  title?: string;
  description?: string;
  iconUrl?: string;
}

export interface PresenceUser {
  name: string;
  lastSeen: string;
  status?: 'online' | 'idle' | 'offline';
}

export type StreamStatus = 'idle' | 'connecting' | 'open' | 'error';
