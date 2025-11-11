import { useEffect, useRef } from 'react';
import { formatTimestamp } from '../utils/time';
import type { ChatMessage } from '../types';

interface MessageListProps {
  messages: ChatMessage[];
}

export const MessageList = ({ messages }: MessageListProps) => {
  const scrollerRef = useRef<HTMLDivElement | null>(null);

  useEffect(() => {
    const el = scrollerRef.current;
    if (!el) {
      return;
    }
    el.scrollTop = el.scrollHeight;
  }, [messages]);

  if (messages.length === 0) {
    return (
      <div className="message-list empty">
        <p>Nothing yet. Say hi to kick off the conversation.</p>
      </div>
    );
  }

  return (
    <div className="message-list" ref={scrollerRef}>
      {messages.map((message) => (
        <article
          key={message.id}
          className={`message ${message.mine ? 'mine' : ''} ${
            message.type === 'system' ? 'system' : ''
          }`}
        >
          <header>
            <span className="author">{message.author}</span>
            <time dateTime={message.createdAt}>
              {formatTimestamp(message.createdAt)}
            </time>
          </header>
          <p>{message.content}</p>
        </article>
      ))}
    </div>
  );
};
