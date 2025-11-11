import { useId, useState } from 'react';
import type { FormEvent } from 'react';

interface LoginFormProps {
  onSubmit: (username: string) => void;
}

export const LoginForm = ({ onSubmit }: LoginFormProps) => {
  const [value, setValue] = useState('');
  const [error, setError] = useState<string | null>(null);
  const inputId = useId();

  console.log('LoginForm rendering...');

  const handleSubmit = (event: FormEvent<HTMLFormElement>) => {
    event.preventDefault();
    const next = value.trim();
    if (!next) {
      setError('Pick a username to continue.');
      return;
    }
    setError(null);
    console.log('Submitting username:', next);
    onSubmit(next);
  };

  return (
    <div className="panel" style={{ 
      background: 'linear-gradient(135deg, #111b36, #0e172f)',
      border: '1px solid rgba(148, 163, 184, 0.25)',
      borderRadius: '1.5rem',
      boxShadow: '0 15px 60px rgba(2, 6, 23, 0.65)',
      padding: 'clamp(1.5rem, 4vw, 3rem)',
      width: 'min(480px, 100%)',
      color: '#f8fafc'
    }}>
      <p style={{ 
        textTransform: 'uppercase',
        letterSpacing: '0.2em',
        fontSize: '0.75rem',
        color: '#94a3b8',
        margin: '0 0 1rem 0'
      }}>SecureCollab</p>
      <h1 style={{ margin: '0 0 1rem 0', fontSize: '2rem' }}>Join the lobby</h1>
      <p style={{ color: '#94a3b8', margin: '0 0 2rem 0' }}>
        Choose a display name to hop into the secure chat room.
      </p>
      <form style={{ 
        display: 'flex',
        flexDirection: 'column',
        gap: '0.75rem'
      }} onSubmit={handleSubmit}>
        <label htmlFor={inputId} style={{ 
          fontSize: '0.9rem',
          fontWeight: 600,
          color: '#94a3b8'
        }}>Display name</label>
        <input
          id={inputId}
          type="text"
          value={value}
          onChange={(event) => setValue(event.target.value)}
          placeholder="e.g. crypt0-cat"
          autoComplete="off"
          style={{
            width: '100%',
            padding: '0.8rem 1rem',
            borderRadius: '0.9rem',
            border: '1px solid rgba(148, 163, 184, 0.25)',
            background: 'rgba(15, 23, 42, 0.6)',
            color: '#f8fafc',
            fontSize: '1rem'
          }}
        />
        {error && <p style={{ color: '#f87171', fontSize: '0.85rem', margin: 0 }}>{error}</p>}
        <button 
          type="submit" 
          style={{
            border: 'none',
            borderRadius: '999px',
            padding: '0.8rem 1.4rem',
            fontWeight: 600,
            fontSize: '0.95rem',
            cursor: 'pointer',
            background: 'linear-gradient(120deg, #38bdf8, #0ea5e9)',
            color: '#0f172a',
            marginTop: '0.5rem'
          }}
        >
          Enter room
        </button>
      </form>
    </div>
  );
};
