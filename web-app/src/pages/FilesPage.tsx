import { useState, useEffect } from 'react';
import { api } from '../services/api';
import type { File } from '../types';

interface FilesPageProps {
  username: string;
}

export const FilesPage = ({ username: _username }: FilesPageProps) => {
  const [files, setFiles] = useState<File[]>([]);
  const [error, setError] = useState<string | null>(null);

  useEffect(() => {
    loadFiles();
  }, []);

  const loadFiles = async () => {
    try {
      const data = await api.getFiles();
      setFiles(data);
    } catch (err) {
      setError(err instanceof Error ? err.message : 'Failed to load files');
    }
  };

  const formatFileSize = (bytes: number) => {
    if (bytes < 1024) return bytes + ' B';
    if (bytes < 1024 * 1024) return (bytes / 1024).toFixed(2) + ' KB';
    return (bytes / (1024 * 1024)).toFixed(2) + ' MB';
  };

  return (
    <div className="files-page">
      <div className="page-header">
        <h2>Files</h2>
        <p className="hint">File transfer via TCP (Member A's responsibility)</p>
      </div>
      {error && <div className="error">{error}</div>}
      <div className="files-list">
        {files.length === 0 ? (
          <p>No files uploaded yet.</p>
        ) : (
          files.map((file) => (
            <div key={file.id} className="file-card">
              <div className="file-header">
                <h3>{file.originalFilename}</h3>
                <span className="file-size">{formatFileSize(file.fileSize)}</span>
              </div>
              <div className="file-meta">
                <span>Uploaded by {file.uploader}</span>
                <span>{new Date(file.createdAt).toLocaleString()}</span>
                {file.checksum && <span>Checksum: {file.checksum}</span>}
              </div>
              <div className="file-actions">
                <button type="button" disabled>
                  Download (TCP Transfer)
                </button>
              </div>
            </div>
          ))
        )}
      </div>
    </div>
  );
};

