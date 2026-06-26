(function () {
    window.PDD.api = window.PDD.api || {};

    window.PDD.api.products = {
        list(category) {
            const query = category ? `?category=${encodeURIComponent(category)}` : '';
            return window.PDD.http.request(`/api/products${query}`);
        },
        get(id) {
            return window.PDD.http.request(`/api/products/${id}`);
        },
        create(data) {
            return window.PDD.http.request('/api/products', {
                method: 'POST',
                body: JSON.stringify(data)
            });
        },
        update(id, data) {
            return window.PDD.http.request(`/api/products/${id}`, {
                method: 'PUT',
                body: JSON.stringify(data)
            });
        },
        delete(id) {
            return window.PDD.http.request(`/api/products/${id}`, {
                method: 'DELETE'
            });
        }
    };
})();
