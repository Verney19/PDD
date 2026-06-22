(function () {
    const PDD = window.PDD;
    const { escapeHtml, number, money, formatTime, activityStatus, activityTypeLabel, activityTypeClass, displayProductName } = PDD.utils;

    async function render(container) {
        if (!PDD.state.isAuthenticated()) {
            container.innerHTML = PDD.components.loginRequired('登录后即可查看完整活动安排。');
            return;
        }

        const activities = await PDD.api.activities.list();
        PDD.state.data.activities = Array.isArray(activities) ? activities : [];
        if (!PDD.state.data.selectedActivityId && PDD.state.data.activities.length) {
            PDD.state.data.selectedActivityId = PDD.state.data.activities[0].id;
        }

        const selected = PDD.state.data.activities.find((item) => item.id === PDD.state.data.selectedActivityId);

        container.innerHTML = `
            <div class="split-grid">
                <div class="page-grid">
                    <div class="section-title">
                        <div>
                            <h3>活动安排</h3>
                            <p>秒杀和抽奖分时开启，提前看好时间别错过。</p>
                        </div>
                        <div class="actions">
                            <button id="refreshActivities" class="secondary-button">刷新</button>
                        </div>
                    </div>
                    <div class="activity-list">
                        ${renderActivityList()}
                    </div>
                </div>
                <aside class="card">
                    ${renderDetail(selected)}
                </aside>
            </div>
        `;

        container.querySelector('#refreshActivities').addEventListener('click', () => PDD.router.refresh());
        container.onclick = handleAction;
    }

    function renderActivityList() {
        if (!PDD.state.data.activities.length) {
            return PDD.components.empty('暂无活动数据。');
        }
        return PDD.state.data.activities.map((activity) => {
            const status = activityStatus(activity);
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
                        <button class="secondary-button" data-action="detail" data-id="${activity.id}">详情</button>
                        <button data-action="enter" data-type="${activity.type}">进入</button>
                    </div>
                </article>
            `;
        }).join('');
    }

    function renderDetail(activity) {
        if (!activity) {
            return PDD.components.empty('请选择一个活动。');
        }
        const status = activityStatus(activity);
        return `
            <div class="section-title">
                <div>
                    <h3>活动详情</h3>
                    <p>查看时间、价格和剩余名额。</p>
                </div>
            </div>
            <div class="product-list">
                <div class="meta-row"><strong>参与方式</strong><span>${activityTypeLabel(activity.type)}</span></div>
                <div class="meta-row"><strong>活动价格</strong><span>${money(activity.activityPrice)}</span></div>
                <div class="meta-row"><strong>剩余名额</strong><span>${number(activity.availableStock)} / ${number(activity.totalStock)}</span></div>
                <div class="meta-row"><strong>状态</strong><span class="status-pill ${status.className}">${status.label}</span></div>
                <div class="meta-row"><strong>开始时间</strong><span>${formatTime(activity.startTime)}</span></div>
                <div class="meta-row"><strong>结束时间</strong><span>${formatTime(activity.endTime)}</span></div>
                ${PDD.components.progress(activity.availableStock, activity.totalStock)}
            </div>
        `;
    }

    async function handleAction(event) {
        const button = event.target.closest('[data-action]');
        if (!button) {
            return;
        }
        const id = Number(button.dataset.id);
        if (button.dataset.action === 'detail') {
            const activity = await PDD.api.activities.detail(id);
            PDD.state.data.selectedActivityId = activity.id;
            const index = PDD.state.data.activities.findIndex((item) => item.id === activity.id);
            if (index >= 0) {
                PDD.state.data.activities[index] = activity;
            }
            PDD.router.refresh();
            return;
        }
        if (button.dataset.action === 'enter') {
            PDD.router.go(button.dataset.type === 'SECKILL' ? 'seckill' : 'lottery');
        }
    }

    PDD.router.register('activities', { render });
})();
