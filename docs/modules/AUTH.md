# Module AUTH — Ghi chú triển khai (memo)

> Mục đích: ghi lại đã làm gì & hoạt động ra sao để **xem lại / debug / trình bày với giảng viên**.
> Owner: M1/Lead (Đồng Thành Minh Trí). Trạng thái: **đã code, chưa commit** → commit trên nhánh `feature/auth`.
> Bám: SRS §3.1 (FR-AUTH-01..06) + §13.1 (BR-USR) · bảng `users`, `roles` trong `db/schema.sql`.

---

## 1. API đã có

| Method | Path | Quyền | Request | Trả về (bọc `ApiResponse`) |
|---|---|---|---|---|
| POST | `/api/auth/register` | public | `RegisterRequest` | `RegisterResponse { userId, status:"PENDING" }` |
| POST | `/api/auth/login` | public | `LoginRequest` | `AuthResponse { accessToken, refreshToken }` |
| POST | `/api/auth/logout` | authenticated | header `Authorization: Bearer ...` | 200, client tự xoá token |

Swagger: `/swagger-ui.html` → nhóm **Auth & Account**.

---

## 2. Luồng hoạt động (để debug nhanh)

**Register** (`AuthServiceImpl.register`)
1. Email trùng? → `409 BR-USR-01`.
2. Mật khẩu khớp regex `^(?=.*[A-Z])(?=.*\d).{8,}$`? Không → `409 BR-USR-05`.
3. FPT → bắt buộc `studentId`; external → bắt buộc `studentId` + `university` (sai → `409 BR-USR-03`).
4. Lấy role `TEAM_MEMBER` từ DB (thiếu seed → `404`), tạo `User`: hash BCrypt, `accountType=PARTICIPANT`, `status=PENDING`.
5. Trả `userId`. **Chưa login được tới khi được duyệt thành ACTIVE.**

**Login** (`AuthServiceImpl.login`)
1. Tìm theo email; sai email **hoặc** sai mật khẩu → cùng thông báo chung `BR-AUTH-01` (chống dò tài khoản).
2. Chặn theo `status`: PENDING→`BR-AUTH-02`, LOCKED→`BR-AUTH-03`, REJECTED→`BR-AUTH-04`, INACTIVE→`BR-AUTH-05`. Chỉ ACTIVE đi tiếp.
3. Cập nhật `last_login_at` → phát `accessToken` + `refreshToken`.

**Logout** — JWT stateless: endpoint **không** thu hồi token phía server (chưa có bảng blacklist); hợp đồng hiện tại là **client xoá token**.

**Approve / Reject account** (service đã có, **chưa có endpoint controller**): `approveAccount` cấm tự duyệt mình (`BR-GOV-02`), chỉ duyệt account PENDING, ghi `AuditPublisher` (ACCOUNT_APPROVED/REJECTED).

---

## 3. Business rule đã enforce (map để báo cáo)

| BR / FR | Nội dung | Nơi enforce |
|---|---|---|
| BR-USR-01 | Email duy nhất | `register` (existsByEmail) |
| BR-USR-02 | Participant khởi tạo PENDING + role TEAM_MEMBER | `register` |
| BR-USR-03 | FPT cần studentId; external cần studentId + university | `register` |
| BR-USR-05 / NFR-SEC-02 | Mật khẩu ≥8, ≥1 hoa, ≥1 số; BCrypt cost ≥10 | `register` (regex + BCrypt) |
| FR-AUTH-05 | Chặn login account non-ACTIVE | `login` (switch status) |
| FR-AUTH-06 | RBAC 7 role | JWT claim `role` + `@PreAuthorize` |
| BR-GOV-02 | Không tự duyệt account của mình | `approveAccount` |

---

## 4. Thiết kế JWT & Security (để debug auth lỗi)

- **`JwtTokenProvider`**: access token mang `subject = userId`, claim `role`; refresh token mang `type=refresh`. Hết hạn đọc từ `jwt.expiration` / `jwt.refresh-expiration`.
- **`JWT_SECRET` trong `.env` PHẢI là Base64 của ≥32 byte** (`openssl rand -base64 32`). Sai định dạng → app crash lúc khởi tạo `JwtTokenProvider`. ⚠️ Đây là lỗi hay gặp nhất.
- **`JwtAuthFilter`** (OncePerRequestFilter): đọc header `Authorization: Bearer`, validate, set `SecurityContext` từ `UserPrincipal`.
- **`SecurityConfig`**: stateless, public cho `/api/auth/register|login` + swagger; còn lại `authenticated()`; `BCryptPasswordEncoder`; `DaoAuthenticationProvider`; `@EnableMethodSecurity` (RBAC ở method-level — không thêm rule path-level vào file này).
- Role trong token dạng `ROLE_<CODE>` → `@PreAuthorize("hasRole('JUDGE')")` dùng được.

---

## 5. File liên quan

```
auth/controller/AuthController.java        3 endpoint
auth/service/AuthService.java              interface (+ logout, approve, reject)
auth/service/impl/AuthServiceImpl.java     toàn bộ logic (190 dòng)
auth/security/JwtTokenProvider.java        tạo/validate token
auth/security/JwtAuthFilter.java           filter mỗi request
auth/security/UserPrincipal.java           UserDetails từ User entity
auth/security/CustomUserDetailsService.java load theo email/id
auth/dto/request/{RegisterRequest,LoginRequest}.java
auth/dto/response/{AuthResponse,RegisterResponse}.java
config/SecurityConfig.java · config/CorsConfig.java
domain/repository/UserRepository.java  (+findByEmail, existsByEmail)
domain/repository/RoleRepository.java  (+findByCode)
pom.xml  (+jjwt 0.12.6, +H2 test)
src/test/java/.../auth/AuthServiceImplTest.java  (14 test)
src/test/resources/application.properties        (H2 + test secret)
```

---

## 6. Cách test nhanh (Swagger)

1. `./mvnw clean test` phải xanh.
2. Chạy app → mở Swagger → `register` user mới → trả `userId`, status PENDING.
3. **Login user vừa tạo sẽ bị chặn (PENDING)** — đúng thiết kế.
4. Để login được, dùng **tài khoản seed đã ACTIVE** (`admin@seal.local`...) — **nhưng phải thay hash giả trước** (xem §7).

---

## 7. Việc còn nợ / điểm cần chú ý (TODO)

- [ ] **Seed dùng hash giả `$2a$10$replace_...`** → 9 user seed **không login được**. Phải thay bằng BCrypt thật của 1 mật khẩu biết trước. → *prompt kế tiếp §8*.
- [ ] **Chưa có endpoint approve/reject** (FR-AUTH-08) — service có sẵn, cần controller (Coordinator/Admin) để duyệt user PENDING.
- [ ] **Token chỉ mang 1 role (primary_role)** — role theo phạm vi sự kiện (`user_role_assignments`, vd JUDGE của event X) chưa nằm trong token. Đủ cho MVP, ghi nhận để mở rộng.
- [ ] **Chưa có refresh endpoint** và **chưa thu hồi token** (logout no-op). Cần bảng blacklist/refresh nếu muốn revoke thật.
- [ ] **Test chạy H2** — H2 không có trigger/JSON/CHECK như MySQL; nếu test sau này nạp `schema.sql` có thể lệch dialect. Theo dõi.
- [ ] **Commit Auth** trên `feature/auth` → PR → merge (đang chưa commit).

---

## 8. Kế hoạch prompt tiếp theo

**Prompt A (nhỏ, làm ngay — mở khoá login bằng seed):**
> "Trong project seal-backend: (1) viết một test/JUnit tạm in ra `new BCryptPasswordEncoder().encode("Password1")`; (2) thay 9 giá trị `$2a$10$replace_with_real_bcrypt_hash_*` trong `db/schema.sql` bằng hash thật của mật khẩu `Password1`; (3) thêm endpoint `POST /api/auth/accounts/{id}/approve` và `/reject` trong AuthController gọi `approveAccount/rejectAccount`, bảo vệ bằng `@PreAuthorize` cho COORDINATOR/ADMIN. Chạy `./mvnw test` và verify login `admin@seal.local / Password1` trên Swagger."

**Prompt B (sau đó — sang module Event của M1):**
> "Implement module `event` (controller/service/impl/dto) cho FR-EVT-01 (tạo event) + FR-EVT-02 (cấu hình round) + FR-EVT-03 (tổng weight tiêu chí = 100%) + FR-EVT-04 (mentor ≠ judge cùng category). Điền thân thật cho `EventQueryAdapter` (RoundView, criteriaForRound, isRoundOpenForSubmission). Bám DEVELOPER_GUIDE, gắn `@PreAuthorize`, mỗi BR có unit test."

**Báo cho M2/M3:** Auth xong → bắt đầu slice đầu của họ song song (submission/scoring · ranking), gắn `@PreAuthorize`, test trên seed data.
