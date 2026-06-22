(function () {
    window.PDD.api = window.PDD.api || {};

    window.PDD.api.auth = {
        login(payload) {
            return window.PDD.http.request('/api/auth/login', {
                method: 'POST',
                body: JSON.stringify(payload),
                anonymous: true
            });
        },
        register(payload) {
            return window.PDD.http.request('/api/auth/register', {
                method: 'POST',
                body: JSON.stringify(payload),
                anonymous: true
            });
        }
    };
})();
