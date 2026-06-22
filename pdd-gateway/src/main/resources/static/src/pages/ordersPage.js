(function () {
    const PDD = window.PDD;
    const { escapeHtml, money, formatTime, orderSourceLabel, orderStatusLabel, prizeNameFromRequest } = PDD.utils;

    async function render(container) {
        if (!PDD.state.isAuthenticated()) {
            container.innerHTML = PDD.components.loginRequired('请登录后查看当前用户订单。');
            return;
        }

        const orders = await PDD.api.orders.mine();
        PDD.state.data.orders = Array.isArray(orders) ? orders : [];

        container.innerHTML = `
            <div class="section-title">
                <div>
                    <h3>我的订单</h3>
                    <p>秒杀成功或抽奖中奖后，订单会自动出现在这里。</p>
                </div>
                <button id="reloadOrders" class="secondary-button">刷新订单</button>
            </div>
            ${renderOrders()}
        `;
        container.querySelector('#reloadOrders').addEventListener('click', () => PDD.router.refresh());
    }

    function renderOrders() {
        if (!PDD.state.data.orders.length) {
            return PDD.components.empty('暂无订单，秒杀排队成功或抽奖中奖后稍等几秒刷新。');
        }
        return `
            <div class="table-wrap">
                <table>
                    <thead>
                    <tr>
                        <th>订单号</th>
                        <th>来源</th>
                        <th>奖品</th>
                        <th>数量</th>
                        <th>金额</th>
                        <th>状态</th>
                        <th>创建时间</th>
                    </tr>
                    </thead>
                    <tbody>
                    ${PDD.state.data.orders.map((order) => `
                        <tr>
                            <td>${escapeHtml(order.id)}</td>
                            <td>${orderSourceLabel(order.source)}</td>
                            <td>${escapeHtml(prizeNameFromRequest(order.requestId))}</td>
                            <td>${escapeHtml(order.quantity || 1)}</td>
                            <td>${money(order.amount)}</td>
                            <td>${orderStatusLabel(order.status)}</td>
                            <td>${formatTime(order.createdAt)}</td>
                        </tr>
                    `).join('')}
                    </tbody>
                </table>
            </div>
        `;
    }

    PDD.router.register('orders', { render });
})();
