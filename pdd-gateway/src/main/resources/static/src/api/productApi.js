(function () {
    window.PDD.api = window.PDD.api || {};

    window.PDD.api.products = {
        list() {
            return window.PDD.http.request('/api/products');
        }
    };
})();
