# SEAL Backend — Developer Guide (ĐỌC TRƯỚC KHI CODE)

> SWP391 · SEAL Hackathon Management System · 3 BE devs làm song song 6 main-flows.
> File này là điểm vào duy nhất. Đọc hết 1 lượt (~15 phút) trước khi viết dòng code đầu tiên.

**Đọc kèm:** [`WORK_SPLIT.md`](WORK_SPLIT.md) (phân công + quy tắc chống conflict) · [`CONTRIBUTING.md`](CONTRIBUTING.md) (Git + cách chạy) · `db/schema.sql` (nguồn sự thật DB) · `docs/SEAL_SRS_v1.2.docx` (yêu cầu).

---

## 1. Bối cảnh & phạm vi (từ SRS v1.2)

SEAL số hoá toàn bộ vòng đời một cuộc thi hackathon: tạo sự kiện → đăng ký đội → nộp bài → giám khảo chấm → xếp hạng/thăng hạng → trao giải/công bố. Phạm vi nhóm đã chốt với giảng viên gồm **6 main-flows**:

1. Event and round configuration
2. Team registration and account approval
3. Project submission tracking
4. Judge scoring and evaluation
5. Automated ranking and promotion logic
6. Award management and result publication

Các module ngoài 6 flow (governance, budget, RBL, analytics, admin) **chưa thuộc scope hiện tại** — entity vẫn có sẵn trong `domain/` để DB toàn vẹn, ai làm tới đâu mở package tới đó.

**Tech stack:** Java 21 · Spring Boot 3.5 · Spring Security (JWT) · Spring Data JPA · MySQL 8 · springdoc/Swagger · Lombok · Maven.

---

## 2. Chạy local lần đầu

```bash
# 1. Tạo DB từ nguồn sự thật (đã gồm seed data demo)
mysql -u root -p < db/schema.sql

# 2. Cấu hình: copy .env.example -> .env và điền DB_PASSWORD, JWT_SECRET...
cp .env.example .env

# 3. Chạy
./mvnw spring-boot:run

# 4. Mở Swagger
#    http://localhost:8080/swagger-ui.html  -> mỗi module có GET /api/<module>/ping
```

> `application.yaml` đặt `ddl-auto: none` — Hibernate **không** tự đổi DB. Mọi thay đổi schema đi qua `db/schema.sql` (PR riêng do Lead chủ trì). Seed data có sẵn giúp bạn test module mà không cần chờ module khác.

---

## 3. Cấu trúc thư mục & ý nghĩa

```
src/main/java/com/seal/seal_backend/
├── SealBackendApplication.java
│
├── common/                 # Kỹ thuật dùng chung — KHÔNG chứa nghiệp vụ   (FREEZE, Lead)
│   ├── api/                #   ApiResponse, PageResponse (vỏ response trả client)
│   ├── exception/          #   BusinessRuleException, ResourceNotFoundException,
│   │                       #   ForbiddenActionException, GlobalExceptionHandler
│   ├── audit/              #   AuditAction + AuditPublisher (log BR-AUD bằng 1 dòng)
│   └── security/           #   CurrentUser (inject user đang đăng nhập)
│
├── config/                 # SecurityConfig, OpenApiConfig, CorsConfig, DotEnv  (FREEZE, Lead)
│
├── domain/                 # KERNEL DÙNG CHUNG — nền của mọi module          (FREEZE)
│   ├── entity/             #   29 JPA entity (+ TeamMemberId) map 1-1 với bảng
│   ├── enums/              #   18 enum, khớp đúng CHECK constraint của DB
│   └── repository/         #   JpaRepository mỗi entity  (ĐƯỢC thêm method mới)
│
├── shared/contract/        # Port + DTO để gọi CHÉO module (đọc dữ liệu module khác)
│   └── EventQueryPort, TeamQueryPort, SubmissionQueryPort,
│       ScoringQueryPort, RankingQueryPort  + dto/
│
│   ──────── MỖI MODULE = 1 flow: controller │ service(+impl) │ dto │ (mapper) ────────
├── auth/          ← M1     # FR-AUTH: register/login/JWT/RBAC + duyệt account
├── event/         ← M1     # (1) Event & Round config
├── team/          ← M1     # (2) Team registration & account approval
├── submission/    ← M2     # (3) Project submission tracking
├── scoring/       ← M2     # (4) Judge scoring & evaluation
├── ranking/       ← M3     # (5) Ranking & promotion
├── award/         ← M3     # (6) Award & publication
└── notification/  ← M3     # FR-AWD-02 thông báo kết quả
```

**Vì sao chia kiểu này (Hybrid):** code viết hằng ngày (controller/service/dto) nằm gọn trong module của từng người → ít chạm file của nhau → ít conflict. Còn entity/enums/repository để **tập trung** ở `domain/` vì schema có FK chéo module rất dày (vd `Evaluation → Submission + ScoringCriterion + Round + User`); để chung giúp `@ManyToOne` đơn giản, tránh phụ thuộc vòng giữa các module.

---

## 4. Phân công & sở hữu (tóm tắt — chi tiết ở WORK_SPLIT.md)

| Member | Sở hữu (CHỈ người này sửa) |
|---|---|
| **M1 — Đồng Thành Minh Trí** (Lead) | `auth/**`, `event/**`, `team/**`, `config/SecurityConfig.java` |
| **M2 — Lê Quang Hải** | `submission/**`, `scoring/**` |
| **M3 — Nguyễn Công Thiên Ân** | `ranking/**`, `award/**`, `notification/**` |

**Vùng FREEZE (đụng vào phải mở PR riêng, cả nhóm review):** `common/`, `config/`, `domain/entity`, `domain/enums`, `shared/contract`, `db/schema.sql`, `pom.xml`, `application.yaml`.
`domain/repository`: được **thêm** file/method mới, **không sửa** method người khác đang dùng.

---

## 5. Nguyên tắc kiến trúc (bắt buộc)

1. **Phân tầng 1 chiều:** `Controller (HTTP) → Service (nghiệp vụ + @Transactional) → Repository (dữ liệu)`. Controller **không** gọi repository thẳng, **không** chứa logic. Repository **không** chứa logic.
2. **Đọc dữ liệu module khác → qua Port** trong `shared/contract` (đã có adapter stub nên project chạy ngay). **Ghi/nghiệp vụ → chỉ trong module mình.** Không import class nội bộ của module khác.
3. **RBAC ở method-level** `@PreAuthorize(...)` trên controller → để `SecurityConfig` đứng yên, không thành điểm nóng conflict.
4. **Không trả entity ra ngoài.** Luôn map sang Response DTO.

---

## 6. Quy ước coding (mọi người theo giống nhau)

### 6.1 Service / ServiceImpl
- `service/XxxService.java` (interface) + `service/impl/XxxServiceImpl.java` (`@Service`).
- **Constructor injection** (field `final`, dùng Lombok `@RequiredArgsConstructor`), không `@Autowired` lên field.
- `@Transactional` đặt ở **Impl**: method đọc `@Transactional(readOnly = true)`, method ghi `@Transactional`. Không đặt ở controller/repository.

### 6.2 Request / Response DTO
- Dùng **Java `record`**; đặt ở `dto/request` và `dto/response`.
- Tên theo hành động: `CreateEventRequest`, `UpdateEventRequest`, `EventResponse`, `EventSummaryResponse`.
- **Validation định dạng** (rỗng/email/độ dài/range) bằng Bean Validation trên Request DTO: `@NotBlank @Email @Size @NotNull @Min...`.
- Map entity↔DTO ở `mapper/` hoặc static factory `EventResponse.from(entity)` — **không** map trong controller.

```java
public record CreateEventRequest(
    @NotBlank @Size(max = 200) String name,
    @NotNull Long disciplineId,
    @NotNull EventType eventType) {}

public record EventResponse(Long id, String name, EventStatus status) {
    public static EventResponse from(Event e) {
        return new EventResponse(e.getId(), e.getName(), e.getStatus());
    }
}
```

### 6.3 Exception — 2 tầng, xử lý tập trung

| Loại lỗi | Ném ở đâu | Exception | HTTP |
|---|---|---|---|
| Sai định dạng input | `@Valid` tự bắt | `MethodArgumentNotValidException` | 400 |
| Vi phạm rule nghiệp vụ | Service | `BusinessRuleException("BR-XXX", msg)` | 409 |
| Không tìm thấy | Service | `ResourceNotFoundException` | 404 |
| Sai quyền / scope | Service | `ForbiddenActionException` | 403 |

- Custom exception **extends `RuntimeException`** (unchecked) → Spring tự rollback.
- **KHÔNG `try/catch` trong controller** cho các lỗi trên — để `GlobalExceptionHandler` (`@RestControllerAdvice`) trả body đồng nhất `ApiResponse.fail(...)`.
- Gắn **mã rule** (đúng `BR-*` trong SRS) vào exception để frontend xử lý theo code và dễ trace ngược tài liệu.
- **Không lộ stack trace/message nội bộ** ra client; log server bằng SLF4J (`LoggerFactory`), không `System.out.println`.

### 6.4 Controller — viết "mỏng"
- Chỉ: nhận input → `@Valid` → gọi service → bọc `ApiResponse` → trả. Không logic.
- URL: danh từ số nhiều, lồng theo sở hữu: `/api/events`, `/api/events/{id}/rounds`. `{id}` cho định danh, query param cho lọc/phân trang.
- HTTP verb: `GET` đọc · `POST` tạo (**201**) · `PUT/PATCH` sửa · `DELETE` (**204**).
- Mọi response bọc `ApiResponse<T>`; danh sách dài → `PageResponse.of(page)`.

```java
@RestController
@RequestMapping("/api/events")
@Tag(name = "Event & Round Configuration")
@RequiredArgsConstructor
public class EventController {
    private final EventService eventService;

    @PostMapping
    @PreAuthorize("hasRole('COORDINATOR')")
    public ResponseEntity<ApiResponse<EventResponse>> create(
            @Valid @RequestBody CreateEventRequest req, @CurrentUser UserPrincipal user) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.ok(eventService.create(req, user.getId())));
    }
}
```

### 6.5 Audit (cho hành động nhạy cảm)
Mọi hành động trong **BR-AUD-02** (duyệt account, lock round, disqualify, publish, approve event/budget, đổi role/config...): gọi
`auditPublisher.log(actor, AuditAction.XXX, "EVENT", id, oldJson, newJson, reason, ip)` ngay trong service — đừng tự viết log riêng.

### 6.6 Linh tinh hay quên
Đặt mọi trạng thái/loại vào **enum** thay vì string rời · method nhỏ, một nhiệm vụ · không hardcode magic number · phân trang đừng trả hết bản ghi · tên biến/method tiếng Anh, rõ nghĩa.

---

## 7. Business Rule PHẢI enforce ở service (DB không tự chặn)

Đây là phần dễ quên nhất khi code nhanh. Mỗi rule → `BusinessRuleException("BR-XXX", ...)` + **unit test**.

**M1 (auth/event/team):** BR-TEAM-01 size 3–5 · BR-TEAM-02 1 user 1 team/event · BR-TEAM-03 1 category/team · BR-EVT-03 tổng weight = 100% · BR-EVT-04 Mentor ≠ Judge cùng category · BR-GOV-02 không tự duyệt event của mình · BR-GOV-06 sửa budget đã duyệt → revert PENDING.

**M2 (submission/scoring):** BR-SCR-01 `score_value ∈ [0, max_score]` (DB chỉ check ≥0) · chống trùng judge assignment · BR-SUB-05 chỉ team `promoted` mới submit round sau.

**M3 (ranking/award):** uniqueness ranking theo scope (vì `category_id` nullable cho ranking cấp event) · BR-RNK-02 công thức `SUM(avg_per_criterion × weight)` · BR-RNK-04 loại team bị disqualify khỏi xếp hạng.

---

## 8. Definition of Done (mỗi flow)

- [ ] Controller + Service(+Impl) + DTO trong package của mình.
- [ ] Business rule liên quan (§7) đã enforce ở service + có unit test.
- [ ] Endpoint hiện trên Swagger, trả `ApiResponse<T>`.
- [ ] Lỗi ném đúng `BusinessRuleException` / `ResourceNotFoundException` / `ForbiddenActionException`.
- [ ] Hành động nhạy cảm đã gọi `AuditPublisher`.
- [ ] `./mvnw verify` xanh; PR nhỏ, được 1 người review.

---

## 9. Quy tắc Git (tóm tắt — chi tiết ở CONTRIBUTING.md)

`main` không push thẳng · nhánh `feature/<flow>/<việc>` · **chỉ commit trong package mình** · **rebase `main` mỗi sáng** · PR nhỏ (≤ ~400 dòng) + 1 reviewer · commit dạng `scoring: add per-criterion input (FR-SCR-01)`.

---

## 10. Checklist trước khi gõ dòng code đầu tiên

1. [ ] Đã chạy được app + mở Swagger thấy `/ping` các module.
2. [ ] Đã đọc `WORK_SPLIT.md` — biết mình sở hữu package nào, vùng nào FREEZE.
3. [ ] Đã đọc các FR/BR liên quan flow của mình trong SRS.
4. [ ] Đã xem các bảng + Port mình sẽ dùng trong `db/schema.sql` và `shared/contract`.
5. [ ] Nắm §5 (kiến trúc) + §6 (quy ước) ở trên.
```
