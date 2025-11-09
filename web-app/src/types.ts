export type MessageType = 'msg' | 'system';

export interface ChatMessage {
  id: string;
  user: string;
  text: string;
  timestamp: string;
  mine: boolean;
  type: MessageType;
}

export interface PresenceUser {
  name: string;
  lastSeen: string;
}

export type StreamStatus = 'idle' | 'connecting' | 'open' | 'error';
