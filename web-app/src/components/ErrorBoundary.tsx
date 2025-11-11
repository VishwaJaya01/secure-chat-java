import React, { Component } from 'react';
import type { ReactNode } from 'react';

interface Props {
  children: ReactNode;
}

interface State {
  hasError: boolean;
  error?: Error;
}

export class ErrorBoundary extends Component<Props, State> {
  constructor(props: Props) {
    super(props);
    this.state = { hasError: false };
  }

  static getDerivedStateFromError(error: Error): State {
    return { hasError: true, error };
  }

  componentDidCatch(error: Error, errorInfo: React.ErrorInfo) {
    console.error('Error caught by boundary:', error, errorInfo);
  }

  render() {
    if (this.state.hasError) {
      return (
        <div style={{ padding: '2rem', minHeight: '100vh', background: '#0f172a', color: '#f8fafc' }}>
          <div style={{ maxWidth: '600px', margin: '0 auto', padding: '2rem', background: '#111b36', borderRadius: '1rem' }}>
            <h1>Something went wrong</h1>
            <p>{this.state.error?.message || 'An unexpected error occurred'}</p>
            <button 
              onClick={() => window.location.reload()}
              style={{ padding: '0.5rem 1rem', marginTop: '1rem', cursor: 'pointer' }}
            >
              Reload Page
            </button>
            <details style={{ marginTop: '1rem' }}>
              <summary>Error Details</summary>
              <pre style={{ overflow: 'auto', background: '#050b19', padding: '1rem', borderRadius: '0.5rem' }}>
                {this.state.error?.stack}
              </pre>
            </details>
          </div>
        </div>
      );
    }

    return this.props.children;
  }
}

