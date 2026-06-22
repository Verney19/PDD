(function () {
    window.PDD.api = window.PDD.api || {};

    window.PDD.api.seckill = {
        grab(activityId) {
            return window.PDD.http.request('/api/seckill/grab', {
                method: 'POST',
                body: JSON.stringify({ activityId })
            });
        }
    };
})();
