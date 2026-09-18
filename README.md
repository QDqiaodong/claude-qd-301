# 建筑工地 · 料具堆场与安全巡检

工地的日常台账：**堆场分区**、**材料台账**、**进出场流水**、**安全巡检**。

这张仓的业务重心在「进出场流水」—— 材料的结存不是手填的，而是进场加、出场减，
一笔一笔流水累出来的；出场时结存不够会被拦住。想调整数量只能补反向流水，改不了历史。

## 技术栈

- 后端：Spring Boot 3.3 / Java 17、Spring Data JPA、MySQL 8、Redis 7
- 前端：Vue 3（Options API）+ Element Plus + Vite，nginx 反代
- 一键起：`./start.sh`（内部就是 `docker compose up -d --build`）

## 业务模块

1. **堆场分区**（`yard`）—— 堆场编号与名称、占地/可堆量、可用与停用（停用前要求清空材料）
2. **材料台账**（`material`）—— 材料编号、类别、归属堆场、结存（只读，由流水推出来）
3. **进出场流水**（`material_movement`）—— 进场 / 出场登记，自动加减结存，出入库留痕
4. **安全巡检**（`safety_inspection`）—— 打分、判定合格与否、不合格整改到闭环

## 本地跑起来

| | 地址 |
| --- | --- |
| 前端页面 | http://127.0.0.1:8231/ |
| 后端接口 | http://127.0.0.1:8331/api/yards |
| MySQL | 127.0.0.1:3531（库 `construction_site`） |
| Redis | 127.0.0.1:6531 |

容器名统一是 `claude-qd-301-{mysql,redis,backend,frontend}`。

```bash
./start.sh              # 起容器（首次构建要几分钟）
docker compose ps       # 看状态
docker compose down -v  # 停掉并清数据
```
