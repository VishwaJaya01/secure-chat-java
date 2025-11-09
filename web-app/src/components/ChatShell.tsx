import { useCallback } from 'react';
import { useChatSession } from '../hooks/useChatSession';
import { MessageList } from './MessageList';
import { MessageComposer } from './MessageComposer';
import { PresencePanel } from './PresencePanel';

interface ChatShellProps {
  username: string;
  onLogout: () => void;
}

export const ChatShell = ({ username, onLogout }: ChatShellProps) => {
  const { messages, presence, status, error, send, clearError } =
    useChatSession(username);

  const handleSend = useCallback(
    async (text: string) => {
      await send(text);
    },
    [send],
  );

  return (
    <div className="chat-shell">
      <header className="chat-header">
        <div>
          <p className="eyebrow">SecureChat</p>
          <h2>Lobby</h2>
          <p className="hint">
            React renders the room. Java services provide the stream at{' '}
            <code>{import.meta.env.VITE_CHAT_API_URL ?? '/api'}</code>.
          </p>
        </div>
        <div className="session">
          <span className={`status-pill ${status}`}>
            {statusLabel(status)}
          </span>
          <span className="user-pill">{username}</span>
          <button type="button" onClick={onLogout}>
            Switch user
          </button>
        </div>
      </header>
      {error && (
        <div className="alert">
          <span>{error}</span>
          <button type="button" onClick={clearError}>
            Dismiss
          </button>
        </div>
      )}
      <div className="chat-content">
        <section className="chat-column">
          <MessageList messages={messages} />
          <MessageComposer
            disabled={status !== 'open'}
            onSend={handleSend}
          />
        </section>
        <aside className="presence-column">
          <PresencePanel users={presence} />
        </aside>
      </div>
    </div>
  );
};

const statusLabel = (status: string) => {
  switch (status) {
    case 'open':
      return 'Live';
    case 'connecting':
      return 'Connecting';
    case 'error':
      return 'Retrying';
    default:
      return 'Idle';
  }
};
