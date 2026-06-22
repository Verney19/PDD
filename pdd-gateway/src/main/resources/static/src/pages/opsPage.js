(function () {
    const PDD = window.PDD;
    const { escapeHtml } = PDD.utils;

    async function render(container) {
        container.innerHTML = `
            <div class="page-grid">
                <div class="section-title">
                    <div>
                        <h3>活动准备</h3>
                        <p>这里用于演示环境准备和活动名额刷新，正式面向客户时可隐藏。</p>
                    </div>
                    <div class="actions">
                        <button id="healthCheck" class="secondary-button">检查服务</button>
                        <button id="opsPreload" ${PDD.state.isAuthenticated() ? '' : 'disabled'}>预热全部库存</button>
                    </div>
                </div>

                <div class="command-list">
                    ${PDD.constants.scripts.map(PDD.components.commandCard).join('')}
                </div>

                <div class="card-grid">
                    <article class="card">
                        <h3>当前服务</h3>
                        <p>${escapeHtml(PDD.state.data.baseUrl)}</p>
                    </article>
                    <article class="card">
                        <h3>登录状态</h3>
                        <p>${PDD.state.isAuthenticated() ? escapeHtml(PDD.state.data.user.username) : '未登录'}</p>
                    </article>
                    <article class="card">
                        <h3>健康检查</h3>
                        <p id="healthResult">${renderHealth()}</p>
                    </article>
                </div>
            </div>
        `;

        container.querySelector('#healthCheck').addEventListener('click', healthCheck);
        container.querySelector('#opsPreload').addEventListener('click', preloadAll);
    }

    function renderHealth() {
        if (!PDD.state.data.lastHealth) {
            return '尚未检查';
        }
        return PDD.state.data.lastHealth.status || JSON.stringify(PDD.state.data.lastHealth);
    }

    async function healthCheck() {
        const result = await PDD.http.raw('/actuator/health');
        PDD.state.data.lastHealth = result;
        PDD.utils.showToast(`服务状态：${renderHealth()}`, 'success');
        PDD.router.refresh();
    }

    async function preloadAll() {
        await PDD.api.activities.preloadAll();
        PDD.utils.showToast('活动名额已刷新', 'success');
    }

    PDD.router.register('ops', { render });
})();
