# CONTRIBUTING — Quy tắc Git & cách chạy (SEAL Backend)

## 0. Chạy lần đầu

1. **DB:** tạo schema từ nguồn sự thật (đã gồm seed data):
   ```bash
   mysql -u root -p < db/schema.sql      # tạo database seal_db trên cổng trong .env (mặc định 3307)
   ```
2. **Cấu hình:** copy/sửa `.env` (DB_URL, DB_USERNAME, DB_PASSWORD, JWT_SECRET...).
   > `application.yaml` đặt `ddl-auto: none` — Hibernate **không** tự đổi DB; schema do `db/schema.sql` quản lý. Chỉ đổi sang `validate` khi entity đã ổn định 100%.
3. **Build & run:**
   ```bash
   ./mvnw spring-boot:run
   ```
4. Mở Swagger: `http://localhost:8080/swagger-ui.html` → mỗi module có `GET /api/<module>/ping`.

## 1. Nhánh (branch)

- `main`: luôn build xanh, **không push thẳng**.
- Nhánh theo flow: `feature/event-config`, `feature/team-reg`, `feature/submission`, `feature/scoring`, `feature/ranking`, `feature/award`.
- Nhánh nhỏ cho từng việc: `feature/scoring/score-input`.

## 2. Vàng — chống conflict

1. **Chỉ commit trong package mình sở hữu** (xem `WORK_SPLIT.md`). Không sửa file người khác.
2. **`domain/repository`**: được thêm file/method mới, **không sửa method người khác đang dùng**.
3. **Đụng vùng FREEZE** (`entity`, `enums`, `common`, `config`, `shared/contract`, `pom.xml`, `application.yaml`, `db/schema.sql`)? → PR riêng, tag cả nhóm, merge trước khi ai khác rebase.
4. **RBAC để ở method-level** `@PreAuthorize(...)` trên controller — để `SecurityConfig` đứng yên, không thành điểm nóng conflict.
5. **Rebase `main` mỗi sáng**: `git fetch origin && git rebase origin/main`.
6. **PR nhỏ, thường xuyên** (≤ ~400 dòng). Đừng dồn 1 PR khổng lồ cuối tuần.

## 3. Quy trình PR

1. `git rebase origin/main` (xử lý conflict tại máy mình).
2. Đảm bảo `./mvnw -q clean verify` xanh (build + test).
3. Mở PR vào `main`, mô tả: flow nào, FR/BR nào, endpoint mới.
4. **1 thành viên khác review** rồi mới merge (squash merge).

## 4. Commit message

```
<flow>: <mô tả ngắn>        # vd: scoring: add per-criterion score input (FR-SCR-01)
```
Prefix: `event` `team` `submission` `scoring` `ranking` `award` `common` `config` `db`.

## 5. Đổi schema DB

- Sửa **`db/schema.sql`** (nguồn sự thật) → PR riêng do Lead chủ trì.
- Cập nhật entity/enum tương ứng trong cùng PR đó.
- Báo cả nhóm chạy lại `db/schema.sql` sau khi merge (script đầu file đã `DROP DATABASE` cho môi trường dev).

## 6. Definition of Done cho mỗi flow

- [ ] Controller + Service + DTO trong package của mình.
- [ ] Các Business Rule liên quan (xem `WORK_SPLIT.md` §5) enforce ở service + có unit test.
- [ ] Endpoint hiện trên Swagger, trả `ApiResponse<T>`.
- [ ] Lỗi nghiệp vụ ném `BusinessRuleException`/`ForbiddenActionException`/`ResourceNotFoundException`.
- [ ] `./mvnw verify` xanh; PR được 1 người review.
