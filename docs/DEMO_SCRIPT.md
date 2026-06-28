# SEAL — Kịch bản DEMO 2 main-flow (M1)

> Mục tiêu: demo **2 luồng** để được duyệt dự án.
> **Flow #1 — Event & Round Configuration** · **Flow #2 — Team Registration & Account Approval**
> Trạng thái: **CẢ HAI ĐÃ DEMO-READY** (FE wire đủ với BE).

---

## 0. Chuẩn bị trước demo (checklist)
- [ ] BE chạy (`./mvnw spring-boot:run`), kết nối Azure DB OK; FE `npm run dev` → http://localhost:5173.
- [ ] Seed có hash thật → login bằng `Password1`.
- [ ] Tài khoản sẵn (mật khẩu `Password1`): `admin@`, `super@`, `coord@`, `judge.internal@`, `mentor@`, `leader@student`, `member1@student`, `member2@student`.
- [ ] **Tạo trước 1–2 participant PENDING** (register vài tài khoản test) để màn Account Approval có dữ liệu.
- [ ] **NHỚ: với event MỚI phải GÁN JUDGE trước khi Submit** (BR-EVT-06) — không thì Submit trả 409. Đây là thứ tự demo bắt buộc.
- [ ] Mở sẵn **Swagger** (dự phòng) để show ca lỗi/BR nếu cần.
- [ ] Token hết hạn ~24h → login lại.

---

## DEMO FLOW #2 — Team Registration & Account Approval
*Story: participant đăng ký → coordinator duyệt account → lập team → coordinator duyệt team.*

1. **(Public)** Register một participant mới → màn báo "chờ duyệt".
2. Login **coord@** → **Account Approvals** → thấy user PENDING → **Approve**.
3. Login participant vừa duyệt → **login OK** → chứng minh "approval gate" (trước khi duyệt login bị chặn).
4. Login **leader@** → **My Team** → tạo team (chọn event đang mở → category → name `test-…`) → **mời** `member1@`, `member2@`.
5. Login **member1@** → **My Invitations** → **Accept** → vào team. (lặp `member2@`).
6. Login **coord@** → **Teams** → chọn event → team đủ 3 → **Approve**.

**Điểm nhấn / BR để nói:** email unique + mật khẩu mạnh + BCrypt (BR-USR), approval gate (BR-USR-02), 1 user 1 team/event (BR-TEAM-02), size 3–5 khi duyệt (BR-TEAM-01 — thử approve team <3 → 409), reject cần reason, RBAC (member gọi `/review` → 403).

---

## DEMO FLOW #1 — Event & Round Configuration
*Story: coordinator cấu hình FULL một event → gán judge → submit để duyệt.*

1. Login **coord@** → **Create Event** (wizard 6 bước):
   - **Basic Info:** chọn discipline → term-plan (hiện slot còn) → eventType auto → name/mô tả/ngày đăng ký.
   - **Rounds** → **Categories** (mentor) → **Criteria** (UI báo **Total 100%**) → **Budget** (amount/total tự tính) → **Review**.
2. **Judge Assignment** → chọn event → round → **gán judge** (hoặc **Create Guest Judge** → nhận temp password).
3. **My Events** → mở event → **Event Detail** → **Submit for Approval** → status **PENDING_APPROVAL**.

**Điểm nhấn / BR để nói:** trùng order round → 409 (BR-EVT-02), tổng weight = 100 (BR-EVT-03), eventType/discipline phải khớp term-plan, mỗi round cần ≥1 judge mới submit (BR-EVT-06), budget total tự tính qua trigger DB, audit log, RBAC (`@PreAuthorize` COORDINATOR; member tạo event → 403).

---

## Giới hạn nên CHỦ ĐỘNG nói (nếu được hỏi)
- **Approve/Reject event**, **quota**, **CRUD discipline/term-plan** thuộc trục **Program Governance (Super Coordinator)** — **ngoài 6 main-flow đã chốt** với giảng viên, làm ở phiên sau. Vì vậy demo dừng ở **PENDING_APPROVAL** (đã đủ thể hiện luồng "configure → submit").
- Lifecycle (Open→In Progress→Completed→Archived) đã có nút trên UI; cần event ở APPROVED để chạy (set qua DB nếu muốn show, vì approve là governance).
- Vài read còn rộng quyền / chính sách soft-delete đang chuẩn hoá (đã có trong backlog).

## Dự phòng khi demo
- Nếu FE lỗi runtime giữa chừng → show endpoint tương ứng qua **Swagger / file `.http`** (API thật vẫn chạy) để chứng minh BE hoạt động.
- Chuẩn bị **2 trình duyệt / cửa sổ ẩn danh** để đăng nhập đồng thời coord + participant (đỡ phải login/logout liên tục).
