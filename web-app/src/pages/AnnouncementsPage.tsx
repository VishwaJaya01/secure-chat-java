import { useState, useEffect, useCallback } from 'react';
import { api } from '../services/api';
import type { Announcement } from '../types';

interface AnnouncementsPageProps {
  username: string;
}

export const AnnouncementsPage = ({ username }: AnnouncementsPageProps) => {
  const [announcements, setAnnouncements] = useState<Announcement[]>([]);
  const [title, setTitle] = useState('');
  const [content, setContent] = useState('');
  const [error, setError] = useState<string | null>(null);

  useEffect(() => {
    // Subscribe to SSE stream. The stream will send historical data upon connection,
    // and then real-time updates.
    const eventSource = new EventSource(`${import.meta.env.VITE_CHAT_API_URL ?? '/api'}/announcements/stream`);
    
    const handleNewAnnouncement = (event: MessageEvent) => {
      try {
        const newAnnouncement = JSON.parse(event.data) as Announcement;
        setAnnouncements((prev) => {
          // Avoid duplicates, add if it's not already in the list
          if (prev.some(a => a.id === newAnnouncement.id)) {
            return prev;
          }
          // Add new announcements to the top
          return [newAnnouncement, ...prev].sort((a, b) => b.id - a.id);
        });
      } catch (err) {
        console.error('Failed to parse announcement:', err);
      }
    };

    eventSource.addEventListener('announcement', handleNewAnnouncement);

    eventSource.onerror = () => {
      setError('Connection to announcement stream failed. Please refresh the page.');
      eventSource.close();
    };

    return () => {
      eventSource.removeEventListener('announcement', handleNewAnnouncement);
      eventSource.close();
    };
  }, []);

  const handleSubmit = useCallback(
    async (e: React.FormEvent) => {
      e.preventDefault();
      if (!title.trim() || !content.trim()) return;

      try {
        // The new announcement will arrive via the SSE stream, so no need to manually add it here.
        await api.createAnnouncement(username, title, content);
        setTitle('');
        setContent('');
        setError(null);
      } catch (err) {
        setError(err instanceof Error ? err.message : 'Failed to create announcement');
      }
    },
    [username, title, content]
  );

  return (
    <div className="announcements-page">
      <div className="page-header">
        <h2>Announcements</h2>
      </div>
      <div className="announcement-form">
        <h3>Create Announcement</h3>
        <form onSubmit={handleSubmit}>
          <input
            type="text"
            placeholder="Title"
            value={title}
            onChange={(e) => setTitle(e.target.value)}
            required
          />
          <textarea
            placeholder="Content"
            value={content}
            onChange={(e) => setContent(e.target.value)}
            required
            rows={4}
          />
          <button type="submit">Post Announcement</button>
        </form>
        {error && <div className="error">{error}</div>}
      </div>
      <div className="announcements-list">
        {announcements.length === 0 && !error && <p>No announcements yet. Be the first to post!</p>}
        {announcements.map((announcement) => (
          <div key={announcement.id} className="announcement-card">
            <div className="announcement-header">
              <h3>{announcement.title}</h3>
              <span className="announcement-author">by {announcement.author}</span>
            </div>
            <div className="announcement-content">{announcement.content}</div>
            <div className="announcement-footer">
              <span className="announcement-date">
                {new Date(announcement.createdAt).toLocaleString()}
              </span>
            </div>
          </div>
        ))}
      </div>
    </div>
  );
};

