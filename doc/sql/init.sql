-- ============================================================
-- SmartStock AI - 数据库初始化脚本
-- 数据库：smartstock_ai
-- 字符集：utf8mb4
-- ============================================================

CREATE DATABASE IF NOT EXISTS smartstock_ai
    DEFAULT CHARACTER SET utf8mb4
    COLLATE utf8mb4_unicode_ci;

USE smartstock_ai;

-- ============================================================
-- 用户模块
-- ============================================================

CREATE TABLE IF NOT EXISTS `users` (
    `id`         BIGINT       NOT NULL AUTO_INCREMENT COMMENT '用户ID',
    `username`   VARCHAR(50)  NOT NULL COMMENT '用户名',
    `email`      VARCHAR(100) NOT NULL COMMENT '邮箱',
    `phone`      VARCHAR(20)           DEFAULT NULL COMMENT '手机号',
    `password`   VARCHAR(255) NOT NULL COMMENT '密码（BCrypt加密）',
    `nickname`   VARCHAR(50)           DEFAULT NULL COMMENT '昵称',
    `avatar`     VARCHAR(255)          DEFAULT NULL COMMENT '头像URL',
    `status`     TINYINT(1)            DEFAULT 1 COMMENT '状态：0-禁用，1-正常',
    `deleted`    TINYINT(1)            DEFAULT 0 COMMENT '逻辑删除：0-未删除，1-已删除',
    `created_at` DATETIME              DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `updated_at` DATETIME              DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_users_email` (`email`),
    UNIQUE KEY `uk_users_phone` (`phone`),
    KEY `idx_users_username` (`username`)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4 COMMENT ='用户表';

CREATE TABLE IF NOT EXISTS `user_profiles` (
    `id`         BIGINT      NOT NULL AUTO_INCREMENT COMMENT '资料ID',
    `user_id`    BIGINT      NOT NULL COMMENT '用户ID',
    `real_name`  VARCHAR(50)          DEFAULT NULL COMMENT '真实姓名',
    `id_card`    VARCHAR(18)          DEFAULT NULL COMMENT '身份证号',
    `gender`     TINYINT(1)           DEFAULT NULL COMMENT '性别：0-女，1-男',
    `birthday`   DATE                 DEFAULT NULL COMMENT '生日',
    `address`    VARCHAR(255)         DEFAULT NULL COMMENT '地址',
    `created_at` DATETIME             DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `updated_at` DATETIME             DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_user_profiles_user_id` (`user_id`),
    CONSTRAINT `fk_user_profiles_user_id` FOREIGN KEY (`user_id`) REFERENCES `users` (`id`) ON DELETE CASCADE
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4 COMMENT ='用户资料表';

-- ============================================================
-- 行情模块
-- ============================================================

CREATE TABLE IF NOT EXISTS `stock_info` (
    `id`           BIGINT      NOT NULL AUTO_INCREMENT COMMENT '股票ID',
    `stock_code`   VARCHAR(10) NOT NULL COMMENT '股票代码（如600519）',
    `stock_name`   VARCHAR(50) NOT NULL COMMENT '股票名称（如贵州茅台）',
    `market`       VARCHAR(10) NOT NULL COMMENT '市场：SH-上海，SZ-深圳',
    `board`        VARCHAR(20)          DEFAULT NULL COMMENT '板块：SH_MAIN/SZ_MAIN/CYB/STAR',
    `industry`     VARCHAR(50)          DEFAULT NULL COMMENT '所属行业',
    `is_st`        TINYINT(1)           DEFAULT 0 COMMENT '是否ST',
    `is_delisted`  TINYINT(1)           DEFAULT 0 COMMENT '是否退市/摘牌',
    `source`       VARCHAR(20)          DEFAULT NULL COMMENT '资料来源',
    `listing_date` DATE                 DEFAULT NULL COMMENT '上市日期',
    `status`       TINYINT(1)           DEFAULT 1 COMMENT '状态：0-停牌，1-正常',
    `created_at`   DATETIME             DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `updated_at`   DATETIME             DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_stock_info_code` (`stock_code`),
    KEY `idx_stock_info_name` (`stock_name`),
    KEY `idx_stock_info_industry` (`industry`),
    KEY `idx_stock_info_board` (`board`)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4 COMMENT ='股票信息表';

CREATE TABLE IF NOT EXISTS `stock_prices` (
    `id`          BIGINT         NOT NULL AUTO_INCREMENT COMMENT '价格ID',
    `stock_code`  VARCHAR(10)    NOT NULL COMMENT '股票代码',
    `trade_date`  DATE           NOT NULL COMMENT '交易日期',
    `open_price`  DECIMAL(10, 2) NOT NULL COMMENT '开盘价',
    `close_price` DECIMAL(10, 2) NOT NULL COMMENT '收盘价',
    `high_price`  DECIMAL(10, 2) NOT NULL COMMENT '最高价',
    `low_price`   DECIMAL(10, 2) NOT NULL COMMENT '最低价',
    `volume`      BIGINT         NOT NULL COMMENT '成交量（手）',
    `amount`      DECIMAL(18, 2) NOT NULL COMMENT '成交额（元）',
    `change_rate` DECIMAL(10, 4)          DEFAULT NULL COMMENT '涨跌幅（%）',
    `created_at`  DATETIME                DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    PRIMARY KEY (`id`, `trade_date`),
    UNIQUE KEY `uk_stock_prices_code_date` (`stock_code`, `trade_date`),
    KEY `idx_stock_prices_date` (`trade_date`)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4 COMMENT ='股票价格表'
    PARTITION BY RANGE (TO_DAYS(`trade_date`)) (
        PARTITION p202601 VALUES LESS THAN (TO_DAYS('2026-02-01')),
        PARTITION p202602 VALUES LESS THAN (TO_DAYS('2026-03-01')),
        PARTITION p202603 VALUES LESS THAN (TO_DAYS('2026-04-01')),
        PARTITION p202604 VALUES LESS THAN (TO_DAYS('2026-05-01')),
        PARTITION p202605 VALUES LESS THAN (TO_DAYS('2026-06-01')),
        PARTITION p202606 VALUES LESS THAN (TO_DAYS('2026-07-01')),
        PARTITION p202607 VALUES LESS THAN (TO_DAYS('2026-08-01')),
        PARTITION p202608 VALUES LESS THAN (TO_DAYS('2026-09-01')),
        PARTITION p202609 VALUES LESS THAN (TO_DAYS('2026-10-01')),
        PARTITION p202610 VALUES LESS THAN (TO_DAYS('2026-11-01')),
        PARTITION p202611 VALUES LESS THAN (TO_DAYS('2026-12-01')),
        PARTITION p202612 VALUES LESS THAN (TO_DAYS('2027-01-01')),
        PARTITION p_future VALUES LESS THAN MAXVALUE
        );

CREATE TABLE IF NOT EXISTS `user_watchlist` (
    `id`         BIGINT      NOT NULL AUTO_INCREMENT COMMENT '自选股ID',
    `user_id`    BIGINT      NOT NULL COMMENT '用户ID',
    `stock_code` VARCHAR(10) NOT NULL COMMENT '股票代码',
    `sort_order` INT                  DEFAULT 0 COMMENT '排序顺序',
    `created_at` DATETIME             DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_watchlist_user_stock` (`user_id`, `stock_code`),
    KEY `idx_watchlist_user_id` (`user_id`),
    CONSTRAINT `fk_watchlist_user_id` FOREIGN KEY (`user_id`) REFERENCES `users` (`id`) ON DELETE CASCADE
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4 COMMENT ='用户自选股表';

-- ============================================================
-- AI 分析模块
-- ============================================================

CREATE TABLE IF NOT EXISTS `ai_analysis` (
    `id`            BIGINT      NOT NULL AUTO_INCREMENT COMMENT '分析ID',
    `user_id`       BIGINT      NOT NULL COMMENT '用户ID',
    `stock_code`    VARCHAR(10) NOT NULL COMMENT '股票代码',
    `analysis_type` VARCHAR(20) NOT NULL COMMENT '分析类型：market-行情解读，qa-智能问答',
    `input_data`    TEXT COMMENT '输入数据（JSON格式）',
    `output_text`   TEXT COMMENT 'AI输出文本',
    `created_at`    DATETIME             DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    PRIMARY KEY (`id`),
    KEY `idx_ai_analysis_user_id` (`user_id`),
    KEY `idx_ai_analysis_stock_code` (`stock_code`),
    KEY `idx_ai_analysis_created_at` (`created_at`)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4 COMMENT ='AI分析记录表';

CREATE TABLE IF NOT EXISTS `news` (
    `id`              BIGINT       NOT NULL AUTO_INCREMENT COMMENT '新闻ID',
    `title`           VARCHAR(255) NOT NULL COMMENT '新闻标题',
    `content`         TEXT COMMENT '新闻内容',
    `source`          VARCHAR(100)          DEFAULT NULL COMMENT '新闻来源',
    `url`             VARCHAR(500)          DEFAULT NULL COMMENT '新闻链接',
    `stock_code`      VARCHAR(10)           DEFAULT NULL COMMENT '关联股票代码',
    `sentiment`       VARCHAR(20)           DEFAULT NULL COMMENT '情绪：positive-正面，neutral-中性，negative-负面',
    `sentiment_score` DECIMAL(5, 4)         DEFAULT NULL COMMENT '情绪分数（-1到1）',
    `publish_time`    DATETIME              DEFAULT NULL COMMENT '发布时间',
    `created_at`      DATETIME              DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    PRIMARY KEY (`id`),
    KEY `idx_news_stock_code` (`stock_code`),
    KEY `idx_news_publish_time` (`publish_time`)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4 COMMENT ='新闻表';

CREATE TABLE IF NOT EXISTS `qa_history` (
    `id`         BIGINT NOT NULL AUTO_INCREMENT COMMENT '问答ID',
    `user_id`    BIGINT NOT NULL COMMENT '用户ID',
    `question`   TEXT   NOT NULL COMMENT '用户问题',
    `answer`     TEXT COMMENT 'AI回答',
    `created_at` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    PRIMARY KEY (`id`),
    KEY `idx_qa_history_user_id` (`user_id`),
    KEY `idx_qa_history_created_at` (`created_at`)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4 COMMENT ='问答历史表';

-- ============================================================
-- 交易模块
-- ============================================================

CREATE TABLE IF NOT EXISTS `accounts` (
    `id`             BIGINT         NOT NULL AUTO_INCREMENT COMMENT '账户ID',
    `user_id`        BIGINT         NOT NULL COMMENT '用户ID',
    `total_assets`   DECIMAL(18, 2) DEFAULT 1000000.00 COMMENT '总资产',
    `available_cash` DECIMAL(18, 2) DEFAULT 1000000.00 COMMENT '可用资金',
    `frozen_cash`    DECIMAL(18, 2) DEFAULT 0.00 COMMENT '冻结资金',
    `position_value` DECIMAL(18, 2) DEFAULT 0.00 COMMENT '持仓市值',
    `total_profit`   DECIMAL(18, 2) DEFAULT 0.00 COMMENT '累计收益',
    `profit_rate`    DECIMAL(10, 4) DEFAULT 0.0000 COMMENT '收益率（%）',
    `created_at`     DATETIME       DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `updated_at`     DATETIME       DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_accounts_user_id` (`user_id`),
    CONSTRAINT `fk_accounts_user_id` FOREIGN KEY (`user_id`) REFERENCES `users` (`id`) ON DELETE CASCADE
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4 COMMENT ='账户表';

CREATE TABLE IF NOT EXISTS `orders` (
    `id`              BIGINT         NOT NULL AUTO_INCREMENT COMMENT '订单ID',
    `user_id`         BIGINT         NOT NULL COMMENT '用户ID',
    `stock_code`      VARCHAR(10)    NOT NULL COMMENT '股票代码',
    `order_type`      VARCHAR(10)    NOT NULL COMMENT '订单类型：buy-买入，sell-卖出',
    `price`           DECIMAL(10, 2) NOT NULL COMMENT '委托价格',
    `quantity`        INT            NOT NULL COMMENT '委托数量（股）',
    `amount`          DECIMAL(18, 2) NOT NULL COMMENT '委托金额',
    `fee`             DECIMAL(10, 2)          DEFAULT 0.00 COMMENT '手续费',
    `status`          VARCHAR(20)             DEFAULT 'pending' COMMENT '订单状态：pending-待成交，filled-已成交，cancelled-已撤销',
    `filled_price`    DECIMAL(10, 2)          DEFAULT NULL COMMENT '成交价格',
    `filled_quantity` INT                     DEFAULT 0 COMMENT '成交数量',
    `filled_time`     DATETIME                DEFAULT NULL COMMENT '成交时间',
    `created_at`      DATETIME                DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `updated_at`      DATETIME                DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (`id`),
    KEY `idx_orders_user_status_time` (`user_id`, `status`, `created_at`),
    KEY `idx_orders_stock_code` (`stock_code`)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4 COMMENT ='订单表';

CREATE TABLE IF NOT EXISTS `positions` (
    `id`                 BIGINT         NOT NULL AUTO_INCREMENT COMMENT '持仓ID',
    `user_id`            BIGINT         NOT NULL COMMENT '用户ID',
    `stock_code`         VARCHAR(10)    NOT NULL COMMENT '股票代码',
    `quantity`           INT            NOT NULL COMMENT '持仓数量（股）',
    `available_quantity` INT            NOT NULL COMMENT '可用数量（股）',
    `cost_price`         DECIMAL(10, 2) NOT NULL COMMENT '成本价',
    `current_price`      DECIMAL(10, 2)          DEFAULT NULL COMMENT '当前价',
    `market_value`       DECIMAL(18, 2)          DEFAULT NULL COMMENT '市值',
    `profit`             DECIMAL(18, 2)          DEFAULT NULL COMMENT '盈亏',
    `profit_rate`        DECIMAL(10, 4)          DEFAULT NULL COMMENT '盈亏率（%）',
    `created_at`         DATETIME                DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `updated_at`         DATETIME                DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_positions_user_stock` (`user_id`, `stock_code`),
    KEY `idx_positions_user_id` (`user_id`)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4 COMMENT ='持仓表';

CREATE TABLE IF NOT EXISTS `trade_records` (
    `id`         BIGINT         NOT NULL AUTO_INCREMENT COMMENT '记录ID',
    `user_id`    BIGINT         NOT NULL COMMENT '用户ID',
    `order_id`   BIGINT         NOT NULL COMMENT '订单ID',
    `stock_code` VARCHAR(10)    NOT NULL COMMENT '股票代码',
    `trade_type` VARCHAR(10)    NOT NULL COMMENT '交易类型：buy-买入，sell-卖出',
    `price`      DECIMAL(10, 2) NOT NULL COMMENT '成交价格',
    `quantity`   INT            NOT NULL COMMENT '成交数量（股）',
    `amount`     DECIMAL(18, 2) NOT NULL COMMENT '成交金额',
    `fee`        DECIMAL(10, 2)          DEFAULT 0.00 COMMENT '手续费',
    `trade_time` DATETIME       NOT NULL COMMENT '成交时间',
    `created_at` DATETIME                DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    PRIMARY KEY (`id`),
    KEY `idx_trade_records_user_id` (`user_id`),
    KEY `idx_trade_records_order_id` (`order_id`),
    KEY `idx_trade_records_stock_code` (`stock_code`),
    KEY `idx_trade_records_trade_time` (`trade_time`)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4 COMMENT ='交易记录表';

-- ============================================================
-- 回测模块
-- ============================================================

CREATE TABLE IF NOT EXISTS `strategies` (
    `id`              BIGINT         NOT NULL AUTO_INCREMENT COMMENT '策略ID',
    `user_id`         BIGINT         NOT NULL COMMENT '用户ID',
    `strategy_name`   VARCHAR(100)   NOT NULL COMMENT '策略名称',
    `strategy_type`   VARCHAR(50)    NOT NULL COMMENT '策略类型：ma-均线，breakout-突破，grid-网格，custom-自定义',
    `parameters`      TEXT COMMENT '策略参数（JSON格式）',
    `initial_capital` DECIMAL(18, 2) DEFAULT 1000000.00 COMMENT '初始资金',
    `description`     TEXT COMMENT '策略描述',
    `created_at`      DATETIME       DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `updated_at`      DATETIME       DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (`id`),
    KEY `idx_strategies_user_id` (`user_id`)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4 COMMENT ='策略表';

CREATE TABLE IF NOT EXISTS `backtest_results` (
    `id`              BIGINT         NOT NULL AUTO_INCREMENT COMMENT '回测ID',
    `strategy_id`     BIGINT         NOT NULL COMMENT '策略ID',
    `stock_code`      VARCHAR(10)    NOT NULL COMMENT '股票代码',
    `start_date`      DATE           NOT NULL COMMENT '回测开始日期',
    `end_date`        DATE           NOT NULL COMMENT '回测结束日期',
    `initial_capital` DECIMAL(18, 2) NOT NULL COMMENT '初始资金',
    `final_capital`   DECIMAL(18, 2) NOT NULL COMMENT '最终资金',
    `total_return`    DECIMAL(18, 2) NOT NULL COMMENT '总收益',
    `return_rate`     DECIMAL(10, 4) NOT NULL COMMENT '收益率（%）',
    `max_drawdown`    DECIMAL(10, 4)          DEFAULT NULL COMMENT '最大回撤（%）',
    `sharpe_ratio`    DECIMAL(10, 4)          DEFAULT NULL COMMENT '夏普比率',
    `win_rate`        DECIMAL(10, 4)          DEFAULT NULL COMMENT '胜率（%）',
    `trade_count`     INT                     DEFAULT 0 COMMENT '交易次数',
    `report_url`      VARCHAR(500)            DEFAULT NULL COMMENT '回测报告URL',
    `created_at`      DATETIME                DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    PRIMARY KEY (`id`),
    KEY `idx_backtest_results_strategy_id` (`strategy_id`),
    KEY `idx_backtest_results_stock_code` (`stock_code`)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4 COMMENT ='回测结果表';

-- ============================================================
-- 风险模块
-- ============================================================

CREATE TABLE IF NOT EXISTS `risk_alerts` (
    `id`          BIGINT      NOT NULL AUTO_INCREMENT COMMENT '预警ID',
    `user_id`     BIGINT      NOT NULL COMMENT '用户ID',
    `alert_type`  VARCHAR(50) NOT NULL COMMENT '预警类型：concentration-持仓集中，drawdown-回撤过大，volatility-波动过大',
    `alert_level` VARCHAR(20) NOT NULL COMMENT '预警级别：low-低，medium-中，high-高',
    `message`     TEXT        NOT NULL COMMENT '预警消息',
    `is_read`     TINYINT(1)           DEFAULT 0 COMMENT '是否已读：0-未读，1-已读',
    `created_at`  DATETIME             DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    PRIMARY KEY (`id`),
    KEY `idx_risk_alerts_user_id` (`user_id`),
    KEY `idx_risk_alerts_is_read` (`is_read`),
    KEY `idx_risk_alerts_created_at` (`created_at`)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4 COMMENT ='风险预警表';

CREATE TABLE IF NOT EXISTS `stop_loss_profit` (
    `id`                BIGINT         NOT NULL AUTO_INCREMENT COMMENT '配置ID',
    `user_id`           BIGINT         NOT NULL COMMENT '用户ID',
    `stock_code`        VARCHAR(10)    NOT NULL COMMENT '股票代码',
    `stop_loss_price`   DECIMAL(10, 2)          DEFAULT NULL COMMENT '止损价',
    `stop_profit_price` DECIMAL(10, 2)          DEFAULT NULL COMMENT '止盈价',
    `notify_method`     VARCHAR(50)             DEFAULT 'message' COMMENT '通知方式：message-站内消息，email-邮件，sms-短信',
    `is_enabled`        TINYINT(1)              DEFAULT 1 COMMENT '是否启用：0-禁用，1-启用',
    `created_at`        DATETIME                DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `updated_at`        DATETIME                DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_stop_loss_profit_user_stock` (`user_id`, `stock_code`),
    KEY `idx_stop_loss_profit_user_id` (`user_id`)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4 COMMENT ='止损止盈配置表';

-- ============================================================
-- 测试数据
-- ============================================================

-- 常用股票
INSERT IGNORE INTO `stock_info` (`stock_code`, `stock_name`, `market`, `industry`)
VALUES ('600519', '贵州茅台', 'SH', '白酒'),
       ('000001', '平安银行', 'SZ', '银行'),
       ('000002', '万科A', 'SZ', '房地产'),
       ('600036', '招商银行', 'SH', '银行'),
       ('601318', '中国平安', 'SH', '保险'),
       ('600276', '恒瑞医药', 'SH', '医药'),
       ('000858', '五粮液', 'SZ', '白酒'),
       ('002415', '海康威视', 'SZ', '安防'),
       ('600887', '伊利股份', 'SH', '食品'),
       ('601888', '中国中免', 'SH', '零售'),
       ('300750', '宁德时代', 'SZ', '新能源'),
       ('600900', '长江电力', 'SH', '电力'),
       ('601166', '兴业银行', 'SH', '银行'),
       ('000568', '泸州老窖', 'SZ', '白酒'),
       ('600309', '万华化学', 'SH', '化工');
