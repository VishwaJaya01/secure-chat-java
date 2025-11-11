import { useState, useEffect } from 'react';
import type { ReactNode } from 'react';
import { Link, useLocation } from 'react-router-dom';
import { PresencePanel } from './PresencePanel';
import { api } from '../services/api';
import type { User } from '../types';

interface LayoutProps {
  username: string;
  isAdmin: boolean;
  onLogout: () => void;
  children: ReactNode;
}

export const Layout = ({ username, isAdmin, onLogout, children }: LayoutProps) => {
  const location = useLocation();
  const [users, setUsers] = useState<User[]>([]);

  useEffect(() => {
    let intervalId: ReturnType<typeof setInterval> | null = null;
    
    const loadUsers = async () => {
      try {
        const [usersData, statusData] = await Promise.all([
          api.getUsers().catch(() => []),
          api.getUserStatus().catch(() => ({})),
        ]);
        if (Array.isArray(usersData) && statusData) {
          setUsers(usersData.map((u: any) => ({ ...u, status: statusData[u.userId] || 'offline' })));
        } else {
          // Backend might not be running - show empty list
          setUsers([]);
        }
      } catch (error) {
        // Backend not available - that's okay, just show empty list
        setUsers([]);
      }
    };

    // Load users with a delay to avoid blocking initial render
    const timeout = setTimeout(() => {
      loadUsers();
      // Set up polling every 10 seconds
      intervalId = setInterval(loadUsers, 10000);
    }, 500);
    
    return () => {
      clearTimeout(timeout);
      if (intervalId) {
        clearInterval(intervalId);
      }
    };
  }, []);

  const navItems = [
    { path: '/chat', label: 'Chat' },
    { path: '/announcements', label: 'Announcements' },
    { path: '/tasks', label: 'Tasks' },
    { path: '/files', label: 'Files' },
  ];

  return (
    <div className="app-layout">
      <header className="app-header">
        <div className="app-header-content">
          <h1>SecureCollab</h1>
          <nav className="app-nav">
            {navItems.map((item) => (
              <Link
                key={item.path}
                to={item.path}
                className={location.pathname === item.path ? 'active' : ''}
              >
                {item.label}
              </Link>
            ))}
          </nav>
          <div className="app-header-user">
            <span className="user-name">{username}</span>
            {isAdmin && <span className="admin-badge">Admin</span>}
            <button type="button" onClick={onLogout} className="logout-btn">
              Logout
            </button>
          </div>
        </div>
      </header>
      <div className="app-content">
        <main className="app-main">{children}</main>
        <aside className="app-sidebar">
          <PresencePanel users={users} />
        </aside>
      </div>
    </div>
  );
};

