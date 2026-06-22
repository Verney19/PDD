(function () {
    const PDD = window.PDD;
    const { qs, showToast } = PDD.utils;

    function bindShellEvents() {
        qs('#loginPageBtn').addEventListener('click', () => {
            window.location.href = './login.html';
        });
        qs('#logoutBtn').addEventListener('click', logout);

        document.addEventListener('pdd:session-changed', renderSession);
    }

    function logout() {
        PDD.state.clearSession();
        renderSession();
        showToast('已退出登录', 'success');
        PDD.router.refresh();
    }

    function renderSession() {
        const state = PDD.state.data;
        if (PDD.state.isAuthenticated()) {
            qs('#loginState').textContent = `${state.user.username} · ${state.user.role}`;
            qs('#loginPageBtn').classList.add('hidden');
            qs('#logoutBtn').classList.remove('hidden');
        } else {
            qs('#loginState').textContent = '未登录';
            qs('#loginPageBtn').classList.remove('hidden');
            qs('#logoutBtn').classList.add('hidden');
        }
    }

    function init() {
        bindShellEvents();
        renderSession();
        PDD.router.init();
    }

    document.addEventListener('DOMContentLoaded', init);
})();
