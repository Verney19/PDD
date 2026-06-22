(function () {
    window.PDD.api = window.PDD.api || {};

    window.PDD.api.lottery = {
        join(activityId) {
            return window.PDD.http.request('/api/lottery/join', {
                method: 'POST',
                body: JSON.stringify({ activityId })
            });
        },
        spin(activityId) {
            return window.PDD.http.request('/api/lottery/spin', {
                method: 'POST',
                body: JSON.stringify({ activityId })
            });
        },
        prizes(activityId) {
            const query = activityId ? `?activityId=${encodeURIComponent(activityId)}` : '';
            return window.PDD.http.request(`/api/lottery/prizes${query}`);
        },
        draw(activityId) {
            return window.PDD.http.request('/api/lottery/draw', {
                method: 'POST',
                body: JSON.stringify({ activityId })
            });
        },
        winner(activityId) {
            return window.PDD.http.request(`/api/lottery/${activityId}/winner`);
        },
        winnerFeed(activityId) {
            return window.PDD.http.request(`/api/lottery/${activityId}/winner-feed`);
        }
    };
})();
