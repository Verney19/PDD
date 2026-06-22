(function () {
    window.PDD.api = window.PDD.api || {};

    window.PDD.api.orders = {
        mine() {
            return window.PDD.http.request('/api/orders/mine');
        }
    };
})();
