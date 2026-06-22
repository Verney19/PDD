(function () {
    const PDD = window.PDD;
    const { escapeHtml, number, money, displayProductName } = PDD.utils;

    const MARKET_PRODUCTS = {
        phone: {
            title: 'iPhone 17',
            marketPrice: 5999,
            note: 'Apple 官网参考价 RMB 5,999 起',
            coupons: [
                { label: '不使用券', type: 'none', value: 0 },
                { label: '秒杀活动价', type: 'price', value: 2999 },
                { label: '1000 元代金券', type: 'amount', value: 1000 },
                { label: '三等奖券', type: 'amount', value: 1000 }
            ]
        },
        laptop: {
            title: 'MacBook Pro M4',
            marketPrice: 13499,
            note: 'Apple 官网参考价 RMB 13,499 起',
            coupons: [
                { label: '不使用券', type: 'none', value: 0 },
                { label: '1 折资格', type: 'discount', value: 0.1 },
                { label: '2 折资格', type: 'discount', value: 0.2 },
                { label: '3 折资格', type: 'discount', value: 0.3 },
                { label: '5 折资格', type: 'discount', value: 0.5 },
                { label: '7 折资格', type: 'discount', value: 0.7 },
                { label: '9 折资格', type: 'discount', value: 0.9 },
                { label: '2000 元 Mac 券', type: 'amount', value: 2000 },
                { label: '1000 元 Mac 券', type: 'amount', value: 1000 }
            ]
        }
    };

    async function render(container) {
        if (!PDD.state.isAuthenticated()) {
            container.innerHTML = PDD.components.loginRequired('登录后即可查看本场活动商品。');
            return;
        }

        const products = await PDD.api.products.list();
        PDD.state.data.products = Array.isArray(products) ? products : [];

        container.innerHTML = `
            <div class="section-title">
                <div>
                    <h3>活动商品</h3>
                    <p>展示本项目手机和电脑商品，选择折扣券后可实时查看券后价。</p>
                </div>
                <button id="reloadProducts" class="secondary-button">刷新商品</button>
            </div>
            <div class="product-list">
                ${renderProducts()}
            </div>
        `;
        container.querySelector('#reloadProducts').addEventListener('click', () => PDD.router.refresh());
        container.querySelectorAll('[data-coupon-select]').forEach((select) => {
            select.addEventListener('change', () => updateCouponPrice(select));
            updateCouponPrice(select);
        });
    }

    function renderProducts() {
        const products = normalizedProducts();
        return products.map((product) => {
            const kind = productKind(product);
            const market = MARKET_PRODUCTS[kind];
            const selected = market.coupons[0];
            return `
                <article class="card product-card product-showcase ${kind === 'laptop' ? 'laptop-product' : 'phone-product'}">
                    <div class="product-media">
                        ${kind === 'laptop' ? renderLaptopVisual() : renderPhoneVisual()}
                    </div>
                    <div class="product-info">
                        <div class="product-title">
                            <h4>${escapeHtml(displayProductName(product.name))}</h4>
                            <span class="badge lottery">${kind === 'laptop' ? 'Mac 抽奖专场' : '手机秒杀专场'}</span>
                        </div>
                        <div class="price-board">
                            <div>
                                <span>市场价</span>
                                <strong>${money(market.marketPrice)}</strong>
                                <small>${escapeHtml(market.note)}</small>
                            </div>
                            <div>
                                <span>项目标价</span>
                                <strong>${money(product.price)}</strong>
                                <small>数据库商品价</small>
                            </div>
                            <div class="final-price-box">
                                <span>券后价</span>
                                <strong data-final-price="${product.id}">${money(applyCoupon(market.marketPrice, selected))}</strong>
                                <small data-save-text="${product.id}">${escapeHtml(saveText(market.marketPrice, selected))}</small>
                            </div>
                        </div>
                        <label class="coupon-control">
                            <span>使用折扣券</span>
                            <select data-coupon-select data-product-id="${product.id}" data-kind="${kind}">
                                ${market.coupons.map((coupon, index) => `
                                    <option value="${index}">${escapeHtml(coupon.label)}</option>
                                `).join('')}
                            </select>
                        </label>
                        <div class="meta-row">
                            <span>本场限量 ${number(product.totalStock)} 台</span>
                            <span>当前剩余 ${number(product.availableStock)} 台</span>
                        </div>
                        ${PDD.components.progress(product.availableStock, product.totalStock)}
                        <div class="actions">
                            <button data-go-seckill>${kind === 'laptop' ? '参与抽奖' : '立即秒杀'}</button>
                            <button class="secondary-button" data-go-activities>查看活动</button>
                        </div>
                    </div>
                </article>
            `;
        }).join('');
    }

    function normalizedProducts() {
        if (PDD.state.data.products.length) {
            return PDD.state.data.products;
        }
        return [
            { id: 6180001, name: 'PDD 618 限量旗舰手机', price: 4999, totalStock: 100000, availableStock: 100000 },
            { id: 6180002, name: 'MacBook Pro M4 最新款', price: 12999, totalStock: 50000, availableStock: 50000 }
        ];
    }

    function productKind(product) {
        return /mac|book|电脑/i.test(String(product.name || '')) ? 'laptop' : 'phone';
    }

    function renderPhoneVisual() {
        return `
            <div class="product-phone-photo" aria-hidden="true">
                <div class="phone-lens lens-one"></div>
                <div class="phone-lens lens-two"></div>
                <div class="phone-lens lens-three"></div>
                <span>iPhone</span>
            </div>
        `;
    }

    function renderLaptopVisual() {
        return `
            <div class="product-laptop-photo" aria-hidden="true">
                <div class="laptop-screen"></div>
                <div class="laptop-base"></div>
                <span>MacBook Pro</span>
            </div>
        `;
    }

    function updateCouponPrice(select) {
        const kind = select.dataset.kind;
        const productId = select.dataset.productId;
        const market = MARKET_PRODUCTS[kind];
        const coupon = market.coupons[Number(select.value || 0)] || market.coupons[0];
        const finalPrice = applyCoupon(market.marketPrice, coupon);
        const priceNode = document.querySelector(`[data-final-price="${productId}"]`);
        const saveNode = document.querySelector(`[data-save-text="${productId}"]`);
        if (priceNode) {
            priceNode.textContent = money(finalPrice);
        }
        if (saveNode) {
            saveNode.textContent = saveText(market.marketPrice, coupon);
        }
    }

    function applyCoupon(price, coupon) {
        if (coupon.type === 'discount') {
            return Math.max(0, Number((price * coupon.value).toFixed(2)));
        }
        if (coupon.type === 'amount') {
            return Math.max(0, price - coupon.value);
        }
        if (coupon.type === 'price') {
            return Math.max(0, coupon.value);
        }
        return price;
    }

    function saveText(price, coupon) {
        const finalPrice = applyCoupon(price, coupon);
        const saved = Math.max(0, price - finalPrice);
        return saved > 0 ? `已优惠 ${money(saved)}` : '未使用优惠';
    }

    document.addEventListener('click', (event) => {
        if (!event.target.closest('#appView')) {
            return;
        }
        if (event.target.closest('[data-go-activities]')) {
            PDD.router.go('activities');
        }
        if (event.target.closest('[data-go-seckill]')) {
            const card = event.target.closest('.product-card');
            PDD.router.go(card?.classList.contains('laptop-product') ? 'lottery' : 'seckill');
        }
    });

    PDD.router.register('products', { render });
})();
