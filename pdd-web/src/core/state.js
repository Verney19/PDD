(function () {
    const { safeJson } = window.PDD.utils;
    const constants = window.PDD.constants;

    const data = {
        baseUrl: localStorage.getItem('pdd.baseUrl') || 'http://localhost:8080',
        token: localStorage.getItem('pdd.token') || '',
        user: safeJson(localStorage.getItem('pdd.user')),
        currentRoute: 'dashboard',
        selectedActivityId: null,
        selectedLotteryActivityId: null,
        products: [],
        activities: [],
        orders: [],
        prizePool: constants.defaultPrizePool.slice(),
        boardCursor: -1,
        wheelRotation: 0,
        winnerFeed: null,
        winnerFeedTimer: null,
        latestSeckill: null,
        latestLottery: null,
        lastHealth: null
    };

    if (data.token && !data.user) {
        clearSession();
    }

    function isAuthenticated() {
        return Boolean(data.token && data.user);
    }

    function setSession(payload) {
        data.token = payload.token;
        data.user = {
            userId: payload.userId,
            username: payload.username,
            role: payload.role
        };
        localStorage.setItem('pdd.token', data.token);
        localStorage.setItem('pdd.user', JSON.stringify(data.user));
        document.dispatchEvent(new CustomEvent('pdd:session-changed'));
    }

    function clearSession() {
        data.token = '';
        data.user = null;
        data.products = [];
        data.activities = [];
        data.orders = [];
        localStorage.removeItem('pdd.token');
        localStorage.removeItem('pdd.user');
        document.dispatchEvent(new CustomEvent('pdd:session-changed'));
    }

    function saveBaseUrl(value) {
        data.baseUrl = String(value || 'http://localhost:8080').replace(/\/$/, '');
        localStorage.setItem('pdd.baseUrl', data.baseUrl);
    }

    window.PDD.state = {
        data,
        isAuthenticated,
        setSession,
        clearSession,
        saveBaseUrl
    };
})();
