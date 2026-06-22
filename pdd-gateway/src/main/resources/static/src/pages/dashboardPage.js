(function () {
    const PDD = window.PDD;
    const { metric } = PDD.components;
    const { escapeHtml, number, money, activityStatus, activityTypeLabel, activityTypeClass, formatTime, displayProductName } = PDD.utils;

    async function loadData() {
        const [products, activities] = await Promise.all([
            PDD.api.products.list(),
            PDD.api.activities.list()
        ]);
        PDD.state.data.products = Array.isArray(products) ? products : [];
        PDD.state.data.activities = Array.isArray(activities) ? activities : [];
    }

    async function render(container) {
        if (!PDD.state.isAuthenticated()) {
            container.innerHTML = PDD.components.loginRequired('请先登录，随后可以查看商品、活动、秒杀、抽奖和订单。');
            return;
        }
        await loadData();

        const product = PDD.state.data.products[0] || {};
        const seckill = PDD.utils.findActivity('SECKILL');
        const lottery = PDD.utils.findActivity('LOTTERY');
        const totalLeft = Number(seckill?.availableStock || 0) + Number(lottery?.availableStock || 0);

        container.innerHTML = `
            <div class="page-grid">
                <section class="campaign-hero">
                    <div class="hero-copy">
                        <span class="hero-kicker">PDD 618 限量手机专场</span>
                        <h3>${escapeHtml(displayProductName(product.name))}</h3>
                        <p>秒杀和抽奖双通道发放，登录后即可查看当前名额并参与活动。</p>
                        <div class="hero-actions">
                            <button data-route="seckill">去秒杀</button>
                            <button class="secondary-button light-button" data-route="lottery">试试手气</button>
                        </div>
                    </div>
                    <div class="hero-phone">
                        <div class="phone-frame">
                            <div class="phone-camera"></div>
                            <div class="phone-screen">618</div>
                        </div>
                    </div>
                </section>

                <div class="metrics">
                    ${metric('本场总量', number(product.totalStock || 100000), '限量发放，先到先得')}
                    ${metric('秒杀名额', number(seckill?.availableStock || 0), '20:00 准点开抢')}
                    ${metric('抽奖名额', number(lottery?.availableStock || 0), '20:45 幸运开启')}
                    ${metric('剩余总名额', number(totalLeft), '实时更新活动余量')}
                </div>

                <div class="section-title">
                    <div>
                        <h3>今日活动</h3>
                        <p>两种参与方式，选择适合你的入场通道。</p>
                    </div>
                    <div class="actions">
                        <button id="dashboardRefresh" class="secondary-button">刷新</button>
                    </div>
                </div>

                <div class="activity-list">
                    ${renderActivityCards()}
                </div>
            </div>
        `;

        container.querySelector('#dashboardRefresh').addEventListener('click', () => PDD.router.refresh());
    }

    function renderActivityCards() {
        if (!PDD.state.data.activities.length) {
            return PDD.components.empty('当前暂无活动，请稍后再来。');
        }
        return PDD.state.data.activities.map((activity) => {
            const status = activityStatus(activity);
            const route = activity.type === 'SECKILL' ? 'seckill' : 'lottery';
            const actionText = activity.type === 'SECKILL' ? '进入秒杀' : '参与抽奖';
            return `
                <article class="activity-card customer-activity-card">
                    <div>
                        <div class="activity-title">
                            <h4>${escapeHtml(displayProductName(activity.productName))}</h4>
                            <span class="status-pill ${status.className}">${status.label}</span>
                        </div>
                        <div class="activity-meta">
                            <span class="badge ${activityTypeClass(activity.type)}">${activityTypeLabel(activity.type)}</span>
                            <span>${money(activity.activityPrice)}</span>
                            <span>剩余 ${number(activity.availableStock)} 份</span>
                            <span>${formatTime(activity.startTime)} 开始</span>
                        </div>
                        ${PDD.components.progress(activity.availableStock, activity.totalStock)}
                    </div>
                    <div class="actions">
                        <button data-route="${route}">${actionText}</button>
                    </div>
                </article>
            `;
        }).join('');
    }

    document.addEventListener('click', (event) => {
        const target = event.target.closest('[data-route]');
        if (target && target.closest('#appView')) {
            PDD.router.go(target.dataset.route);
        }
    });

    PDD.router.register('dashboard', { render });
})();
