# Nhật ký phát triển mã nguồn (BE) - SEAL

**Thành viên:** Nguyễn Công Thiên Ân (M3)  
**Module phụ trách:** (5) Ranking & Promotion

---

# Kiến trúc & Luồng xử lý hệ thống (Workflow Architecture)

Hệ thống tính điểm và xếp hạng được thiết kế theo kiến trúc 3 lớp (3-Tier Architecture) hoàn chỉnh, kết hợp linh hoạt giữa Spring Data JPA và Native SQL (`JdbcTemplate`) để đảm bảo hiệu năng tối ưu:

1. **Giai đoạn 1: Tiếp nhận yêu cầu (Tầng Controller)**
   - API `POST /api/rankings/rounds/{roundId}/compute` tiếp nhận lệnh kích hoạt tính toán từ Điều phối viên (Coordinator).
   - Yêu cầu được đóng gói và ủy quyền xử lý xuống tầng Service.
2. **Giai đoạn 2: Thu thập nguyên liệu thật (Tầng Service - Phần Helper)**
   - Sử dụng `RankingDataProvider` (vận hành qua `JdbcTemplate`) để viết các câu lệnh Native SQL, quét trực tiếp dữ liệu thời gian thực từ các bảng `teams`, `scoring_criteria`, `evaluations`, và `scores` trong MySQL. Không còn sử dụng mảng tĩnh giả lập.
3. **Giai đoạn 3: Thuật toán xử lý nghiệp vụ (Tầng Service - Phần Core)**
   - **Sàng lọc:** Loại bỏ triệt để các đội dính trạng thái `DISQUALIFIED` ra khỏi luồng tính toán (Thỏa mãn BR-RNK-04).
   - **Xử lý số liệu:** Nhóm điểm theo đội và theo tiêu chí, tính điểm trung bình của từng tiêu chí và áp dụng công thức nhân trọng số để ra tổng điểm. Toàn bộ tính toán sau đó được chuẩn hóa về định dạng dữ liệu chính xác cao `BigDecimal`.
   - **Định hạng:** Sắp xếp điểm giảm dần, cấp phát thứ hạng và xác định quyền thăng hạng dựa trên chỉ tiêu `promotionTopN` (Thỏa mãn BR-RNK-02, BR-RNK-05).
4. **Giai đoạn 4: Đóng gói & Giao tiếp (Tầng DTO & Contract)**
   - Toàn bộ kết quả được ép vào cấu trúc dữ liệu bất biến `RankingResponse` (Java Record) để trả về cho Client.
   - Các module khác muốn xem thứ hạng sẽ gọi qua `RankingQueryAdapter` ở vùng giao thoa kiến trúc.
5. **Giai đoạn 5: Persistence (Tầng Repository - Thành công)**
   - Sử dụng cơ chế `@Transactional` để bọc trọn gói hành động: Tự động dọn dẹp (Xóa sạch) dữ liệu cũ của vòng thi tương ứng trước khi dùng `Spring Data JPA` ghi đè toàn bộ thực thể mới xuống Database MySQL.

---

# Danh sách các file Backend đã phát triển

## 1. Tầng DTO & Contract (Giao tiếp dữ liệu)

### [x] RankingResponse.java
- Tạo Java Record chuẩn hóa dữ liệu trả về cho client, bao gồm:
  - `rankingId`
  - `teamId`
  - `teamName`
  - `roundId`
  - `totalScore` (Kiểu `Double` ở đầu ra API để tương thích với FE)
  - `rankPosition`
  - `isPromoted`

### [x] RankingQueryAdapter.java
- Triển khai implementation (stub) cho `RankingQueryPort` ở vùng `shared/contract`.
- Hỗ trợ các team (M1, M2) tránh lỗi phụ thuộc chéo trong quá trình chờ liên kết hệ thống.

---

## 2. Tầng Controller (Điều hướng API)

### [x] RankingController.java
- Áp dụng quy chuẩn `ApiResponse` cho toàn bộ dữ liệu trả về.
- Xây dựng endpoint `POST /api/rankings/rounds/{roundId}/compute` và `GET /api/rankings/rounds/{roundId}`.
- Thiết lập sẵn khung phân quyền `@PreAuthorize` (tạm thời tắt để kiểm thử thuật toán độc lập).

---

## 3. Tầng Service (Xử lý nghiệp vụ & thuật toán)

### [x] RankingService.java
- Định nghĩa Interface chứa các lệnh điều phối nghiệp vụ.

### [x] RankingDataProvider.java
- Thành phần bổ trợ tầng Service, sử dụng `JdbcTemplate` kết nối và thực hiện các câu lệnh SQL kết hợp dữ liệu liên bảng từ `submissions`, `scoring_criteria`, và `scores` nhằm cung cấp dữ liệu thật 100% từ MySQL database.

### [x] RankingServiceImpl.java
Triển khai thành công thuật toán tính điểm cốt lõi bằng Java Stream API, tuân thủ các yêu cầu trong SRS:
- **BR-RNK-04 (Loại Team):** Sử dụng `Stream.filter()` loại bỏ các Đội mang trạng thái `DISQUALIFIED` trước khi đưa số liệu vào bộ nhớ tính toán.
- **BR-RNK-02 (Công thức tính điểm):** Gom nhóm điểm bằng `Collectors.groupingBy()` kết hợp `Collectors.averagingDouble()`. Thực thi công thức tổng trọng số:
  $$Total\_Score = \sum (Average\_Score\_per\_Criterion \times Weight)$$
- **BR-RNK-05 (Ranking & Promotion):** Định cấu hình sắp xếp điểm giảm dần, tự động gán `rankPosition` lũy tiến (1, 2, 3...) và cấp quyền thăng hạng `isPromoted` theo hạn ngạch.
- **Đồng bộ hóa Mô hình Thực thể:** Loại bỏ xung đột định danh entity tên `Ranking`, chuyển đổi thành công kiểu dữ liệu số thực thành dạng `BigDecimal` và thiết lập dữ liệu thông qua cơ chế gán liên kết Object tuần tuý (`setTeam()`, `setRound()`, `setEvent()`, `setComputedBy()`).

---

## 4. Tầng Repository (Lưu trữ dữ liệu)

### [x] RankingRepository.java
- Kế thừa `JpaRepository` giao tiếp trực tiếp bảng `rankings`.
- Triển khai thành công các Custom `@Query` thao tác với thuộc tính Object quan hệ:
  - `findByRoundIdOrderByRankPositionAsc()`
  - `@Modifying` `deleteByRoundId()`

---

# Các công việc Backend tiếp theo (To-do)

## [ ] Tích hợp giải pháp Decoupling (Mô hình hóa Contract bóc tách Port)
- Sau khi code của thành viên M1 (Quản lý Team) và M2 (Quản lý Điểm) hoàn thiện tầng Database và đẩy lên nhánh chính (Main Branch), tiến hành thay thế thành phần `RankingDataProvider` bằng việc tiêm trực tiếp các Interface Port chuẩn:
  - `TeamQueryPort`
  - `ScoringQueryPort`
- Đảm bảo module Ranking tách biệt hoàn toàn khỏi việc viết các câu lệnh SQL thô truy vấn chọc thẳng bảng của module khác.

---

# Tổng kết tiến độ

| Tầng | Thành phần | Trạng thái |
|---|---|---|
| DTO & Contract | RankingResponse, RankingQueryAdapter | ✅ Hoàn thành |
| Controller | RankingController | ✅ Hoàn thành |
| Service | RankingService, RankingDataProvider, RankingServiceImpl | ✅ Hoàn thành |
| Repository | RankingRepository (Custom Query) | ✅ Hoàn thành |
| Persistence | @Modifying delete & saveAll() xuống MySQL thật | ✅ Hoàn thành |
| Integration | Chuyển đổi dữ liệu từ JdbcTemplate sang Domain Port | ⏳ Đang thực hiện |

---

**Tiến độ hiện tại:** Hoàn thành 100% logic nghiệp vụ, API, và lưu trữ cơ sở dữ liệu thực tế cho luồng xử lý Xếp hạng & Thăng hạng.