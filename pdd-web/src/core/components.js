(function () {
    const { escapeHtml, number } = window.PDD.utils;

    function loading(text) {
        return `<div class="loading">${escapeHtml(text || '加载中...')}</div>`;
    }

    function empty(text) {
        return `<div class="empty-state">${escapeHtml(text)}</div>`;
    }

    function loginRequired(text) {
        return `
            <div class="empty-state">
                <p>${escapeHtml(text || '请先登录后继续操作。')}</p>
                <div class="actions centered-actions">
                    <a class="link-button" href="./login.html">前往登录页</a>
                </div>
            </div>
        `;
    }

    function metric(label, value, help) {
        return `
            <div class="metric">
                <span>${escapeHtml(label)}</span>
                <strong>${escapeHtml(value)}</strong>
                ${help ? `<small>${escapeHtml(help)}</small>` : ''}
            </div>
        `;
    }

    function progress(available, total) {
        const percent = window.PDD.utils.stockPercent(available, total);
        return `
            <div class="progress">
                <div class="progress-track">
                    <div class="progress-bar" style="width: ${percent}%"></div>
                </div>
                <div class="progress-label">
                    <span>剩余名额 ${number(available)}</span>
                    <span>总名额 ${number(total)}</span>
                </div>
            </div>
        `;
    }

    function commandCard(item) {
        return `
            <article class="command-card">
                <div class="product-title">
                    <h4>${escapeHtml(item.name)}</h4>
                    <span class="badge service">PowerShell</span>
                </div>
                <p class="command-help">${escapeHtml(item.purpose)}</p>
                <pre><code>${escapeHtml(item.command)}</code></pre>
            </article>
        `;
    }

    window.PDD.components = {
        loading,
        empty,
        loginRequired,
        metric,
        progress,
        commandCard
    };
})();
