import { formatTimestamp } from '../utils/time';
import type { User } from '../types';

interface PresencePanelProps {
  users: User[];
}

export const PresencePanel = ({ users }: PresencePanelProps) => {
  if (users.length === 0) {
    return (
      <div className="presence-panel empty">
        <p>No users yet.</p>
      </div>
    );
  }

  const getStatusColor = (status?: string) => {
    switch (status) {
      case 'online':
        return '#22c55e';
      case 'idle':
        return '#eab308';
      case 'offline':
        return '#6b7280';
      default:
        return '#6b7280';
    }
  };

  return (
    <div className="presence-panel">
      <header>
        <p className="eyebrow">Users</p>
        <span>{users.length}</span>
      </header>
      <ul>
        {users.map((user) => (
          <li key={user.userId}>
            <span className="avatar" style={{ backgroundColor: getStatusColor(user.status) }}>
              {(user.displayName || user.userId).charAt(0).toUpperCase()}
            </span>
            <div>
              <p className="name">{user.displayName || user.userId}</p>
              <p className="meta">
                {user.status || 'offline'}
                {user.lastSeen && ` • ${formatTimestamp(user.lastSeen)}`}
              </p>
            </div>
          </li>
        ))}
      </ul>
    </div>
  );
};
