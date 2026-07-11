# SEAL Backend — API Inventory
> Tạo tự động từ source code (không bịa endpoint hay field).  
> Cập nhật lần cuối: 2026-07-03 | Branch: dev

---

## Ghi chú chung

| Mục | Chi tiết |
|-----|----------|
| Response wrapper | Mọi endpoint (trừ Scoring — xem chú thích) trả về `ApiResponse<T> { boolean success, String message, T data, LocalDateTime timestamp }` |
| Auth | `Authorization: Bearer <JWT>`. Role suy từ token. |
| Pagination | Trả `PageResponse<T> { List<T> content, int page, int size, long totalElements, int totalPages, boolean last }` |
| Validation lỗi | 400 khi @Valid fail; 409 khi BusinessRuleException; 403 khi @PreAuthorize fail; 404 khi không tìm thấy |
| Ký hiệu | `*` = bắt buộc; `?` = optional; `(str≤N)` = @Size(max=N); `(email)` = @Email |

---

## Enum Reference

| Enum | Giá trị hợp lệ |
|------|----------------|
| `EventStatus` | DRAFT, PENDING_APPROVAL, REJECTED, APPROVED, OPEN, IN_PROGRESS, COMPLETED, ARCHIVED |
| `EventType` | SPRING, SUMMER, FALL, SPECIAL |
| `RoundStatus` | DRAFT, OPEN_FOR_SUBMISSION, SUBMISSION_CLOSED, SCORING_OPEN, SCORING_LOCKED, COMPLETED |
| `TeamStatus` | REGISTERED, APPROVED, REJECTED, ACTIVE, DISQUALIFIED, WITHDRAWN |
| `InvitationStatus` | PENDING, ACCEPTED, DECLINED, EXPIRED, CANCELLED |
| `EvaluationStatus` | DRAFT, SUBMITTED, LOCKED |
| `ResourceType` | DATASET, DOC, SAMPLE, LINK, OTHER |
| `BudgetStatus` | DRAFT, PENDING_APPROVAL, APPROVED, REJECTED, REQUIRES_REAPPROVAL |
| `AwardType` | FIRST_PLACE, SECOND_PLACE, THIRD_PLACE, SPECIAL, BEST_TECHNICAL, BEST_PRESENTATION |
| `AccountType` | PARTICIPANT, STAFF, GUEST_JUDGE |
| `UserStatus` | PENDING, ACTIVE, INACTIVE, LOCKED, REJECTED |

---

## Module 1 — Auth & Account
**Owner: Trí** | Endpoints: 11 | Controller: `AuthController` `/api/auth`

**CRUD coverage:** Register ✓ | Login ✓ | Approve/Reject ✓ | View profile ✓ | Guest-judge create ✓ | Update profile ✗ (thiếu) | Delete ✗ (thiếu)

---

### GET /api/auth/ping   [public] — status: working
Purpose: Health check cho module auth.  
Response `String`: `"Auth module is alive"`

---

### GET /api/auth/me   [isAuthenticated] — status: working
Purpose: Lấy thông tin tài khoản đang đăng nhập (FR-AUTH-05).  
Response `MeResponse`: id, email, fullName, phone, roleCode, accountType(`AccountType`), status(`UserStatus`), studentId, university, isFptStudent, lastLoginAt, createdAt

---

### POST /api/auth/register   [public] — status: working
Purpose: Đăng ký tài khoản participant mới — tạo với status PENDING (FR-AUTH-01/02).  
Request `RegisterRequest`:
- email\* (email)
- password\* (str≥8)
- fullName\* (str)
- phone? (str)
- fptStudent\* (bool, default false)
- studentId? (str)
- university? (str)

Response `RegisterResponse`: userId, status="PENDING"

---

### POST /api/auth/login   [public] — status: working
Purpose: Đăng nhập, nhận JWT access + refresh token (FR-AUTH-05).  
Request `LoginRequest`: email\* (str), password\* (str)  
Response `AuthResponse`: accessToken, refreshToken

---

### POST /api/auth/logout   [public/bearer optional] — status: working
Purpose: Đăng xuất phía server (stateless — client phải tự huỷ token).  
Response `Void`: null (luôn 200)

---

### GET /api/auth/accounts/pending   [COORDINATOR | ADMIN] — status: working
Purpose: Danh sách tài khoản đang chờ duyệt, phân trang (FR-AUTH-08).  
Query params: page?(int, default=0), size?(int, default=20)  
Response `PageResponse<PendingAccountResponse>`: id, fullName, email, studentId, university, fptStudent, createdAt

---

### GET /api/auth/accounts   [COORDINATOR | ADMIN] — status: working
Purpose: Danh sách tài khoản theo status tuỳ chọn; "APPROVED" là alias cho ACTIVE.  
Query params: status?(str, default="PENDING") | page?(int) | size?(int)  
Response `PageResponse<PendingAccountResponse>`: (xem trên)

---

### POST /api/auth/accounts/{userId}/approve   [COORDINATOR | ADMIN] — status: working
Purpose: Duyệt tài khoản PENDING → ACTIVE (FR-AUTH-08, BR-GOV-02). Coordinator không tự duyệt mình.  
Path: userId\* (Long)  
Response `AccountStatusResponse`: userId, status=ACTIVE

---

### POST /api/auth/accounts/{userId}/reject   [COORDINATOR | ADMIN] — status: working
Purpose: Từ chối tài khoản PENDING → REJECTED (FR-AUTH-08). Reason bắt buộc.  
Path: userId\* (Long)  
Request `RejectAccountRequest`: reason\* (str)  
Response `AccountStatusResponse`: userId, status=REJECTED

---

### POST /api/auth/guest-judges   [COORDINATOR | ADMIN] — status: working
Purpose: Tạo tài khoản guest judge với mật khẩu tạm (FR-AUTH-09). Chỉ hiện mật khẩu 1 lần.  
Request `CreateGuestJudgeRequest`: email\* (email), fullName\* (str), phone? (str)  
Response `GuestJudgeResponse`: userId, email, fullName, temporaryPassword

---

## Module 2 — User Directory
**Owner: Trí** | Endpoints: 2 | Controller: `UserDirectoryController`

**CRUD coverage:** Read ✓ | Create/Update/Delete ✗ (chỉ lookup)

---

### GET /api/judges   [COORDINATOR] — status: working
Purpose: Danh sách judge đang active (nội bộ + khách) để gán vào round.  
Response `List<JudgeResponse>`: id, fullName, email, accountType(`AccountType`)

---

### GET /api/mentors   [COORDINATOR] — status: working
Purpose: Danh sách mentor đang active để gán vào category.  
Response `List<MentorResponse>`: id, fullName, email

---

## Module 3 — Governance (Disciplines & Term Plans)
**Owner: Trí** | Endpoints: 2 | Controller: `GovernanceQueryController`

**CRUD coverage:** Read ✓ | Create/Update/Delete ✗ (read-only query — quản lý qua DB/seed)

---

### GET /api/disciplines   [isAuthenticated] — status: working
Purpose: Danh sách ngành học đang active (FR-GOV).  
Response `List<DisciplineResponse>`: id, code, name, description

---

### GET /api/term-plans   [isAuthenticated] — status: working
Purpose: Danh sách kỳ kế hoạch kèm slot usage; hỗ trợ filter.  
Query params: disciplineId?(Long), year?(Integer)  
Response `List<TermPlanResponse>`: id, term, year, disciplineId, disciplineName, maxEvents, usedEvents, remaining

---

## Module 4 — Event
**Owner: Trí** | Endpoints: 11 (CRUD + lifecycle) | Controller: `EventController` `/api/events`

**CRUD coverage:** Create ✓ | Read ✓ (list + detail) | Update ✓ (PATCH) | Delete ✗ (thiếu — chỉ có archive)

---

### GET /api/events/ping   [public] — status: working
Purpose: Health check.

---

### POST /api/events   [COORDINATOR] — status: working
Purpose: Tạo event mới trạng thái DRAFT (FR-EVT-01).  
Request `CreateEventRequest`:
- name\* (str≤200)
- disciplineId\* (Long)
- termPlanId\* (Long)
- eventType\* (`EventType`)
- description? (str)
- registrationStart? (datetime)
- registrationEnd? (datetime)

Response `EventResponse`: id, name, slug, eventType, disciplineId, disciplineName, termPlanId, description, registrationStart, registrationEnd, status, ownerCoordinatorId, maxTeamSize, maxTeams, maxParticipants, maxTeamsPerMentor, createdAt, updatedAt

---

### GET /api/events   [public] — status: working
Purpose: Danh sách events; filter tuỳ chọn theo status.  
Query params: status?(`EventStatus` — DRAFT|APPROVED|OPEN|…)  
Response `List<EventSummaryResponse>`: id, name, slug, eventType, status, registrationStart, registrationEnd

---

### GET /api/events/{id}   [public] — status: working
Purpose: Chi tiết event theo id.  
Path: id\* (Long)  
Response `EventResponse`: (xem POST /api/events)

---

### PATCH /api/events/{id}   [COORDINATOR] — status: working
Purpose: Cập nhật event (name, description, registration window, capacity overrides).  
Path: id\* (Long)  
Request `UpdateEventRequest` (tất cả optional):
- name? (str≤200)
- description? (str)
- registrationStart? (datetime)
- registrationEnd? (datetime)
- maxTeamSize? (Integer — override tầng 2)
- maxTeams? (Integer — override tầng 2)
- maxParticipants? (Integer — override tầng 2)
- maxTeamsPerMentor? (Integer — override tầng 2)

Response `EventResponse`

---

### POST /api/events/{eventId}/submit   [COORDINATOR] — status: working
Purpose: Gửi event lên duyệt DRAFT/REJECTED → PENDING_APPROVAL (FR-EVT-07). Chỉ owner mới submit.  
Path: eventId\* (Long)  
Response `EventResponse`

---

### POST /api/events/{eventId}/approve   [SUPER_COORDINATOR] — status: working
Purpose: Duyệt event PENDING_APPROVAL → APPROVED (BR-GOV-02). Approver ≠ owner.  
Path: eventId\* (Long)  
Response `EventResponse`

---

### POST /api/events/{eventId}/reject   [SUPER_COORDINATOR] — status: working
Purpose: Từ chối event PENDING_APPROVAL → REJECTED. Reason bắt buộc.  
Path: eventId\* (Long)  
Request `RejectEventRequest`: reason\* (str)  
Response `EventResponse`

---

### POST /api/events/{eventId}/open   [COORDINATOR] — status: working
Purpose: Mở cổng đăng ký team APPROVED → OPEN.  
Path: eventId\* (Long)  
Response `EventResponse`

---

### POST /api/events/{eventId}/start   [COORDINATOR] — status: working
Purpose: Bắt đầu thi OPEN → IN_PROGRESS.  
Path: eventId\* (Long)  
Response `EventResponse`

---

### POST /api/events/{eventId}/complete   [COORDINATOR] — status: working
Purpose: Kết thúc event IN_PROGRESS → COMPLETED.  
Path: eventId\* (Long)  
Response `EventResponse`

---

### POST /api/events/{eventId}/archive   [COORDINATOR] — status: working
Purpose: Lưu trữ event COMPLETED → ARCHIVED.  
Path: eventId\* (Long)  
Response `EventResponse`

---

## Module 5 — Round
**Owner: Trí** | Endpoints: 5 | Controller: `EventController` `/api/events/{eventId}/rounds`

**CRUD coverage:** Create ✓ | Read ✓ (list + detail) | Update ✓ (PATCH) | Delete ✓

---

### POST /api/events/{eventId}/rounds   [COORDINATOR] — status: working
Purpose: Thêm round vào event (FR-EVT-02).  
Path: eventId\* (Long)  
Request `CreateRoundRequest`:
- name\* (str≤150)
- orderNumber\* (int≥1)
- submissionDeadline? (datetime)
- scoringDeadline? (datetime)
- promotionTopN? (int)
- finalRound\* (bool)
- requiresRepo? (bool)
- requiresDemo? (bool)
- requiresSlide? (bool)
- requiresReport? (bool)

Response `RoundResponse`: id, eventId, name, orderNumber, submissionDeadline, scoringDeadline, status(`RoundStatus`), promotionTopN, finalRound, requiresRepo, requiresDemo, requiresSlide, requiresReport, createdAt, updatedAt

---

### GET /api/events/{eventId}/rounds   [public] — status: working
Purpose: Danh sách rounds của event theo thứ tự.  
Response `List<RoundResponse>`

---

### GET /api/events/{eventId}/rounds/{roundId}   [public] — status: working
Purpose: Chi tiết một round.  
Response `RoundResponse`

---

### PATCH /api/events/{eventId}/rounds/{roundId}   [COORDINATOR] — status: working
Purpose: Cập nhật round (không đổi orderNumber).  
Request `UpdateRoundRequest` (tất cả optional): name?, submissionDeadline?, scoringDeadline?, promotionTopN?, finalRound?, requiresRepo?, requiresDemo?, requiresSlide?, requiresReport?  
Response `RoundResponse`

---

### DELETE /api/events/{eventId}/rounds/{roundId}   [COORDINATOR] — status: working
Purpose: Xoá round (chỉ khi DRAFT hoặc REJECTED; 409 nếu đã có judge/criteria gắn).  
Response `Void`

---

## Module 6 — Judge Assignment
**Owner: Trí** | Endpoints: 3 | Controller: `EventController` `/api/events/{eventId}/rounds/{roundId}/judges`

**CRUD coverage:** Create ✓ | Read ✓ | Delete ✓ | Update ✗ (không cần — revoke + re-assign)

---

### POST /api/events/{eventId}/rounds/{roundId}/judges   [COORDINATOR | ADMIN] — status: working
Purpose: Gán judge vào round (và tùy chọn một category).  
Path: eventId\*, roundId\* (Long)  
Request `AssignJudgeRequest`: judgeId\* (Long), categoryId? (Long)  
Response `JudgeAssignmentResponse`: id, judgeId, judgeName, judgeEmail, roundId, categoryId, categoryName, assignedAt, status

---

### GET /api/events/{eventId}/rounds/{roundId}/judges   [isAuthenticated] — status: working
Purpose: Danh sách judge assignments còn hiệu lực cho round.  
Response `List<JudgeAssignmentResponse>`

---

### DELETE /api/events/{eventId}/rounds/{roundId}/judges/{assignmentId}   [COORDINATOR | ADMIN] — status: working
Purpose: Thu hồi assignment của judge.  
Path: eventId\*, roundId\*, assignmentId\* (Long)  
Response `Void`

---

## Module 7 — Category
**Owner: Trí** | Endpoints: 5 | Controller: `EventController` `/api/events/{eventId}/categories`

**CRUD coverage:** Create ✓ | Read ✓ (list + detail) | Update ✓ | Delete ✓

---

### POST /api/events/{eventId}/categories   [COORDINATOR] — status: working
Purpose: Tạo hạng mục cho event (FR-EVT-04).  
Path: eventId\* (Long)  
Request `CreateCategoryRequest`: name\* (str≤150), description? (str), mentorId? (Long)  
Response `CategoryResponse`: id, eventId, name, description, mentorId, mentorName, active, createdAt, updatedAt

---

### GET /api/events/{eventId}/categories   [public] — status: working
Purpose: Danh sách categories của event.  
Response `List<CategoryResponse>`

---

### GET /api/events/{eventId}/categories/{categoryId}   [public] — status: working
Purpose: Chi tiết một category.  
Response `CategoryResponse`

---

### PATCH /api/events/{eventId}/categories/{categoryId}   [COORDINATOR] — status: working
Purpose: Cập nhật category (name, description, mentor, active flag).  
Request `UpdateCategoryRequest` (tất cả optional): name?(str≤150), description?, mentorId?, active?(bool)  
Response `CategoryResponse`

---

### DELETE /api/events/{eventId}/categories/{categoryId}   [COORDINATOR] — status: working
Purpose: Xoá category (chỉ DRAFT hoặc REJECTED; 409 nếu team đã đăng ký).  
Response `Void`

---

## Module 8 — CriteriaSet
**Owner: Trí** | Endpoints: 6 | Controller: `EventController` `/api/events/{eventId}/criteria-sets`

**CRUD coverage:** Create ✓ | Read ✓ | Update ✓ (name/description) | Delete ✓ (set + individual criterion) | Replace criteria ✓

---

### POST /api/events/{eventId}/criteria-sets   [COORDINATOR] — status: working
Purpose: Tạo criteria set kèm criteria; tổng weight phải = 100 (BR-EVT-03).  
Path: eventId\* (Long)  
Request `CreateCriteriaSetRequest`:
- name\* (str≤150)
- description? (str)
- roundId? (Long — null = set template)
- categoryId? (Long — null = round-level)
- promotionTopN? (int)
- criteria\* (List, min 1 item) — mỗi item `AddCriterionRequest`:
  - name\* (str≤150)
  - description? (str)
  - maxScore\* (decimal≥0.01)
  - weight\* (decimal 0–100)
  - displayOrder\* (int≥1)

Response `CriteriaSetResponse`: id, name, description, eventId, roundId, categoryId, categoryName, promotionTopN, template, defaultSet, criteria[{id, name, description, maxScore, weight, displayOrder}]

---

### GET /api/events/{eventId}/criteria-sets   [public] — status: working
Purpose: Danh sách criteria sets của event (kèm criteria chi tiết).  
Response `List<CriteriaSetResponse>`

---

### GET /api/events/{eventId}/criteria-sets/{setId}   [public] — status: working
Purpose: Chi tiết một criteria set.  
Response `CriteriaSetResponse`

---

### PATCH /api/events/{eventId}/criteria-sets/{setId}   [COORDINATOR] — status: working
Purpose: Cập nhật name/description (chỉ khi event DRAFT hoặc REJECTED).  
Request `UpdateCriteriaSetRequest`: name?(str≤150), description?  
Response `CriteriaSetResponse`

---

### PUT /api/events/{eventId}/criteria-sets/{setId}/criteria   [COORDINATOR] — status: working
Purpose: Thay thế toàn bộ criteria; tổng weight phải = 100 (chỉ DRAFT hoặc REJECTED).  
Request `ReplaceCriteriaRequest`: criteria\* (List `AddCriterionRequest`, xem trên)  
Response `CriteriaSetResponse`

---

### DELETE /api/events/{eventId}/criteria-sets/{setId}   [COORDINATOR] — status: working
Purpose: Xoá cả criteria set và criteria con (chỉ DRAFT hoặc REJECTED).  
Response `Void`

---

### DELETE /api/events/{eventId}/criteria-sets/{setId}/criteria/{criterionId}   [COORDINATOR] — status: working
Purpose: Xoá một criterion đơn lẻ khỏi set (chỉ DRAFT hoặc REJECTED).  
Response `CriteriaSetResponse` (set còn lại)

---

## Module 9 — CategoryResource
**Owner: Trí** | Endpoints: 3 | Controller: `EventController` `/api/events/{eventId}/categories/{categoryId}/resources`

**CRUD coverage:** Create ✓ | Read ✓ | Delete ✓ | Update ✗ (thiếu)

---

### POST /api/events/{eventId}/categories/{categoryId}/resources   [COORDINATOR] — status: working
Purpose: Thêm resource link (dataset/tài liệu) vào category.  
Path: eventId\*, categoryId\* (Long)  
Request `CreateCategoryResourceRequest`:
- label? (str≤150)
- url\* (str≤500)
- resourceType\* (`ResourceType`)

Response `CategoryResourceResponse`: id, categoryId, label, url, resourceType, createdAt

---

### GET /api/events/{eventId}/categories/{categoryId}/resources   [isAuthenticated] — status: working
Purpose: Danh sách resource links của category (thí sinh xem được).  
Response `List<CategoryResourceResponse>`

---

### DELETE /api/events/{eventId}/categories/{categoryId}/resources/{resourceId}   [COORDINATOR] — status: working
Purpose: Xoá resource link.  
Response `Void`

---

## Module 10 — Mentor Planning
**Owner: Trí** | Endpoints: 1 | Controller: `EventController`

---

### GET /api/events/{eventId}/mentor-planning   [COORDINATOR | SUPER_COORDINATOR] — status: working
Purpose: Tính toán nhu cầu mentor: activeTeams, mentorsNeeded=ceil(teams/maxTeamsPerMentor), currentMentors, gap.  
Path: eventId\* (Long)  
Response `MentorPlanningResponse`: eventId, activeTeams, maxTeamsPerMentor, mentorsNeeded, currentMentors, gap

---

## Module 11 — Budget
**Owner: Trí** | Endpoints: 7 | Controller: `BudgetController`

**CRUD coverage:** Create ✓ | Read ✓ | Update ✓ | Delete ✓ (items) | Budget header delete ✗ (thiếu)

---

### POST /api/events/{eventId}/budget   [COORDINATOR] — status: working
Purpose: Tạo budget cho event (FR-BGT-01); tối đa 1 budget/event; mặc định VND.  
Request `CreateBudgetRequest`: currency?(str≤10)  
Response `BudgetResponse`: id, eventId, currency, totalEstimatedCost, status, items[], createdAt, updatedAt

---

### GET /api/events/{eventId}/budget   [isAuthenticated] — status: working
Purpose: Xem budget kèm items và tổng.  
Response `BudgetResponse`

---

### PATCH /api/events/{eventId}/budget   [COORDINATOR] — status: working
Purpose: Cập nhật header budget (currency, status). Chỉ khi event không PENDING_APPROVAL.  
Request `UpdateBudgetRequest`: currency?(str≤10), status?(`BudgetStatus`)  
Response `BudgetResponse`

---

### POST /api/events/{eventId}/budget/items   [COORDINATOR] — status: working
Purpose: Thêm item vào budget (FR-BGT-02).  
Request `CreateBudgetItemRequest`:
- categoryId\* (Long — id trong budget_categories)
- description\* (str)
- quantity\* (decimal≥0.01)
- unitCost\* (decimal≥0)
- notes? (str)

Response `BudgetResponse` (toàn bộ budget sau khi thêm)

---

### PUT /api/events/{eventId}/budget/items/{itemId}   [COORDINATOR] — status: working
Purpose: Cập nhật item (FR-BGT-03).  
Request `UpdateBudgetItemRequest`: categoryId\*, description\*, quantity\*, unitCost\*, notes? (giống Create)  
Response `BudgetResponse`

---

### DELETE /api/events/{eventId}/budget/items/{itemId}   [COORDINATOR] — status: working
Purpose: Xoá item (FR-BGT-04).  
Response `BudgetResponse` (budget sau khi xoá)

---

### GET /api/budget-categories   [isAuthenticated] — status: working
Purpose: Danh sách loại ngân sách để FE populate dropdown.  
Response `List<BudgetCategoryResponse>`: id, code, name, description

---

## Module 12 — Team
**Owner: Trí** | Endpoints: 9 | Controller: `TeamController` `/api/teams`

**CRUD coverage:** Create ✓ | Read ✓ | Update (category) ✓ | Delete ✗ (thiếu; chỉ có withdraw/reject qua review)

---

### POST /api/teams   [TEAM_MEMBER | COORDINATOR | TEAM_LEADER] — status: working
Purpose: Tạo team và đăng ký vào category (FR-TEAM-01). Kiểm BR-CAP-01/02.  
Request `CreateTeamRequest`: name\*(str≤150), description?, eventId\*(Long), categoryId\*(Long)  
Response `TeamResponse`: id, eventId, categoryId, categoryName, leaderId, leaderName, name, description, status(`TeamStatus`), rejectionReason, createdAt, updatedAt, members[TeamMemberResponse]

> `TeamMemberResponse`: userId, fullName, email, role(`TeamMemberRole`), status(`TeamMemberStatus`), joinedAt

---

### GET /api/teams/{teamId}   [public] — status: working
Purpose: Chi tiết team kèm members.  
Response `TeamResponse`

---

### GET /api/teams/by-event/{eventId}   [public] — status: working
Purpose: Danh sách tóm tắt teams trong event; filter tuỳ chọn theo status.  
Path: eventId\* (Long)  
Query: status?(`TeamStatus`)  
Response `List<TeamSummaryResponse>`: id, eventId, categoryId, categoryName, name, status

---

### PUT /api/teams/{teamId}/category   [isAuthenticated] — status: working
Purpose: Đổi category của team (BR-TEAM-04: phải trong cửa sổ đăng ký).  
Request `RegisterTeamCategoryRequest`: categoryId\* (Long)  
Response `TeamResponse`

---

### POST /api/teams/{teamId}/review   [COORDINATOR] — status: working
Purpose: Duyệt hoặc từ chối team (FR-TEAM-05). Khi approve: kiểm BR-CAP-04 (min size) + BR-TEAM-01 (max size).  
Request `ApproveTeamRequest`: approved\*(bool), reason?(str)  
Response `TeamResponse`

---

### DELETE /api/teams/{teamId}/members/{targetUserId}   [isAuthenticated] — status: working
Purpose: Xoá thành viên (leader tự xoá người khác, hoặc thành viên tự rời). Leader không bị xoá.  
Path: teamId\*, targetUserId\* (Long)  
Response `TeamResponse`

---

## Module 13 — Team Invitation
**Owner: Trí** | Endpoints: 5 | Controller: `TeamController`

**CRUD coverage:** Create ✓ | Read ✓ (team view + my view) | Accept/Decline ✓ | Delete ✗ (chỉ có cancel implicit qua expire)

---

### POST /api/teams/{teamId}/invitations   [isAuthenticated] — status: working
Purpose: Mời thành viên bằng email (BR-TEAM-06: không trùng event).  
Request `InviteMemberRequest`: email\*(email)  
Response `InvitationResponse`: id, teamId, email, invitedUserId, status(`InvitationStatus`), expiresAt, createdAt

---

### GET /api/teams/{teamId}/invitations   [isAuthenticated] — status: working
Purpose: Danh sách invitations của team — chỉ member + coordinator thấy.  
Response `List<InvitationResponse>`

---

### GET /api/teams/invitations/mine   [isAuthenticated] — status: working
Purpose: Danh sách invitation đang chờ gửi đến email của người dùng hiện tại.  
Response `List<MyInvitationResponse>`: invitationId, teamId, teamName, eventId, eventName, invitedByName, expiresAt

---

### POST /api/teams/invitations/{invitationId}/accept   [isAuthenticated] — status: working
Purpose: Chấp nhận invitation — join team, tạo TeamMember (BR-TEAM-01/02, BR-CAP-02/03).  
Path: invitationId\* (Long)  
Response `TeamResponse`

---

### POST /api/teams/invitations/{invitationId}/decline   [isAuthenticated] — status: working
Purpose: Từ chối invitation.  
Response `InvitationResponse`

---

## Module 14 — Submission
**Owner: Hải** | Endpoints: 4 | Controller: `SubmissionController` `/api/submissions`

**CRUD coverage:** Create ✓ | Read ✓ (detail + current + history + my-overview) | Update ✗ (thiếu) | Delete ✗ (thiếu)

---

### GET /api/submissions/ping   [public] — status: working
Purpose: Health check.

---

### POST /api/submissions   [TEAM_LEADER] — status: working
Purpose: Nộp bài dự thi.  
Request `CreateSubmissionRequestDTO`:
- teamId\* (Long)
- roundId\* (Long)
- repoUrl? (str≤500)
- demoUrl? (str≤500)
- slideUrl? (str≤500)
- reportUrl? (str≤500)
- changeNote? (str≤255)

Response `SubmissionDetailResponseDTO`: submissionId, teamId, teamName, roundId, roundName, eventId, eventName, categoryId, categoryName, attemptNumber, repoUrl, demoUrl, slideUrl, reportUrl, changeNote, submittedBy, status, submittedAt, lastUpdatedAt

---

### GET /api/submissions/current   [TEAM_LEADER | TEAM_MEMBER | COORDINATOR | SUPER_COORDINATOR | ADMIN] — status: working
Purpose: Lấy bài nộp hiện tại (mới nhất) của team trong round.  
Query: teamId\*(Long), roundId\*(Long)  
Response `SubmissionDetailResponseDTO`

---

### GET /api/submissions/{id}   [TEAM_LEADER | TEAM_MEMBER | COORDINATOR | SUPER_COORDINATOR | ADMIN] — status: working
Purpose: Chi tiết bài nộp theo id; staff thấy mọi team, participant chỉ thấy team mình.  
Path: id\* (Long)  
Response `SubmissionDetailResponseDTO`

---

### GET /api/submissions/history   [TEAM_LEADER | TEAM_MEMBER | COORDINATOR | SUPER_COORDINATOR | ADMIN] — status: working
Purpose: Lịch sử tất cả lần nộp (attemptNumber 1..N) của team trong round.  
Query: teamId\*(Long), roundId\*(Long)  
Response `List<SubmissionDetailResponseDTO>`

---

### GET /api/submissions/my-overview   [TEAM_LEADER | TEAM_MEMBER] — status: working
Purpose: Tổng quan bài nộp của tất cả team mình tham gia theo từng round.  
Response `SubmissionMyOverviewResponseDTO { teams: [{ teamId, teamName, eventId, eventName, categoryId, categoryName, memberRole, rounds: [...] }] }`

---

## Module 15 — Scoring / Evaluation
**Owner: Hải** | Endpoints: 5 (+1 ping) | Controller: `EvaluationController` `/api/scoring` (alias: `/api/evaluations`)

⚠️ **Lưu ý:** Controller này KHÔNG bọc response trong `ApiResponse<T>` — trả trực tiếp `ResponseEntity<EvaluationResponse>` và `List<...>`. Đây là ngoại lệ so với chuẩn chung.

**CRUD coverage:** Start ✓ | Read ✓ | SaveDraft ✓ | Submit ✓ | Delete ✗ (không cho phép)

---

### GET /api/scoring/ping   [public] — status: working
Purpose: Health check.

---

### GET /api/scoring/assigned-submissions   [JUDGE] — status: working
Purpose: Danh sách submissions được giao cho judge hiện tại.  
Response `List<JudgeAssignedSubmissionResponse>`: submissionId, teamId, teamName, categoryId, categoryName, roundId, roundName, eventId, eventName, attemptNumber, submittedAt, repoUrl, demoUrl, slideUrl, reportUrl, evaluationId, evaluationStatus, scoredCriteriaCount, totalCriteriaCount

---

### POST /api/scoring/start   [JUDGE] — status: working
Purpose: Bắt đầu phiên chấm điểm cho một submission.  
Request `StartEvaluationRequest`: submissionId\* (Long)  
Response `EvaluationResponse`: id, judgeAssignmentId, judgeId, submissionId, roundId, eventId, eventName, teamId, teamName, categoryId, categoryName, attemptNumber, repoUrl, demoUrl, slideUrl, reportUrl, submittedById, status(`EvaluationStatus`), generalComment, totalRawScore, totalWeightedScore, startedAt, submittedAt, lockedAt, createdAt, updatedAt, scores[], criteria[]

---

### GET /api/scoring/{evaluationId}   [JUDGE] — status: working
Purpose: Chi tiết evaluation (judge chỉ thấy của mình).  
Response `EvaluationResponse`

---

### PUT /api/scoring/{evaluationId}/draft   [JUDGE] — status: working
Purpose: Lưu nháp điểm (status giữ DRAFT).  
Request `SaveScoresRequest`:
- generalComment? (str)
- scores\* (List, min 1):
  - criterionId\* (Long)
  - scoreValue\* (decimal≥0)
  - comment? (str)

Response `EvaluationResponse`

---

### POST /api/scoring/{evaluationId}/submit   [JUDGE] — status: working
Purpose: Nộp evaluation (DRAFT → SUBMITTED).  
Request `SubmitEvaluationRequest`: generalComment? (str)  
Response `EvaluationResponse`

---

### GET /api/scoring/{evaluationId}/audit   [JUDGE] — status: working
Purpose: Lịch sử thay đổi của evaluation.  
Response `List<EvaluationAuditEntryResponse>`: id, actionType, targetType, targetId, oldValue, newValue, actorId, actorName, actorEmail, createdAt

---

## Module 16 — Ranking & Promotion
**Owner: Ân** | Endpoints: 6 (+1 ping) | Controller: `RankingController` `/api/rankings`

**CRUD coverage:** Compute ✓ | Read ✓ | Promote ✓ | Disqualify ✓ | Delete ✗

---

### GET /api/rankings/ping   [public] — status: working
Purpose: Health check.

---

### GET /api/rankings/events/{eventId}/categories   [isAuthenticated] — status: working
Purpose: Danh sách categories trong event (dùng cho filter ranking).  
Response `List<CategoryResponse(ranking)>`: id, name

---

### POST /api/rankings/rounds/{roundId}/compute   [COORDINATOR | SUPER_COORDINATOR] — status: working
Purpose: Tính toán điểm và xếp hạng (có filter theo categoryId).  
Path: roundId\* (Long)  
Query: categoryId?(Long, default=0 = tất cả)  
Response `List<RankingResponse>`: rankingId, teamId, teamName, categoryName, roundId, totalScore, rankPosition, isPromoted

---

### GET /api/rankings/rounds/{roundId}   [isAuthenticated] — status: working
Purpose: Xem kết quả xếp hạng của một vòng.  
Response `List<RankingResponse>`

---

### POST /api/rankings/teams/{teamId}/disqualify   [COORDINATOR | SUPER_COORDINATOR] — status: working
Purpose: Đình chỉ đội vi phạm (FR-RNK-04).  
Path: teamId\* (Long)  
Request body (raw JSON): `{ "reason": "..." }`  
Response `String`: thông báo thành công

---

### GET /api/rankings/events/{eventId}/disqualified   [COORDINATOR | SUPER_COORDINATOR] — status: working
Purpose: Danh sách đội bị đình chỉ trong event.  
Response `List<DisqualifiedTeamResponse>`: id, name, disqualifiedReason

---

### POST /api/rankings/rounds/{roundId}/promote   [COORDINATOR | SUPER_COORDINATOR] — status: working
Purpose: Thăng hạng thủ công danh sách đội sang vòng tiếp theo.  
Path: roundId\* (Long)  
Request body: `List<Long>` (danh sách teamId)  
Response `String`: thông báo thành công

---

### GET /api/rankings/teams/{teamId}/rounds/{roundId}/breakdown   [isAuthenticated] — status: working
Purpose: Chi tiết bảng điểm của đội trong một vòng.  
Response `List<ScoreBreakdownResponse>`: judgeName, criterionName, criterionWeight, scoreValue, judgeComment

---

## Module 17 — Award & Results
**Owner: Ân** | Endpoints: 5 (+1 ping) | Controller: `AwardController` `/api/awards`

⚠️ **Lưu ý `POST /api/awards`:** Phương thức lấy userId qua reflection thủ công (không dùng `@CurrentUser`). Đây là anti-pattern — có thể throw `IllegalStateException` nếu UserPrincipal thay đổi. **Ghi status: unknown — cần review**.

**CRUD coverage:** Create ✓ | Read ✓ | Publish ✓ | Update ✗ | Delete ✗

---

### GET /api/awards/ping   [public] — status: working
Purpose: Health check.

---

### POST /api/awards   [COORDINATOR | SUPER_COORDINATOR] — status: unknown
Purpose: Gán giải thưởng cho đội thi.  
Request `AwardCreateRequest`:
- eventId\* (Long)
- categoryId\* (Long)
- teamId\* (Long)
- rankingId? (Long — null nếu giải phụ)
- awardType\* (`AwardType`)
- description? (str)

Response `AwardResponse`: awardId, eventId, teamId, teamName, awardType, description, awardedBy, awardedAt

---

### GET /api/awards/events/{eventId}   [isAuthenticated] — status: working
Purpose: Danh sách giải thưởng của event.  
Response `List<AwardResponse>`

---

### GET /api/awards/events/{eventId}/categories   [isAuthenticated] — status: working
Purpose: Danh sách categories trong event (dùng bổ trợ cho form trao giải).  
Response `List<Map<String,Object>>` — ⚠️ không typed DTO; cấu trúc phụ thuộc impl service

---

### GET /api/awards/events/{eventId}/eligible-teams   [COORDINATOR | SUPER_COORDINATOR] — status: working
Purpose: Danh sách đội từ Vòng Chung Kết theo category để trao giải.  
Query: categoryId\*(Long)  
Response `List<Map<String,Object>>` — ⚠️ không typed DTO

---

### POST /api/awards/events/{eventId}/publish   [COORDINATOR | SUPER_COORDINATOR] — status: working
Purpose: Công bố kết quả xếp hạng và giải thưởng cho thí sinh.  
Path: eventId\* (Long)  
Response `String`: thông báo thành công

---

## Module 18 — Notification
**Owner: Ân** | Endpoints: 1 | Controller: `NotificationController` `/api/notifications`

⚠️ **Module này chỉ có ping — chưa implement bất kỳ endpoint nào (stub).**

---

### GET /api/notifications/ping   [public] — status: stub
Purpose: Health check placeholder.

---

## Tổng kết

### Endpoint count theo module

| Module | Owner | Endpoints | Working | Stub/Unknown |
|--------|-------|-----------|---------|--------------|
| 1. Auth & Account | Trí | 11 | 11 | 0 |
| 2. User Directory | Trí | 2 | 2 | 0 |
| 3. Governance | Trí | 2 | 2 | 0 |
| 4. Event | Trí | 11 | 11 | 0 |
| 5. Round | Trí | 5 | 5 | 0 |
| 6. Judge Assignment | Trí | 3 | 3 | 0 |
| 7. Category | Trí | 5 | 5 | 0 |
| 8. CriteriaSet | Trí | 7 | 7 | 0 |
| 9. CategoryResource | Trí | 3 | 3 | 0 |
| 10. Mentor Planning | Trí | 1 | 1 | 0 |
| 11. Budget | Trí | 7 | 7 | 0 |
| 12. Team | Trí | 6 | 6 | 0 |
| 13. Team Invitation | Trí | 5 | 5 | 0 |
| 14. Submission | Hải | 5 | 5 | 0 |
| 15. Scoring/Evaluation | Hải | 6 | 6 | 0 |
| 16. Ranking | Ân | 7 | 7 | 0 |
| 17. Award | Ân | 6 | 5 | 1 (POST /api/awards — unknown) |
| 18. Notification | Ân | 1 | 0 | 1 (ping only — stub) |
| **TOTAL** | | **93** | **91** | **2** |

---

### Danh sách endpoint cần review trước khi wire FE

| # | Endpoint | Vấn đề |
|---|----------|---------|
| 1 | `POST /api/awards` | Lấy userId qua reflection — anti-pattern; cần đổi sang `@CurrentUser UserPrincipal` |
| 2 | `GET /api/awards/events/{eventId}/categories` | Response là `List<Map<String,Object>>` không typed — FE không biết shape |
| 3 | `GET /api/awards/events/{eventId}/eligible-teams` | Response là `List<Map<String,Object>>` không typed |
| 4 | `GET /api/notifications/ping` | Module Notification chưa có endpoint thực — chỉ là placeholder |
| 5 | `POST /api/rankings/teams/{teamId}/disqualify` | Body là raw `Map<String,String>` thay vì typed DTO |
| 6 | `POST /api/rankings/rounds/{roundId}/promote` | Body là raw `List<Long>` thay vì typed DTO |

---

### Thiếu CRUD đáng chú ý

| Resource | Thiếu |
|----------|-------|
| Event | DELETE (xoá event) |
| CategoryResource | UPDATE (sửa label/url/type) |
| Budget header | DELETE (xoá budget) |
| Team | DELETE (tự giải thể — chỉ có Coordinator reject) |
| Submission | UPDATE (chỉ nộp lại tạo attempt mới), DELETE |
| Award | UPDATE, DELETE |
| Account | UPDATE profile, DELETE/deactivate |
| Notification | Tất cả (module chưa implement) |
