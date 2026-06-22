(function () {
    const PDD = window.PDD;
    const { escapeHtml, number, money, formatTime, activityStatus, displayProductName } = PDD.utils;

    async function render(container) {
        if (!PDD.state.isAuthenticated()) {
            container.innerHTML = PDD.components.loginRequired('登录后即可参与幸运抽奖。');
            return;
        }

        const activities = await PDD.api.activities.list();
        PDD.state.data.activities = Array.isArray(activities) ? activities : [];
        const lotteryActivities = PDD.state.data.activities.filter((item) => item.type === 'LOTTERY');
        if (!lotteryActivities.length) {
            container.innerHTML = PDD.components.empty('没有找到抽奖活动。');
            return;
        }

        const selectedActivity = resolveSelectedActivity(lotteryActivities);
        const prizePool = await PDD.api.lottery.prizes(selectedActivity.id).catch(() => PDD.state.data.prizePool);
        if (Array.isArray(prizePool) && prizePool.length) {
            PDD.state.data.prizePool = prizePool;
        }

        const status = activityStatus(selectedActivity);
        const canOperate = PDD.state.data.user?.role === 'ADMIN' || !status.disabled;
        container.innerHTML = `
            <div class="lottery-layout">
                <section class="card lottery-card">
                    <div class="section-title">
                        <div>
                            <h3>${escapeHtml(displayProductName(selectedActivity.productName))}</h3>
                            <p>${formatTime(selectedActivity.startTime)} 开启，普通用户每日 3 次，管理端可重复验证奖池。</p>
                        </div>
                        <span class="status-pill ${status.className}">${status.label}</span>
                    </div>
                    <div class="activity-tabs">
                        ${lotteryActivities.map((activity) => `
                            <button class="activity-tab ${activity.id === selectedActivity.id ? 'active' : ''}" data-lottery-activity="${activity.id}">
                                ${escapeHtml(displayProductName(activity.productName))}
                            </button>
                        `).join('')}
                    </div>
                    <div class="board-stage">
                        ${renderBoard(selectedActivity, canOperate)}
                    </div>
                    <div id="lotteryResult" class="result-box">${renderLatestResult(selectedActivity.id)}</div>
                    <div class="actions">
                        <button id="joinLottery" class="secondary-button" ${canOperate ? '' : 'disabled'}>报名抽奖</button>
                        <button id="drawLottery" class="secondary-button">查看开奖</button>
                        <button id="winnerLottery" class="secondary-button">查中奖</button>
                        <button id="refreshLottery" class="secondary-button">刷新</button>
                    </div>
                    <div id="winnerTicker" class="winner-ticker">
                        ${renderWinnerTicker(selectedActivity.id)}
                    </div>
                </section>

                <aside class="card lottery-sidebar">
                    <div class="metrics">
                        ${PDD.components.metric('抽奖名额', number(selectedActivity.availableStock), `共 ${number(selectedActivity.totalStock)} 份`)}
                        ${PDD.components.metric('活动价格', money(selectedActivity.activityPrice), '中奖后自动生成订单')}
                    </div>
                    <div class="section-title">
                        <div>
                            <h3>奖池</h3>
                            <p>${renderPrizeCopy(selectedActivity)}</p>
                        </div>
                    </div>
                    <div class="prize-list">
                        ${renderPrizeList()}
                    </div>
                </aside>
            </div>
        `;

        container.querySelector('#spinLottery').addEventListener('click', () => spin(selectedActivity.id, container));
        container.querySelector('#joinLottery').addEventListener('click', () => join(selectedActivity.id));
        container.querySelector('#drawLottery').addEventListener('click', () => draw(selectedActivity.id));
        container.querySelector('#winnerLottery').addEventListener('click', () => winner(selectedActivity.id));
        container.querySelector('#refreshLottery').addEventListener('click', () => PDD.router.refresh());
        setupWinnerTicker(container, selectedActivity.id);
        container.querySelectorAll('[data-lottery-activity]').forEach((button) => {
            button.addEventListener('click', () => {
                PDD.state.data.selectedLotteryActivityId = Number(button.dataset.lotteryActivity);
                PDD.state.data.latestLottery = null;
                PDD.state.data.boardCursor = -1;
                PDD.state.data.wheelRotation = 0;
                PDD.router.refresh();
            });
        });
    }

    function resolveSelectedActivity(lotteryActivities) {
        const selected = lotteryActivities.find((activity) => activity.id === PDD.state.data.selectedLotteryActivityId);
        const macActivity = lotteryActivities.find((activity) => isMacActivity(activity));
        const next = selected || macActivity || lotteryActivities[0];
        PDD.state.data.selectedLotteryActivityId = next.id;
        return next;
    }

    function isMacActivity(activity) {
        return /mac|macbook/i.test(String(activity?.productName || ''));
    }

    function isPhoneActivity(activity) {
        return /手机|旗舰|iphone|phone|pdd\s*618/i.test(String(activity?.productName || ''));
    }

    function renderPrizeCopy(activity) {
        return isMacActivity(activity)
            ? 'Mac 活动包含折扣资格、代金券和会员兑换码，棋盘跑动后定格本次结果。'
            : '奖项按权重抽取，轮盘转动后定格本次结果。';
    }

    function renderBoard(activity, canOperate) {
        if (isPhoneActivity(activity) && !isMacActivity(activity)) {
            return renderWheel(activity, canOperate);
        }

        const prizes = PDD.state.data.prizePool;
        const grandPrizeIndex = findGrandPrizeIndex(prizes);
        const boardPrizes = prizes
            .map((prize, index) => ({ prize, index }))
            .filter((item) => item.index !== grandPrizeIndex);
        const boardSize = boardPrizes.length > 8 ? 5 : 3;
        const track = buildTrack(boardSize);
        const prizePositions = distributePrizes(boardPrizes.length, track.length);
        const slots = new Map(prizePositions.map((position, index) => [position, boardPrizes[index]]));
        const latest = PDD.state.data.latestLottery;
        const latestIndex = latest && latest.activityId === activity.id ? Number(latest.prizeIndex) : -1;
        const centerStart = boardSize === 5 ? 2 : 2;
        const centerEnd = boardSize === 5 ? 5 : 3;
        const grandPrize = grandPrizeIndex >= 0 ? prizes[grandPrizeIndex] : null;

        return `
            <div class="lottery-board" data-track-length="${track.length}" style="--board-size: ${boardSize};">
                ${track.map((point, position) => {
                    const item = slots.get(position);
                    return renderBoardCell(item?.prize, item?.index, position, point, latestIndex);
                }).join('')}
                <div class="board-center" style="grid-column: ${centerStart} / ${centerEnd}; grid-row: ${centerStart} / ${centerEnd};">
                    ${renderFeaturedPrize(grandPrize, grandPrizeIndex, latestIndex, activity)}
                    <button id="spinLottery" class="board-spin-button" ${canOperate ? '' : 'disabled'}>开始抽奖</button>
                </div>
            </div>
        `;
    }

    function renderWheel(activity, canOperate) {
        const prizes = PDD.state.data.prizePool;
        const latest = PDD.state.data.latestLottery;
        const latestIndex = latest && latest.activityId === activity.id ? Number(latest.prizeIndex) : -1;
        const sliceAngle = 360 / Math.max(1, prizes.length);
        return `
            <div class="lottery-wheel-shell">
                <div class="wheel-pointer" aria-hidden="true"></div>
                <div class="lottery-wheel" data-wheel-size="${prizes.length}" style="${wheelGradient(prizes)} transform: rotate(${Number(PDD.state.data.wheelRotation || 0)}deg);">
                    <div class="wheel-labels">
                        ${prizes.map((prize, index) => renderWheelSlice(prize, index, sliceAngle, latestIndex)).join('')}
                    </div>
                </div>
                <div class="wheel-center">
                    <strong>618</strong>
                    <span>限量旗舰手机</span>
                    <button id="spinLottery" class="wheel-spin-button" ${canOperate ? '' : 'disabled'}>开始抽奖</button>
                </div>
            </div>
        `;
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

    function renderWheelSlice(prize, index, sliceAngle, latestIndex) {
        const selectedClass = index === latestIndex ? 'winner' : '';
        const rotate = index * sliceAngle + sliceAngle / 2;
        return `
            <div class="wheel-slice-label ${selectedClass}"
                 data-prize-index="${index}"
                 style="--slice-rotate: ${rotate}deg;">
                <span>${escapeHtml(prize.level)}</span>
                <strong>${escapeHtml(compactPrizeName(prize))}</strong>
            </div>
        `;
    }

    function findGrandPrizeIndex(prizes) {
        const index = prizes.findIndex((prize) => /超级大奖|特等奖|大奖/.test(String(prize.level || '')));
        if (index >= 0) {
            return index;
        }
        return prizes.findIndex((prize) => /1\s*折|MacBook Pro/i.test(String(prize.name || '')) && Boolean(prize.winning));
    }

    function renderFeaturedPrize(prize, prizeIndex, latestIndex, activity) {
        if (!prize) {
            return `
                <div class="featured-prize ${isMacActivity(activity) ? 'mac-featured' : ''}" data-featured-prize>
                    ${isMacActivity(activity) ? '<div class="macbook-visual" aria-hidden="true"><span></span></div>' : '<div class="board-mark" aria-hidden="true">618</div>'}
                    <span>本场大奖</span>
                    <strong>${isMacActivity(activity) ? 'Mac 幸运棋盘' : '幸运棋盘'}</strong>
                    <small>一格一奖，即抽即看</small>
                </div>
            `;
        }

        const selectedClass = prizeIndex === latestIndex ? 'winner' : '';
        const stock = prize.stock === null || prize.stock === undefined ? '不限量' : `限量 ${number(prize.stock)}`;
        return `
            <div class="featured-prize ${selectedClass}" data-featured-prize data-prize-index="${prizeIndex}">
                ${isMacActivity(activity) ? '<div class="macbook-visual" aria-hidden="true"><span></span></div>' : '<div class="board-mark" aria-hidden="true">618</div>'}
                <span>本场大奖</span>
                <strong>${escapeHtml(compactPrizeName(prize))}</strong>
                <small>${escapeHtml(prize.name)} · ${stock}</small>
            </div>
        `;
    }

    function renderBoardCell(prize, prizeIndex, position, point, latestIndex) {
        const style = `grid-column: ${point.column + 1}; grid-row: ${point.row + 1};`;
        if (!prize) {
            return `
                <div class="lottery-board-cell board-filler" data-board-position="${position}" style="${style}">
                    <span>好运</span>
                    <strong>加速格</strong>
                </div>
            `;
        }

        const selectedClass = prizeIndex === latestIndex ? 'winner' : '';
        const stock = prize.stock === null || prize.stock === undefined ? '不限量' : `限量 ${number(prize.stock)}`;
        return `
            <div class="lottery-board-cell prize-cell prize-cell-${prizeIndex % 6} ${selectedClass}"
                 data-board-position="${position}"
                 data-prize-index="${prizeIndex}"
                 style="${style}"
                 title="${escapeHtml(prize.name)}">
                <span>${escapeHtml(prize.level)}</span>
                <strong>${escapeHtml(compactPrizeName(prize))}</strong>
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

    function compactPrizeName(prize) {
        const name = String(prize.name || '');
        const discount = name.match(/(\d)\s*折/);
        if (discount) {
            return `${discount[1]}折资格`;
        }
        const coupon = name.match(/(\d+)\s*元/);
        if (coupon) {
            return `${coupon[1]}元券`;
        }
        if (/爱奇艺/.test(name)) {
            return '爱奇艺年卡';
        }
        if (/优酷/.test(name)) {
            return '优酷年卡';
        }
        if (/咪咕/.test(name)) {
            return '咪咕年卡';
        }
        if (/bilibili|B站/i.test(name)) {
            return 'B站年卡';
        }
        if (/谢谢|参与/.test(name)) {
            return '谢谢参与';
        }
        return name.length > 8 ? `${name.slice(0, 8)}…` : name;
    }

    function renderPrizeList() {
        const totalWeight = PDD.state.data.prizePool.reduce((sum, prize) => sum + Number(prize.weight || 0), 0) || 1;
        return PDD.state.data.prizePool.map((prize, index) => {
            const probability = ((Number(prize.weight || 0) / totalWeight) * 100).toFixed(prize.weight < 100 ? 2 : 1);
            const stock = prize.stock === null || prize.stock === undefined ? '不限量' : `限量 ${number(prize.stock)}`;
            return `
                <div class="prize-item">
                    <span class="prize-dot prize-dot-${index % 4}"></span>
                    <div>
                        <strong>${escapeHtml(prize.name)}</strong>
                        <div class="muted">${escapeHtml(prize.level)} · 概率 ${probability}% · ${stock}</div>
                    </div>
                </div>
            `;
        }).join('');
    }

    function renderLatestResult(activityId) {
        const latest = PDD.state.data.latestLottery;
        if (!latest || latest.activityId !== activityId) {
            return '点击“开始抽奖”，轮盘或棋盘会转动并停在你的结果。';
        }
        const prizeName = latest.prize ? latest.prize.name : '参与奖';
        return latest.winner
            ? `恭喜抽中 ${prizeName}，订单会自动生成。`
            : `${prizeName}，本次未中奖。`;
    }

    function renderWinnerTicker(activityId) {
        const entries = currentWinnerFeed(activityId);
        return `
            <div class="winner-ticker-head">
                <div>
                    <span>实时中奖显示屏</span>
                    <strong>系统用户中奖播报</strong>
                </div>
                <small>仅展示大奖、一/二/三等奖</small>
            </div>
            <div class="winner-feed-list" data-winner-feed>
                ${renderWinnerFeedRows(entries)}
            </div>
        `;
    }

    function setupWinnerTicker(container, activityId) {
        window.clearInterval(PDD.state.data.winnerFeedTimer);
        refreshWinnerFeed(container, activityId);
        PDD.state.data.winnerFeedTimer = window.setInterval(() => {
            if (!container.isConnected || PDD.state.data.currentRoute !== 'lottery') {
                window.clearInterval(PDD.state.data.winnerFeedTimer);
                return;
            }
            refreshWinnerFeed(container, activityId);
        }, 3000);
    }

    async function refreshWinnerFeed(container, activityId) {
        try {
            const entries = await PDD.api.lottery.winnerFeed(activityId);
            PDD.state.data.winnerFeed = {
                activityId,
                entries: Array.isArray(entries) ? entries : []
            };
            const list = container.querySelector('[data-winner-feed]');
            if (list) {
                list.innerHTML = renderWinnerFeedRows(PDD.state.data.winnerFeed.entries);
            }
        } catch (error) {
            const list = container.querySelector('[data-winner-feed]');
            if (list) {
                list.innerHTML = renderWinnerFeedRows(currentWinnerFeed(activityId));
            }
        }
    }

    function currentWinnerFeed(activityId) {
        const feed = PDD.state.data.winnerFeed;
        if (feed && feed.activityId === activityId) {
            return feed.entries;
        }
        return [];
    }

    function renderWinnerFeedRows(entries) {
        if (!entries.length) {
            return '<div class="winner-feed-empty">暂无高等级中奖播报</div>';
        }
        return entries.map((entry, index) => `
            <div class="winner-feed-row ${index === 0 ? 'latest' : ''}">
                <span class="winner-feed-level">${escapeHtml(entry.level)}</span>
                <strong>${escapeHtml(entry.userLabel || entry.user || '系统用户')} 抽中了 ${escapeHtml(entry.prizeName)}!</strong>
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

    async function spin(activityId, container) {
        const button = container.querySelector('#spinLottery');
        const resultBox = container.querySelector('#lotteryResult');
        button.disabled = true;
        resultBox.textContent = '抽奖转动中...';
        try {
            const result = await PDD.api.lottery.spin(activityId);
            if (Array.isArray(result.prizePool) && result.prizePool.length) {
                PDD.state.data.prizePool = result.prizePool;
            }
            if (container.querySelector('.lottery-wheel')) {
                await spinWheelTo(container.querySelector('.lottery-wheel'), result.prizeIndex || 0);
            } else {
                await spinBoardTo(container.querySelector('.lottery-board'), result.prizeIndex || 0);
            }
            PDD.state.data.latestLottery = result;
            PDD.utils.showToast(result.winner ? `抽中 ${result.prize.name}` : '本次未中奖', result.winner ? 'success' : 'error');
            await PDD.router.refresh();
        } finally {
            button.disabled = false;
        }
    }

    async function spinWheelTo(wheel, prizeIndex) {
        if (!wheel) {
            return;
        }
        const size = Number(wheel.dataset.wheelSize || PDD.state.data.prizePool.length || 1);
        const slice = 360 / Math.max(1, size);
        const targetAngle = Number(prizeIndex) * slice + slice / 2;
        const current = Number(PDD.state.data.wheelRotation || 0);
        const next = current + 1440 + (360 - ((current + targetAngle) % 360));
        PDD.state.data.wheelRotation = next;
        wheel.style.transform = `rotate(${next}deg)`;
        await sleep(2600);
    }

    async function spinBoardTo(board, prizeIndex) {
        if (!board) {
            return;
        }

        const targetCell = board.querySelector(`[data-prize-index="${Number(prizeIndex)}"]`);
        const targetIsFeatured = targetCell?.hasAttribute('data-featured-prize');
        const targetPosition = targetIsFeatured ? null : Number(targetCell?.dataset.boardPosition || 0);
        const trackLength = Number(board.dataset.trackLength || 1);
        const current = Number.isFinite(PDD.state.data.boardCursor) ? PDD.state.data.boardCursor : -1;
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
            await sleep(delay);
        }

        PDD.state.data.boardCursor = cursor;
        if (targetIsFeatured && targetCell) {
            board.querySelectorAll('.lottery-board-cell').forEach((cell) => {
                cell.classList.remove('active', 'winner');
            });
            targetCell.classList.add('active', 'winner');
            await sleep(720);
        }
    }

    function setActiveCell(board, position, isWinner) {
        board.querySelectorAll('.lottery-board-cell').forEach((cell) => {
            const active = Number(cell.dataset.boardPosition) === position;
            cell.classList.toggle('active', active);
            cell.classList.toggle('winner', active && isWinner);
        });
    }

    function sleep(delay) {
        return new Promise((resolve) => window.setTimeout(resolve, delay));
    }

    async function join(activityId) {
        const result = await PDD.api.lottery.join(activityId);
        PDD.utils.showToast(`报名成功，当前报名人数 ${number(result.participantCount)}`, 'success');
    }

    async function draw(activityId) {
        const result = await PDD.api.lottery.draw(activityId);
        PDD.utils.showToast(`开奖完成，本轮中奖 ${number(result.winnerCount)} 人`, 'success');
    }

    async function winner(activityId) {
        const result = await PDD.api.lottery.winner(activityId);
        PDD.utils.showToast(result ? '你已中奖' : '暂未中奖', result ? 'success' : 'error');
    }

    PDD.router.register('lottery', { render });
})();
