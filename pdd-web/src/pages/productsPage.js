(function () {
    const PDD = window.PDD;
    const { escapeHtml, number, money } = PDD.utils;

    const CATEGORIES = [
        { value: '',        label: '全部' },
        { value: 'DIGITAL',       label: '数码' },
        { value: 'DAILY_GOODS',   label: '日用商品' },
        { value: 'FRUITS',        label: '水果' }
    ];

    const CATEGORY_LABEL = {
        DIGITAL:     '数码',
        DAILY_GOODS: '日用商品',
        FRUITS:      '水果'
    };

    const CATEGORY_BADGE = {
        DIGITAL:     'digital',
        DAILY_GOODS: 'daily',
        FRUITS:      'fruit'
    };

    let currentCategory = '';

    async function render(container) {
        if (!PDD.state.isAuthenticated()) {
            container.innerHTML = PDD.components.loginRequired('登录后即可浏览商品中心。');
            return;
        }

        await loadProducts();

        const isAdmin = PDD.state.data.user && PDD.state.data.user.role === 'ADMIN';

        container.innerHTML = `
            <div class="section-title">
                <div>
                    <h3>商品中心</h3>
                    <p>数码、日用商品、水果 — 查看和管理全部商品</p>
                </div>
                <div class="actions">
                    <button id="reloadProductsBtn" class="secondary-button">刷新商品</button>
                    ${isAdmin ? '<button id="addProductBtn">添加商品</button>' : ''}
                </div>
            </div>
            <div class="activity-tabs" id="categoryTabs">
                ${CATEGORIES.map((cat, idx) => `
                    <button class="activity-tab${idx === 0 ? ' active' : ''}"
                            data-category="${escapeHtml(cat.value)}">${escapeHtml(cat.label)}</button>
                `).join('')}
            </div>
            <div id="productGrid" class="product-grid">
                ${renderProductCards(isAdmin)}
            </div>
            <div id="productModal" class="modal-overlay hidden"></div>
        `;

        bindEvents(container, isAdmin);
    }

    async function loadProducts() {
        try {
            const products = await PDD.api.products.list(currentCategory);
            PDD.state.data.products = Array.isArray(products) ? products : [];
        } catch (err) {
            PDD.state.data.products = [];
        }
    }

    function renderProductCards(isAdmin) {
        const products = PDD.state.data.products || [];
        if (!products.length) {
            return `<div class="empty-state">暂无商品</div>`;
        }

        return products.map(p => {
            const badgeClass = CATEGORY_BADGE[p.category] || 'digital';
            const catLabel = CATEGORY_LABEL[p.category] || p.category || '未知';
            const desc = p.description || '暂无描述';
            return `
                <article class="product-grid-card" data-product-id="${p.id}">
                    <div class="product-grid-header">
                        <h4>${escapeHtml(p.name)}</h4>
                        <span class="badge ${badgeClass}">${escapeHtml(catLabel)}</span>
                    </div>
                    <div class="product-grid-body">
                        <div class="price">${money(p.price)}</div>
                        <div class="desc">${escapeHtml(desc)}</div>
                        <div class="meta-row">
                            <span>库存 ${number(p.availableStock)} / ${number(p.totalStock)}</span>
                            ${p.status !== undefined && p.status !== null ? `<span>${p.status === 1 ? '🟢 在售' : '⚫ 下架'}</span>` : ''}
                        </div>
                        ${PDD.components.progress(p.availableStock, p.totalStock)}
                    </div>
                    ${isAdmin ? `
                    <div class="product-grid-actions">
                        <button class="secondary-button small-button edit-product-btn" data-product-id="${p.id}">编辑</button>
                        <button class="danger-button small-button delete-product-btn" data-product-id="${p.id}">删除</button>
                    </div>` : ''}
                </article>
            `;
        }).join('');
    }

    function bindEvents(container, isAdmin) {
        container.querySelector('#reloadProductsBtn').addEventListener('click', async () => {
            await loadProducts();
            container.querySelector('#productGrid').innerHTML = renderProductCards(isAdmin);
            rebindCardButtons(container, isAdmin);
        });

        container.querySelectorAll('#categoryTabs .activity-tab').forEach(btn => {
            btn.addEventListener('click', async () => {
                container.querySelectorAll('#categoryTabs .activity-tab').forEach(b => b.classList.remove('active'));
                btn.classList.add('active');
                currentCategory = btn.dataset.category;
                await loadProducts();
                container.querySelector('#productGrid').innerHTML = renderProductCards(isAdmin);
                rebindCardButtons(container, isAdmin);
            });
        });

        if (isAdmin) {
            container.querySelector('#addProductBtn').addEventListener('click', () => {
                showModal(container, null);
            });

            rebindCardButtons(container, isAdmin);
        }
    }

    function rebindCardButtons(container, isAdmin) {
        if (!isAdmin) return;

        container.querySelectorAll('.edit-product-btn').forEach(btn => {
            btn.addEventListener('click', () => {
                const id = Number(btn.dataset.productId);
                const product = (PDD.state.data.products || []).find(p => p.id === id);
                if (product) {
                    showModal(container, product);
                }
            });
        });

        container.querySelectorAll('.delete-product-btn').forEach(btn => {
            btn.addEventListener('click', async () => {
                const id = Number(btn.dataset.productId);
                if (!confirm('确认删除该商品？')) return;
                try {
                    await PDD.api.products.delete(id);
                    PDD.utils.showToast('删除成功', 'success');
                    await loadProducts();
                    container.querySelector('#productGrid').innerHTML = renderProductCards(isAdmin);
                    rebindCardButtons(container, isAdmin);
                } catch (err) {
                    // error already shown by http.js
                }
            });
        });
    }

    function showModal(container, product) {
        const isEdit = product != null;
        const modal = container.querySelector('#productModal');

        const categories = [
            { value: 'DIGITAL',     label: '数码' },
            { value: 'DAILY_GOODS', label: '日用商品' },
            { value: 'FRUITS',      label: '水果' }
        ];

        modal.innerHTML = `
            <div class="modal-dialog">
                <div class="modal-header">
                    <h3>${isEdit ? '编辑商品' : '添加商品'}</h3>
                    <button class="secondary-button small-button" id="closeModalBtn">✕</button>
                </div>
                <div class="modal-body">
                    <div class="form-group">
                        <label>商品名称</label>
                        <input type="text" id="formName" value="${isEdit ? escapeHtml(product.name) : ''}" placeholder="请输入商品名称">
                    </div>
                    <div class="form-row">
                        <div class="form-group">
                            <label>价格</label>
                            <input type="number" id="formPrice" step="0.01" min="0" value="${isEdit ? product.price : ''}" placeholder="0.00">
                        </div>
                        <div class="form-group">
                            <label>类别</label>
                            <select id="formCategory">
                                ${categories.map(c => `
                                    <option value="${c.value}" ${isEdit && product.category === c.value ? 'selected' : ''}>${c.label}</option>
                                `).join('')}
                            </select>
                        </div>
                    </div>
                    <div class="form-row">
                        <div class="form-group">
                            <label>总库存</label>
                            <input type="number" id="formTotalStock" min="0" value="${isEdit ? product.totalStock : ''}" placeholder="0">
                        </div>
                        <div class="form-group">
                            <label>可用库存</label>
                            <input type="number" id="formAvailableStock" min="0" value="${isEdit ? product.availableStock : ''}" placeholder="0">
                        </div>
                    </div>
                    <div class="form-group">
                        <label>描述（选填）</label>
                        <textarea id="formDesc" placeholder="商品描述">${isEdit && product.description ? escapeHtml(product.description) : ''}</textarea>
                    </div>
                    <div class="form-group">
                        <label>图片URL（选填）</label>
                        <input type="text" id="formImageUrl" value="${isEdit && product.imageUrl ? escapeHtml(product.imageUrl) : ''}" placeholder="https://...">
                    </div>
                    ${isEdit ? `
                    <div class="form-group">
                        <label>状态</label>
                        <select id="formStatus">
                            <option value="1" ${product.status === 1 ? 'selected' : ''}>在售</option>
                            <option value="0" ${product.status === 0 ? 'selected' : ''}>下架</option>
                        </select>
                    </div>` : ''}
                </div>
                <div class="modal-footer">
                    <button class="secondary-button" id="cancelModalBtn">取消</button>
                    <button id="submitFormBtn">${isEdit ? '保存修改' : '创建商品'}</button>
                </div>
            </div>
        `;
        modal.classList.remove('hidden');

        const closeModal = () => {
            modal.classList.add('hidden');
            modal.innerHTML = '';
        };

        modal.querySelector('#closeModalBtn').addEventListener('click', closeModal);
        modal.querySelector('#cancelModalBtn').addEventListener('click', closeModal);
        modal.addEventListener('click', (e) => {
            if (e.target === modal) closeModal();
        });

        modal.querySelector('#submitFormBtn').addEventListener('click', async () => {
            const name = modal.querySelector('#formName').value.trim();
            const price = modal.querySelector('#formPrice').value;
            const category = modal.querySelector('#formCategory').value;
            const totalStock = modal.querySelector('#formTotalStock').value;
            const availableStock = modal.querySelector('#formAvailableStock').value;

            if (!name) { PDD.utils.showToast('请输入商品名称', 'error'); return; }
            if (!price) { PDD.utils.showToast('请输入价格', 'error'); return; }
            if (!totalStock) { PDD.utils.showToast('请输入总库存', 'error'); return; }
            if (!availableStock) { PDD.utils.showToast('请输入可用库存', 'error'); return; }

            const data = {
                name: name,
                price: Number(price),
                totalStock: Number(totalStock),
                availableStock: Number(availableStock),
                category: category,
                description: modal.querySelector('#formDesc').value.trim() || null,
                imageUrl: modal.querySelector('#formImageUrl').value.trim() || null
            };

            if (isEdit) {
                data.status = Number(modal.querySelector('#formStatus').value);
            }

            try {
                if (isEdit) {
                    await PDD.api.products.update(product.id, data);
                    PDD.utils.showToast('商品更新成功', 'success');
                } else {
                    await PDD.api.products.create(data);
                    PDD.utils.showToast('商品创建成功', 'success');
                }
                closeModal();
                await loadProducts();
                container.querySelector('#productGrid').innerHTML = renderProductCards(true);
                rebindCardButtons(container, true);
            } catch (err) {
                // error already shown by http.js
            }
        });
    }

    PDD.router.register('products', { render });
})();
