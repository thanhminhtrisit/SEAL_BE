# Nhật ký phát triển mã nguồn (BE) - SEAL

**Thành viên:** Nguyễn Công Thiên Ân (M3)  
**Module phụ trách:** (5) Ranking & Promotion

---

# Danh sách các file Backend đã phát triển

## 1. Tầng DTO & Contract (Giao tiếp dữ liệu)

### [x] RankingResponse.java
- Tạo Java Record chuẩn hóa dữ liệu trả về cho client, bao gồm:
  - `rankingId`
  - `teamId`
  - `teamName`
  - `roundId`
  - `totalScore`
  - `rankPosition`
  - `isPromoted`

### [x] RankingQueryAdapter.java
- Triển khai implementation (stub) cho `RankingQueryPort` ở vùng `shared/contract`.
- Hỗ trợ các team (M1, M2) tránh lỗi phụ thuộc chéo trong quá trình chờ hoàn thiện logic Database.

---

## 2. Tầng Controller (Điều hướng API)

### [x] RankingController.java

- Áp dụng quy chuẩn `ApiResponse` cho toàn bộ dữ liệu trả về.
- Xây dựng endpoint:

```http
POST /api/rankings/rounds/{roundId}/compute
```

- Nhận lệnh kích hoạt quá trình tính toán xếp hạng từ Coordinator.
- Thiết lập sẵn khung phân quyền `@PreAuthorize` (tạm thời tắt để kiểm thử thuật toán độc lập).

---

## 3. Tầng Service (Xử lý nghiệp vụ & thuật toán)

### [x] RankingService.java

- Định nghĩa Interface chứa các method xử lý nghiệp vụ chuẩn.

### [x] RankingServiceImpl.java

Triển khai thành công thuật toán tính điểm cốt lõi bằng Java Stream API, tuân thủ các yêu cầu trong SRS:

#### BR-RNK-04: Loại Team

- Sử dụng `Stream.filter()` để loại bỏ các Team có trạng thái `DISQUALIFIED`.
- Đảm bảo các Team bị loại không tham gia quá trình tính điểm và xếp hạng.

#### BR-RNK-02: Công thức tính điểm

- Sử dụng `Collectors.groupingBy()` để nhóm điểm theo từng `criterionId`.
- Kết hợp `Collectors.averagingDouble()` để tính điểm trung bình cho từng tiêu chí.
- Thực hiện công thức tính điểm tổng:

```
TOTAL_SCORE = Σ(averageScore × weight)
```

- Đảm bảo kết quả cuối cùng phản ánh chính xác trọng số của từng tiêu chí.

#### BR-RNK-05: Ranking & Promotion

- Sử dụng `Comparator.comparing().reversed()` để sắp xếp điểm từ cao xuống thấp.
- Dùng vòng lặp để:
  - Gán thứ hạng (`rankPosition`) theo thứ tự 1, 2, 3,...
  - Xác định trạng thái thăng hạng (`isPromoted`) dựa trên giá trị `promotionTopN`.

---

# Các công việc Backend tiếp theo (To-do)

## [ ] Tầng Repository

### RankingRepository.java

Cần xây dựng các Custom Query:

- `deleteByRoundId()`
- `findByRoundIdOrderByRankPositionAsc()`

---

## [ ] Persistence (Lưu Database)

- Tích hợp lệnh:

```java
rankingRepository.saveAll(rankings);
```

- Lưu kết quả tính toán xếp hạng xuống Database.

---

## [ ] Tích hợp dữ liệu thật qua Port

- Loại bỏ các lớp Mock Data:
  - `TeamView`
  - `ScoreView`

- Thực hiện Inject các Port:
  - `TeamQueryPort`
  - `ScoringQueryPort`

- Lấy dữ liệu thực tế từ các Module khác để thay thế dữ liệu giả lập.

---

# Tổng kết tiến độ

| Tầng | Thành phần | Trạng thái |
|---|---|---|
| DTO & Contract | RankingResponse, RankingQueryAdapter | ✅ Hoàn thành |
| Controller | RankingController | ✅ Hoàn thành |
| Service | RankingService, RankingServiceImpl | ✅ Hoàn thành |
| Repository | Custom Query | ⏳ Đang thực hiện |
| Persistence | saveAll() lưu Database | ⏳ Đang thực hiện |
| Integration | TeamQueryPort, ScoringQueryPort | ⏳ Đang thực hiện |

---

**Tiến độ hiện tại:** Hoàn thành phần thiết kế API và thuật toán tính Ranking & Promotion.  
Các bước tiếp theo tập trung vào tích hợp Database và kết nối dữ liệu liên module.