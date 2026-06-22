(function () {
    window.PDD = window.PDD || {};

    window.PDD.constants = {
        routes: {
            dashboard: {
                title: '618 手机发放中心',
                subtitle: '100000 台手机 · 90000 台秒杀 · 10000 台抽奖'
            },
            products: {
                title: '商品中心',
                subtitle: '甄选限量手机，查看本场可参与权益'
            },
            activities: {
                title: '活动中心',
                subtitle: '秒杀活动 20:00 开始，抽奖活动 20:45 开始'
            },
            seckill: {
                title: '秒杀入口',
                subtitle: '20:00 准点开抢，先到先得'
            },
            lottery: {
                title: '抽奖中心',
                subtitle: '手机轮盘抽奖，Mac 棋盘抽奖，中奖后异步生成订单'
            },
            orders: {
                title: '我的订单',
                subtitle: '查看秒杀和抽奖获得的订单'
            },
            ops: {
                title: '内部工具',
                subtitle: '活动准备和服务检查'
            },
            topology: {
                title: '服务说明',
                subtitle: '活动能力概览'
            }
        },
        defaultPrizePool: [
            { code: 'IPHONE_17_PLUS', name: 'iPhone 17 Plus', level: '一等奖', weight: 50, stock: 10, winning: true },
            { code: 'REDMI_K100_PRO', name: '红米 K100 Pro', level: '二等奖', weight: 450, stock: 100, winning: true },
            { code: 'COUPON_1000', name: '1000 元代金券', level: '三等奖', weight: 4500, stock: 1000, winning: true },
            { code: 'PARTICIPATION', name: '参与奖', level: '参与奖', weight: 5000, stock: null, winning: false }
        ],
        scripts: [
            {
                name: 'start-infra.ps1',
                purpose: '启动 MySQL、Redis、RabbitMQ、Nacos 等基础设施容器',
                command: 'powershell -ExecutionPolicy Bypass -File .\\scripts\\start-infra.ps1'
            },
            {
                name: 'build.ps1',
                purpose: '执行 Maven 打包，生成各微服务可运行 jar',
                command: 'powershell -ExecutionPolicy Bypass -File .\\scripts\\build.ps1'
            },
            {
                name: 'run-services.ps1',
                purpose: '注入统一环境变量并后台启动 6 个 Spring Boot 服务',
                command: 'powershell -ExecutionPolicy Bypass -File .\\scripts\\run-services.ps1'
            },
            {
                name: 'stop-services.ps1',
                purpose: '停止当前项目相关 Java 服务进程',
                command: 'powershell -ExecutionPolicy Bypass -File .\\scripts\\stop-services.ps1'
            },
            {
                name: 'start-web.ps1',
                purpose: '用本地静态服务器启动 pdd-web，默认地址 http://localhost:5173',
                command: 'powershell -ExecutionPolicy Bypass -File .\\scripts\\start-web.ps1'
            }
        ],
        services: [
            { name: '统一入口', port: 8080, tag: '入口', detail: '承接用户访问并保护账号安全' },
            { name: '账号中心', port: 8081, tag: '账号', detail: '支持登录、注册和身份确认' },
            { name: '活动中心', port: 8082, tag: '活动', detail: '展示商品、活动时间和剩余名额' },
            { name: '订单中心', port: 8083, tag: '订单', detail: '生成订单并提供查询能力' },
            { name: '秒杀中心', port: 8084, tag: '秒杀', detail: '处理限时抢购和一人一单' },
            { name: '抽奖中心', port: 8085, tag: '抽奖', detail: '处理幸运抽奖和中奖结果' }
        ]
    };
})();
