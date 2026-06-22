(function () {
    window.PDD.api = window.PDD.api || {};

    window.PDD.api.activities = {
        list() {
            return window.PDD.http.request('/api/activities');
        },
        detail(id) {
            return window.PDD.http.request(`/api/activities/${id}`);
        },
        preload(id) {
            return window.PDD.http.request(`/api/activities/${id}/preload`, { method: 'POST' });
        },
        preloadAll() {
            return window.PDD.http.request('/api/activities/preload', { method: 'POST' });
        }
    };
})();
