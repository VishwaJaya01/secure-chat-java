import { useEffect, useRef, useState } from 'react';
import { formatTimestamp } from '../utils/time';
import { api } from '../services/api';
import type { ChatMessage, LinkPreview } from '../types';

interface MessageListProps {
  messages: ChatMessage[];
}

// URL detection regex
const URL_REGEX = /(https?:\/\/[^\s]+|www\.[^\s]+)/gi;

// Extract URLs from text
function extractUrls(text: string): string[] {
  const matches = text.match(URL_REGEX);
  if (!matches) return [];
  return matches.map(url => url.startsWith('www.') ? `https://${url}` : url);
}

// Component to render a single link preview
function LinkPreviewCard({ url }: { url: string }) {
  const [preview, setPreview] = useState<LinkPreview | null>(null);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState(false);

  useEffect(() => {
    let cancelled = false;
    
    api.getLinkPreview(url)
      .then((data) => {
        if (!cancelled) {
          setPreview(data);
          setLoading(false);
        }
      })
      .catch(() => {
        if (!cancelled) {
          setError(true);
          setLoading(false);
        }
      });

    return () => {
      cancelled = true;
    };
  }, [url]);

  if (loading) {
    return (
      <div className="link-preview loading">
        <span>Loading preview...</span>
      </div>
    );
  }

  if (error || !preview) {
    return (
      <a href={url} target="_blank" rel="noopener noreferrer" className="link-preview-fallback">
        {url}
      </a>
    );
  }

  return (
    <a
      href={preview.url}
      target="_blank"
      rel="noopener noreferrer"
      className="link-preview"
    >
      {preview.iconUrl && (
        <img src={preview.iconUrl} alt="" className="link-preview-icon" />
      )}
      <div className="link-preview-content">
        {preview.title && <div className="link-preview-title">{preview.title}</div>}
        {preview.description && (
          <div className="link-preview-description">{preview.description}</div>
        )}
        <div className="link-preview-url">{preview.url}</div>
      </div>
    </a>
  );
}

// Component to render message content with link detection
function MessageContent({ content }: { content: string }) {
  const urls = extractUrls(content);
  
  if (urls.length === 0) {
    return <p>{content}</p>;
  }

  // Split content by URLs and render with previews
  const parts: (string | { type: 'url'; url: string })[] = [];
  let lastIndex = 0;
  let match;

  // Reset regex
  URL_REGEX.lastIndex = 0;
  
  while ((match = URL_REGEX.exec(content)) !== null) {
    if (match.index > lastIndex) {
      parts.push(content.substring(lastIndex, match.index));
    }
    const url = match[0].startsWith('www.') ? `https://${match[0]}` : match[0];
    parts.push({ type: 'url', url });
    lastIndex = match.index + match[0].length;
  }
  
  if (lastIndex < content.length) {
    parts.push(content.substring(lastIndex));
  }

  return (
    <div>
      <p>
        {parts.map((part, idx) => {
          if (typeof part === 'string') {
            return <span key={idx}>{part}</span>;
          } else {
            return (
              <a
                key={idx}
                href={part.url}
                target="_blank"
                rel="noopener noreferrer"
                className="message-link"
              >
                {part.url}
              </a>
            );
          }
        })}
      </p>
      <div className="link-previews">
        {urls.map((url, idx) => (
          <LinkPreviewCard key={idx} url={url} />
        ))}
      </div>
    </div>
  );
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
          <MessageContent content={message.content} />
        </article>
      ))}
    </div>
  );
};
