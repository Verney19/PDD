(function () {
    const PDD = window.PDD;
    const { escapeHtml } = PDD.utils;

    function render(container) {
        container.innerHTML = `
            <div class="page-grid">
                <div class="section-title">
                    <div>
                        <h3>活动能力</h3>
                        <p>这里用于内部说明活动能力，正式面向客户时可隐藏。</p>
                    </div>
                </div>

                <div class="system-grid card-grid">
                    ${PDD.constants.services.map(renderService).join('')}
                </div>

                <div class="split-grid">
                    <section class="card page-grid">
                        <div class="section-title">
                            <div>
                                <h3>秒杀链路</h3>
                                <p>快速提交参与请求，成功后自动生成订单。</p>
                            </div>
                        </div>
                        <div class="flow-list">
                            ${renderFlow(['登录确认身份', '进入秒杀专场', '提交抢购请求', '等待订单生成', '在我的订单查看结果'])}
                        </div>
                    </section>
                    <section class="card page-grid">
                        <div class="section-title">
                            <div>
                                <h3>抽奖链路</h3>
                                <p>参与抽奖后即可查看结果，中奖订单自动生成。</p>
                            </div>
                        </div>
                        <div class="flow-list">
                            ${renderFlow(['登录参与活动', '进入幸运棋盘', '抽取奖品结果', '中奖后生成订单', '在我的订单查看结果'])}
                        </div>
                    </section>
                </div>
            </div>
        `;
    }

    function renderService(service) {
        return `
            <article class="system-node">
                <div class="product-title">
                    <strong>${escapeHtml(service.name)}</strong>
                    <span class="badge service">${escapeHtml(service.tag)}</span>
                </div>
                <div class="meta-row">
                    <span>服务正常</span>
                </div>
                <p class="muted">${escapeHtml(service.detail)}</p>
            </article>
        `;
    }

    function renderFlow(steps) {
        return steps.map((step, index) => `
            <div class="flow-step">
                <span class="step-index">${index + 1}</span>
                <div>
                    <strong>${escapeHtml(step)}</strong>
                </div>
            </div>
        `).join('');
    }

    PDD.router.register('topology', { render });
})();
