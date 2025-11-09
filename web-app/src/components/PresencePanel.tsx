import type { PresenceUser } from '../types';
import { formatTimestamp } from '../utils/time';

interface PresencePanelProps {
  users: PresenceUser[];
}

export const PresencePanel = ({ users }: PresencePanelProps) => {
  if (users.length === 0) {
    return (
      <div className="presence-panel empty">
        <p>No one is online yet.</p>
      </div>
    );
  }

  return (
    <div className="presence-panel">
      <header>
        <p className="eyebrow">Online</p>
        <span>{users.length}</span>
      </header>
      <ul>
        {users.map((user) => (
          <li key={user.name}>
            <span className="avatar">{user.name.charAt(0).toUpperCase()}</span>
            <div>
              <p className="name">{user.name}</p>
              <p className="meta">
                Active {formatTimestamp(user.lastSeen)}
              </p>
            </div>
          </li>
        ))}
      </ul>
    </div>
  );
};
