(function () {
    const PDD = window.PDD;
    const { showToast } = PDD.utils;

    async function request(path, options) {
        const opts = options || {};
        const headers = Object.assign({}, opts.headers || {});
        if (opts.body && !headers['Content-Type']) {
            headers['Content-Type'] = 'application/json';
        }
        if (!opts.anonymous && PDD.state.data.token) {
            headers.Authorization = `Bearer ${PDD.state.data.token}`;
        }

        try {
            const response = await fetch(`${PDD.state.data.baseUrl}${path}`, {
                method: opts.method || 'GET',
                headers,
                body: opts.body
            });
            const text = await response.text();
            const payload = text ? JSON.parse(text) : {};
            if (!response.ok || payload.code !== 0) {
                if (response.status === 401) {
                    PDD.state.clearSession();
                }
                throw new Error(payload.message || `HTTP ${response.status}`);
            }
            return payload.data;
        } catch (error) {
            if (!opts.silent) {
                showToast(error.message || '请求失败', 'error');
            }
            throw error;
        }
    }

    async function raw(path, options) {
        const opts = options || {};
        const response = await fetch(`${PDD.state.data.baseUrl}${path}`, {
            method: opts.method || 'GET',
            headers: opts.headers || {}
        });
        const text = await response.text();
        try {
            return text ? JSON.parse(text) : null;
        } catch (error) {
            return text;
        }
    }

    window.PDD.http = {
        request,
        raw
    };
})();
