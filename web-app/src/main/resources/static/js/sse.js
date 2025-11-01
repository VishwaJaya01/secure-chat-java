(function () {
    function $(selector) {
        return document.querySelector(selector);
    }

    function appendMessage(feed, message) {
        const wrapper = document.createElement('div');
        wrapper.className = 'message' + (message.mine ? ' message-mine' : '');

        const meta = document.createElement('small');
        meta.className = 'text-muted d-block';
        const time = new Date(message.timestamp || Date.now());
        meta.textContent = time.toLocaleTimeString([], {hour: '2-digit', minute: '2-digit'});

        const author = document.createElement('strong');
        author.textContent = message.user;

        const body = document.createElement('div');
        body.textContent = message.text;

        wrapper.appendChild(meta);
        wrapper.appendChild(author);
        wrapper.appendChild(body);
        feed.appendChild(wrapper);
        feed.scrollTop = feed.scrollHeight;
    }

    function badge(state) {
        const el = $('#connection-badge');
        if (!el) {
            return;
        }
        const classes = {
            connected: 'bg-success',
            reconnecting: 'bg-warning text-dark',
            disconnected: 'bg-danger'
        };
        el.className = 'badge position-fixed bottom-0 start-0 m-3 ' + (classes[state] || 'bg-secondary');
        el.textContent = state.charAt(0).toUpperCase() + state.slice(1);
    }

    document.addEventListener('DOMContentLoaded', function () {
        if (!window.securechat) {
            return;
        }
        const feed = $('#feed');
        const composer = $('#composer');
        const input = composer ? composer.querySelector('input[name="text"]') : null;
        const usernameInput = composer ? composer.querySelector('input[name="username"]') : null;

        const source = new EventSource(window.securechat.streamUrl);
        source.addEventListener('open', function () {
            badge('connected');
        });
        source.addEventListener('error', function () {
            if (source.readyState === EventSource.CLOSED) {
                badge('disconnected');
            } else {
                badge('reconnecting');
            }
        });
        source.addEventListener('connected', function () {
            badge('connected');
        });
        source.addEventListener('message', function (event) {
            try {
                const payload = JSON.parse(event.data);
                appendMessage(feed, payload);
            } catch (err) {
                console.warn('Unable to parse SSE payload', err);
            }
        });

        if (composer && input && usernameInput) {
            composer.addEventListener('submit', function (evt) {
                evt.preventDefault();
                const text = input.value.trim();
                if (!text) {
                    return;
                }
                const payload = new URLSearchParams();
                payload.append('username', usernameInput.value);
                payload.append('text', text);
                fetch(window.securechat.sendUrl, {
                    method: 'POST',
                    body: payload,
                    headers: {
                        'Accept': 'application/json'
                    }
                }).then(function (response) {
                    if (!response.ok && response.status !== 204) {
                        console.warn('Send failed', response.status);
                    }
                    input.value = '';
                }).catch(function (err) {
                    console.error('Send failed', err);
                    badge('reconnecting');
                });
            });
        }
    });
})();
