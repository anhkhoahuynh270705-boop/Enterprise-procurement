# Enterprise Procurement Management System (EPMS)

[![Java](https://img.shields.io/badge/Java-21-orange.svg?style=flat&logo=openjdk)](https://openjdk.org/)
[![Spring Boot](https://img.shields.io/badge/Spring%20Boot-3.3.6-brightgreen.svg?style=flat&logo=springboot)](https://spring.io/projects/spring-boot)
[![Angular](https://img.shields.io/badge/Angular-22.1-red.svg?style=flat&logo=angular)](https://angular.dev/)
[![Keycloak](https://img.shields.io/badge/Keycloak-26.0.7-blue.svg?style=flat&logo=keycloak)](https://www.keycloak.org/)
[![Camunda](https://img.shields.io/badge/Camunda-7.x-orange.svg?style=flat&logo=camunda)](https://camunda.com/)
[![Kafka](https://img.shields.io/badge/Apache%20Kafka-4.3.1-black.svg?style=flat&logo=apachekafka)](https://kafka.apache.org/)
[![PostgreSQL](https://img.shields.io/badge/PostgreSQL-15%2B-blue.svg?style=flat&logo=postgresql)](https://www.postgresql.org/)

---

## 1. Tổng quan hệ thống (Executive Overview)

**Enterprise Procurement Management System (EPMS)** là giải pháp phần mềm cấp doanh nghiệp được thiết kế nhằm số hóa, chuẩn hóa và tự động hóa toàn bộ quy trình mua sắm nội bộ, kiểm soát ngân sách và quản trị quan hệ nhà cung cấp (SRM - Supplier Relationship Management).

Hệ thống áp dụng mô hình kiểm soát kép **Maker - Checker (Người tạo - Người duyệt)** dựa trên công cụ quản lý quy trình doanh nghiệp **Camunda BPMN**, tích hợp xác thực tập trung **Keycloak Single Sign-On (SSO)** và kiến trúc hướng sự kiện thời gian thực **Apache Kafka**.

### Giá trị mang lại cho doanh nghiệp:
- **Minh bạch hóa & Kiểm soát chi phí:** Ngăn chặn chi tiêu vượt ngân sách với cơ chế tự động thẩm định ngân sách (Budget Check) trước khi phê duyệt.
- **Tuân thủ quy trình kiểm soát nội bộ (Governance & Compliance):** Phân định rõ ràng trách nhiệm giữa người lập đề xuất (Maker) và cấp có thẩm quyền phê duyệt (Checker).
- **Chuẩn hóa hồ sơ nhà cung cấp:** Đánh giá năng lực nhà cung cấp, lưu trữ tài liệu pháp lý và phân tích hồ sơ đề xuất (Proposals).
- **Chứng từ & Báo cáo chuẩn doanh nghiệp:** Tự động kết xuất phiếu mua sắm, hồ sơ nhà cung cấp định dạng PDF/Excel độ nét cao phục vụ lưu trữ kế toán và kiểm toán.

---

## 2. Kiến trúc kỹ thuật (Architecture & Tech Stack)

Hệ thống được xây dựng theo mô hình kiến trúc phân lớp hiện đại (Layered & Event-Driven Architecture):

```mermaid
flowchart TB
    subgraph ClientLayer [Tầng Giao Diện Người Dùng]
        AngularApp["Angular SPA / Angular Material\n(Port: 4200)"]
    end

    subgraph SecurityGateway [Bảo Mật & Định Danh]
        Keycloak["Keycloak IAM / OAuth2 / OIDC\n(Enterprise Branded Theme)\n(Port: 8081)"]
    end

    subgraph ApplicationLayer [Tầng Nghiệp Vụ - Backend]
        SpringBoot["Spring Boot 3.3.6 REST API\n(Port: 8080)"]
        Camunda["Camunda 7 BPMN Workflow Engine"]
        Jasper["JasperReports & Apache POI\n(Export PDF / Excel)"]
    end

    subgraph DataAndIntegration [Tầng Dữ Liệu & Tích Hợp]
        Postgres[(PostgreSQL 5432\nshopping_db)]
        Kafka["Apache Kafka 4.3.1\nEvent Streaming (Port: 9092)"]
        MailHog["MailHog SMTP Service\n(Port: 1025 / UI: 8025)"]
    end

    AngularApp -->|OIDC Authentication| Keycloak
    AngularApp -->|REST API with Bearer JWT| SpringBoot
    SpringBoot -->|Validate Token / JWKS| Keycloak
    SpringBoot --> Camunda
    SpringBoot --> Jasper
    SpringBoot -->|Spring Data JPA / HikariCP| Postgres
    SpringBoot -->|Publish / Consume Events| Kafka
    SpringBoot -->|Async Email Notifications| MailHog
```

### Công nghệ sử dụng:

| Thành phần | Công nghệ chính | Vai trò / Chi tiết |
| :--- | :--- | :--- |
| **Frontend** | Angular 22, TypeScript, SCSS | Single Page Application với Angular Material, Chart.js, OAuth2-OIDC |
| **Backend** | Java 21, Spring Boot 3.3.6 | RESTful APIs, Spring Security Resource Server, Actuator, MapStruct, Lombok |
| **BPMN Workflow** | Camunda BPM 7 | Điều phối luồng duyệt Maker - Checker, kiểm tra ngân sách, gửi thông báo |
| **Cơ sở dữ liệu** | PostgreSQL 15+ | Lưu trữ quan hệ ACID, HikariCP Connection Pool |
| **Xác thực & Ủy quyền**| Keycloak 26.0.7 | Quản lý User/Role tập trung, JWT Token, giao diện đăng nhập tùy biến doanh nghiệp |
| **Truyền thông điệp** | Apache Kafka 4.3.1 | Xử lý sự kiện mua sắm bất đồng bộ (Procurement Events), tích hợp Kafka UI |
| **Báo cáo & Chứng từ** | JasperReports & Apache POI | Tự động sinh phiếu duyệt, biên bản bàn giao PDF và xuất báo cáo Excel |
| **Email Mocking** | MailHog | Máy chủ kiểm thử gửi nhận email thông báo luồng duyệt |
| **Hạ tầng hỗ trợ** | Docker & Docker Compose | Đóng gói và chạy môi trường dev một chạm cho Kafka, Keycloak, MailHog |

---

## 3. Các phân hệ chức năng chính (Core Modules)

### 📌 1. Phân hệ Quản lý Mua sắm (Procurement Management)
- Tạo yêu cầu mua sắm (Purchase Requisition/Order) với chi tiết danh mục hàng hóa, số lượng, đơn giá và nhà cung cấp.
- Tự động tính toán tổng giá trị đơn hàng, thuế VAT và đối soát hạn mức ngân sách phòng ban.
- Tra cứu lịch sử thay đổi trạng thái và mã định danh nghiệp vụ duy nhất.

### 📌 2. Luồng phê duyệt Maker - Checker (BPMN Workflow)
- Tách bạch quyền hạn theo chuẩn quản trị rủi ro:
  - **Maker (Nhân viên mua sắm):** Lập phiếu đề xuất mua sắm, bổ sung hồ sơ và gửi duyệt.
  - **Checker (Trưởng bộ phận / Kế toán trưởng):** Tiếp nhận danh sách nhiệm vụ (Task List), xem xét thông tin, phê duyệt (Approve) hoặc từ chối (Reject) kèm lý do.
- Tự động kích hoạt các Service Delegates của Camunda: `BudgetCheckDelegate`, `ApprovalNotificationDelegate`, `RejectionNotificationDelegate`.

### 📌 3. Quản lý Nhà cung cấp (Supplier Relationship Management - SRM)
- Quản lý danh bạ đối tác, thông tin pháp lý, mã số thuế, đánh giá độ tin cậy và phân loại nhà cung cấp.
- Quản lý hồ sơ đề xuất (Supplier Proposals) và tài liệu đính kèm (hợp đồng, hồ sơ năng lực).
- Hỗ trợ nhập liệu hàng loạt danh sách nhà cung cấp qua file Excel (Apache POI) với cơ chế kiểm tra lỗi từng dòng.

### 📌 4. Báo cáo & Xuất chứng từ (Reports & Analytics)
- Dashboard phân tích tổng quan: Biểu đồ thống kê chi tiêu theo thời gian, tỷ lệ đơn hàng được duyệt/từ chối, chi phí theo nhà cung cấp (Chart.js).
- Xuất phiếu mua sắm chính thức (Procurement Voucher) định dạng PDF chuẩn in ấn qua JasperReports.
- Xuất báo cáo tổng hợp danh sách nhà cung cấp và lịch sử mua sắm ra file Excel (.xlsx).

### 📌 5. Bảo mật & Quản lý danh tính (Security & IAM)
- Đăng nhập một lần (Single Sign-On) qua Keycloak OIDC.
- Kiểm soát truy cập dựa trên vai trò (RBAC): `ADMIN`, `MAKER`, `CHECKER`, `VIEWER`.
- Giao diện đăng nhập Keycloak được thiết kế độc quyền theo nhận diện thương hiệu doanh nghiệp (hỗ trợ đa ngôn ngữ Tiếng Việt / Tiếng Anh, video background).

---

## 4. Yêu cầu môi trường (Prerequisites)

Trước khi bắt đầu cài đặt, đảm bảo máy chủ/máy phát triển đã cài đặt các công cụ sau:

- **JDK 21** (hoặc OpenJDK 21) & **Maven 3.9+** (hoặc sử dụng wrapper `mvnw`)
- **Node.js 20.x+** & **npm 10.x+**
- **Docker** & **Docker Compose v2+**
- **PostgreSQL 15+** (chạy tại port mặc định `5432`, có database tên `shopping_db`)

---

## 5. Hướng dẫn cài đặt & Khởi chạy nhanh (Quick Start)

Thực hiện lần lượt 5 bước sau để triển khai toàn bộ hệ thống lên môi trường nội bộ:

### Bước 1: Clone mã nguồn
```bash
git clone https://github.com/your-organization/enterprise-procurement.git
cd enterprise-procurement
```

### Bước 2: Khởi tạo cơ sở dữ liệu PostgreSQL
Đảm bảo bạn đã có một cơ sở dữ liệu PostgreSQL đang chạy ở port `5432`:
- **Tên cơ sở dữ liệu:** `shopping_db`
- **Schema Keycloak:** Tạo thêm schema `keycloak` trong database:
  ```sql
  CREATE DATABASE shopping_db;
  \c shopping_db;
  CREATE SCHEMA IF NOT EXISTS keycloak;
  ```

### Bước 3: Khởi chạy các dịch vụ phụ trợ qua Docker Compose
Khởi động cụm dịch vụ Keycloak, Apache Kafka, Kafka UI và MailHog:
```bash
docker compose up -d
```
> [!NOTE]
> Lần đầu chạy, Keycloak có thể mất từ 1 - 2 phút để hoàn tất khởi tạo cấu trúc bảng trong schema `keycloak`.

### Bước 4: Cấu hình biến môi trường
Hệ thống sử dụng các file template mẫu để bảo mật thông tin nhạy cảm:

1. **Cấu hình Backend:**
   ```bash
   cd Backend/src/main/resources
   # Sao chép file cấu hình mẫu:
   cp application.properties.example application.properties
   # Trên Windows PowerShell:
   # Copy-Item application.properties.example application.properties
   ```
   *Mở file `application.properties` vừa tạo và cập nhật mật khẩu PostgreSQL (`spring.datasource.password`) hoặc Keycloak Secret tương ứng.*

2. **Cấu hình Frontend:**
   ```bash
   cd ../../../Frontend
   # Sao chép file biến môi trường mẫu:
   cp .env.example .env
   # Trên Windows PowerShell:
   # Copy-Item .env.example .env
   ```

### Bước 5: Chạy ứng dụng

1. **Khởi chạy Backend (Spring Boot API):**
   ```bash
   cd Backend
   ./mvnw spring-boot:run
   # Trên Windows:
   # .\mvnw.cmd spring-boot:run
   ```
   *Backend sẽ khởi động tại địa chỉ: `http://localhost:8080`.*

2. **Khởi chạy Frontend (Angular SPA):**
   ```bash
   cd ../Frontend
   npm install
   npm start
   ```
   *Frontend sẽ khởi động tại địa chỉ: `http://localhost:4200`.*

---

## 6. Bảng tra cứu dịch vụ & Thông tin đăng nhập mặc định

| Dịch vụ | URL truy cập | Tài khoản / Mật khẩu mặc định | Mô tả |
| :--- | :--- | :--- | :--- |
| **Giao diện người dùng (Frontend)** | [http://localhost:4200](http://localhost:4200) | Đăng nhập qua Keycloak SSO | Màn hình thao tác chính |
| **Backend REST API** | [http://localhost:8080/api](http://localhost:8080/api) | — | Cổng giao tiếp API |
| **Keycloak Admin Console** | [http://localhost:8081](http://localhost:8081) | `admin` / `admin` | Quản trị Realm `Shopping`, Client, Roles, Users |
| **Kafka UI** | [http://localhost:8085](http://localhost:8085) | — | Giám sát Topics, Consumers, Messages Kafka |
| **MailHog Web UI** | [http://localhost:8025](http://localhost:8025) | — | Hộp thư kiểm thử nhận thông báo email |
| **MailHog SMTP Server** | `localhost:1025` | *(Không yêu cầu)* | Cổng gửi nhận mail nội bộ của Spring Boot |

---

## 7. Cấu trúc thư mục dự án (Project Structure)

```text
Enterprise/
├── .gitignore                         # Gitignore cấp monorepo (chặn file rác, .env, properties)
├── docker-compose.yml                 # Docker cấu hình Keycloak, Kafka, Kafka UI, MailHog
├── README.md                          # Tài liệu hướng dẫn dự án doanh nghiệp
│
├── Backend/                           # Phân hệ Backend (Java Spring Boot)
│   ├── .gitignore                     # Bỏ qua target/, file build, application.properties
│   ├── pom.xml                        # Maven dependencies & plugins cấu hình dự án
│   ├── src/main/java/com/example/shopping/
│   │   ├── audit/                     # Ghi nhận lịch sử kiểm toán (Audit Trail)
│   │   ├── auth/                      # Tiện ích liên quan tới phiên xác thực
│   │   ├── company/                   # Quản lý tài liệu & thông tin công ty
│   │   ├── config/                    # Các cấu hình hệ thống (Async, WebMvc, v.v.)
│   │   ├── dashboard/                 # Thống kê số liệu, KPI mua sắm
│   │   ├── integration/               # Tích hợp Kafka Producer/Consumer, Keycloak Client
│   │   ├── procurement/               # Nghiệp vụ mua sắm & đơn đặt hàng
│   │   ├── security/                  # Cấu hình Spring Security OAuth2 Resource Server
│   │   ├── supplier/                  # Quản lý nhà cung cấp, đề xuất, nhập xuất báo cáo
│   │   ├── user/                      # Nghiệp vụ tài khoản người dùng
│   │   └── workflow/                  # Camunda BPMN integration, Controllers, Delegates
│   └── src/main/resources/
│       ├── application.properties.example # Cấu hình mẫu an toàn để chia sẻ
│       ├── bpmn/                      # File định nghĩa quy trình Camunda (.bpmn)
│       ├── db/migration/              # Kịch bản khởi tạo & cập nhật cơ sở dữ liệu SQL
│       ├── reports/                   # Mẫu template báo cáo JasperReports (.jrxml)
│       └── templates/mail/            # Mẫu email thông báo HTML Thymeleaf
│
├── Frontend/                          # Phân hệ Frontend (Angular 22)
│   ├── .env.example                   # Biến môi trường mẫu
│   ├── .gitignore                     # Bỏ qua node_modules/, dist/, .env
│   ├── package.json                   # Thư viện npm và các lệnh thực thi
│   ├── scripts/generate-env.mjs       # Script tự động trích xuất .env sang environment.ts
│   └── src/app/
│       ├── core/                      # Interceptors, Guards, dịch vụ cốt lõi
│       ├── features/                  # Các màn hình nghiệp vụ
│       │   ├── auth/                  # Màn hình đăng nhập & callback Keycloak
│       │   ├── dashboard/             # Màn hình bảng điều khiển thống kê
│       │   ├── procurement/           # Màn hình tạo đơn & quy trình Maker-Checker
│       │   ├── suppliers/             # Màn hình quản lý nhà cung cấp & đề xuất
│       │   ├── company/               # Màn hình tài liệu công ty
│       │   └── users/                 # Màn hình phân quyền & tài khoản
│       └── shared/                    # Các components, pipes, UI dùng chung
│
└── Keycloak/                          # Tùy biến Keycloak Identity Provider
    ├── themes/enterprise/             # Theme đăng nhập nhận diện thương hiệu doanh nghiệp
    │   └── login/                     # Giao diện FTL, CSS, đa ngôn ngữ VI/EN
    └── inspect-theme.mjs              # Công cụ hỗ trợ kiểm tra theme
```

## 8. Giấy phép & Bản quyền (License & Copyright)

Dự án này được phát triển và thuộc quyền sở hữu nội bộ của Doanh Nghiệp. Mọi hành vi sao chép, phân phối mã nguồn ra bên ngoài khi chưa có văn bản chấp thuận đều bị nghiêm cấm theo chính sách bảo mật thông tin của tổ chức.