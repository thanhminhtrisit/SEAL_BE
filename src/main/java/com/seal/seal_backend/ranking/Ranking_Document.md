# Nhật ký phát triển mã nguồn (BE) - SEAL

**Thành viên:** Nguyễn Công Thiên Ân (M3)
**Module phụ trách:** (5) Ranking & Promotion

---

# Kiến trúc & Luồng xử lý hệ thống (Workflow Architecture)

Hệ thống tính điểm và xếp hạng được thiết kế theo kiến trúc 3 lớp (3-Tier Architecture), với luồng dữ liệu đi qua 5 giai đoạn cốt lõi:

## Giai đoạn 1: Tiếp nhận yêu cầu (Tầng Controller)

* API:

  ```http
  POST /api/rankings/rounds/{roundId}/compute
  ```
* Tiếp nhận lệnh kích hoạt quá trình tính toán xếp hạng từ Coordinator.
* Yêu cầu được chuyển xuống tầng Service để xử lý nghiệp vụ.

## Giai đoạn 2: Thu thập dữ liệu (Tầng Service - Phần 1)

* Thu thập các dữ liệu đầu vào:

  * Danh sách đội thi (Teams)
  * Bộ tiêu chí và trọng số (Criteria & Weight)
  * Điểm số chi tiết từ các giám khảo (Scores)
* Hiện tại sử dụng Mock Data.
* Trong giai đoạn tích hợp sẽ gọi dữ liệu thật thông qua các Port giữa các Module.

## Giai đoạn 3: Thuật toán cốt lõi (Tầng Service - Phần 2)

### BR-RNK-04: Loại Team

* Sử dụng `Stream.filter()` để loại bỏ các Team có trạng thái `DISQUALIFIED`.
* Các đội bị loại sẽ không tham gia quá trình tính điểm và xếp hạng.

### BR-RNK-02: Công thức tính điểm

* Sử dụng `Collectors.groupingBy()` để gom nhóm điểm theo từng `criterionId`.
* Sử dụng `Collectors.averagingDouble()` để tính điểm trung bình của từng tiêu chí.
* Áp dụng công thức:

[
Total_Score = \sum(Average_Score_per_Criterion \times Weight)
]

* Đảm bảo điểm tổng phản ánh chính xác trọng số của từng tiêu chí.

### BR-RNK-05: Ranking & Promotion

* Sử dụng `Comparator.comparing().reversed()` để sắp xếp điểm theo thứ tự giảm dần.
* Cấp phát thứ hạng:

  * Rank 1, Rank 2, Rank 3, ...
* Xác định trạng thái thăng hạng `isPromoted` dựa trên chỉ tiêu `promotionTopN`.

## Giai đoạn 4: Đóng gói & Giao tiếp (DTO & Contract)

* Kết quả tính toán được đóng gói vào `RankingResponse` trước khi trả về Client.
* Các Module khác truy cập dữ liệu Ranking thông qua `RankingQueryAdapter` nhằm đảm bảo nguyên tắc kiến trúc.

## Giai đoạn 5: Persistence (Repository - To-do)

* Xóa dữ liệu Ranking cũ theo vòng thi.
* Lưu danh sách Ranking mới xuống Database MySQL.

---

# Danh sách các file Backend đã phát triển

## 1. DTO & Contract

### ✅ RankingResponse.java

Java Record chuẩn hóa dữ liệu trả về cho Client:

* `rankingId`
* `teamId`
* `teamName`
* `roundId`
* `totalScore`
* `rankPosition`
* `isPromoted`

### ✅ RankingQueryAdapter.java

* Triển khai Stub Implementation cho `RankingQueryPort` trong vùng `shared/contract`.
* Hỗ trợ các Module khác (M1, M2) tránh lỗi phụ thuộc chéo trong quá trình phát triển.

---

## 2. Controller

### ✅ RankingController.java

Chức năng:

* Áp dụng chuẩn phản hồi `ApiResponse`.
* Cung cấp Endpoint:

```http
POST /api/rankings/rounds/{roundId}/compute
```

* Nhận yêu cầu tính toán Ranking từ Coordinator.
* Chuẩn bị cơ chế phân quyền `@PreAuthorize` (tạm thời tắt để kiểm thử thuật toán độc lập).

---

## 3. Service

### ✅ RankingService.java

* Định nghĩa Interface chứa các phương thức xử lý nghiệp vụ Ranking.

### ✅ RankingServiceImpl.java

Triển khai thuật toán tính Ranking bằng Java Stream API, đảm bảo tuân thủ các Business Rule trong SRS:

* BR-RNK-02: Tính điểm theo trọng số.
* BR-RNK-04: Loại bỏ Team bị Disqualified.
* BR-RNK-05: Xếp hạng và xác định Promotion.

---

# Các công việc Backend tiếp theo (To-do)

## ⏳ Repository

### RankingRepository.java

Cần xây dựng các Custom Query:

* `deleteByRoundId()`
* `findByRoundIdOrderByRankPositionAsc()`

---

## ⏳ Persistence (Lưu Database)

Tích hợp thao tác:

```java
rankingRepository.saveAll(rankings);
```

Mục tiêu:

* Lưu kết quả tính toán Ranking xuống Database.

---

## ⏳ Tích hợp dữ liệu thật qua Port

Loại bỏ Mock Data:

* `TeamView`
* `ScoreView`

Inject các Port:

* `TeamQueryPort`
* `ScoringQueryPort`

Mục tiêu:

* Lấy dữ liệu thật từ các Module khác thay cho dữ liệu giả lập.

---

# Tổng kết tiến độ

| Tầng           | Thành phần                           | Trạng thái       |
| -------------- | ------------------------------------ | ---------------- |
| DTO & Contract | RankingResponse, RankingQueryAdapter | ✅ Hoàn thành     |
| Controller     | RankingController                    | ✅ Hoàn thành     |
| Service        | RankingService, RankingServiceImpl   | ✅ Hoàn thành     |
| Repository     | Custom Query                         | ⏳ Đang thực hiện |
| Persistence    | `saveAll()` lưu Database             | ⏳ Đang thực hiện |
| Integration    | TeamQueryPort, ScoringQueryPort      | ⏳ Đang thực hiện |

---

## Tiến độ hiện tại

✅ Đã hoàn thành:

* Thiết kế API Ranking.
* Xây dựng kiến trúc xử lý dữ liệu.
* Triển khai thuật toán tính điểm và xếp hạng theo SRS.

⏳ Các bước tiếp theo:

* Hoàn thiện Repository.
* Tích hợp lưu trữ Database.
* Kết nối dữ liệu liên Module thông qua Port.
