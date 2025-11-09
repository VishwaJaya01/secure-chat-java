import { useState } from 'react';
import { ChatShell } from './components/ChatShell';
import { LoginForm } from './components/LoginForm';

function App() {
  const [username, setUsername] = useState<string | null>(null);

  return (
    <div className="app-shell">
      {username ? (
        <ChatShell username={username} onLogout={() => setUsername(null)} />
      ) : (
        <LoginForm onSubmit={setUsername} />
      )}
    </div>
  );
}

export default App;
