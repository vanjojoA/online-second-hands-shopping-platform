-- 收藏 + 交易额统计所需的表结构变更（已在本项目云端库执行过）

-- 1. 商品收藏表
CREATE TABLE IF NOT EXISTS favorite (
  id BIGINT NOT NULL AUTO_INCREMENT,
  user_id BIGINT NOT NULL,
  product_id BIGINT NOT NULL,
  create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (id),
  UNIQUE KEY uk_user_product (user_id, product_id),
  KEY idx_user (user_id)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4;

-- 2. 商品成交时间（交易额统计用）
-- 如果已经加过这一列，执行会报 "Duplicate column name"，忽略即可
ALTER TABLE product ADD COLUMN sold_time DATETIME NULL AFTER status;

-- 3. 历史已售商品补一个成交时间（用发布时间兜底）
UPDATE product SET sold_time = create_time WHERE status = 2 AND sold_time IS NULL;
