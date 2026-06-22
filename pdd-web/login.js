(function () {
    const PDD = window.PDD;
    const { qs, qsa, showToast } = PDD.utils;

    function init() {
        qs('#baseUrl').value = PDD.state.data.baseUrl;
        qs('#loginBtn').addEventListener('click', () => submit(false));
        qs('#registerBtn').addEventListener('click', () => submit(true));
        qs('#password').addEventListener('keydown', (event) => {
            if (event.key === 'Enter') {
                submit(false);
            }
        });
        qsa('[data-account]').forEach((button) => {
            button.addEventListener('click', () => fillAccount(button.dataset.account));
        });
    }

    function fillAccount(account) {
        qs('#username').value = account;
        qs('#password').value = '123456';
    }

    async function submit(register) {
        const baseUrl = qs('#baseUrl').value.trim();
        const username = qs('#username').value.trim();
        const password = qs('#password').value.trim();
        if (!username || !password) {
            showToast('请输入用户名和密码', 'error');
            return;
        }

        PDD.state.saveBaseUrl(baseUrl);
        const result = register
            ? await PDD.api.auth.register({ username, password })
            : await PDD.api.auth.login({ username, password });

        PDD.state.setSession(result);
        showToast(register ? '注册成功，正在进入控制台' : '登录成功，正在进入控制台', 'success');
        window.setTimeout(() => {
            window.location.href = './index.html';
        }, 500);
    }

    document.addEventListener('DOMContentLoaded', init);
})();
