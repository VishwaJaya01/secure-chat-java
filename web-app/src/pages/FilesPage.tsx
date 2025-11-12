import { useState, useEffect, type FormEvent } from 'react';
import { api } from '../services/api';
import type { File as FileMetadata } from '../types';

export const FilesPage = () => {
  const [files, setFiles] = useState<FileMetadata[]>([]);
  const [error, setError] = useState<string | null>(null);
  const [owner, setOwner] = useState('');
  const [selectedFile, setSelectedFile] = useState<File | null>(null);
  const [uploading, setUploading] = useState(false);
  const [uploadMessage, setUploadMessage] = useState<string | null>(null);

  useEffect(() => {
    loadFiles();

    const eventSource = new EventSource(`${import.meta.env.VITE_CHAT_API_URL ?? '/api'}/files/stream`);
    eventSource.addEventListener('files-init', (event) => {
      try {
        const initialFiles = JSON.parse(event.data) as FileMetadata[];
        setFiles(initialFiles);
      } catch (err) {
        console.error('Failed to parse initial files payload:', err);
      }
    });

    eventSource.addEventListener('file-announced', (event) => {
      try {
        const newFile = JSON.parse(event.data) as FileMetadata;
        setFiles((prev) => {
          if (prev.some(f => f.fileId === newFile.fileId)) {
            return prev;
          }
          return [newFile, ...prev].sort((a, b) => new Date(b.createdAt).getTime() - new Date(a.createdAt).getTime());
        });
        setUploading(false);
        setUploadMessage(`Upload complete: ${newFile.filename}`);
      } catch (err) {
        console.error('Failed to parse file announcement:', err);
      }
    });

    eventSource.onerror = (_err) => {
      setError('SSE connection failed. Real-time updates may not work.');
      eventSource.close();
    };

    return () => {
      eventSource.close();
    };
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

  const handleUpload = async (event: FormEvent<HTMLFormElement>) => {
    event.preventDefault();
    if (!selectedFile) {
      setUploadMessage('Select a file to upload.');
      return;
    }
    if (!owner.trim()) {
      setUploadMessage('Enter the owner name.');
      return;
    }

    try {
      setUploading(true);
      setUploadMessage('Uploading...');
      console.log('Starting upload:', { filename: selectedFile.name, size: selectedFile.size, owner: owner.trim() });
      await api.uploadFile(owner.trim(), selectedFile);
      console.log('Upload request completed');
      setUploadMessage('Upload sent. Waiting for server confirmation…');
      setSelectedFile(null);
      const fileInput = event.currentTarget.querySelector('input[type="file"]') as HTMLInputElement | null;
      if (fileInput) {
        fileInput.value = '';
      }
    } catch (err) {
      console.error('Upload error:', err);
      setUploading(false);
      const errorMessage = err instanceof Error ? err.message : 'Upload failed';
      setUploadMessage(errorMessage);
    }
  };

  const handleDownload = async (fileId: string, filename: string) => {
    try {
      await api.downloadFile(fileId, filename);
    } catch (err) {
      console.error('Download error:', err);
      setUploadMessage(err instanceof Error ? err.message : 'Download failed');
    }
  };

  return (
    <div className="files-page">
      <div className="page-header">
        <h2>Files</h2>
      </div>
      <form className="file-upload-form" onSubmit={handleUpload}>
        <label>
          Owner
          <input
            type="text"
            value={owner}
            onChange={(e) => setOwner(e.target.value)}
            placeholder="Your name"
            disabled={uploading}
            required
          />
        </label>
        <label>
          Choose File
          <input
            type="file"
            onChange={(e) => setSelectedFile(e.target.files?.[0] ?? null)}
            disabled={uploading}
            required
          />
        </label>
        <button type="submit" disabled={uploading}>
          {uploading ? 'Uploading…' : 'Upload File'}
        </button>
      </form>
      {uploadMessage && <div className="upload-status">{uploadMessage}</div>}
      {error && <div className="error">{error}</div>}
      <div className="files-list">
        {files.length === 0 ? (
          <p>No files uploaded yet.</p>
        ) : (
          files.map((file) => (
            <div key={file.fileId} className="file-card">
              <div className="file-header">
                <h3>{file.filename}</h3>
                <span className="file-size">{formatFileSize(file.fileSize)}</span>
              </div>
              <div className="file-meta">
                <span>Uploaded by {file.owner}</span>
                <span>{new Date(file.createdAt).toLocaleString()}</span>
                <span>TCP Address: {file.tcpHost}:{file.tcpPort}</span>
              </div>
              <div className="file-actions">
                <button
                  onClick={() => handleDownload(file.fileId, file.filename)}
                  className="download-button"
                >
                  Download via TCP
                </button>
                <p className="download-hint">
                  TCP: <code>{file.tcpHost}:{file.tcpPort}</code> / <code>{file.filename}</code>
                </p>
              </div>
            </div>
          ))
        )}
      </div>
    </div>
  );
};

