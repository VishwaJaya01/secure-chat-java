export interface WebMessagePayload {
  user: string;
  text: string;
  timestamp: string;
  mine?: boolean;
  type?: string;
}

interface StreamHandlers {
  onMessage?: (payload: WebMessagePayload) => void;
  onError?: (message?: string) => void;
  onOpen?: () => void;
}

const RAW_BASE = import.meta.env.VITE_CHAT_API_URL ?? '/api';
const API_BASE = RAW_BASE.endsWith('/') ? RAW_BASE.slice(0, -1) : RAW_BASE;

const toUrl = (path: string, params?: Record<string, string>) => {
  const normalizedPath = path.startsWith('/') ? path : `/${path}`;
  const target = `${API_BASE}${normalizedPath}`;
  if (!params || Object.keys(params).length === 0) {
    return target;
  }
  const query = new URLSearchParams(params).toString();
  return `${target}?${query}`;
};

export const openStream = (username: string, handlers: StreamHandlers = {}) => {
  let source: EventSource | null = null;

  try {
    source = new EventSource(toUrl('/stream', { u: username }));
  } catch (error) {
    handlers.onError?.(
      error instanceof Error ? error.message : 'Unable to create stream',
    );
    return () => {
      /* noop because stream never opened */
    };
  }

  source.addEventListener('open', () => handlers.onOpen?.());

  source.addEventListener('message', (event: MessageEvent<string>) => {
    try {
      const payload = JSON.parse(event.data) as WebMessagePayload;
      handlers.onMessage?.(payload);
    } catch (error) {
      handlers.onError?.(
        error instanceof Error ? error.message : 'Invalid message payload',
      );
    }
  });

  source.addEventListener('error', () => {
    handlers.onError?.('Stream connection issue');
  });

  return () => {
    source?.close();
  };
};

export const sendMessage = async (username: string, text: string) => {
  const response = await fetch(toUrl('/send'), {
    method: 'POST',
    headers: {
      'Content-Type': 'application/x-www-form-urlencoded;charset=UTF-8',
    },
    body: new URLSearchParams({ username, text }).toString(),
  }).catch((error) => {
    throw new Error(
      error instanceof Error ? error.message : 'Unable to reach chat API',
    );
  });

  if (!response.ok) {
    const detail = await response.text().catch(() => '');
    throw new Error(detail || `Send failed (${response.status})`);
  }
};
