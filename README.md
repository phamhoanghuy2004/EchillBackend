<div align="center">
  <br />
  <h1>🌟 E-Chill Backend 🌟</h1>
  <p>
    <strong>Hệ thống API RESTful mạnh mẽ, dễ dàng mở rộng dành cho Nền tảng học trực tuyến E-Chill, được xây dựng với Spring Boot.</strong>
  </p>
  <br />
</div>

## 📖 Giới thiệu

**E-Chill Backend** là ứng dụng máy chủ cốt lõi cung cấp sức mạnh cho nền tảng học trực tuyến E-Chill. Được thiết kế theo hướng module hóa và dễ dàng mở rộng, hệ thống cung cấp các API mạnh mẽ để quản lý khóa học, bài kiểm tra đánh giá năng lực thích ứng (Adaptive Learning), trò chuyện theo thời gian thực (Real-time chat), và bảo mật xác thực an toàn.

---

## ✨ Các chức năng chính

- 🔐 **Xác thực & Phân quyền:** Xác thực bảo mật bằng JWT thông qua OAuth2 Resource Server. Phân quyền truy cập theo vai trò (Quản trị viên, Giáo viên, Học viên).
- 📚 **Quản lý khóa học:** Đầy đủ các thao tác CRUD cho khóa học, bài học, và tài liệu. Bao gồm cả quản lý đánh giá (Review) và ghi danh khóa học (Enrollment).
- 📝 **Kiểm tra & Đánh giá:** Tích hợp Bài kiểm tra đầu vào (Placement Tests), Bài kiểm tra thích ứng (Adaptive Tests), và theo dõi toàn diện kết quả kiểm tra để phân tích kỹ năng học viên.
- 💬 **Trò chuyện trực tuyến (Real-time Chat):** Giao tiếp mượt mà qua WebSocket giúp nhắn tin theo thời gian thực.
- ☁️ **Quản lý Media:** Tích hợp với Cloudinary để lưu trữ hình ảnh, video và tài liệu tải lên một cách nhanh chóng và an toàn.
- 💳 **Thanh toán & Giao dịch:** Quản lý Voucher, các Gói xu (Coin Packages), và lịch sử giao dịch chi tiết.
- 📊 **Bảng điều khiển Thống kê (Analytics Dashboard):** Cung cấp các thống kê và phân tích nâng cao dành cho Quản trị viên, Giáo viên, và tiến độ học tập của từng cá nhân học viên.
- 🤖 **Tích hợp AI:** Tận dụng sức mạnh của Spring AI kết hợp với Google GenAI để đem đến các tính năng thông minh (như hệ thống bài thi thích ứng và hồ sơ kỹ năng cá nhân hóa).
- 📂 **Xử lý Excel:** Khả năng nhập và xuất dữ liệu hàng loạt thông qua Apache POI.

---

## 🛠️ Công nghệ sử dụng

### Framework cốt lõi
- **Java 21**
- **Spring Boot 3.3.0** (Web, Data JPA, Validation, Mail)
- **Spring Security** (OAuth2 Resource Server)
- **Spring Boot WebSocket**

### Cơ sở dữ liệu & Caching
- **MySQL** (Hệ quản trị cơ sở dữ liệu quan hệ)
- **Redis** (Lưu trữ dữ liệu trong bộ nhớ & Caching)
- **Elasticsearch** (Công cụ tìm kiếm & Tìm kiếm toàn văn bản)

### Tiện ích & Tích hợp khác
- **Lombok** & **MapStruct** (Giảm thiểu mã rập khuôn & Ánh xạ đối tượng)
- **Spring AI** (Tích hợp Google GenAI)
- **Cloudinary HTTP44** (Lưu trữ phương tiện)
- **Apache POI** (Xử lý file Excel)
- **TSID Creator** (Tạo ID định danh duy nhất)

---

## 🚀 Cài đặt & Chạy dự án

### Yêu cầu hệ thống
Đảm bảo bạn đã cài đặt các phần mềm sau trên máy:
- **Java 21** (JDK 21)
- **Maven**
- **MySQL** Database Server
- **Redis** Server
- **Elasticsearch** Server (Tùy chọn, phụ thuộc vào cấu hình profile bạn sử dụng)

### Hướng dẫn cài đặt

1. **Clone mã nguồn về máy:**
   ```bash
   git clone <repository_url>
   cd echill-backend
   ```

2. **Cấu hình biến môi trường:**
   Tạo một file `.env` ở thư mục gốc (hoặc cấu hình thông qua file `application.yml`/`application.properties`) với các thông tin đăng nhập tương ứng cho Database, Redis, Cloudinary, Google AI, và máy chủ Mail.

3. **Build dự án:**
   Sử dụng Maven wrapper:
   ```bash
   ./mvnw clean install -DskipTests
   ```

4. **Chạy ứng dụng:**
   ```bash
   ./mvnw spring-boot:run
   ```
   *Máy chủ sẽ mặc định chạy ở cổng `8080`.*

---

## 📡 Danh sách các API chính

Ứng dụng cung cấp nhiều endpoint RESTful. Dưới đây là tổng quan về các nhóm resource (tiền tố) chính:

| Tiền tố (Prefix) | Mô tả |
|---|---|
| `/api/v1/auth` | Đăng nhập, Đăng ký, Xác thực token, Đặt lại mật khẩu |
| `/api/v1/users` | Quản lý thông tin hồ sơ người dùng |
| `/api/v1/courses` | Xem danh sách, chi tiết và chỉnh sửa thông tin khóa học |
| `/api/v1/lessons` | Nội dung bài học và tiến độ |
| `/api/v1/enrollments` | Quản lý ghi danh khóa học của học viên |
| `/api/v1/tests` | Bài kiểm tra tiêu chuẩn và đánh giá đầu vào |
| `/api/v1/adaptive-tests`| Đánh giá kỹ năng cá nhân hóa được hỗ trợ bởi AI |
| `/api/v1/chat` | Các endpoint cho WebSocket và truy xuất lịch sử trò chuyện |
| `/api/v1/payments` | Giao dịch, Gói xu (Coin), và áp dụng Voucher |
| `/api/v1/admin/*` | Các endpoint quản trị dành riêng cho việc quản lý người dùng, thống kê và kiểm soát hệ thống |

*(Lưu ý: Tài liệu API chi tiết thường có thể truy cập qua giao diện Swagger UI nếu đã được cấu hình trong môi trường.)*

---

## 🔑 Tài khoản & Mật khẩu mặc định

Sử dụng tài khoản quản trị viên dưới đây dành cho mục đích phát triển và thử nghiệm:

- **Tài khoản (Username):** `hoanghuy`
- **Mật khẩu (Password):** `17102004Huy@`

---
*Được phát triển với ❤️ bởi Đội ngũ E-Chill.*
