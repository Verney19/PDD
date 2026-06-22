const defaultPrizePool = [
    { code: 'IPHONE_17_PLUS', name: 'iPhone 17 Plus', level: '一等奖', weight: 50, stock: 10, winning: true },
    { code: 'REDMI_K100_PRO', name: '红米 K100 Pro', level: '二等奖', weight: 450, stock: 100, winning: true },
    { code: 'COUPON_1000', name: '1000 元代金券', level: '三等奖', weight: 4500, stock: 1000, winning: true },
    { code: 'PARTICIPATION', name: '参与奖', level: '参与奖', weight: 5000, stock: null, winning: false },
];

const state = {
    baseUrl: localStorage.getItem('pdd.baseUrl') || 'http://localhost:8080',
    token: localStorage.getItem('pdd.token') || '',
    user: safeJson(localStorage.getItem('pdd.user')),
    activities: [],
    prizePool: defaultPrizePool,
    boardCursor: -1,
    wheelRotation: 0,
    selectedLotteryActivityId: null,
    latestPrizeIndex: -1,
    latestAnnouncement: '',
    winnerFeed: [],
    winnerFeedTimer: null,
};

if (state.token && !state.user) {
    clearSession();
}

const ids = {
    baseUrl: document.querySelector('#baseUrl'),
    saveBaseUrl: document.querySelector('#saveBaseUrl'),
    username: document.querySelector('#username'),
    password: document.querySelector('#password'),
    loginBtn: document.querySelector('#loginBtn'),
    registerBtn: document.querySelector('#registerBtn'),
    logoutBtn: document.querySelector('#logoutBtn'),
    loginState: document.querySelector('#loginState'),
    authPanel: document.querySelector('#authPanel'),
    activityList: document.querySelector('#activityList'),
    refreshActivitiesBtn: document.querySelector('#refreshActivitiesBtn'),
    refreshOrdersBtn: document.querySelector('#refreshOrdersBtn'),
    orderList: document.querySelector('#orderList'),
    preloadBtn: document.querySelector('#preloadBtn'),
    drawBtn: document.querySelector('#drawBtn'),
    winnerBtn: document.querySelector('#winnerBtn'),
    lotteryBoard: document.querySelector('#lotteryBoard'),
    spinBtn: document.querySelector('#spinBtn'),
    lotteryResult: document.querySelector('#lotteryResult'),
    winnerTicker: document.querySelector('#winnerTicker'),
    prizeList: document.querySelector('#prizeList'),
    toast: document.querySelector('#toast'),
};

ids.baseUrl.value = state.baseUrl;
renderLoginState();
bindEvents();
renderPrizePool();
loadActivities();
openInitialView();
window.addEventListener('resize', () => renderBoard());

function bindEvents() {
    document.querySelectorAll('.nav-item').forEach((button) => {
        button.addEventListener('click', () => switchView(button.dataset.view));
    });

    ids.saveBaseUrl.addEventListener('click', () => {
        state.baseUrl = ids.baseUrl.value.replace(/\/$/, '');
        localStorage.setItem('pdd.baseUrl', state.baseUrl);
        showToast('网关地址已保存', 'success');
    });

    ids.loginBtn.addEventListener('click', () => login(false));
    ids.registerBtn.addEventListener('click', () => login(true));
    ids.logoutBtn.addEventListener('click', logout);
    ids.refreshActivitiesBtn.addEventListener('click', loadActivities);
    ids.refreshOrdersBtn.addEventListener('click', loadOrders);
    ids.preloadBtn.addEventListener('click', preloadStock);
    ids.drawBtn.addEventListener('click', drawLottery);
    ids.winnerBtn.addEventListener('click', checkWinner);
    if (ids.spinBtn) {
        ids.spinBtn.addEventListener('click', spinLottery);
    }
}

function switchView(view) {
    document.querySelectorAll('.nav-item').forEach((item) => item.classList.toggle('active', item.dataset.view === view));
    document.querySelectorAll('.view').forEach((panel) => panel.classList.remove('active'));
    document.querySelector(`#${view}View`).classList.add('active');
    if (view === 'orders') {
        loadOrders();
    }
    if (view === 'lottery') {
        loadActivities();
        setupWinnerTicker();
    } else {
        window.clearInterval(state.winnerFeedTimer);
    }
}

function openInitialView() {
    const view = window.location.hash.replace('#', '');
    if (['dashboard', 'lottery', 'orders', 'ops'].includes(view)) {
        switchView(view);
    }
}

async function login(register) {
    const path = register ? '/api/auth/register' : '/api/auth/login';
    const payload = {
        username: ids.username.value.trim(),
        password: ids.password.value.trim(),
    };
    const result = await request(path, {
        method: 'POST',
        body: JSON.stringify(payload),
        anonymous: true,
    });
    state.token = result.token;
    state.user = {
        userId: result.userId,
        username: result.username,
        role: result.role,
    };
    localStorage.setItem('pdd.token', state.token);
    localStorage.setItem('pdd.user', JSON.stringify(state.user));
    renderLoginState();
    showToast(`${register ? '注册' : '登录'}成功`, 'success');
    await loadActivities();
}

function logout() {
    state.token = '';
    state.user = null;
    state.activities = [];
    localStorage.removeItem('pdd.token');
    localStorage.removeItem('pdd.user');
    renderLoginState();
    renderActivities();
    ids.lotteryResult.textContent = '登录后可参与 618 棋盘抽奖。';
    showToast('已退出登录', 'success');
}

function renderLoginState() {
    if (state.token && state.user) {
        ids.loginState.textContent = `${state.user.username} · ${state.user.role}`;
        ids.logoutBtn.classList.remove('hidden');
        ids.authPanel.classList.add('hidden');
    } else {
        ids.loginState.textContent = '未登录';
        ids.logoutBtn.classList.add('hidden');
        ids.authPanel.classList.remove('hidden');
    }
}

async function loadActivities() {
    if (!state.token) {
        state.activities = [];
        renderActivities();
        return;
    }
    const list = await request('/api/activities');
    state.activities = Array.isArray(list) ? list : [];
    const lottery = findLotteryActivity();
    if (lottery) {
        state.selectedLotteryActivityId = lottery.id;
    }
    renderActivities();
    await loadPrizePool();
}

async function loadPrizePool() {
    if (!state.token) {
        renderPrizePool();
        return;
    }
    try {
        const activity = findLotteryActivity();
        const query = activity ? `?activityId=${encodeURIComponent(activity.id)}` : '';
        const list = await request(`/api/lottery/prizes${query}`);
        if (Array.isArray(list) && list.length) {
            state.prizePool = list;
            renderPrizePool();
        }
    } catch (error) {
        renderPrizePool();
    }
}

function renderActivities() {
    if (!state.token) {
        ids.activityList.innerHTML = '<div class="activity-card"><div class="activity-meta">请先登录后查看活动列表。</div></div>';
        return;
    }

    if (!state.activities.length) {
        ids.activityList.innerHTML = '<div class="activity-card"><div class="activity-meta">暂无活动数据。请确认 pdd-product-service 已启动并注册到 Nacos。</div></div>';
        return;
    }

    ids.activityList.innerHTML = state.activities.map((activity) => {
        const type = activity.type === 'SECKILL' ? 'seckill' : 'lottery';
        const status = activityStatus(activity);
        const action = activity.type === 'SECKILL'
            ? `<button onclick="grabSeckill(${activity.id})" ${status.disabled ? 'disabled' : ''}>立即秒杀</button>`
            : `<button onclick="openLottery(${activity.id})">进入抽奖</button>`;
        return `
            <article class="activity-card">
                <div>
                    <div class="activity-title">
                        <span class="tag ${type}">${activity.type === 'SECKILL' ? '秒杀' : '抽奖'}</span>
                        <h4>${activity.productName || '活动商品'}</h4>
                        <span class="status-pill ${status.className}">${status.label}</span>
                    </div>
                    <div class="activity-meta">
                        <span>活动ID：${activity.id}</span>
                        <span>价格：${formatAmount(activity.activityPrice)}</span>
                        <span>库存：${activity.availableStock}/${activity.totalStock}</span>
                        <span>开始：${formatTime(activity.startTime)}</span>
                        <span>结束：${formatTime(activity.endTime)}</span>
                    </div>
                </div>
                <div class="activity-actions">
                    ${action}
                    <button class="ghost-button" onclick="preloadOne(${activity.id})">预热库存</button>
                </div>
            </article>
        `;
    }).join('');
}

function renderPrizePool() {
    const totalWeight = state.prizePool.reduce((sum, prize) => sum + Number(prize.weight || 0), 0) || 1;
    renderBoard();

    ids.prizeList.innerHTML = state.prizePool.map((prize, index) => {
        const probability = ((Number(prize.weight || 0) / totalWeight) * 100).toFixed(prize.weight < 100 ? 2 : 1);
        const stockText = prize.stock === null || prize.stock === undefined ? '不限库存' : `限量 ${prize.stock}`;
        return `
            <div class="prize-item">
                <span class="prize-dot prize-dot-${index}"></span>
                <div>
                    <strong>${prize.name}</strong>
                    <span>${prize.level} · 概率 ${probability}% · ${stockText}</span>
                </div>
            </div>
        `;
    }).join('');
}

function renderBoard() {
    const boardRoot = ids.lotteryBoard;
    if (!boardRoot) {
        return;
    }

    const activity = findLotteryActivity();
    if (activity && isPhoneActivity(activity) && !isMacActivity(activity)) {
        renderWheel(boardRoot);
        return;
    }

    const prizes = state.prizePool;
    const grandPrizeIndex = findGrandPrizeIndex(prizes);
    const boardPrizes = prizes
        .map((prize, index) => ({ prize, index }))
        .filter((item) => item.index !== grandPrizeIndex);
    const boardSize = boardPrizes.length > 8 ? 5 : 3;
    const track = buildTrack(boardSize);
    const positions = distributePrizes(boardPrizes.length, track.length);
    const slots = new Map(positions.map((position, index) => [position, boardPrizes[index]]));
    const centerStart = boardSize === 5 ? 2 : 2;
    const centerEnd = boardSize === 5 ? 5 : 3;
    const grandPrize = grandPrizeIndex >= 0 ? prizes[grandPrizeIndex] : null;

    boardRoot.innerHTML = `
        <div class="lottery-board" data-track-length="${track.length}" style="--board-size: ${boardSize};">
            ${track.map((point, position) => {
                const item = slots.get(position);
                return renderBoardCell(item?.prize, item?.index, position, point);
            }).join('')}
            <div class="board-center" style="grid-column: ${centerStart} / ${centerEnd}; grid-row: ${centerStart} / ${centerEnd};">
                ${renderFeaturedPrize(grandPrize, grandPrizeIndex)}
                <button id="spinBtn" class="board-spin-button">开始抽奖</button>
            </div>
        </div>
    `;
    ids.spinBtn = document.querySelector('#spinBtn');
    ids.spinBtn.addEventListener('click', spinLottery);
}

function renderWheel(boardRoot) {
    const prizes = state.prizePool;
    const sliceAngle = 360 / Math.max(1, prizes.length);
    boardRoot.innerHTML = `
        <div class="lottery-wheel-shell">
            <div class="wheel-pointer" aria-hidden="true"></div>
            <div class="lottery-wheel" data-wheel-size="${prizes.length}" style="${wheelGradient(prizes)} transform: rotate(${Number(state.wheelRotation || 0)}deg);">
                <div class="wheel-labels">
                    ${prizes.map((prize, index) => renderWheelSlice(prize, index, sliceAngle)).join('')}
                </div>
            </div>
            <div class="wheel-center">
                <strong>618</strong>
                <span>限量旗舰手机</span>
                <button id="spinBtn" class="wheel-spin-button">开始抽奖</button>
            </div>
        </div>
    `;
    ids.spinBtn = document.querySelector('#spinBtn');
    ids.spinBtn.addEventListener('click', spinLottery);
}

function wheelGradient(prizes) {
    const colors = ['#ef4444', '#f59e0b', '#22c55e', '#2563eb', '#ec4899', '#14b8a6'];
    const slice = 100 / Math.max(1, prizes.length);
    const stops = prizes.map((_, index) => {
        const start = (index * slice).toFixed(4);
        const end = ((index + 1) * slice).toFixed(4);
        return `${colors[index % colors.length]} ${start}% ${end}%`;
    });
    return `background: conic-gradient(from -90deg, ${stops.join(', ')});`;
}

function renderWheelSlice(prize, index, sliceAngle) {
    const selectedClass = index === state.latestPrizeIndex ? 'winner' : '';
    const rotate = index * sliceAngle + sliceAngle / 2;
    return `
        <div class="wheel-slice-label ${selectedClass}"
             data-prize-index="${index}"
             style="--slice-rotate: ${rotate}deg;">
            <span>${prize.level}</span>
            <strong>${compactPrizeName(prize.name)}</strong>
        </div>
    `;
}

function isMacActivity(activity) {
    return /mac|macbook/i.test(String(activity?.productName || ''));
}

function isPhoneActivity(activity) {
    return /手机|旗舰|iphone|phone|pdd\s*618/i.test(String(activity?.productName || ''));
}

function findGrandPrizeIndex(prizes) {
    const index = prizes.findIndex((prize) => /超级大奖|特等奖|大奖/.test(String(prize.level || '')));
    if (index >= 0) {
        return index;
    }
    return prizes.findIndex((prize) => /1\s*折|MacBook Pro/i.test(String(prize.name || '')) && Boolean(prize.winning));
}

function renderFeaturedPrize(prize, prizeIndex) {
    if (!prize) {
        return `
            <div class="featured-prize" data-featured-prize>
                <div class="macbook-visual" aria-hidden="true"><span></span></div>
                <span>本场大奖</span>
                <strong>Mac 幸运棋盘</strong>
                <small>一格一奖，即抽即看</small>
            </div>
        `;
    }

    const selectedClass = prizeIndex === state.latestPrizeIndex ? 'winner' : '';
    const stock = prize.stock === null || prize.stock === undefined ? '不限量' : `限量 ${prize.stock}`;
    return `
        <div class="featured-prize ${selectedClass}" data-featured-prize data-prize-index="${prizeIndex}">
            <div class="macbook-visual" aria-hidden="true"><span></span></div>
            <span>本场大奖</span>
            <strong>${compactPrizeName(prize.name)}</strong>
            <small>${prize.name} · ${stock}</small>
        </div>
    `;
}

function renderBoardCell(prize, prizeIndex, position, point) {
    const style = `grid-column: ${point.column + 1}; grid-row: ${point.row + 1};`;
    if (!prize) {
        return `
            <div class="lottery-board-cell board-filler" data-board-position="${position}" style="${style}">
                <span>好运</span>
                <strong>加速格</strong>
            </div>
        `;
    }

    const selectedClass = prizeIndex === state.latestPrizeIndex ? 'winner' : '';
    const stock = prize.stock === null || prize.stock === undefined ? '不限量' : `限量 ${prize.stock}`;
    return `
        <div class="lottery-board-cell prize-cell prize-cell-${prizeIndex % 6} ${selectedClass}"
             data-board-position="${position}"
             data-prize-index="${prizeIndex}"
             style="${style}">
            <span>${prize.level}</span>
            <strong>${compactPrizeName(prize.name)}</strong>
            <small>${stock}</small>
        </div>
    `;
}

function buildTrack(size) {
    const track = [];
    for (let column = 0; column < size; column++) {
        track.push({ row: 0, column });
    }
    for (let row = 1; row < size; row++) {
        track.push({ row, column: size - 1 });
    }
    for (let column = size - 2; column >= 0; column--) {
        track.push({ row: size - 1, column });
    }
    for (let row = size - 2; row > 0; row--) {
        track.push({ row, column: 0 });
    }
    return track;
}

function distributePrizes(prizeCount, trackLength) {
    const used = new Set();
    return Array.from({ length: prizeCount }, (_, index) => {
        let position = Math.floor((index * trackLength) / prizeCount);
        while (used.has(position)) {
            position = (position + 1) % trackLength;
        }
        used.add(position);
        return position;
    });
}

function compactPrizeName(name) {
    const text = String(name || '');
    const discount = text.match(/(\d)\s*折/);
    if (discount) {
        return `${discount[1]}折资格`;
    }
    const coupon = text.match(/(\d+)\s*元/);
    if (coupon) {
        return `${coupon[1]}元券`;
    }
    if (/爱奇艺/.test(text)) {
        return '爱奇艺年卡';
    }
    if (/优酷/.test(text)) {
        return '优酷年卡';
    }
    if (/咪咕/.test(text)) {
        return '咪咕年卡';
    }
    if (/bilibili|B站/i.test(text)) {
        return 'B站年卡';
    }
    if (/谢谢|参与/.test(text)) {
        return '谢谢参与';
    }
    return text.length > 8 ? `${text.slice(0, 8)}...` : text;
}

async function grabSeckill(activityId) {
    const result = await request('/api/seckill/grab', {
        method: 'POST',
        body: JSON.stringify({ activityId }),
    });
    showToast(`秒杀请求已排队：${result.requestId}`, 'success');
    await loadActivities();
}

async function spinLottery() {
    if (!state.token) {
        showToast('请先登录后再抽奖', 'error');
        return;
    }

    const activity = findLotteryActivity();
    if (!activity) {
        showToast('未找到抽奖活动', 'error');
        return;
    }

    ids.spinBtn.disabled = true;
    ids.lotteryResult.textContent = '抽奖转动中...';
    try {
        const result = await request('/api/lottery/spin', {
            method: 'POST',
            body: JSON.stringify({ activityId: activity.id }),
        });
        if (Array.isArray(result.prizePool) && result.prizePool.length) {
            state.prizePool = result.prizePool;
            renderPrizePool();
        }
        if (document.querySelector('.lottery-wheel')) {
            await spinWheelTo(result.prizeIndex || 0);
        } else {
            await spinBoardTo(result.prizeIndex || 0);
        }
        state.latestPrizeIndex = Number(result.prizeIndex || 0);
        const prizeName = result.prize?.name || '参与奖';
        state.latestAnnouncement = `${state.user?.username || `用户 ${result.userId || ''}`.trim() || '用户'} 抽了 ${prizeName}!`;
        ids.lotteryResult.textContent = result.winner
            ? `恭喜抽中：${prizeName}。订单会异步生成，可到“我的订单”查看。`
            : `${prizeName}，下次继续冲。`;
        showToast(`抽奖结果：${prizeName}`, result.winner ? 'success' : 'error');
        await Promise.all([loadActivities(), loadOrders()]);
    } finally {
        ids.spinBtn.disabled = false;
    }
}

async function spinBoardTo(prizeIndex) {
    const board = document.querySelector('.lottery-board');
    if (!board) {
        return;
    }

    const targetCell = board.querySelector(`[data-prize-index="${Number(prizeIndex)}"]`);
    const targetIsFeatured = targetCell?.hasAttribute('data-featured-prize');
    const targetPosition = targetIsFeatured ? null : Number(targetCell?.dataset.boardPosition || 0);
    const trackLength = Number(board.dataset.trackLength || 1);
    const current = Number.isFinite(state.boardCursor) ? state.boardCursor : -1;
    const normalized = ((current % trackLength) + trackLength) % trackLength;
    const delta = targetIsFeatured ? 0 : (targetPosition - normalized + trackLength) % trackLength;
    const totalSteps = trackLength * 3 + delta;
    let cursor = current;

    board.querySelectorAll('.lottery-board-cell').forEach((cell) => {
        cell.classList.remove('active', 'winner');
    });
    board.querySelectorAll('[data-featured-prize]').forEach((cell) => {
        cell.classList.remove('active', 'winner');
    });

    for (let step = 1; step <= totalSteps; step++) {
        cursor = (cursor + 1) % trackLength;
        setActiveCell(board, cursor, step === totalSteps);
        const progress = step / Math.max(1, totalSteps);
        const delay = 38 + Math.round(150 * progress * progress);
        await new Promise((resolve) => window.setTimeout(resolve, delay));
    }

    state.boardCursor = cursor;
    if (targetIsFeatured && targetCell) {
        board.querySelectorAll('.lottery-board-cell').forEach((cell) => {
            cell.classList.remove('active', 'winner');
        });
        targetCell.classList.add('active', 'winner');
        await new Promise((resolve) => window.setTimeout(resolve, 720));
    }
}

async function spinWheelTo(prizeIndex) {
    const wheel = document.querySelector('.lottery-wheel');
    if (!wheel) {
        return;
    }
    const size = Number(wheel.dataset.wheelSize || state.prizePool.length || 1);
    const slice = 360 / Math.max(1, size);
    const targetAngle = Number(prizeIndex) * slice + slice / 2;
    const current = Number(state.wheelRotation || 0);
    const next = current + 1440 + (360 - ((current + targetAngle) % 360));
    state.wheelRotation = next;
    wheel.style.transform = `rotate(${next}deg)`;
    await new Promise((resolve) => window.setTimeout(resolve, 2600));
}

function setActiveCell(board, position, isWinner) {
    board.querySelectorAll('.lottery-board-cell').forEach((cell) => {
        const active = Number(cell.dataset.boardPosition) === position;
        cell.classList.toggle('active', active);
        cell.classList.toggle('winner', active && isWinner);
    });
}

function openLottery(activityId) {
    state.selectedLotteryActivityId = activityId;
    state.latestPrizeIndex = -1;
    state.boardCursor = -1;
    state.wheelRotation = 0;
    switchView('lottery');
    ids.lotteryResult.textContent = `已选择抽奖活动 ${activityId}，点击中间按钮开始。`;
}

async function joinLottery(activityId) {
    const result = await request('/api/lottery/join', {
        method: 'POST',
        body: JSON.stringify({ activityId }),
    });
    showToast(`报名成功，当前报名人数：${result.participantCount}`, 'success');
}

async function preloadOne(activityId) {
    await request(`/api/activities/${activityId}/preload`, { method: 'POST' });
    showToast('库存预热完成', 'success');
}

async function preloadStock() {
    await request('/api/activities/preload', { method: 'POST' });
    showToast('全部活动库存已预热到 Redis', 'success');
}

async function drawLottery() {
    const activity = findLotteryActivity();
    if (!activity) {
        showToast('未找到抽奖活动', 'error');
        return;
    }
    const result = await request('/api/lottery/draw', {
        method: 'POST',
        body: JSON.stringify({ activityId: activity.id }),
    });
    showToast(`开奖任务已处理，中奖数：${result.winnerCount}`, 'success');
}

async function checkWinner() {
    const activity = findLotteryActivity();
    if (!activity) {
        showToast('未找到抽奖活动', 'error');
        return;
    }
    const winner = await request(`/api/lottery/${activity.id}/winner`);
    showToast(winner ? '恭喜，你已中奖' : '暂未中奖', winner ? 'success' : 'error');
}

function setupWinnerTicker() {
    window.clearInterval(state.winnerFeedTimer);
    refreshWinnerFeed();
    state.winnerFeedTimer = window.setInterval(refreshWinnerFeed, 3000);
}

async function refreshWinnerFeed() {
    const activity = findLotteryActivity();
    const list = document.querySelector('[data-winner-feed]');
    if (!activity || !list || !state.token) {
        if (list) {
            list.innerHTML = renderWinnerFeedRows([]);
        }
        return;
    }
    try {
        const entries = await request(`/api/lottery/${activity.id}/winner-feed`);
        state.winnerFeed = Array.isArray(entries) ? entries : [];
        list.innerHTML = renderWinnerFeedRows(state.winnerFeed);
    } catch (error) {
        list.innerHTML = renderWinnerFeedRows(state.winnerFeed || []);
    }
}

function renderWinnerFeedRows(entries) {
    if (!entries.length) {
        return '<div class="winner-feed-empty">暂无高等级中奖播报</div>';
    }
    return entries.map((entry, index) => `
        <div class="winner-feed-row ${index === 0 ? 'latest' : ''}">
            <span class="winner-feed-level">${entry.level || '大奖'}</span>
            <strong>${entry.userLabel || '系统用户'} 抽中了 ${entry.prizeName || '大奖'}!</strong>
            <small>${formatFeedTime(entry.winTime, index)}</small>
        </div>
    `).join('');
}

function formatFeedTime(value, index) {
    if (!value) {
        return index === 0 ? '刚刚' : `${index * 8} 秒前`;
    }
    const time = new Date(value).getTime();
    if (!Number.isFinite(time)) {
        return '刚刚';
    }
    const seconds = Math.max(0, Math.round((Date.now() - time) / 1000));
    if (seconds < 5) {
        return '刚刚';
    }
    if (seconds < 60) {
        return `${seconds} 秒前`;
    }
    return `${Math.floor(seconds / 60)} 分钟前`;
}

async function loadOrders() {
    if (!state.token) {
        ids.orderList.innerHTML = '<div class="order-empty">请先登录后查看订单。</div>';
        return;
    }
    const orders = await request('/api/orders/mine');
    if (!orders || !orders.length) {
        ids.orderList.innerHTML = '<div class="order-empty">暂无订单，秒杀或抽奖成功后订单会异步生成。</div>';
        return;
    }

    ids.orderList.innerHTML = `
        <table>
            <thead>
            <tr>
                <th>订单号</th>
                <th>活动ID</th>
                <th>来源</th>
                <th>奖品</th>
                <th>金额</th>
                <th>状态</th>
                <th>创建时间</th>
            </tr>
            </thead>
            <tbody>
            ${orders.map((order) => `
                <tr>
                    <td>${order.id}</td>
                    <td>${order.activityId}</td>
                    <td>${order.source}</td>
                    <td>${formatPrize(order.requestId)}</td>
                    <td>${formatAmount(order.amount)}</td>
                    <td>${order.status}</td>
                    <td>${formatTime(order.createdAt)}</td>
                </tr>
            `).join('')}
            </tbody>
        </table>
    `;
}

async function request(path, options = {}) {
    try {
        const headers = {
            'Content-Type': 'application/json',
            ...(options.headers || {}),
        };
        if (!options.anonymous && state.token) {
            headers.Authorization = `Bearer ${state.token}`;
        }

        const response = await fetch(`${state.baseUrl}${path}`, {
            method: options.method || 'GET',
            headers,
            body: options.body,
        });
        const payload = await response.json();
        if (!response.ok || payload.code !== 0) {
            if (response.status === 401) {
                clearSession();
                renderLoginState();
                renderActivities();
            }
            throw new Error(payload.message || `HTTP ${response.status}`);
        }
        return payload.data;
    } catch (error) {
        showToast(error.message || '请求失败', 'error');
        throw error;
    }
}

function findLotteryActivity() {
    return state.activities.find((item) => item.type === 'LOTTERY' && item.id === state.selectedLotteryActivityId)
        || state.activities.find((item) => item.type === 'LOTTERY' && /mac|macbook/i.test(String(item.productName || '')))
        || state.activities.find((item) => item.type === 'LOTTERY');
}

function activityStatus(activity) {
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

function clearSession() {
    state.token = '';
    state.user = null;
    state.activities = [];
    localStorage.removeItem('pdd.token');
    localStorage.removeItem('pdd.user');
}

function safeJson(value) {
    try {
        return value ? JSON.parse(value) : null;
    } catch (error) {
        return null;
    }
}

function showToast(message, type) {
    ids.toast.textContent = message;
    ids.toast.className = `toast ${type || ''}`;
    window.clearTimeout(showToast.timer);
    showToast.timer = window.setTimeout(() => ids.toast.classList.add('hidden'), 3200);
}

function formatAmount(value) {
    const amount = Number(value || 0);
    return amount === 0 ? '免费' : `¥${amount.toFixed(2)}`;
}

function formatTime(value) {
    if (!value) {
        return '-';
    }
    return String(value).replace('T', ' ');
}

function formatPrize(requestId) {
    if (!requestId || !requestId.includes(':spin:')) {
        return '-';
    }
    const prizeCode = requestId.split(':').pop();
    const prize = state.prizePool.find((item) => item.code === prizeCode);
    return prize?.name || prizeCode;
}

window.grabSeckill = grabSeckill;
window.joinLottery = joinLottery;
window.openLottery = openLottery;
window.preloadOne = preloadOne;
