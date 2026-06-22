(function () {
    window.PDD = window.PDD || {};

    function qs(selector, root) {
        return (root || document).querySelector(selector);
    }

    function qsa(selector, root) {
        return Array.from((root || document).querySelectorAll(selector));
    }

    function safeJson(value) {
        try {
            return value ? JSON.parse(value) : null;
        } catch (error) {
            return null;
        }
    }

    function escapeHtml(value) {
        return String(value ?? '')
            .replace(/&/g, '&amp;')
            .replace(/</g, '&lt;')
            .replace(/>/g, '&gt;')
            .replace(/"/g, '&quot;')
            .replace(/'/g, '&#39;');
    }

    function money(value) {
        const amount = Number(value || 0);
        return amount === 0 ? '免费' : `¥${amount.toFixed(2)}`;
    }

    function number(value) {
        return Number(value || 0).toLocaleString('zh-CN');
    }

    function formatTime(value) {
        if (!value) {
            return '-';
        }
        return String(value).replace('T', ' ');
    }

    function activityTypeLabel(type) {
        return type === 'SECKILL' ? '秒杀' : '抽奖';
    }

    function activityTypeClass(type) {
        return type === 'SECKILL' ? 'seckill' : 'lottery';
    }

    function orderSourceLabel(source) {
        if (source === 'SECKILL') {
            return '秒杀';
        }
        if (source === 'LOTTERY') {
            return '抽奖';
        }
        return source || '-';
    }

    function orderStatusLabel(status) {
        const labels = {
            CREATED: '已创建',
            PAID: '已支付',
            CANCELED: '已取消'
        };
        return labels[status] || status || '-';
    }

    function displayProductName(value) {
        const text = String(value || '').trim();
        if (!text || /[�ÃÂ]/.test(text)) {
            return 'PDD 618 限量旗舰手机';
        }
        return text;
    }

    function activityStatus(activity) {
        if (!activity || activity.status !== 1) {
            return { label: '已关闭', className: 'closed', disabled: true };
        }
        const now = Date.now();
        const start = new Date(activity.startTime).getTime();
        const end = new Date(activity.endTime).getTime();
        if (Number.isFinite(start) && now < start) {
            return { label: '未开始', className: 'pending', disabled: true };
        }
        if (Number.isFinite(end) && now > end) {
            return { label: '已结束', className: 'ended', disabled: true };
        }
        return { label: '进行中', className: 'live', disabled: false };
    }

    function stockPercent(availableStock, totalStock) {
        const total = Number(totalStock || 0);
        const available = Number(availableStock || 0);
        if (total <= 0) {
            return 0;
        }
        return Math.max(0, Math.min(100, Math.round((available / total) * 100)));
    }

    function countdownParts(startTime) {
        const target = new Date(startTime).getTime();
        const diff = Math.max(0, target - Date.now());
        const day = Math.floor(diff / 86400000);
        const hour = Math.floor((diff % 86400000) / 3600000);
        const minute = Math.floor((diff % 3600000) / 60000);
        const second = Math.floor((diff % 60000) / 1000);
        return { day, hour, minute, second };
    }

    function findActivity(type) {
        return PDD.state.data.activities.find((activity) => activity.type === type);
    }

    function prizeNameFromRequest(requestId) {
        if (!requestId || !requestId.includes(':spin:')) {
            return '-';
        }
        const code = requestId.split(':').pop();
        const prize = PDD.state.data.prizePool.find((item) => item.code === code);
        return prize ? prize.name : code;
    }

    function showToast(message, type) {
        const toast = qs('#toast');
        toast.textContent = message;
        toast.className = `toast ${type || ''}`;
        window.clearTimeout(showToast.timer);
        showToast.timer = window.setTimeout(() => toast.classList.add('hidden'), 3200);
    }

    window.PDD.utils = {
        qs,
        qsa,
        safeJson,
        escapeHtml,
        money,
        number,
        formatTime,
        activityTypeLabel,
        activityTypeClass,
        orderSourceLabel,
        orderStatusLabel,
        displayProductName,
        activityStatus,
        stockPercent,
        countdownParts,
        findActivity,
        prizeNameFromRequest,
        showToast
    };
})();
