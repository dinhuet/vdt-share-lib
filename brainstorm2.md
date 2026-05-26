# Brainstorm 2: VDT Shared Library & Management System

> Ngày: 2026-05-26
> Bối cảnh: Đồ án / POC
> Kiến trúc: **Flat Monorepo** — Management System trước, Shared Lib sau
> Thay đổi so với brainstorm v1: Tách 3 mini project, đảo thứ tự triển khai, bổ sung phân tích intercept/auth/anomaly theo yêu cầu SRS

---

## 1. Bài toán

Trong kiến trúc Microservices, các dịch vụ chia sẻ và đồng bộ dữ liệu với nhau (hoặc với bên thứ ba) qua Webhook và Message Queue. Khi hệ thống phình to, doanh nghiệp đối mặt với:

- Khó quản lý danh sách client bên ngoài
- Rủi ro bảo mật dữ liệu (ai được gọi API nào, rate limit, blacklist...)
- Thiếu log tập trung để giám sát và phát hiện bất thường
- Mỗi microservice tự implement logic auth/retry/log → code trùng lặp

**Giải pháp:** Xây dựng một **Shared Library (.jar)** kết hợp **Hệ thống quản lý tập trung (Management System)**, đóng vai trò cầu nối giữa hệ thống nội bộ và client bên ngoài.

---

## 2. Quyết định thiết kế

| Quyết định | Lựa chọn | Lý do |
|---|---|---|
| Kiến trúc project | Flat Monorepo (3 module) | Đơn giản, ranh giới rõ ràng, mỗi module build độc lập |
| Thứ tự triển khai | Management System trước, Lib sau | Dựng hệ thống quản lý trước, Lib đọc config từ Redis cache |
| Intercept method | Spring AOP | Chuẩn Spring, declarative annotation, dễ demo |
| Xác thực client | API Key (Client-ID / Secret Key) | Đơn giản, đủ cho POC, dễ implement và debug |
| Anomaly detection | Hybrid: Rule-based + ML nhẹ (Isolation Forest) | Rule-based realtime trong Lib + ML batch trong Management System |
| Frontend | ReactJS | Ecosystem lớn, phổ biến, dễ tìm tài liệu |
| Primary storage | PostgreSQL (source of truth) + Redis (cache/rate limit) + Elasticsearch (logs) | SQL lưu quan hệ, Redis cho fast lookup, ES cho log |
| Build tool | Maven | Phổ biến enterprise Java, Spring Boot hỗ trợ tốt |
| Java version | Java 17 (LTS) | Stable, Spring Boot 3.x yêu cầu tối thiểu |
| Kênh giao tiếp | REST + Kafka | Đầy đủ theo SRS |

---

## 3. Phân tích phương thức Intercept (yêu cầu SRS)

SRS yêu cầu đánh giá: AOP, Java Agent (Elastic APM), OpenTelemetry, HTTP Interceptor.

### 3.1 So sánh

| Tiêu chí | Spring AOP | Java Agent (Elastic APM) | OpenTelemetry | HTTP Interceptor |
|---|---|---|---|---|
| **Cơ chế** | Proxy-based, intercept method qua annotation | Bytecode instrumentation tại JVM level | SDK + Agent tự động instrument | Spring HandlerInterceptor / Filter |
| **Scope** | Mọi Spring bean method | Mọi class/method (kể cả non-Spring) | Chủ yếu tracing, metrics | Chỉ HTTP request (Controller layer) |
| **Custom annotation** | Hỗ trợ native (`@Around`, `@Before`, `@After`) | Không hỗ trợ trực tiếp, phải custom plugin | Không hỗ trợ trực tiếp | Không hỗ trợ |
| **Tích hợp Spring** | Native, zero-config | Cần thêm JVM arg `-javaagent` | Cần thêm dependency + config | Native |
| **Độ phức tạp** | Thấp | Cao (bytecode manipulation) | Trung bình | Thấp |
| **Interceptable** | Public method qua bean injection | Mọi method kể cả private | HTTP request/response | Chỉ HTTP request |
| **Kafka support** | Có (intercept producer/consumer method) | Có (tự động nếu dùng APM agent) | Có (Kafka instrumentation) | Không |
| **Use case chính** | Cross-cutting concerns (logging, auth, tx) | APM, distributed tracing | Observability, tracing, metrics | Request/response filtering |

### 3.2 Tại sao chọn Spring AOP

1. **Custom Annotation là yêu cầu cốt lõi của SRS.** AOP là cách duy nhất trong Spring ecosystem cho phép tạo custom annotation (`@SharedApi`, `@ClientCall`) rồi tự động intercept runtime. Java Agent và OpenTelemetry không hỗ trợ custom annotation một cách tự nhiên.

2. **Scope rộng hơn HTTP Interceptor.** HTTP Interceptor chỉ bắt được REST request ở Controller layer. AOP intercept được mọi method trong mọi Spring bean -- bao gồm cả Service layer (cho `@ClientCall` gọi đi client).

3. **Không cần JVM argument.** Java Agent yêu cầu thêm `-javaagent:path/to/agent.jar` khi khởi động JVM. AOP hoạt động native trong Spring Boot, consumer chỉ cần thêm dependency.

4. **Phù hợp đồ án.** "Gắn annotation → tự động log/auth/retry" là demo ấn tượng. Java Agent và OpenTelemetry phù hợp hơn cho observability platform (Datadog, New Relic) -- scope khác.

### 3.3 Khi nào KHÔNG chọn AOP

- AOP không intercept được **private method** hoặc **self-invocation** (method A gọi method B trong cùng class). Đây là hạn chế của proxy-based AOP. Document rõ cho consumer.
- Nếu cần tracing phân tán (distributed tracing) xuyên suốt nhiều service, **OpenTelemetry** là lựa chọn tốt hơn. Tuy nhiên đây nằm ngoài scope SRS.

---

## 4. Phân tích phương thức xác thực Client (yêu cầu SRS)

SRS yêu cầu đánh giá: JWT, Client-ID/Secret Key, Ký số/Timestamp.

### 4.1 So sánh

| Tiêu chí | JWT | Client-ID / Secret Key (API Key) | Ký số / Timestamp (HMAC) |
|---|---|---|---|
| **Cơ chế** | Client lấy token từ auth endpoint → gửi `Authorization: Bearer <token>` | Client gửi `X-Client-Id` + `X-Client-Secret` mỗi request | Client tạo `HMAC-SHA256(secret, method+path+timestamp+body)` → gửi `X-Signature` + `X-Timestamp` |
| **Stateless** | Có (verify bằng secret/public key) | Không (lookup Redis mỗi request) | Có (verify bằng shared secret) |
| **Bảo mật** | Trung bình (token lộ → dùng được đến khi hết hạn) | Thấp-TB (secret truyền mỗi request, cần HTTPS) | Cao (secret không truyền qua mạng, chống replay + tamper) |
| **Revoke tức thì** | Khó (cần maintain blacklist) | Dễ (xóa key trong Redis) | Dễ (xóa shared secret) |
| **Độ phức tạp implement** | Trung bình (cần auth endpoint + token refresh) | Thấp (chỉ cần lookup) | Cao (signing logic, clock sync, SDK cho mỗi ngôn ngữ) |
| **Client effort** | Trung bình (phải implement token refresh flow) | Rất thấp (chỉ gửi header) | Cao (phải implement signing mỗi request) |
| **Debug** | Trung bình (decode JWT để xem claims) | Dễ (nhìn header là biết) | Khó (phải tái tạo signature để so sánh) |
| **Expiration** | Có (tự động, exp claim trong token) | Tùy config (set TTL trong Redis) | Có (timestamp window, thường 5 phút) |
| **Phù hợp POC** | Có nhưng thừa | Có, rất phù hợp | Quá phức tạp |

### 4.2 Lựa chọn: API Key (Client-ID / Secret Key)

**Lý do chọn cho POC:**

1. **Đơn giản nhất** -- Management System cấp API Key, client chỉ cần gửi header. Không cần token refresh flow.
2. **Revoke tức thì** -- Xóa key trong Redis → client bị chặn ngay lập tức. JWT không làm được điều này nếu không dùng thêm blacklist.
3. **Dễ demo** -- Postman/curl chỉ cần thêm 2 header. Không cần gọi auth endpoint trước.
4. **Lookup Redis rất nhanh** -- Redis đọc O(1), latency < 1ms. "Stateful" không phải vấn đề khi dùng Redis.

**Flow xác thực:**

```
Client Request
    │
    ├─ Header: X-Client-Id: partner-A
    ├─ Header: X-Client-Secret: sk_abc123...
    │
    ▼
[WhitelistBlacklistFilter]
    │
    ├─ Lookup Redis: vdt:acl:partner-A
    ├─ Nếu BLACKLIST → 403 Forbidden + Log
    │
    ▼
[ClientAuthenticator]
    │
    ├─ Lookup Redis: vdt:apikey:sk_abc123...
    ├─ Verify: key tồn tại? chưa hết hạn? clientId khớp?
    ├─ Nếu fail → 401 Unauthorized + Log
    │
    ▼
[Rate Limiter]
    │
    ├─ INCR Redis: vdt:rate:partner-A:get-orders:{minute}
    ├─ Nếu > limit → 429 Too Many Requests + Log
    │
    ▼
[Proceed to @SharedApi method]
```

---

## 5. Phân tích thuật toán phát hiện bất thường (yêu cầu SRS)

SRS yêu cầu đánh giá: xác suất thống kê, trung bình response time, AI agent, ML -- với đánh đổi chi phí vs hiệu quả.

### 5.1 So sánh các hướng tiếp cận

| Hướng tiếp cận | Mô tả | Chi phí | Hiệu quả | Phù hợp |
|---|---|---|---|---|
| **Rule-based (threshold)** | Đếm fail liên tiếp, so sánh rate với threshold cố định | Rất thấp (Redis counter) | ~60% -- chỉ bắt được pattern đơn giản, nhiều false positive | POC nhanh |
| **Xác suất thống kê (Z-Score)** | Tính mean + std deviation, đánh dấu outlier khi |z| > 2-3 | Thấp (tính toán đơn giản trên ES aggregation) | ~75% -- phát hiện outlier 1 chiều tốt, yếu với multivariate | POC có phân tích |
| **Trung bình response time (Moving Average)** | Dùng Exponential Moving Average (EMA) + Bollinger Bands để phát hiện trend | Thấp (tính trên time-series từ ES) | ~70% -- tốt cho slow degradation, yếu với spike đột ngột | Monitoring |
| **ML nhẹ (Isolation Forest)** | Unsupervised learning, phát hiện anomaly đa chiều (response time, error rate, request count, body size) | Trung bình (cần train model, nhưng Isolation Forest nhẹ) | ~85% -- phát hiện pattern phức tạp mà rule-based bỏ sót | Đồ án gây ấn tượng |
| **ML nặng (Deep Learning, LSTM)** | Time-series anomaly detection bằng neural network | Cao (cần GPU, data lớn, training time dài) | ~90%+ nhưng cần data lớn mới hiệu quả | Production enterprise |
| **AI Agent (LLM-based)** | Dùng LLM phân tích log pattern, tự viết rule mới | Rất cao (API cost, latency cao, non-deterministic) | Không ổn định, phụ thuộc prompt engineering | Research, chưa production-ready |

### 5.2 Đánh đổi chi phí vs hiệu quả

```
Hiệu quả
  ▲
  │              ┌─────────────┐
  │              │ Deep Learning│
  │          ┌───┤             │
  │          │   └─────────────┘
  │    ┌─────┤ Isolation Forest   ◄── Sweet spot cho đồ án
  │    │     └────────────────┘
  │  ┌─┤ Z-Score + Moving Avg
  │  │ └──────────────────┘
  │┌─┤ Rule-based
  ││ └────────────┘
  │└ AI Agent (unstable)
  └──────────────────────────────────► Chi phí
       Thấp        TB        Cao
```

### 5.3 Lựa chọn: Hybrid 2 tầng

#### Tầng 1: Rule-based (realtime, chạy trong Shared Lib)

Xử lý ngay khi request đến, dùng Redis counter:

| Rule | Logic | Action |
|---|---|---|
| **Consecutive Fail** | Client fail > N lần liên tiếp (`INCR vdt:fail:{clientId}`, reset khi success) | Ghi cảnh báo vào ES + auto temp blacklist nếu > 2N |
| **Rate Spike** | Request count trong 1 phút > X × trung bình 7 ngày cùng khung giờ (X configurable, mặc định 3.0). Trung bình lưu trong Redis key `vdt:avg:{clientId}:{resource}:{hour}`, cập nhật hàng ngày bởi Management System batch job | Ghi cảnh báo vào ES |
| **Slow Response** | Response time > threshold (configurable per client/resource) | Log warning vào ES |
| **Body Size Violation** | Request/Response body > max size config | Reject 413 + log |

#### Tầng 2: ML nhẹ -- Isolation Forest (batch, chạy trong Management System)

Chạy định kỳ (cron, mặc định mỗi 15 phút), đọc log từ ES:

**Features đầu vào:**
- `avg_response_time` (trung bình response time trong window)
- `error_rate` (tỉ lệ lỗi)
- `request_count` (số lượng request)
- `avg_body_size` (kích thước trung bình body)
- `time_of_day` (giờ trong ngày -- phát hiện off-hours activity)

**Thư viện:** Sử dụng **Tribuo** (Oracle open-source ML library cho Java) -- chạy native trong JVM, không cần Python, không cần GPU. Hoặc dùng **Smile** (Statistical Machine Intelligence and Learning Engine) -- pure Java.

**Output:** Ghi kết quả vào ES index `vdt-anomaly-YYYY.MM.dd`, hiển thị trên Dashboard.

---

## 6. Kiến trúc tổng quan

### 6.1 Cấu trúc Monorepo

```
vdt-share-lib/                              (parent POM)
│
├── vdt-management-system/                  # Module 1: Hệ thống quản lý tập trung
│   ├── pom.xml
│   └── src/
│       ├── main/
│       │   ├── java/com/vdt/management/
│       │   │   ├── ManagementApplication.java
│       │   │   ├── controller/             # REST API controllers
│       │   │   │   ├── ServiceController.java
│       │   │   │   ├── ClientController.java
│       │   │   │   ├── ApiKeyController.java
│       │   │   │   ├── InboundEndpointController.java
│       │   │   │   ├── OutboundEndpointController.java
│       │   │   │   ├── AclController.java
│       │   │   │   ├── AuthConfigController.java
│       │   │   │   ├── AlertConfigController.java
│       │   │   │   ├── LogController.java
│       │   │   │   ├── DashboardController.java
│       │   │   │   └── AlertController.java
│       │   │   ├── service/                # Business logic
│       │   │   │   ├── ServiceService.java
│       │   │   │   ├── ClientService.java
│       │   │   │   ├── ApiKeyService.java
│       │   │   │   ├── InboundEndpointService.java
│       │   │   │   ├── OutboundEndpointService.java
│       │   │   │   ├── AclService.java
│       │   │   │   ├── AuthConfigService.java
│       │   │   │   ├── AlertConfigService.java
│       │   │   │   ├── LogQueryService.java
│       │   │   │   ├── DashboardService.java
│       │   │   │   ├── AlertService.java
│       │   │   │   ├── AnomalyMlService.java       # Isolation Forest batch job
│       │   │   │   └── ConfigSyncService.java      # SQL → Redis sync
│       │   │   ├── entity/                  # JPA entities (source of truth)
│       │   │   │   ├── ServiceEntity.java
│       │   │   │   ├── ClientEntity.java
│       │   │   │   ├── AuthConfigEntity.java
│       │   │   │   ├── InboundEndpointEntity.java
│       │   │   │   ├── OutboundEndpointEntity.java
│       │   │   │   ├── InboundAccessRuleEntity.java
│       │   │   │   └── AlertConfigEntity.java
│       │   │   ├── repository/              # Spring Data JPA repositories
│       │   │   │   ├── ServiceRepository.java
│       │   │   │   ├── ClientRepository.java
│       │   │   │   ├── AuthConfigRepository.java
│       │   │   │   ├── InboundEndpointRepository.java
│       │   │   │   ├── OutboundEndpointRepository.java
│       │   │   │   ├── InboundAccessRuleRepository.java
│       │   │   │   └── AlertConfigRepository.java
│       │   │   ├── model/                  # DTOs (không phải JPA)
│       │   │   │   ├── AnomalyAlert.java
│       │   │   │   └── dto/
│       │   │   │       ├── ClientRequest.java
│       │   │   │       ├── ClientResponse.java
│       │   │   │       ├── LogQueryRequest.java
│       │   │   │       ├── DashboardStats.java
│       │   │   │       ├── AclRequest.java
│       │   │   │       ├── EndpointRequest.java
│       │   │   │       └── AlertResponse.java
│       │   │   ├── config/                 # Spring config
│       │   │   │   ├── PostgresConfig.java
│       │   │   │   ├── RedisConfig.java
│       │   │   │   ├── ElasticsearchConfig.java
│       │   │   │   ├── SecurityConfig.java        # Spring Security (form login)
│       │   │   │   └── SchedulingConfig.java      # Cron job cho ML + sync
│       │   │   └── security/
│       │   │       └── DashboardUserDetails.java
│       │   ├── resources/
│       │   │   ├── application.yml
│       │   │   ├── db/migration/            # Flyway migration scripts
│       │   │   │   ├── V1__init_schema.sql
│       │   │   │   └── V2__seed_data.sql
│       │   │   └── static/                 # React build output (copy vào đây)
│       │   └── frontend/                   # React source code
│       │       ├── package.json
│       │       ├── src/
│       │       │   ├── App.jsx
│       │       │   ├── pages/
│       │       │   │   ├── Dashboard.jsx
│       │       │   │   ├── ClientManagement.jsx
│       │       │   │   ├── ServiceManagement.jsx
│       │       │   │   ├── EndpointManagement.jsx
│       │       │   │   ├── AuthConfigPage.jsx
│       │       │   │   ├── AlertConfigPage.jsx
│       │       │   │   ├── LogViewer.jsx
│       │       │   │   ├── Alerts.jsx
│       │       │   │   └── KibanaEmbed.jsx
│       │       │   ├── components/
│       │       │   │   ├── charts/         # Recharts / Chart.js components
│       │       │   │   ├── tables/
│       │       │   │   └── common/
│       │       │   └── services/
│       │       │       └── api.js          # Axios API client
│       │       └── public/
│       └── test/
│
├── vdt-share-lib/                          # Module 2: Shared Library
│   ├── vdt-share-lib-core/                 # Sub-module 2a: Core
│   │   ├── pom.xml
│   │   └── src/main/java/com/vdt/share/
│   │       ├── annotation/
│   │       │   ├── SharedApi.java          # @SharedApi -- đánh dấu API outbound
│   │       │   ├── ClientCall.java         # @ClientCall -- đánh dấu gọi client inbound
│   │       │   └── SharedResource.java     # @SharedResource -- đánh dấu resource
│   │       ├── model/
│   │       │   ├── ShareLog.java           # Log entity ghi vào ES
│   │       │   ├── ClientInfo.java         # Thông tin client (đọc từ Redis)
│   │       │   └── RequestContext.java     # Context cho mỗi request
│   │       ├── enums/
│   │       │   ├── Direction.java          # INBOUND / OUTBOUND
│   │       │   ├── ChannelType.java        # REST / KAFKA
│   │       │   └── RequestStatus.java      # SUCCESS / FAILED / RETRY
│   │       └── exception/
│   │           ├── ClientBlockedException.java
│   │           └── RetryExhaustedException.java
│   │
│   ├── vdt-share-lib-autoconfigure/        # Sub-module 2b: Auto-configuration
│   │   ├── pom.xml
│   │   └── src/main/java/com/vdt/share/autoconfigure/
│   │       ├── VdtShareAutoConfiguration.java
│   │       ├── VdtShareProperties.java
│   │       ├── aspect/
│   │       │   ├── SharedApiAspect.java    # Intercept @SharedApi
│   │       │   └── ClientCallAspect.java   # Intercept @ClientCall
│   │       ├── service/
│   │       │   ├── ElasticsearchLogService.java
│   │       │   ├── KafkaShareService.java
│   │       │   ├── RedisClientCacheService.java
│   │       │   ├── RetryService.java
│   │       │   └── RuleBasedAnomalyService.java  # Rule-based anomaly (tầng 1)
│   │       ├── security/
│   │       │   ├── ApiKeyAuthenticator.java       # API Key auth (chỉ API Key)
│   │       │   └── WhitelistBlacklistFilter.java
│   │       └── config/
│   │           ├── KafkaConfig.java
│   │           ├── ElasticsearchConfig.java
│   │           └── RedisConfig.java
│   │
│   └── vdt-share-lib-starter/              # Sub-module 2c: Starter POM
│       └── pom.xml                         # Chỉ gom dependencies
│
├── vdt-demo/                               # Module 3: Demo microservice (phân tích sau)
│   └── (TBD)
│
├── docker-compose.yml                      # PostgreSQL + Elasticsearch + Kibana + Kafka + Redis
├── pom.xml                                 # Parent POM
└── README.md
```

### 6.2 Luồng giao tiếp giữa các module

```
┌──────────────────────────────────────────────────────────────────────────┐
│                         MONOREPO                                         │
│                                                                          │
│  ┌──────────────────────────┐         ┌─────────────────────────────┐   │
│  │  Management System       │         │  Shared Lib                 │   │
│  │  (Spring Boot + React)   │         │  (Spring Boot Starter)      │   │
│  │                          │         │                             │   │
│  │  GHI/ĐỌC → PostgreSQL   │         │  (không chạm SQL)           │   │
│  │  (source of truth:       │         │                             │   │
│  │   service, client,       │         │  ĐỌC ← Redis              │   │
│  │   endpoint, auth, acl,   │         │  (client info, apikey,     │   │
│  │   alert config)          │         │   acl, config, rate limit) │   │
│  │                          │         │                             │   │
│  │  SYNC → Redis            │         │  GHI → Elasticsearch       │   │
│  │  (cache từ SQL)          │         │  (request/response logs,   │   │
│  │                          │         │   anomaly alerts)          │   │
│  │  ĐỌC ← Elasticsearch    │         │                             │   │
│  │  (logs, anomaly,         │         │  Rule-based realtime →     │   │
│  │   dashboard stats)       │         │  Counter + Threshold       │   │
│  │                          │         └─────────────────────────────┘   │
│  │  ML batch job →          │                                          │
│  │  Isolation Forest        │                                          │
│  └──────────────────────────┘                                          │
│              │                   │              │                       │
│     ┌────────┘                   │              └────────┐              │
│     ▼                           ▼                       ▼              │
│  ┌──────────┐          ┌──────────────┐       ┌──────────────────┐     │
│  │PostgreSQL│          │    Redis     │       │ Elasticsearch    │     │
│  │source of │          │cache + rate  │       │ logs + anomaly   │     │
│  │truth     │          │limit counter │       │                  │     │
│  └──────────┘          └──────────────┘       └──────────────────┘     │
│                                                                          │
│  ┌──────────────────────────┐                                            │
│  │  Demo App                │  (dùng vdt-share-lib-starter)              │
│  │  → test end-to-end       │                                            │
│  └──────────────────────────┘                                            │
└──────────────────────────────────────────────────────────────────────────┘
```

**Nguyên tắc giao tiếp:**
- Management System và Shared Lib **KHÔNG** gọi API trực tiếp lẫn nhau
- Chia sẻ qua **Redis** (Management System GHI config, Lib ĐỌC config)
- Chia sẻ qua **Elasticsearch** (Lib GHI log, Management System ĐỌC log)
- Contract giữa 2 module = Redis key format + ES index schema (xem mục 10)

---

## 7. Module 1: Management System (chi tiết)

### 7.1 Backend API

> **Storage note:** Tất cả CRUD đều ghi vào PostgreSQL (source of truth). Sau mỗi lần ghi, `ConfigSyncService` đồng bộ lên Redis cache để Shared Lib đọc nhanh (xem 7.3).

| Nhóm | Method | Endpoint | Mô tả | Storage |
|---|---|---|---|---|
| **Service** | POST | `/api/services` | Tạo service mới | PostgreSQL `service` |
| | GET | `/api/services` | Danh sách service | PostgreSQL |
| | GET | `/api/services/{id}` | Chi tiết service | PostgreSQL |
| | PUT | `/api/services/{id}` | Cập nhật service | PostgreSQL |
| | DELETE | `/api/services/{id}` | Xóa service | PostgreSQL |
| **Client** | POST | `/api/clients` | Tạo client mới | PostgreSQL `client` |
| | GET | `/api/clients` | Danh sách client | PostgreSQL |
| | GET | `/api/clients/{id}` | Chi tiết client | PostgreSQL |
| | PUT | `/api/clients/{id}` | Cập nhật client | PostgreSQL |
| | DELETE | `/api/clients/{id}` | Xóa client (soft delete) | PostgreSQL |
| **Inbound Endpoint** | POST | `/api/services/{id}/inbound-endpoints` | Tạo inbound endpoint | PostgreSQL `inbound_endpoint` |
| | GET | `/api/services/{id}/inbound-endpoints` | Danh sách endpoint | PostgreSQL |
| | GET | `/api/inbound-endpoints/{id}` | Chi tiết endpoint | PostgreSQL |
| | PUT | `/api/inbound-endpoints/{id}` | Cập nhật endpoint | PostgreSQL |
| | PUT | `/api/inbound-endpoints/{id}/toggle` | Bật/tắt endpoint | PostgreSQL |
| **Outbound Endpoint** | POST | `/api/services/{id}/outbound-endpoints` | Tạo outbound endpoint | PostgreSQL `outbound_endpoint` |
| | GET | `/api/services/{id}/outbound-endpoints` | Danh sách endpoint | PostgreSQL |
| | GET | `/api/outbound-endpoints/{id}` | Chi tiết endpoint | PostgreSQL |
| | PUT | `/api/outbound-endpoints/{id}` | Cập nhật endpoint | PostgreSQL |
| **Auth Config** | POST | `/api/auth-configs` | Tạo auth config (JWT/API Key/HMAC) | PostgreSQL `auth_config` |
| | GET | `/api/auth-configs` | Danh sách auth config | PostgreSQL |
| | GET | `/api/auth-configs/{id}` | Chi tiết auth config | PostgreSQL |
| | PUT | `/api/auth-configs/{id}` | Cập nhật auth config | PostgreSQL |
| | DELETE | `/api/auth-configs/{id}` | Xóa auth config | PostgreSQL |
| **API Key** | POST | `/api/clients/{id}/keys` | Tạo API Key cho client | PostgreSQL `auth_config` |
| | GET | `/api/clients/{id}/keys` | Danh sách key của client | PostgreSQL |
| | DELETE | `/api/keys/{keyId}` | Thu hồi API Key | PostgreSQL |
| **ACL** | POST | `/api/inbound-endpoints/{id}/access-rules` | Thêm access rule (whitelist/blacklist) | PostgreSQL `inbound_access_rule` |
| | GET | `/api/inbound-endpoints/{id}/access-rules` | Danh sách access rules | PostgreSQL |
| | DELETE | `/api/access-rules/{id}` | Xóa access rule | PostgreSQL |
| **Alert Config** | POST | `/api/alert-configs` | Tạo alert config | PostgreSQL `alert_config` |
| | GET | `/api/alert-configs` | Danh sách alert config | PostgreSQL |
| | GET | `/api/alert-configs/{id}` | Chi tiết alert config | PostgreSQL |
| | PUT | `/api/alert-configs/{id}` | Cập nhật alert config (channels, severity) | PostgreSQL |
| **Logs** | GET | `/api/logs` | Query log (filter by client, endpoint, direction, status, time) | ES `vdt-share-logs-*` |
| | GET | `/api/logs/export` | Export logs (CSV/JSON) | ES |
| **Dashboard** | GET | `/api/dashboard/stats` | Tổng quan: request count, error rate, top clients | ES aggregation |
| | GET | `/api/dashboard/timeline` | Time-series data cho biểu đồ | ES date histogram |
| **Alerts** | GET | `/api/alerts` | Danh sách cảnh báo anomaly | ES `vdt-anomaly-*` |
| | PUT | `/api/alerts/{id}/acknowledge` | Đánh dấu đã xem | ES update |
| **Sync** | POST | `/api/admin/sync` | Force sync SQL → Redis | Sync all |
| | POST | `/api/admin/sync/{entity}/{id}` | Sync 1 entity lên Redis | Sync single |

### 7.2 Anomaly ML Batch Job (chạy trong Management System)

```
Cron job (mỗi 15 phút, configurable)
    │
    ├─ 1. Query ES: lấy log 1 giờ gần nhất, group by clientId
    │
    ├─ 2. Tính features cho mỗi client:
    │      - avg_response_time
    │      - error_rate (failed / total)
    │      - request_count
    │      - avg_body_size
    │      - hour_of_day
    │
    ├─ 3. Chạy Isolation Forest (Tribuo hoặc Smile)
    │      - contamination = 0.1 (10% outlier)
    │      - n_estimators = 100
    │
    ├─ 4. Với mỗi anomaly detected:
    │      - Ghi vào ES index vdt-anomaly-YYYY.MM.dd
    │      - Gửi cảnh báo (nếu config: email/SMS/webhook)
    │
    └─ 5. Log kết quả batch job
```

### 7.3 Database Schema (PostgreSQL)

Dựa trên `db.txt`, source of truth cho tất cả config. Shared Lib KHÔNG chạm SQL, chỉ đọc từ Redis cache.

```
Table service {
  id bigint [pk, increment]
  name varchar(100) [unique, not null]
  description text
  base_url varchar(255)
  owner_team varchar(100)
  status varchar(20) [default: 'ACTIVE', note: 'ACTIVE / INACTIVE / DEPRECATED']
  created_at timestamp
  updated_at timestamp
}

Table client {
  id bigint [pk, increment]
  name varchar(100) [not null]
  client_key varchar(255) [unique, not null]
  contact_email varchar(255)
  status varchar(20) [default: 'ACTIVE', note: 'ACTIVE / SUSPENDED / REVOKED']
  created_at timestamp
  updated_at timestamp
}

Table auth_config {
  id bigint [pk, increment]
  client_id bigint [ref: > client.id, not null]
  inbound_endpoint_id bigint [ref: > inbound_endpoint.id, not null]
  type varchar(20) [not null, note: 'JWT / API_KEY / HMAC_SIGNATURE']
  secret_ref varchar(255)
  public_key text
  algorithm varchar(50)
  expires_at timestamp
}

Table inbound_endpoint {
  id bigint [pk, increment]
  service_id bigint [ref: > service.id, not null]
  name varchar(100)
  path varchar(255)
  method varchar(10)
  protocol varchar(20) [not null, note: 'HTTP / MQ / WEBHOOK']
  rate_limit int
  rate_limit_window_seconds int [default: 60]
  request_size_limit_kb int
  response_size_limit_kb int
  response_time_threshold_ms int
  timeout_ms int [default: 30000]
  log_retention_days int [default: 30]
  alert_config_id bigint [ref: > alert_config.id]
  enabled boolean [default: true]
}

Table outbound_endpoint {
  id bigint [pk, increment]
  service_id bigint [ref: > service.id, not null]
  name varchar(100)
  target_url varchar(255)
  method varchar(10)
  protocol varchar(20) [not null, note: 'HTTP / MQ / WEBHOOK']
  response_time_threshold_ms int
  timeout_ms int [default: 30000]
  retry_count int [default: 3]
  retry_backoff_ms int [default: 1000]
  rollback_strategy varchar(50) [note: 'COMPENSATE / IGNORE / MANUAL']
  log_retention_days int [default: 30]
  alert_config_id bigint [ref: > alert_config.id]
  enabled boolean [default: true]
}

Table inbound_access_rule {
  id bigint [pk, increment]
  inbound_endpoint_id bigint [ref: > inbound_endpoint.id, not null]
  type varchar(20) [not null, note: 'WHITELIST / BLACKLIST']
  value_type varchar(20) [not null, note: 'IP / CIDR / CLIENT_ID / HEADER']
  value varchar(255) [not null]
  temporary boolean [default: false]
  expires_at timestamp
  created_by varchar(100)
  created_at timestamp
}

Table alert_config {
  id bigint [pk, increment]
  name varchar(100)
  channels json [not null, note: '[{"type":"EMAIL","target":"..."},{"type":"SMS","target":"..."}]']
  severity varchar(20) [default: 'WARNING', note: 'INFO / WARNING / CRITICAL']
  throttle_minutes int [default: 5]
  enabled boolean [default: true]
}
```

### 7.4 Sync Mechanism: PostgreSQL → Redis

**Nguyên tắc:** PostgreSQL là source of truth. Redis chỉ là cache để Shared Lib lookup nhanh (O(1)).

**Flow sync:**

```
Service ghi dữ liệu vào PostgreSQL
    │
    ├─ Nếu thành công → gọi ConfigSyncService.sync(entity, id)
    │                      │
    │                      ├─ Đọc entity mới từ PostgreSQL
    │                      ├─ Map entity → Redis hash format
    │                      ├─ GHI vào Redis (SET/HSET)
    │                      └─ Nếu xóa → DEL Redis key
    │
    └─ Nếu thất bại → rollback, không sync Redis
```

**Sync triggers:**

| Trigger | Cơ chế | Độ trễ |
|---|---|---|
| **Inline sync** | Gọi `ConfigSyncService` ngay sau khi service ghi SQL thành công | ~5ms |
| **Cron sync (full)** | Chạy mỗi 5 phút, đọc toàn bộ SQL, so sánh với Redis, đồng bộ nếu lệch | ~30s |
| **Admin manual sync** | API `POST /api/admin/sync` cho admin force sync | Theo request |
| **Startup sync** | Khi Management System khởi động, sync toàn bộ SQL lên Redis | ~10s |

**Redis key format (sau sync):**

```
# Client info → vdt:client:{client.id}  (Hash)
vdt:client:1 → { name, clientKey, status, ... }

# API Key → vdt:apikey:{auth_config.secret_ref}  (Hash)
vdt:apikey:sk_abc123 → { clientId: 1, type: "API_KEY", ... }

# Inbound endpoint config → vdt:endpoint:inbound:{id}  (Hash)
vdt:endpoint:inbound:5 → { rateLimit, timeout, ... }

# ACL → vdt:acl:{inbound_endpoint_id}:{value_type}:{value}  (String)
vdt:acl:5:CLIENT_ID:partner-A → "WHITELIST" | "BLACKLIST"

# Alert config → vdt:alert:config:{id}  (Hash)
vdt:alert:config:2 → { channels, severity, throttleMinutes }
```

**Implementation trong Management System:**

```java
@Service
public class ConfigSyncService {

    public void syncClient(Long clientId) {
        ClientEntity client = clientRepo.findById(clientId).orElseThrow();
        redisTemplate.opsForHash().putAll(
            "vdt:client:" + client.getId(), Map.of(
                "name", client.getName(),
                "clientKey", client.getClientKey(),
                "status", client.getStatus()
            ));
    }

    public void syncAll() {
        clientRepo.findAll().forEach(c -> syncClient(c.getId()));
        // sync endpoints, access rules, auth configs...
    }
}
```

### 7.5 Frontend (ReactJS)

| Trang | URL | Mô tả |
|---|---|---|---|
| **Dashboard** | `/` | Biểu đồ tổng quan: request count over time (line chart), error rate (pie chart), top 10 clients (bar chart), average response time (gauge). Dùng Recharts hoặc Chart.js |
| **Service Management** | `/services` | CRUD service, xem danh sách microservice nội bộ |
| **Client Management** | `/clients` | CRUD client, cấp API Key, xem status |
| **Client Detail** | `/clients/:id` | Chi tiết 1 client: auth configs, endpoints được phép, log gần nhất |
| **Inbound Endpoints** | `/services/:id/inbound-endpoints` | CRUD inbound endpoint, cấu hình rate limit, timeout, ACL |
| **Outbound Endpoints** | `/services/:id/outbound-endpoints` | CRUD outbound endpoint, cấu hình retry, rollback |
| **Auth Config** | `/auth-configs` | Quản lý auth config (JWT/API Key/HMAC) cho từng cặp (client, endpoint) |
| **Alert Config** | `/alert-configs` | Cấu hình kênh cảnh báo (email/SMS/webhook), severity threshold |
| **Log Viewer** | `/logs` | Bảng log có filter (client, endpoint, direction, status, time range), search, pagination |
| **Alerts** | `/alerts` | Danh sách cảnh báo anomaly, acknowledge, filter by severity |
| **Kibana Embed** | `/kibana` | iframe nhúng Kibana dashboard (URL configurable) |

**Build & Deploy:** Dùng `maven-frontend-plugin` để build React app trong Maven lifecycle:
1. `mvn generate-resources` → chạy `npm install` + `npm run build`
2. Copy build output vào `src/main/resources/static/`
3. Spring Boot serve static files + API trong cùng 1 jar

**Auth cho Dashboard:** Spring Security form login (username/password lưu trong PostgreSQL). Đủ cho POC, không cần Keycloak.

---

## 8. Module 2: Shared Library (chi tiết)

### 8.1 Annotations

```java
// === OUTBOUND: Đánh dấu API expose ra ngoài cho client gọi vào ===
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface SharedApi {
    String name() default "";              // Tên resource (vd: "orders")
    ChannelType channel() default REST;    // REST hoặc KAFKA
    boolean authenticate() default true;   // Có yêu cầu auth không
    String description() default "";       // Mô tả API
}

// === INBOUND: Đánh dấu method gọi tới client bên ngoài ===
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface ClientCall {
    String clientId() default "";          // ID client đích
    ChannelType channel() default REST;    // REST hoặc KAFKA
    int maxRetry() default 3;             // Số lần retry tối đa
    long retryDelay() default 1000;       // Delay giữa các lần retry (ms)
    boolean async() default true;          // Xử lý bất đồng bộ
    String fallbackMethod() default "";    // Method fallback khi fail
}
```

**Thay đổi so với brainstorm v1:**
- Bỏ `rateLimit` khỏi `@SharedApi` annotation → rate limit đọc từ Redis config (do Management System quản lý), không hardcode trong annotation. Đây là sự khác biệt quan trọng: config thay đổi runtime qua Dashboard mà không cần redeploy microservice.

### 8.2 AOP Flow: `@SharedApi` (outbound)

```
Request đến method có @SharedApi
    │
    ├─ 1. Extract metadata từ annotation (name, channel, authenticate)
    ├─ 2. Lấy clientId + apiKey từ request header
    │      (X-Client-Id, X-Client-Secret)
    │
    ├─ 3. Check whitelist/blacklist (Redis: vdt:acl:{inboundEndpointId}:CLIENT_ID:{clientId})
    │      → Blacklist: 403 Forbidden + async log ES
    │      → Whitelist: skip rate limit check
    │
    ├─ 4. Authenticate (nếu authenticate=true)
    │      → Lookup Redis: vdt:apikey:{apiKey}
    │      → Verify clientId khớp, key chưa hết hạn
    │      → Fail: 401 Unauthorized + async log ES
    │
    ├─ 5. Check rate limit (nếu không phải whitelist)
    │      → Đọc config từ Redis: vdt:endpoint:inbound:{endpointId} → rateLimit, rateLimitWindowSeconds
    │      → INCR Redis: vdt:rate:{clientId}:{endpointId}:{minute}
    │      → Over limit: 429 Too Many Requests + async log ES
    │
    ├─ 6. proceed() — chạy method gốc
    │
    ├─ 7. Rule-based anomaly check
    │      → Slow response? (duration > threshold từ config)
    │      → Body size violation? (response body > maxBodySize từ config)
    │
    ├─ 8. Async ghi log vào Elasticsearch
    │      (request, response, duration, status, clientId, traceId)
    │
    └─ 9. Nếu exception → catch, log error vào ES, trả response lỗi chuẩn
```

### 8.3 AOP Flow: `@ClientCall` (inbound)

```
Method có @ClientCall được gọi
    │
    ├─ 1. Extract metadata từ annotation (clientId, channel, maxRetry...)
    │
    ├─ 2. Resolve client endpoint
    │      → Đọc từ Redis: vdt:client:{clientId} → endpoint URL
    │
    ├─ 3. Nếu async=true → submit vào @Async thread pool
    │
    ├─ 4. proceed() — thực hiện gọi client
    │
    ├─ 5. Nếu fail → retry (exponential backoff)
    │      Lần 1: chờ retryDelay
    │      Lần 2: chờ retryDelay × 2
    │      Lần N: chờ retryDelay × 2^(N-1)
    │
    ├─ 6. Nếu retry hết:
    │      → Gọi fallbackMethod (nếu có)
    │      → INCR Redis: vdt:fail:{clientId} (consecutive fail counter)
    │      → Nếu consecutive fail > threshold → ghi anomaly alert vào ES
    │
    ├─ 7. Nếu success:
    │      → Reset Redis: DEL vdt:fail:{clientId}
    │
    └─ 8. Async ghi log vào Elasticsearch
           (request, response, duration, status, retryCount)

### 8.4 Retry & Rollback

**Retry (exponential backoff):**
- `@ClientCall` mặc định retry 3 lần với delay: 1s → 2s → 4s
- Số lần retry và delay configurable qua Redis `vdt:endpoint:outbound:{endpointId}` (Management System quản lý, sync từ SQL outbound_endpoint)
- Dùng Spring `RetryTemplate` hoặc custom loop trong AOP aspect
- Retry chỉ áp dụng với chiều inbound (`@ClientCall`), không retry chiều outbound (`@SharedApi`)

**Rollback:**
- Rollback áp dụng khi có nhiều `@ClientCall` trong 1 business transaction (Saga pattern)
- Mỗi `@ClientCall` hỗ trợ thêm thuộc tính `rollbackMethod` (tương tự `fallbackMethod`)
- Nếu `@ClientCall` A fail sau khi `@ClientCall` B đã thành công → gọi `rollbackMethod` của B để hoàn tác
- Rollback không đồng bộ (chạy trong @Async thread pool riêng với priority queue)

```java
// Saga pattern: createOrder → deductInventory → chargePayment
@ClientCall(clientId = "inventory-service", maxRetry = 2,
            rollbackMethod = "restoreInventory")
public void deductInventory(Order order) { ... }

@ClientCall(clientId = "payment-service", maxRetry = 2,
            rollbackMethod = "refundPayment")
public void chargePayment(Order order) { ... }
```

**Cơ chế rollback chi tiết:**
1. Khi `@ClientCall` fail sau khi retry hết, kiểm tra xem có `rollbackMethod` không
2. Nếu có → gọi `rollbackMethod` bất đồng bộ
3. `rollbackMethod` được đánh dấu riêng để tránh vòng lặp (không bị AOP intercept lại)
4. Ghi log rollback vào ES với status=ROLLBACK và traceId gốc
5. Nếu rollback cũng fail → ghi log CRITICAL vào ES, gửi cảnh báo admin

**Giới hạn:** Đây là rollback ở application level, không phải distributed transaction. Phù hợp POC. Production nên dùng Saga orchestrator riêng (Camunda, Temporal).

### 8.5 Elasticsearch Log Service

**Log document structure (ES index: `vdt-share-logs-YYYY.MM.dd`):**

```json
{
  "id": "uuid",
  "timestamp": "2026-05-26T10:30:00Z",
  "direction": "OUTBOUND | INBOUND",
  "channel": "REST | KAFKA",
  "resourceName": "get-orders",
  "clientId": "partner-A",
  "clientIp": "192.168.1.100",
  "method": "GET",
  "endpoint": "/api/orders",
  "requestHeaders": { "...": "..." },
  "requestBody": "...",
  "responseBody": "...",
  "requestBodySize": 0,
  "responseBodySize": 1024,
  "httpStatus": 200,
  "status": "SUCCESS | FAILED | RETRY",
  "duration": 150,
  "retryCount": 0,
  "errorMessage": null,
  "serviceName": "order-service",
  "traceId": "abc-123"
}
```

**Cơ chế ghi async:**
- Dùng `@Async` + `ThreadPoolTaskExecutor` (configurable pool size)
- Buffer logs trong queue nội bộ, flush theo batch (mỗi 5s hoặc đạt 100 records)
- Nếu ES down → fallback ghi vào local file log (SLF4J), retry gửi ES sau

**ES Index Lifecycle (thời gian lưu log):**
SRS yêu cầu cấu hình thời gian lưu log. Dùng ES Index Lifecycle Management (ILM):

| Chỉ mục | Chính sách | Mặc định |
|---|---|---|
| `vdt-share-logs-YYYY.MM.dd` | `hot` 7 ngày → `warm` 30 ngày → `cold` 90 ngày → `delete` sau 365 ngày | 90 ngày |
| `vdt-anomaly-YYYY.MM.dd` | `hot` 30 ngày → `delete` sau 365 ngày | 365 ngày |

Cấu hình qua Management System: mỗi client có thể có `logRetentionDays` riêng trong `vdt:config:{clientId}`, override mặc định.

**Anomaly alert document (ES index: `vdt-anomaly-YYYY.MM.dd`):**

```json
{
  "id": "uuid",
  "timestamp": "2026-05-26T10:35:00Z",
  "type": "CONSECUTIVE_FAIL | RATE_SPIKE | SLOW_RESPONSE | BODY_SIZE | ML_ANOMALY",
  "severity": "WARNING | CRITICAL",
  "clientId": "partner-A",
  "description": "Client partner-A failed 5 consecutive times",
  "details": { "failCount": 5, "threshold": 5 },
  "source": "RULE_BASED | ML_ISOLATION_FOREST",
  "acknowledged": false,
  "serviceName": "order-service"
}
```

### 8.6 Kafka Share Service

| Mode | Trigger | Hành vi |
|---|---|---|
| **Producer** | `@SharedApi(channel=KAFKA)` | Serialize payload (Jackson JSON), produce vào topic `vdt.share.outbound.{resourceName}` |
| **Consumer** | `@ClientCall(channel=KAFKA)` | Consume từ topic `vdt.share.inbound.{resourceName}`, deserialize, gọi handler |

**Serialization:** JSON (Jackson) mặc định.

### 8.7 Redis Client Cache Service

| Mục đích | Key pattern | TTL | Ghi bởi | Đọc bởi |
|---|---|---|---|---|
| Client info | `vdt:client:{clientId}` | Không hết hạn | Management System | Lib |
| API Keys | `vdt:apikey:{apiKeyValue}` | Configurable | Management System | Lib |
| Whitelist/Blacklist | `vdt:acl:{clientId}` | Permanent hoặc TTL (temp blacklist) | Management System | Lib |
| Client config | `vdt:config:{clientId}` | Không hết hạn | Management System | Lib |
| Rate limiting | `vdt:rate:{clientId}:{resource}:{minute}` | 60s | Lib (INCR) | Lib |
| Consecutive fail | `vdt:fail:{clientId}` | 1 giờ | Lib (INCR) | Lib |

### 8.8 Auto-Configuration

```java
@Configuration
@ConditionalOnProperty(prefix = "vdt.share", name = "enabled", havingValue = "true")
@EnableConfigurationProperties(VdtShareProperties.class)
@EnableAsync
public class VdtShareAutoConfiguration {
    // Tự động đăng ký:
    // - SharedApiAspect, ClientCallAspect
    // - ElasticsearchLogService
    // - KafkaShareService (conditional on kafka config)
    // - RedisClientCacheService
    // - RetryService
    // - RuleBasedAnomalyService
    // - ApiKeyAuthenticator
    // - WhitelistBlacklistFilter
    //
    // Mỗi bean có @ConditionalOnMissingBean → consumer override được
}
```

**META-INF/spring/org.springframework.boot.autoconfigure.AutoConfiguration.imports:**
```
com.vdt.share.autoconfigure.VdtShareAutoConfiguration
```
(Spring Boot 3.x dùng file imports thay cho spring.factories)

**Consumer config (application.yml):**

```yaml
vdt:
  share:
    enabled: true
    service-name: order-service

    elasticsearch:
      uris: http://localhost:9200
      index-prefix: vdt-share-logs
      batch-size: 100
      flush-interval: 5000

    kafka:
      bootstrap-servers: localhost:9092
      group-id: ${vdt.share.service-name}
      topic-prefix: vdt.share

    redis:
      host: localhost
      port: 6379
      key-prefix: vdt

    security:
      auth-type: API_KEY
      client-id-header: X-Client-Id
      client-secret-header: X-Client-Secret

    retry:
      max-attempts: 3
      initial-delay: 1000
      multiplier: 2.0

    anomaly:
      consecutive-fail-threshold: 5
      spike-multiplier: 3.0

    async:
      core-pool-size: 5
      max-pool-size: 20
      queue-capacity: 500
```

---

## 9. Ví dụ sử dụng Shared Lib trong Microservice

```java
@RestController
@RequestMapping("/api/orders")
public class OrderController {

    @Autowired
    private OrderService orderService;

    // Client bên ngoài gọi API này để lấy đơn hàng
    // AOP tự động: auth → whitelist/blacklist → rate limit → log ES
    @SharedApi(name = "get-orders")
    @GetMapping
    public List<Order> getOrders(
            @RequestHeader("X-Client-Id") String clientId) {
        return orderService.findByClient(clientId);
    }

    // Microservice gọi tới client để thông báo đơn hàng mới
    // AOP tự động: resolve endpoint → async → retry → log ES
    @ClientCall(clientId = "partner-A", maxRetry = 3, async = true,
                fallbackMethod = "notifyFallback")
    public void notifyNewOrder(Order order) {
        restTemplate.postForEntity(clientUrl, order, Void.class);
    }

    // Fallback khi notify thất bại sau tất cả retry
    public void notifyFallback(Order order, Exception ex) {
        log.error("Failed to notify partner-A for order {}: {}",
                  order.getId(), ex.getMessage());
        // Lưu vào dead letter queue hoặc retry sau
    }
}
```

---

## 10. Contract giữa Management System và Shared Lib

Hai module không có dependency trực tiếp. Chúng giao tiếp qua SQL (Management System), Redis (Management System GHI → Lib ĐỌC), và ES (Lib GHI → Management System ĐỌC).

### 10.1 PostgreSQL Schema (Source of Truth)

Dùng schema từ **db.txt** (xem mục 7.3). Đây là contract chính giữa 2 module:

| Entity | Table | Management System | Shared Lib |
|---|---|---|---|
| Service | `service` | CRUD | Không truy cập |
| Client | `client` | CRUD | Không truy cập |
| Auth Config | `auth_config` | CRUD | Không truy cập |
| Inbound Endpoint | `inbound_endpoint` | CRUD | Không truy cập |
| Outbound Endpoint | `outbound_endpoint` | CRUD | Không truy cập |
| Access Rule | `inbound_access_rule` | CRUD | Không truy cập |
| Alert Config | `alert_config` | CRUD | Không truy cập |

**Giải thích:** Shared Lib không bao giờ chạm vào PostgreSQL vì:
- Lib là .jar nhúng vào microservice bên ngoài, không nên phụ thuộc vào DB schema
- SQL connection overhead không phù hợp với hot path (mỗi request)
- Thay vào đó, Lib đọc từ Redis cache (xem 10.2)

### 10.2 Redis Key Format (Cache Contract)

Redis key format do `ConfigSyncService` (Management System) tạo ra. Shared Lib đọc theo các pattern cố định sau:

```
# Rate limit counter (String, auto-expire) - Lib INCR
vdt:rate:{clientId}:{inboundEndpointId}:{minuteTimestamp}
    → TTL: rate_limit_window_seconds (từ SQL inbound_endpoint)

# Consecutive fail counter (String, auto-expire) - Lib INCR
vdt:fail:{clientId}
    → TTL: 3600s

# Client info (Hash) - Management System SET, Lib GET
vdt:client:{clientId}
    → fields: id, name, clientKey, status, contactEmail

# API Key lookup (Hash) - Management System SET, Lib GET
vdt:apikey:{secretKey}
    → fields: clientId, type, expiresAt

# Endpoint config (Hash) - Management System SET, Lib GET
vdt:endpoint:inbound:{endpointId}
    → fields: rateLimit, rateLimitWindowSeconds, requestSizeLimitKb,
              responseSizeLimitKb, responseTimeThresholdMs, timeoutMs,
              logRetentionDays, enabled

# Access rule (String) - Management System SET, Lib GET
vdt:acl:{inboundEndpointId}:{valueType}:{value}
    → value: "WHITELIST" | "BLACKLIST"

# Alert config (Hash) - Management System SET, Lib GET
vdt:alert:config:{alertConfigId}
    → fields: channels (JSON), severity, throttleMinutes, enabled
```

**Sync đảm bảo:** Khi Management System ghi SQL, `ConfigSyncService` lập tức cập nhật Redis (xem 7.4). Độ trễ tối đa: 5 phút (cron sync fallback), thường ~5ms (inline sync).

### 10.3 ES Index Schema

```
# Log index: vdt-share-logs-YYYY.MM.dd
{
  "mappings": {
    "properties": {
      "id":                { "type": "keyword" },
      "timestamp":         { "type": "date" },
      "direction":         { "type": "keyword" },      // OUTBOUND | INBOUND
      "channel":           { "type": "keyword" },       // REST | KAFKA
      "resourceName":      { "type": "keyword" },
      "clientId":          { "type": "keyword" },
      "clientIp":          { "type": "ip" },
      "method":            { "type": "keyword" },
      "endpoint":          { "type": "keyword" },
      "requestBody":       { "type": "text", "index": false },
      "responseBody":      { "type": "text", "index": false },
      "requestBodySize":   { "type": "long" },
      "responseBodySize":  { "type": "long" },
      "httpStatus":        { "type": "integer" },
      "status":            { "type": "keyword" },       // SUCCESS | FAILED | RETRY
      "duration":          { "type": "long" },
      "retryCount":        { "type": "integer" },
      "errorMessage":      { "type": "text" },
      "serviceName":       { "type": "keyword" },
      "traceId":           { "type": "keyword" }
    }
  }
}

# Anomaly index: vdt-anomaly-YYYY.MM.dd
{
  "mappings": {
    "properties": {
      "id":                { "type": "keyword" },
      "timestamp":         { "type": "date" },
      "type":              { "type": "keyword" },
      "severity":          { "type": "keyword" },
      "clientId":          { "type": "keyword" },
      "description":       { "type": "text" },
      "details":           { "type": "object" },
      "source":            { "type": "keyword" },
      "acknowledged":      { "type": "boolean" },
      "serviceName":       { "type": "keyword" }
    }
  }
}
```

---

## 11. Tech Stack & Dependencies

### Management System

| Dependency | Version | Mục đích |
|---|---|---|
| Java | 17 (LTS) | Runtime |
| Spring Boot | 3.1.x | Framework |
| **Spring Data JPA** | **(từ Boot)** | **PostgreSQL ORM** |
| **Spring Data Redis** | **(từ Boot)** | **Redis operations** |
| **Spring Data Elasticsearch** | **(từ Boot)** | **ES operations** |
| **Spring Security** | **(từ Boot)** | **Dashboard auth (form login)** |
| **Spring Scheduling** | **(từ Boot)** | **Cron job cho ML batch + sync** |
| **PostgreSQL** | **15.x** | **Database** |
| **Flyway** | **9.x** | **DB migration** |
| Tribuo hoặc Smile | Latest | Isolation Forest ML |
| ReactJS | 18.x | Frontend |
| Recharts hoặc Chart.js | Latest | Biểu đồ |
| Axios | Latest | HTTP client (frontend) |
| React Router | 6.x | Routing (frontend) |
| maven-frontend-plugin | 1.15.x | Build React trong Maven |

### Shared Lib

| Dependency | Version | Module |
|---|---|---|
| Java | 17 (LTS) | All |
| Spring Boot | 3.1.x | Parent (provided scope) |
| Spring AOP | (từ Boot) | autoconfigure |
| Spring Kafka | (từ Boot) | autoconfigure |
| Spring Data Elasticsearch | (từ Boot) | autoconfigure |
| Spring Data Redis | (từ Boot) | autoconfigure |
| Spring Retry | 2.0.x | autoconfigure |
| Jackson | (từ Boot) | core |
| Lombok | 1.18.x | core |
| JUnit 5 + Mockito | (từ Boot) | test |

---

## 12. Docker Compose (Môi trường dev)

```yaml
# docker-compose.yml
services:
  postgres:
    image: postgres:15-alpine
    ports: ["5432:5432"]
    environment:
      POSTGRES_DB: vdt_share
      POSTGRES_USER: vdt
      POSTGRES_PASSWORD: vdt123
    volumes:
      - pgdata:/var/lib/postgresql/data

  elasticsearch:
    image: docker.elastic.co/elasticsearch/elasticsearch:8.11.0
    ports: ["9200:9200"]
    environment:
      - discovery.type=single-node
      - xpack.security.enabled=false

  kibana:
    image: docker.elastic.co/kibana/kibana:8.11.0
    ports: ["5601:5601"]
    depends_on: [elasticsearch]

  redis:
    image: redis:7-alpine
    ports: ["6379:6379"]

  zookeeper:
    image: confluentinc/cp-zookeeper:7.5.0
    ports: ["2181:2181"]
    environment:
      ZOOKEEPER_CLIENT_PORT: 2181
      ZOOKEEPER_TICK_TIME: 2000

  kafka:
    image: confluentinc/cp-kafka:7.5.0
    ports: ["9092:9092"]
    depends_on: [zookeeper]
    environment:
      KAFKA_BROKER_ID: 1
      KAFKA_ZOOKEEPER_CONNECT: zookeeper:2181
      KAFKA_ADVERTISED_LISTENERS: PLAINTEXT://localhost:9092
      KAFKA_OFFSETS_TOPIC_REPLICATION_FACTOR: 1

volumes:
  pgdata:
```

---

## 13. Phân pha triển khai

### Phase 1: Management System (dựng trước)

| # | Công việc | Mô tả | Phụ thuộc |
|---|---|---|---|---|
| 1 | Setup monorepo structure | Parent POM, module structure, .gitignore | Không |
| 2 | Docker Compose | PostgreSQL + ES + Kibana + Redis + Kafka | Không |
| 3 | Management Backend: project setup | Spring Boot app, PostgreSQL + Redis + ES config, Security | #1, #2 |
| 4 | Management Backend: DB schema (Flyway) | Viết migration V1__init_schema.sql (db.txt tables) | #3 |
| 5 | Management Backend: JPA entities + repos | Entity classes + Spring Data JPA repositories | #4 |
| 6 | Management Backend: Service CRUD API | REST API quản lý service | #5 |
| 7 | Management Backend: Client CRUD API | REST API quản lý client | #5 |
| 8 | Management Backend: Inbound Endpoint API | REST API quản lý inbound endpoint per service | #6 |
| 9 | Management Backend: Outbound Endpoint API | REST API quản lý outbound endpoint per service | #6 |
| 10 | Management Backend: Auth Config API | REST API quản lý auth config (JWT/API Key/HMAC) | #7, #8 |
| 11 | Management Backend: ACL API | REST API quản lý access rule (whitelist/blacklist) | #8 |
| 12 | Management Backend: Alert Config API | REST API quản lý alert config | #5 |
| 13 | Management Backend: ConfigSyncService | Sync SQL → Redis (inline + cron + startup) | #5 |
| 14 | Management Backend: Log query API | Query ES logs, filter, pagination, aggregation | #3 |
| 15 | Management Backend: Dashboard stats API | Aggregation cho biểu đồ | #14 |
| 16 | Management Backend: Anomaly ML batch job | Isolation Forest cron job | #14 |
| 17 | Management Backend: Alert API | CRUD cảnh báo anomaly từ ES | #16 |
| 18 | Management Frontend: setup | React app, routing, auth, Axios | #3 |
| 19 | Management Frontend: Dashboard page | Biểu đồ tổng quan (Recharts) | #15, #18 |
| 20 | Management Frontend: Service management page | CRUD service | #6, #18 |
| 21 | Management Frontend: Client management page | CRUD client, API Key, ACL | #7-#11, #18 |
| 22 | Management Frontend: Inbound Endpoint page | CRUD endpoint per service, config rate limit | #8, #18 |
| 23 | Management Frontend: Outbound Endpoint page | CRUD endpoint, config retry, rollback | #9, #18 |
| 24 | Management Frontend: Auth Config page | Quản lý auth config per (client, endpoint) | #10, #18 |
| 25 | Management Frontend: Alert Config page | Cấu hình kênh cảnh báo | #12, #18 |
| 26 | Management Frontend: Log viewer page | Bảng log có filter, search | #14, #18 |
| 27 | Management Frontend: Alert page | Danh sách cảnh báo, acknowledge | #17, #18 |
| 28 | Management Frontend: Kibana embed | iframe nhúng Kibana | #18 |

### Phase 2: Shared Library (dựng sau)

| # | Công việc | Mô tả | Phụ thuộc |
|---|---|---|---|
| 29 | vdt-share-lib-core: Annotations + Enums | `@SharedApi`, `@ClientCall`, enums | #1 |
| 30 | vdt-share-lib-core: Models + Exceptions | `ShareLog`, `ClientInfo`, exceptions | #1 |
| 31 | autoconfigure: Properties + AutoConfig | `VdtShareProperties`, auto-config class | #29, #30 |
| 32 | autoconfigure: Redis services | Redis cache, rate limit counter, ACL lookup | #31 |
| 33 | autoconfigure: Security (API Key auth) | `ApiKeyAuthenticator`, `WhitelistBlacklistFilter` | #32 |
| 34 | autoconfigure: ES log service | Async batch write, fallback | #31 |
| 35 | autoconfigure: AOP -- SharedApiAspect | Intercept @SharedApi full flow | #33, #34 |
| 36 | autoconfigure: Retry service | Exponential backoff, fallback | #31 |
| 37 | autoconfigure: AOP -- ClientCallAspect | Intercept @ClientCall full flow | #34, #36 |
| 38 | autoconfigure: Kafka service | Producer/Consumer cho channel=KAFKA | #31 |
| 39 | autoconfigure: Rule-based anomaly | Consecutive fail, spike, slow response | #32, #34 |
| 40 | vdt-share-lib-starter | Starter POM | #35, #37 |
| 41 | Testing | Unit + Integration test | #40 |

### Phase 3: Demo (phân tích sau)

| # | Công việc | Mô tả | Phụ thuộc |
|---|---|---|---|
| 42 | vdt-demo | Demo microservice dùng Lib, test end-to-end | #40 |

---

## 14. Rủi ro & Giảm thiểu

| Rủi ro | Giảm thiểu |
|---|---|
| Management System trước nhưng chưa có Lib → không có data thật trên Dashboard | Seed data vào ES bằng script để có dữ liệu giả. Dashboard hiển thị được, chờ Lib tạo data thật |
| Redis không phải persistent DB → mất dữ liệu khi restart | Bật Redis persistence (RDB/AOF). Cho POC, accept risk. Production cần backup strategy |
| AOP không intercept private method / self-invocation | Document rõ: chỉ dùng trên public method, gọi qua bean injection |
| ES/Kafka/Redis down trong lúc demo | Docker Compose đảm bảo infra. Lib có fallback log vào file khi ES down |
| Isolation Forest cần data để train | Ban đầu dùng rule-based only. ML bật sau khi có đủ data (>= 1000 records) |
| **SQL-Redis sync: dữ liệu không nhất quán nếu sync fail** | **Inline sync + Cron sync fallback. Nếu sync fail, Lib dùng data cũ trong Redis (eventual consistency). Không mất dữ liệu vì SQL là source of truth** |
| Spring Boot version conflict giữa Management System và Lib | Dùng cùng Spring Boot version trong parent POM. Lib dùng `provided` scope |
| Frontend build phức tạp trong Maven | `maven-frontend-plugin` đã mature, nhiều project dùng. Fallback: build npm riêng, copy static manually |
| Scope quá rộng cho đồ án | Ưu tiên Phase 1 (Management System) + Phase 2 core (AOP + ES log). Kafka, ML, advanced features làm nếu còn thời gian |

---

## 15. Critical Path (con đường nhanh nhất để demo được)

### Demo Management System (Phase 1):

```
#1 → #2 → #3 → #4 → #5 → #6 → #7 → #8 → #13 → #14 → #15 → #18 → #19 → #21
```

14 bước để có: PostgreSQL schema → CRUD service/client/endpoint → ConfigSyncService → Log ES → Dashboard + Service/Client pages.

### Demo Shared Lib (Phase 2):

```
#29 → #30 → #31 → #32 → #33 → #34 → #35 → #40 → #42
```

9 bước để có: Client gọi API → Auth → Rate limit → Proceed → Log ES → Xem trên Dashboard.

### Full demo end-to-end:

Management System quản lý service/client/endpoint/auth → ConfigSyncService đẩy lên Redis → Shared Lib đọc Redis, intercept request, ghi log ES → Dashboard hiển thị realtime từ ES.
