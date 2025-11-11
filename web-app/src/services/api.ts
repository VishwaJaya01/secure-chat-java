const API_BASE = import.meta.env.VITE_CHAT_API_URL ?? '/api';

const toUrl = (path: string, params?: Record<string, string>) => {
  const normalizedPath = path.startsWith('/') ? path : `/${path}`;
  const target = `${API_BASE}${normalizedPath}`;
  if (!params || Object.keys(params).length === 0) {
    return target;
  }
  const query = new URLSearchParams(params).toString();
  return `${target}?${query}`;
};

export const api = {
  // Chat
  sendMessage: async (username: string, text: string) => {
    const response = await fetch(toUrl('/send'), {
      method: 'POST',
      headers: {
        'Content-Type': 'application/x-www-form-urlencoded;charset=UTF-8',
      },
      body: new URLSearchParams({ username, text }).toString(),
    });
    if (!response.ok) throw new Error(`Failed to send message: ${response.status}`);
    return response;
  },

  getMessages: async () => {
    const response = await fetch(toUrl('/stream', { u: 'user' }));
    if (!response.ok) throw new Error(`Failed to get messages: ${response.status}`);
    return response.json();
  },

  // Announcements
  createAnnouncement: async (author: string, title: string, content: string) => {
    const response = await fetch(toUrl('/announcements'), {
      method: 'POST',
      headers: {
        'Content-Type': 'application/x-www-form-urlencoded;charset=UTF-8',
      },
      body: new URLSearchParams({ author, title, content }).toString(),
    });
    if (!response.ok) throw new Error(`Failed to create announcement: ${response.status}`);
    return response.json();
  },

  getAnnouncements: async () => {
    const response = await fetch(toUrl('/announcements'));
    if (!response.ok) throw new Error(`Failed to get announcements: ${response.status}`);
    return response.json();
  },

  // Tasks
  createTask: async (title: string, description: string, createdBy: string, assignee?: string) => {
    const params: Record<string, string> = { title, createdBy };
    if (description) params.description = description;
    if (assignee) params.assignee = assignee;
    const response = await fetch(toUrl('/tasks'), {
      method: 'POST',
      headers: {
        'Content-Type': 'application/x-www-form-urlencoded;charset=UTF-8',
      },
      body: new URLSearchParams(params).toString(),
    });
    if (!response.ok) throw new Error(`Failed to create task: ${response.status}`);
    return response.json();
  },

  getTasks: async () => {
    const response = await fetch(toUrl('/tasks'));
    if (!response.ok) throw new Error(`Failed to get tasks: ${response.status}`);
    return response.json();
  },

  updateTask: async (id: number, title?: string, description?: string, status?: string, assignee?: string) => {
    const params: Record<string, string> = {};
    if (title) params.title = title;
    if (description) params.description = description;
    if (status) params.status = status;
    if (assignee) params.assignee = assignee;
    const response = await fetch(toUrl(`/tasks/${id}`), {
      method: 'PUT',
      headers: {
        'Content-Type': 'application/x-www-form-urlencoded;charset=UTF-8',
      },
      body: new URLSearchParams(params).toString(),
    });
    if (!response.ok) throw new Error(`Failed to update task: ${response.status}`);
    return response.json();
  },

  deleteTask: async (id: number) => {
    const response = await fetch(toUrl(`/tasks/${id}`), {
      method: 'DELETE',
    });
    if (!response.ok) throw new Error(`Failed to delete task: ${response.status}`);
  },

  // Files
  getFiles: async () => {
    const response = await fetch(toUrl('/files'));
    if (!response.ok) throw new Error(`Failed to get files: ${response.status}`);
    return response.json();
  },

  createFileMetadata: async (
    filename: string,
    originalFilename: string,
    contentType: string,
    fileSize: number,
    uploader: string,
    checksum?: string,
    filePath?: string
  ) => {
    const params: Record<string, string> = {
      filename,
      originalFilename,
      contentType: contentType || '',
      fileSize: fileSize.toString(),
      uploader,
    };
    if (checksum) params.checksum = checksum;
    if (filePath) params.filePath = filePath;
    const response = await fetch(toUrl('/files/meta'), {
      method: 'POST',
      headers: {
        'Content-Type': 'application/x-www-form-urlencoded;charset=UTF-8',
      },
      body: new URLSearchParams(params).toString(),
    });
    if (!response.ok) throw new Error(`Failed to create file metadata: ${response.status}`);
    return response.json();
  },

  // Presence
  getUsers: async () => {
    const response = await fetch(toUrl('/users'));
    if (!response.ok) throw new Error(`Failed to get users: ${response.status}`);
    return response.json();
  },

  getUserStatus: async () => {
    const response = await fetch(toUrl('/users/status'));
    if (!response.ok) throw new Error(`Failed to get user status: ${response.status}`);
    return response.json();
  },

  updatePresence: async (userId: string, displayName?: string) => {
    const params: Record<string, string> = { userId };
    if (displayName) params.displayName = displayName;
    const response = await fetch(toUrl('/presence/beat'), {
      method: 'POST',
      headers: {
        'Content-Type': 'application/x-www-form-urlencoded;charset=UTF-8',
      },
      body: new URLSearchParams(params).toString(),
    });
    if (!response.ok) throw new Error(`Failed to update presence: ${response.status}`);
  },

  // Link Preview
  getLinkPreview: async (url: string) => {
    const response = await fetch(toUrl('/link/preview', { url }));
    if (!response.ok) throw new Error(`Failed to get link preview: ${response.status}`);
    return response.json();
  },
};

