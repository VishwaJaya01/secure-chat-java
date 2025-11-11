import { StrictMode } from 'react';
import { createRoot } from 'react-dom/client';
import App from './App.tsx';
import { ErrorBoundary } from './components/ErrorBoundary';
import './styles/tokens.css';
import './styles/global.css';

// Add error handler for unhandled errors - log them but don't crash
window.addEventListener('error', (event) => {
  console.error('Unhandled error:', event.error);
  event.preventDefault();
});

window.addEventListener('unhandledrejection', (event) => {
  console.error('Unhandled promise rejection:', event.reason);
  event.preventDefault();
});

const rootElement = document.getElementById('root');
if (!rootElement) {
  document.body.innerHTML = '<div style="padding: 2rem; color: red;"><h1>Error: Root element not found</h1></div>';
  throw new Error('Root element not found');
}

console.log('Initializing React app...');
console.log('Root element found:', rootElement);

try {
  const root = createRoot(rootElement);
  
  console.log('Rendering app...');
  root.render(
    <StrictMode>
      <ErrorBoundary>
        <App />
      </ErrorBoundary>
    </StrictMode>
  );
  console.log('App rendered successfully');
} catch (error) {
  console.error('Failed to render app:', error);
  rootElement.innerHTML = `
    <div style="padding: 2rem; color: white; background: #0f172a; min-height: 100vh; font-family: system-ui;">
      <h1 style="color: #f87171;">Failed to Load App</h1>
      <p>Error: ${error instanceof Error ? error.message : String(error)}</p>
      <details style="margin-top: 1rem;">
        <summary style="cursor: pointer; color: #94a3b8;">Show error details</summary>
        <pre style="background: #050b19; padding: 1rem; border-radius: 0.5rem; overflow: auto; color: #f87171;">
          ${error instanceof Error ? error.stack : String(error)}
        </pre>
      </details>
      <button onclick="window.location.reload()" style="margin-top: 1rem; padding: 0.5rem 1rem; cursor: pointer;">
        Reload Page
      </button>
    </div>
  `;
}
