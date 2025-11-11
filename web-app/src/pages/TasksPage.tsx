import { useState, useEffect, useCallback } from 'react';
import { api } from '../services/api';
import type { Task } from '../types';

interface TasksPageProps {
  username: string;
}

const STATUSES = ['todo', 'in-progress', 'done'];

export const TasksPage = ({ username }: TasksPageProps) => {
  const [tasks, setTasks] = useState<Task[]>([]);
  const [title, setTitle] = useState('');
  const [description, setDescription] = useState('');
  const [error, setError] = useState<string | null>(null);

  useEffect(() => {
    loadTasks();

    // Subscribe to SSE stream
    const eventSource = new EventSource(`${import.meta.env.VITE_CHAT_API_URL ?? '/api'}/tasks/stream`);

    eventSource.addEventListener('task', (event) => {
      try {
        const data = JSON.parse(event.data);
        const task: Task = {
          id: data.id,
          title: data.title,
          description: data.description,
          status: data.status,
          assignee: data.assignee,
          createdBy: data.createdBy,
          createdAt: data.createdAt,
          updatedAt: data.updatedAt,
        };
        setTasks((prev) => {
          const filtered = prev.filter((t) => t.id !== task.id);
          return [task, ...filtered];
        });
      } catch (err) {
        console.error('Failed to parse task:', err);
      }
    });

    return () => {
      eventSource.close();
    };
  }, []);

  const loadTasks = async () => {
    try {
      const data = await api.getTasks();
      setTasks(data);
    } catch (err) {
      setError(err instanceof Error ? err.message : 'Failed to load tasks');
    }
  };

  const handleCreateTask = useCallback(
    async (e: React.FormEvent) => {
      e.preventDefault();
      if (!title.trim()) return;

      try {
        await api.createTask(title, description, username);
        setTitle('');
        setDescription('');
        setError(null);
      } catch (err) {
        setError(err instanceof Error ? err.message : 'Failed to create task');
      }
    },
    [username, title, description]
  );

  const handleUpdateTask = async (task: Task, newStatus: string) => {
    try {
      await api.updateTask(task.id, task.title, task.description, newStatus, task.assignee);
    } catch (err) {
      setError(err instanceof Error ? err.message : 'Failed to update task');
    }
  };

  const handleDeleteTask = async (id: number) => {
    try {
      await api.deleteTask(id);
      setTasks((prev) => prev.filter((t) => t.id !== id));
    } catch (err) {
      setError(err instanceof Error ? err.message : 'Failed to delete task');
    }
  };

  const tasksByStatus = STATUSES.reduce((acc, status) => {
    acc[status] = tasks.filter((t) => t.status === status);
    return acc;
  }, {} as Record<string, Task[]>);

  return (
    <div className="tasks-page">
      <div className="page-header">
        <h2>Task Board</h2>
      </div>
      <div className="task-form">
        <h3>Create Task</h3>
        <form onSubmit={handleCreateTask}>
          <input
            type="text"
            placeholder="Title"
            value={title}
            onChange={(e) => setTitle(e.target.value)}
            required
          />
          <textarea
            placeholder="Description"
            value={description}
            onChange={(e) => setDescription(e.target.value)}
            rows={2}
          />
          <button type="submit">Create Task</button>
        </form>
        {error && <div className="error">{error}</div>}
      </div>
      <div className="task-board">
        {STATUSES.map((status) => (
          <div key={status} className="task-column">
            <h3 className="task-column-title">{status.toUpperCase()}</h3>
            <div className="task-list">
              {tasksByStatus[status]?.map((task) => (
                <div key={task.id} className="task-card">
                  <div className="task-header">
                    <h4>{task.title}</h4>
                    <button
                      type="button"
                      onClick={() => handleDeleteTask(task.id)}
                      className="task-delete"
                    >
                      ×
                    </button>
                  </div>
                  {task.description && <p className="task-description">{task.description}</p>}
                  <div className="task-footer">
                    <span className="task-author">by {task.createdBy}</span>
                    {task.assignee && <span className="task-assignee">@{task.assignee}</span>}
                  </div>
                  <div className="task-actions">
                    {STATUSES.map((s) => (
                      <button
                        key={s}
                        type="button"
                        onClick={() => handleUpdateTask(task, s)}
                        disabled={task.status === s}
                        className={task.status === s ? 'active' : ''}
                      >
                        {s}
                      </button>
                    ))}
                  </div>
                </div>
              ))}
            </div>
          </div>
        ))}
      </div>
    </div>
  );
};

