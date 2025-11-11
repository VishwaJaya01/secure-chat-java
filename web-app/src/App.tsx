import { useState, useEffect } from 'react';
import { BrowserRouter, Routes, Route, Navigate } from 'react-router-dom';
import { LoginForm } from './components/LoginForm';
import { ChatPage } from './pages/ChatPage';
import { AnnouncementsPage } from './pages/AnnouncementsPage';
import { TasksPage } from './pages/TasksPage';
import { FilesPage } from './pages/FilesPage';
import { Layout } from './components/Layout';
import { api } from './services/api';

function App() {
  const [username, setUsername] = useState<string | null>(null);
  const [isAdmin, setIsAdmin] = useState(false);

  const checkAdminStatus = async (user: string) => {
    try {
      const users = await api.getUsers();
      const userData = users.find((u: any) => u.userId === user);
      setIsAdmin(userData?.isAdmin || false);
    } catch (error) {
      // Backend might not be running - that's okay
      setIsAdmin(false);
    }
  };

  useEffect(() => {
    // Check if user is logged in from sessionStorage
    try {
      const savedUsername = sessionStorage.getItem('username');
      if (savedUsername) {
        setUsername(savedUsername);
        // Check admin status in background, don't block rendering
        checkAdminStatus(savedUsername).catch(() => {
          // Backend might not be running
        });
        // Update presence periodically (with error handling)
        const interval = setInterval(() => {
          api.updatePresence(savedUsername, savedUsername).catch(() => {
            // Silently fail - backend might not be running
          });
        }, 5000);
        return () => clearInterval(interval);
      }
    } catch (error) {
      console.error('Error in useEffect:', error);
    }
  }, []);

  const handleLogin = (user: string) => {
    setUsername(user);
    sessionStorage.setItem('username', user);
    checkAdminStatus(user).catch(() => {
      // Backend might not be running
    });
    // Update presence in background, don't block on error
    api.updatePresence(user, user).catch(() => {
      // Silently fail
    });
  };

  const handleLogout = () => {
    setUsername(null);
    sessionStorage.removeItem('username');
    setIsAdmin(false);
  };

  // Show login form if no username
  if (!username) {
    return (
      <div className="app-shell">
        <LoginForm onSubmit={handleLogin} />
      </div>
    );
  }

  // Show main app with routing
  return (
    <BrowserRouter>
      <Layout username={username} isAdmin={isAdmin} onLogout={handleLogout}>
        <Routes>
          <Route path="/" element={<Navigate to="/chat" replace />} />
          <Route path="/chat" element={<ChatPage username={username} />} />
          <Route path="/announcements" element={<AnnouncementsPage username={username} isAdmin={isAdmin} />} />
          <Route path="/tasks" element={<TasksPage username={username} />} />
          <Route path="/files" element={<FilesPage username={username} />} />
        </Routes>
      </Layout>
    </BrowserRouter>
  );
}

export default App;
