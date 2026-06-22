(function () {
    const PDD = window.PDD;
    const pages = {};

    function register(route, page) {
        pages[route] = page;
    }

    function init() {
        PDD.utils.qsa('.nav-item').forEach((button) => {
            button.addEventListener('click', () => go(button.dataset.route));
        });
        window.addEventListener('hashchange', renderFromHash);
        renderFromHash();
    }

    function go(route) {
        const nextHash = `#${route}`;
        if (window.location.hash === nextHash) {
            render(route);
            return;
        }
        window.location.hash = nextHash;
    }

    function renderFromHash() {
        const route = window.location.hash.replace('#', '') || 'dashboard';
        render(pages[route] ? route : 'dashboard');
    }

    async function render(route) {
        const meta = PDD.constants.routes[route] || PDD.constants.routes.dashboard;
        PDD.state.data.currentRoute = route;
        PDD.utils.qs('#pageTitle').textContent = meta.title;
        PDD.utils.qs('#pageSubtitle').textContent = meta.subtitle;
        PDD.utils.qsa('.nav-item').forEach((item) => {
            item.classList.toggle('active', item.dataset.route === route);
        });

        const container = PDD.utils.qs('#appView');
        container.onclick = null;
        container.innerHTML = PDD.components.loading();
        try {
            await pages[route].render(container);
        } catch (error) {
            container.innerHTML = PDD.components.empty(error.message || '页面加载失败');
        }
    }

    function refresh() {
        return render(PDD.state.data.currentRoute);
    }

    window.PDD.router = {
        register,
        init,
        go,
        refresh
    };
})();
