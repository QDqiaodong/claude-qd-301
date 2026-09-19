-- 建筑工地 · 料具堆场与安全巡检
-- 结存口径：material.balance = 该材料所有进场流水之和 - 出场流水之和（种子数据已对齐）
-- 余量口径：可用余量 = 结存 - 「占用中」浇筑预扣合计；预扣只占余量不动结存，开盘兑现时才补等量出场流水
SET NAMES utf8mb4;

DROP TABLE IF EXISTS pour_reservation;
DROP TABLE IF EXISTS safety_inspection;
DROP TABLE IF EXISTS material_movement;
DROP TABLE IF EXISTS material;
DROP TABLE IF EXISTS yard;

CREATE TABLE yard (
  id         BIGINT       NOT NULL AUTO_INCREMENT,
  no         VARCHAR(32)  NOT NULL COMMENT '堆场编号',
  title      VARCHAR(64)  NOT NULL COMMENT '堆场名称',
  area_size  INT          NULL     COMMENT '占地面积（㎡）',
  max_load   INT          NULL     COMMENT '可堆量（件）',
  state      VARCHAR(16)  NOT NULL COMMENT '可用 / 停用',
  PRIMARY KEY (id),
  UNIQUE KEY uk_yard_no (no)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE material (
  id       BIGINT       NOT NULL AUTO_INCREMENT,
  no       VARCHAR(32)  NOT NULL COMMENT '材料编号',
  title    VARCHAR(64)  NOT NULL COMMENT '材料名称',
  category VARCHAR(16)  NOT NULL COMMENT '钢筋 / 水泥 / 砂石 / 模板',
  yard_id  BIGINT       NULL     COMMENT '所在堆场',
  balance  INT          NOT NULL DEFAULT 0 COMMENT '结存（由流水累加）',
  state    VARCHAR(16)  NOT NULL COMMENT '在库 / 已清空',
  PRIMARY KEY (id),
  UNIQUE KEY uk_material_no (no),
  KEY idx_material_yard (yard_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE material_movement (
  id          BIGINT      NOT NULL AUTO_INCREMENT,
  no          VARCHAR(32) NOT NULL COMMENT '流水单号',
  material_id BIGINT      NOT NULL COMMENT '对应材料',
  direction   VARCHAR(16) NOT NULL COMMENT '进场 / 出场',
  amount      INT         NOT NULL COMMENT '数量',
  move_date   DATE        NULL     COMMENT '进出场日期',
  handler     VARCHAR(32) NOT NULL COMMENT '经办人',
  PRIMARY KEY (id),
  UNIQUE KEY uk_movement_no (no),
  KEY idx_movement_material (material_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE safety_inspection (
  id           BIGINT      NOT NULL AUTO_INCREMENT,
  no           VARCHAR(32) NOT NULL COMMENT '巡检单号',
  yard_id      BIGINT      NOT NULL COMMENT '被巡检堆场',
  inspect_date DATE        NULL     COMMENT '巡检日期',
  inspector    VARCHAR(32) NOT NULL COMMENT '巡检人',
  score        INT         NOT NULL COMMENT '得分 0~100',
  verdict      VARCHAR(16) NOT NULL COMMENT '合格 / 不合格',
  state        VARCHAR(16) NOT NULL COMMENT '待整改 / 已闭环',
  PRIMARY KEY (id),
  UNIQUE KEY uk_inspection_no (no),
  KEY idx_inspection_yard (yard_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- 浇筑配料预扣：开盘前先把料从堆场占住，只占可用余量、不动账面结存。
-- 占用中 → 已兑现（补一笔等量出场流水）或已作废（余量还回），只走一次。
CREATE TABLE pour_reservation (
  id          BIGINT      NOT NULL AUTO_INCREMENT,
  no          VARCHAR(32) NOT NULL COMMENT '预扣单号',
  yard_id     BIGINT      NOT NULL COMMENT '从哪个堆场占料',
  material_id BIGINT      NOT NULL COMMENT '占哪批材料',
  amount      INT         NOT NULL COMMENT '预扣数量',
  plan_date   DATE        NULL     COMMENT '计划开盘日',
  state       VARCHAR(16) NOT NULL COMMENT '占用中 / 已兑现 / 已作废',
  movement_no VARCHAR(32) NULL     COMMENT '兑现时补的那笔出场流水单号',
  PRIMARY KEY (id),
  UNIQUE KEY uk_reservation_no (no),
  KEY idx_reservation_material (material_id),
  KEY idx_reservation_yard (yard_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

INSERT INTO yard (no, title, area_size, max_load, state) VALUES
('Y-01', '主楼东侧堆场', 320, 600, '可用'),
('Y-02', '钢筋加工区堆场', 180, 400, '可用'),
('Y-03', '地下车库顶板堆场', 240, 300, '停用'),
('Y-04', '生活区临时堆场', 90, 120, '可用');

INSERT INTO material (no, title, category, yard_id, balance, state) VALUES
('M-01', '螺纹钢 HRB400', '钢筋', 1, 90, '在库'),
('M-02', '普通硅酸盐水泥', '水泥', 1, 80, '在库'),
('M-03', '中砂', '砂石', 2, 0, '已清空'),
('M-04', '木模板 15mm', '模板', 1, 160, '在库'),
('M-05', '碎石 5-25mm', '砂石', 2, 0, '已清空');

INSERT INTO material_movement (no, material_id, direction, amount, move_date, handler) VALUES
('MV-001', 1, '进场', 120, '2026-09-10', '老张'),
('MV-002', 2, '进场',  80, '2026-09-10', '老张'),
('MV-003', 3, '进场',  60, '2026-09-11', '老李'),
('MV-004', 4, '进场', 200, '2026-09-11', '老李'),
('MV-005', 1, '出场',  30, '2026-09-13', '老张'),
('MV-006', 3, '出场',  60, '2026-09-14', '老李'),
('MV-007', 5, '进场',  50, '2026-09-15', '老张'),
('MV-008', 5, '出场',  50, '2026-09-16', '老李'),
('MV-009', 4, '出场',  40, '2026-09-18', '老张');

INSERT INTO safety_inspection (no, yard_id, inspect_date, inspector, score, verdict, state) VALUES
('SC-01', 1, '2026-09-14', '王安全', 92, '合格',   '已闭环'),
('SC-02', 2, '2026-09-14', '王安全', 55, '不合格', '待整改'),
('SC-03', 1, '2026-09-16', '刘工',   88, '合格',   '已闭环'),
('SC-04', 4, '2026-09-16', '刘工',   48, '不合格', '待整改'),
('SC-05', 2, '2026-09-17', '王安全', 76, '合格',   '已闭环');

-- PR-01 占着螺纹钢 20（M-01 可用余量 = 90 - 20 = 70）；PR-02 作废不占量；PR-03 已兑现，对应出场流水 MV-009
INSERT INTO pour_reservation (no, yard_id, material_id, amount, plan_date, state, movement_no) VALUES
('PR-01', 1, 1, 20, '2026-09-21', '占用中', NULL),
('PR-02', 1, 2, 10, '2026-09-20', '已作废', NULL),
('PR-03', 1, 4, 40, '2026-09-18', '已兑现', 'MV-009');
