-- 浏览记录表（跨设备同步）
-- 同一个用户对同一个商品只保留一条，重复浏览时更新 view_time

CREATE TABLE IF NOT EXISTS browse_history (
  id BIGINT NOT NULL AUTO_INCREMENT,
  user_id BIGINT NOT NULL,
  product_id BIGINT NOT NULL,
  view_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (id),
  UNIQUE KEY uk_user_product (user_id, product_id),
  KEY idx_user_view (user_id, view_time)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4;
