(function () {
    const PDD = window.PDD;
    const { escapeHtml, number, money, formatTime, activityStatus, countdownParts, displayProductName } = PDD.utils;

    async function render(container) {
        if (!PDD.state.isAuthenticated()) {
            container.innerHTML = PDD.components.loginRequired('登录后即可参与本场秒杀。');
            return;
        }

        const activities = await PDD.api.activities.list();
        PDD.state.data.activities = Array.isArray(activities) ? activities : [];
        const activity = PDD.utils.findActivity('SECKILL');

        if (!activity) {
            container.innerHTML = PDD.components.empty('没有找到秒杀活动。');
            return;
        }

        const status = activityStatus(activity);
        const parts = countdownParts(activity.startTime);

        container.innerHTML = `
            <div class="seckill-panel">
                <section class="card page-grid">
                    <div class="section-title">
                        <div>
                            <h3>${escapeHtml(displayProductName(activity.productName))}</h3>
                            <p>每个账号限购 1 台，名额有限，开抢后请尽快提交。</p>
                        </div>
                        <span class="status-pill ${status.className}">${status.label}</span>
                    </div>
                    <div class="metrics">
                        ${PDD.components.metric('秒杀价', money(activity.activityPrice), '618 专享到手价')}
                        ${PDD.components.metric('剩余名额', number(activity.availableStock), `共 ${number(activity.totalStock)} 台`)}
                        ${PDD.components.metric('开始时间', formatTime(activity.startTime), '准点开启')}
                        ${PDD.components.metric('结束时间', formatTime(activity.endTime), '秒杀结束后不可下单')}
                    </div>
                    ${PDD.components.progress(activity.availableStock, activity.totalStock)}
                    <div class="actions">
                        <button id="grabSeckill" ${status.disabled ? 'disabled' : ''}>立即秒杀</button>
                        <button id="refreshSeckill" class="secondary-button">刷新</button>
                    </div>
                    ${renderLatest()}
                </section>
                <aside class="card page-grid">
                    <div class="section-title">
                        <div>
                            <h3>开抢倒计时</h3>
                            <p>请提前登录，开抢后按钮会自动可用。</p>
                        </div>
                    </div>
                    <div class="countdown-grid">
                        <div class="countdown-item"><strong>${parts.day}</strong><span>天</span></div>
                        <div class="countdown-item"><strong>${parts.hour}</strong><span>时</span></div>
                        <div class="countdown-item"><strong>${parts.minute}</strong><span>分</span></div>
                        <div class="countdown-item"><strong>${parts.second}</strong><span>秒</span></div>
                    </div>
                    <div class="command-card">
                        <h4>参与提示</h4>
                        <p class="command-help">提交成功后请稍等片刻，订单会自动出现在“我的订单”。重复点击不会增加名额。</p>
                    </div>
                </aside>
            </div>
        `;

        container.querySelector('#grabSeckill').addEventListener('click', () => grab(activity.id));
        container.querySelector('#refreshSeckill').addEventListener('click', () => PDD.router.refresh());
    }

    function renderLatest() {
        const latest = PDD.state.data.latestSeckill;
        if (!latest) {
            return '<div class="result-box">提交后请稍等，成功结果会在这里提示。</div>';
        }
        return `
            <div class="result-box">
                抢购请求已提交，当前剩余约 ${number(latest.remainingStock)} 台。请稍后到“我的订单”查看结果。
            </div>
        `;
    }

    async function grab(activityId) {
        const result = await PDD.api.seckill.grab(activityId);
        PDD.state.data.latestSeckill = result;
        PDD.utils.showToast('抢购请求已提交，请稍后查看订单', 'success');
        await PDD.router.refresh();
    }

    PDD.router.register('seckill', { render });
})();
