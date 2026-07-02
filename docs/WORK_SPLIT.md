# SEAL Backend — Phân chia công việc (3 BE Devs)

> Mục tiêu: 3 thành viên Java BE làm **song song 6 main-flows**, mỗi người commit + có API demo riêng, **merge không conflict**.

## 1. Thành viên & sở hữu module

| Thành viên | Vai trò | Flows phụ trách | Package SỞ HỮU (chỉ người này được sửa) |
|---|---|---|---|
| **M1 — Đồng Thành Minh Trí** | Lead | Auth & RBAC, (1) Event & Round config, (2) Team registration & account approval | `auth/**`, `event/**`, `team/**`, `config/SecurityConfig.java` |
| **M2 — Lê Quang Hải** | Dev | (3) Project submission tracking, (4) Judge scoring & evaluation | `submission/**`, `scoring/**` |
| **M3 — Nguyễn Công Thiên Ân** | Dev | (5) Automated ranking & promotion, (6) Award & result publication, notifications | `ranking/**`, `award/**`, `notification/**` |

> Nhóm còn 3 người (đã loại 1). Các module ngoài 6 flow (governance, budget, rbl, analytics, admin) **chưa thuộc scope hiện tại** — entity vẫn có sẵn trong `domain/entity` để DB toàn vẹn, ai làm tới đâu mở package tới đó.

## 2. Vùng CHUNG — đã đóng băng (freeze)

Các file/thư mục dưới đây **không sửa tự do**. Muốn đổi → mở issue/PR riêng, cả nhóm review:

| Vùng | Nội dung | Chủ trì |
|---|---|---|
| `domain/entity/**` | 29 JPA entity (sinh từ `db/schema.sql`) | Lead — chỉ thêm field khi schema đổi |
| `domain/enums/**` | 18 enum khớp CHECK constraint | Lead |
| `domain/repository/**` | JpaRepository mỗi entity. **Được phép THÊM file/method mới**, không sửa file người khác | Mỗi người tự thêm query mình cần |
| `common/**` | `ApiResponse`, `api/PageResponse`, exceptions, `GlobalExceptionHandler`, `audit/` (AuditAction + AuditPublisher), `security/CurrentUser` | Lead |
| `config/**` | `SecurityConfig`, `OpenApiConfig`, `CorsConfig`, `DotEnvPostProcessor` | Lead |
| `shared/contract/**` | Port interfaces + DTO gọi chéo module | Cả nhóm chốt 1 lần ở Sprint nền |
| `db/schema.sql` | **Nguồn sự thật của DB**. Đổi schema chỉ qua 1 PR riêng | Lead |
| `pom.xml`, `application.yaml`, `.env` | Cấu hình chạy | Lead |

## 3. Sơ đồ phụ thuộc & cách làm SONG SONG (không chờ nhau)

6 flow thực chất là 1 pipeline:

```
(1) Event/Round ─┬─> (3) Submission ─> (4) Scoring ─> (5) Ranking ─> (6) Award
(2) Team ────────┘
```

Để không phải chờ module người khác, dùng **2 cơ chế**:

1. **`shared/contract` (Port + DTO):** module phụ thuộc chỉ `@Autowired` interface (vd `EventQueryPort`), KHÔNG gọi thẳng class nội bộ của người khác. Mỗi port đã có sẵn **adapter stub** (`*QueryAdapter`) trả rỗng → toàn bộ project compile & chạy ngay từ ngày 1. Owner thay phần thân stub bằng query thật sau.
2. **Seed data trong `db/schema.sql`:** DB đã có sẵn event/team/submission/score/ranking mẫu. Ví dụ M3 (ranking) test được engine tính điểm ngay trên dữ liệu seed mà **không cần chờ** M2 làm xong scoring.

| Module | Cần đọc dữ liệu của | Phụ thuộc qua |
|---|---|---|
| submission (M2) | round (M1), team (M1) | `EventQueryPort`, `TeamQueryPort` |
| scoring (M2) | round + criteria (M1), submitted attempt (M2) | `EventQueryPort`, `SubmissionQueryPort` |
| ranking (M3) | scores (M2), team (M1) | `ScoringQueryPort`, `TeamQueryPort` |
| award (M3) | ranking (M3), team (M1) | `RankingQueryPort`, `TeamQueryPort` |

**Quy tắc:** đọc dữ liệu cũ của module khác → qua Port. Ghi/nghiệp vụ → chỉ trong package của mình.

## 4. Mỗi module tự có API để báo cáo giảng viên

Mỗi flow đã có `*Controller` với endpoint `GET /api/<module>/ping` và `@Tag` Swagger riêng. Chạy app → mở `/swagger-ui.html` → mỗi người demo nhóm endpoint của mình độc lập, kể cả khi pipeline tổng chưa nối xong.

## 5. 11 Business Rule PHẢI enforce ở service layer (không có ở DB)

Đây là phần dễ quên nhất khi code nhanh — chia theo owner:

**M1 (event/team):**
- BR-TEAM-01 Team size 3–5 · BR-TEAM-02 1 user 1 team/event · BR-TEAM-03 1 category/team
- BR-EVT-03 Tổng weight tiêu chí = 100% · BR-EVT-04 Mentor ≠ Judge cùng category
- BR-GOV-02 Không tự duyệt event của mình · BR-GOV-06 Sửa budget đã duyệt → revert PENDING

**M2 (submission/scoring):**
- BR-SCR-01 `score_value` trong `[0, max_score]` (DB chỉ check ≥0) · chống trùng judge assignment
- BR-SUB-05 Chỉ team `promoted` mới submit round sau

**M3 (ranking/award):**
- Uniqueness ranking theo scope (vì `category_id` nullable cho ranking cấp event)
- BR-RNK-02 công thức `SUM(avg_per_criterion × weight)` · BR-RNK-04 loại team bị disqualify

> `dùng @PreAuthorize` ở controller cho RBAC; `BusinessRuleException("BR-XXX", ...)` cho rule nghiệp vụ → `GlobalExceptionHandler` tự map ra HTTP 409.
> Mọi hành động trong BR-AUD-02 (duyệt account, lock round, disqualify, publish, approve event/budget...): gọi `auditPublisher.log(actor, AuditAction.XXX, targetType, targetId, oldJson, newJson, reason, ip)` — đừng tự viết audit riêng.

## 6. Cấu trúc thư mục

```
src/main/java/com/seal/seal_backend/
├── SealBackendApplication.java
├── common/        ApiResponse, api/PageResponse, exceptions, GlobalExceptionHandler,
│                  audit/(AuditAction, AuditPublisher), security/CurrentUser        (FREEZE)
├── config/        SecurityConfig, OpenApiConfig, CorsConfig, DotEnvPostProcessor   (FREEZE)
├── domain/
│   ├── entity/    29 entity (+ TeamMemberId)                         (FREEZE)
│   ├── enums/     18 enum                                            (FREEZE)
│   └── repository/JpaRepository mỗi entity        (thêm method tự do)
├── shared/contract/  EventQueryPort, TeamQueryPort, ... + dto/       (chốt chung)
├── auth/    ← M1   controller │ service │ dto/(request,response) │ security │ mapper
├── event/   ← M1   controller │ service │ dto
├── team/    ← M1
├── submission/ ← M2
├── scoring/    ← M2
├── ranking/    ← M3
├── award/      ← M3
└── notification/ ← M3   controller │ service
db/schema.sql      Nguồn sự thật của DB (+ seed) — bản duy nhất
```
