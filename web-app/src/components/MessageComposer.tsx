import { useState } from 'react';
import type { FormEvent } from 'react';

interface ComposerProps {
  disabled?: boolean;
  onSend: (text: string) => Promise<void> | void;
}

export const MessageComposer = ({ disabled, onSend }: ComposerProps) => {
  const [value, setValue] = useState('');

  const handleSubmit = async (event: FormEvent<HTMLFormElement>) => {
    event.preventDefault();
    const text = value.trim();
    if (!text) {
      return;
    }
    await onSend(text);
    setValue('');
  };

  return (
    <form className="composer" onSubmit={handleSubmit}>
      <textarea
        placeholder="Send a message"
        value={value}
        onChange={(event) => setValue(event.target.value)}
        disabled={disabled}
        rows={2}
      />
      <button
        type="submit"
        className="primary"
        disabled={disabled || value.trim().length === 0}
      >
        Send
      </button>
    </form>
  );
};
