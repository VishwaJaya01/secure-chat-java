import { useId, useState } from 'react';
import type { FormEvent } from 'react';

interface LoginFormProps {
  onSubmit: (username: string) => void;
}

export const LoginForm = ({ onSubmit }: LoginFormProps) => {
  const [value, setValue] = useState('');
  const [error, setError] = useState<string | null>(null);
  const inputId = useId();

  const handleSubmit = (event: FormEvent<HTMLFormElement>) => {
    event.preventDefault();
    const next = value.trim();
    if (!next) {
      setError('Pick a username to continue.');
      return;
    }
    setError(null);
    onSubmit(next);
  };

  return (
    <div className="panel login-panel">
      <p className="eyebrow">SecureChat</p>
      <h1>Join the lobby</h1>
      <p className="lead">
        Choose a display name to hop into the secure chat room.
      </p>
      <form className="stack" onSubmit={handleSubmit}>
        <label htmlFor={inputId}>Display name</label>
        <input
          id={inputId}
          type="text"
          value={value}
          onChange={(event) => setValue(event.target.value)}
          placeholder="e.g. crypt0-cat"
          autoComplete="off"
        />
        {error && <p className="form-error">{error}</p>}
        <button type="submit" className="primary">
          Enter room
        </button>
      </form>
    </div>
  );
};
