import { useCallback, useEffect, useMemo, useRef, useState } from 'react';
import type { ChatMessage, PresenceUser, StreamStatus } from '../types';
import {
  openStream,
  sendMessage as sendChatMessage,
  type WebMessagePayload,
} from '../services/chatApi';

const MAX_MESSAGES = Number(import.meta.env.VITE_CHAT_MAX_MESSAGES ?? 200);

export interface ChatSession {
  messages: ChatMessage[];
  presence: PresenceUser[];
  status: StreamStatus;
  error: string | null;
  send: (text: string) => Promise<void>;
  clearError: () => void;
}

export const useChatSession = (username: string | null): ChatSession => {
  const [messages, setMessages] = useState<ChatMessage[]>([]);
  const [status, setStatus] = useState<StreamStatus>('idle');
  const [error, setError] = useState<string | null>(null);
  const nameRef = useRef(username ?? '');

  useEffect(() => {
    nameRef.current = username ?? '';
  }, [username]);

  useEffect(() => {
    if (!username) {
      setMessages([]);
      setStatus('idle');
      setError(null);
      return undefined;
    }

    setStatus('connecting');
    setError(null);
    setMessages([]);

    const close = openStream(username, {
      onOpen: () => setStatus('open'),
      onMessage: (payload) => {
        setMessages((prev) => {
          const normalized = normalizeMessage(payload, nameRef.current);
          if (prev.some((message) => message.id === normalized.id)) {
            return prev;
          }
          const next = [...prev, normalized];
          return next.length > MAX_MESSAGES ? next.slice(-MAX_MESSAGES) : next;
        });
      },
      onError: (message) => {
        setError(message ?? 'Stream disconnected');
        setStatus('connecting');
      },
    });

    return () => {
      close();
      setStatus('idle');
    };
  }, [username]);

  const presence = useMemo<PresenceUser[]>(() => {
    const users = new Map<string, string>();
    messages.forEach((message) => {
      if (message.type === 'system') {
        return;
      }
      users.set(message.user, message.timestamp);
    });
    return Array.from(users.entries()).map(([name, ts]) => ({
      name,
      lastSeen: ts,
    }));
  }, [messages]);

  const send = useCallback(
    async (text: string) => {
      if (!username) {
        throw new Error('Choose a username before sending messages.');
      }
      if (!text.trim()) {
        return;
      }
      try {
        await sendChatMessage(username, text.trim());
      } catch (err) {
        const detail =
          err instanceof Error ? err.message : 'Failed to send message';
        setError(detail);
        throw err;
      }
    },
    [username],
  );

  return {
    messages,
    presence,
    status,
    error,
    send,
    clearError: () => setError(null),
  };
};

const normalizeMessage = (
  payload: WebMessagePayload,
  currentUser: string,
): ChatMessage => {
  const timestamp = payload.timestamp ?? new Date().toISOString();
  const user = payload.user?.trim() || 'system';
  const text = payload.text ?? '';
  const type = payload.type === 'system' ? 'system' : 'msg';
  const mine =
    typeof payload.mine === 'boolean'
      ? payload.mine
      : user.localeCompare(currentUser ?? '', undefined, {
          sensitivity: 'accent',
        }) === 0;

  return {
    id: `${timestamp}-${user}-${text.length}`,
    user,
    text,
    timestamp,
    mine,
    type,
  };
};
