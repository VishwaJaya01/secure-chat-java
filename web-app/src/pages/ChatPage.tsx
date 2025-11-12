import { useState, useEffect, useCallback } from 'react';
import { MessageList } from '../components/MessageList';
import { MessageComposer } from '../components/MessageComposer';
import { api } from '../services/api';
import type { ChatMessage } from '../types';

interface ChatPageProps {
  username: string;
}

export const ChatPage = ({ username }: ChatPageProps) => {
  const [messages, setMessages] = useState<ChatMessage[]>([]);
  const [status, setStatus] = useState<'idle' | 'connecting' | 'open' | 'error'>('idle');
  const [error, setError] = useState<string | null>(null);

  useEffect(() => {
    setStatus('connecting');
    const eventSource = new EventSource(`${import.meta.env.VITE_CHAT_API_URL ?? '/api'}/stream?u=${username}`);

    // Set status to open when connection is established
    eventSource.onopen = () => {
      setStatus('open');
      setError(null);
    };

    // Also set status to open when we receive the first message (connection is working)
    let firstMessageReceived = false;
    eventSource.addEventListener('message', (event) => {
      if (!firstMessageReceived) {
        firstMessageReceived = true;
        setStatus('open');
        setError(null);
      }
      
      try {
        const data = JSON.parse(event.data);
        const message: ChatMessage = {
          id: data.id,
          author: data.author,
          content: data.content,
          createdAt: data.createdAt,
          mine: data.author === username,
        };
        
        setMessages((prev) => {
          // Check if message already exists (avoid duplicates)
          if (prev.some(m => m.id === message.id)) {
            return prev;
          }
          // Add new message and sort by createdAt (oldest first)
          const updated = [...prev, message];
          return updated
            .sort((a, b) => new Date(a.createdAt).getTime() - new Date(b.createdAt).getTime())
            .slice(-200); // Keep last 200 messages
        });
      } catch (err) {
        console.error('Failed to parse message:', err);
      }
    });

    eventSource.onerror = (err) => {
      // Only set error status if connection is actually broken
      if (eventSource.readyState === EventSource.CLOSED) {
        setStatus('error');
        setError('Connection error');
      }
    };

    return () => {
      eventSource.close();
    };
  }, [username]);

  const handleSend = useCallback(
    async (text: string) => {
      try {
        await api.sendMessage(username, text);
        setError(null);
      } catch (err) {
        setError(err instanceof Error ? err.message : 'Failed to send message');
      }
    },
    [username]
  );

  return (
    <div className="chat-page">
      <div className="page-header">
        <h2>Chat</h2>
        <span className={`status-pill ${status}`}>
          {status === 'open' ? 'Live' : status === 'connecting' ? 'Connecting' : 'Error'}
        </span>
      </div>
      {error && (
        <div className="alert">
          <span>{error}</span>
          <button type="button" onClick={() => setError(null)}>
            Dismiss
          </button>
        </div>
      )}
      <MessageList messages={messages} />
      <MessageComposer disabled={status === 'error'} onSend={handleSend} />
    </div>
  );
};

