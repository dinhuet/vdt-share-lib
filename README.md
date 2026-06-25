# VDT Share Lib

VDT Share Lib la he thong quan ly va bao ve viec chia se API giua cac microservice. Du an gom mot thu vien Spring Boot dung de khai bao API/call runtime, mot he thong quan tri de cau hinh client/quyen/truy cap, va cac service demo de kiem thu luong HTTP/MQ.

## Muc tieu

- Tu dong dang ky API noi bo/thong diep MQ duoc danh dau bang annotation.
- Quan ly client, credential, quyen goi API va cau hinh gioi han truy cap.
- Bao ve runtime bang HMAC, rate limit, access policy, kich thuoc request/response va audit log.
- Ghi log bao mat len Kafka, xu ly metric/anomaly, va ho tro quan sat qua Elasticsearch/Logstash/Kibana.
- Cung cap giao dien quan tri React de van hanh he thong.

## Cau truc du an

```text
.
|-- shared-lib/             Thu vien Spring Boot auto-configuration
|-- management-system/
|   |-- be/                 Backend quan tri Spring Boot
|   `-- fe/                 Frontend quan tri React/Vite
|-- vdt-demo/               Microservice demo expose API va publish MQ
|-- vdt-client/             Client demo nhan/giai lap outbound call
|-- doc/                    Tai lieu thiet ke va cac phase anomaly/security
|-- spec/                   Dac ta bo sung
|-- plan/                   Ke hoach trien khai
`-- brainstorm/             Ghi chu y tuong
```

## Thanh phan chinh

### `shared-lib`

Thu vien duoc gan vao cac microservice can tham gia he sinh thai VDT Share Lib.

Chuc nang noi bat:

- `@SharedApi`: khai bao endpoint HTTP hoac topic MQ ma service expose ra ngoai.
- `@ClientCall`: khai bao call outbound tu service den service/client khac.
- Auto scan endpoint va publish registration event len Kafka.
- Runtime filter/interceptor cho HTTP va Kafka listener.
- Xac thuc client credential/HMAC, kiem tra permission, access policy va rate limit.
- Ghi audit log bao mat len local log va Kafka topic `security.logs`.

### `management-system/be`

Backend quan tri chay tai `http://localhost:8081`.

Chuc nang noi bat:

- Quan ly microservice da dang ky.
- Quan ly exposed API, client API, default config va access policy.
- Quan ly client, credential va permission.
- Dong bo cau hinh runtime xuong Redis de `shared-lib` doc khi xu ly request.
- Nhan registration event tu Kafka.
- Xu ly security log, metric, anomaly rule, baseline rule, alert, occurrence va notification workflow.

### `management-system/fe`

Frontend quan tri React/Vite. Giao dien su dung Keycloak tai `http://localhost:8080`, realm `vdt-shared-lib`, client `fe-app`.

Cac man hinh chinh:

- Dashboard.
- Microservices.
- Exposed APIs.
- Client APIs.
- Clients, credentials va permissions.
- Default configurations.
- Access policies.
- Security alerts va anomaly rules.

### `vdt-demo`

Service demo chay tai `http://localhost:8082`.

Service nay expose API mau nhu:

- `GET /api/orders`
- `POST /api/orders`
- `POST /api/orders/notify-client`
- `POST /api/orders/publish-client-mq`
- `POST /api/orders/publish-client-mq/fail`
- `POST /api/orders/publish-client-mq/timeout`
- `POST /api/orders/publish-client-mq/retry`

### `vdt-client`

Client demo chay tai `http://localhost:8083`, dung de nhan thong bao/outbound call va demo luong MQ.

Endpoint mau:

- `POST /api/client/outbound/orders`
- `DELETE /api/client/outbound/failures`

## Yeu cau moi truong

- Java 17.
- Docker va Docker Compose.
- Node.js phu hop voi Vite/React hien tai.
- PowerShell tren Windows hoac shell tuong duong.

Moi module Java da co Maven Wrapper, vi vay khong bat buoc cai Maven global.

## Khoi dong he thong local

### 1. Chay cac dich vu ha tang

```powershell
cd management-system\be
docker compose up -d
```

Compose se khoi dong:

- PostgreSQL: `localhost:5433`, database `shared-lib`, user `admin`, password `123456`
- Redis: `localhost:6379`
- Keycloak: `http://localhost:8080`
- Kafka: `localhost:9092`
- Elasticsearch: `http://localhost:9200`
- Kibana: `http://localhost:5601`
- Logstash

Keycloak import realm tu file:

```text
management-system/be/keycloak/realm-export.json
```

### 2. Build va cai `shared-lib` vao Maven local

```powershell
cd shared-lib
.\mvnw.cmd clean install
```

Buoc nay can thiet vi `vdt-demo` phu thuoc vao artifact `com.pm:shared-lib:0.0.1-SNAPSHOT`.

### 3. Chay backend quan tri

```powershell
cd management-system\be
.\mvnw.cmd spring-boot:run
```

Backend mac dinh chay o:

```text
http://localhost:8081
```

### 4. Chay frontend quan tri

```powershell
cd management-system\fe
npm install
npm run dev
```

Vite se hien thi URL tren terminal, thuong la:

```text
http://localhost:3000
```

Neu muon nhung Kibana dashboard vao trang Dashboard, tao file `.env` trong `management-system/fe`:

```env
VITE_KIBANA_DASHBOARD_URL=http://localhost:5601
```

Sau khi sua `.env`, can restart frontend.

### 5. Chay service demo

```powershell
cd vdt-demo
.\mvnw.cmd spring-boot:run
```

Service chay tai:

```text
http://localhost:8082
```

### 6. Chay client demo

```powershell
cd vdt-client
.\mvnw.cmd spring-boot:run
```

Client chay tai:

```text
http://localhost:8083
```

## Thu tu khoi dong de demo day du

1. `docker compose up -d` trong `management-system/be`.
2. `shared-lib`: `.\mvnw.cmd clean install`.
3. `management-system/be`: `.\mvnw.cmd spring-boot:run`.
4. `management-system/fe`: `npm install` roi `npm run dev`.
5. `vdt-demo`: `.\mvnw.cmd spring-boot:run`.
6. `vdt-client`: `.\mvnw.cmd spring-boot:run`.

Sau khi cac service len, `vdt-demo` se quet annotation va gui registration event. Backend quan tri nhan event, luu thong tin microservice/API, va dong bo cau hinh runtime qua Redis.

## Huong dan su dung giao dien quan tri

1. Mo frontend Vite.
2. Dang nhap bang Keycloak theo tai khoan da cau hinh trong realm import.
3. Vao `Microservices` de kiem tra service da dang ky.
4. Vao `Exposed APIs` de xem API/topic service expose, bat/tat API va cau hinh limit.
5. Vao `Clients` de tao client, tao credential va gan permission.
6. Vao `Access Policies` de tao rule cho exposed API.
7. Vao `Default Configs` de cau hinh gia tri mac dinh cho API moi.
8. Vao `Client APIs` de quan ly outbound call duoc khai bao bang `@ClientCall`.
9. Vao `Security Alerts` va `Anomaly Rules` de theo doi/cau hinh bat thuong bao mat.

## Tich hop `shared-lib` vao microservice

### 1. Them dependency

```xml
<dependency>
    <groupId>com.pm</groupId>
    <artifactId>shared-lib</artifactId>
    <version>0.0.1-SNAPSHOT</version>
</dependency>
```

### 2. Cau hinh ung dung

Vi du `application.yml`:

```yaml
spring:
  application:
    name: order-service
  kafka:
    bootstrap-servers: localhost:9092

vdt:
  share:
    enabled: true
    runtime:
      http-filter-enabled: true
      mq-interceptor-enabled: true
      fail-open: false
      hmac-enabled: true
      credential-encryption-key: vdt_encryption_k7Nq9xP2rT8mL4sZ6aBcDeFg
      hmac-max-clock-skew-seconds: 300
      nonce-key-prefix: vdt:hmac-nonce
```

`spring.application.name` la bat buoc khi `vdt.share.enabled=true`.

### 3. Khai bao exposed API

```java
@RestController
@RequestMapping("/api/orders")
public class OrderController {

    @SharedApi(name = "get-orders", path = "/api/orders", method = "GET")
    @GetMapping
    public List<String> getOrders() {
        return List.of("ORDER-001", "ORDER-002");
    }

    @SharedApi(name = "create-order", path = "/api/orders", method = "POST")
    @PostMapping
    public Map<String, Object> createOrder(@RequestBody Map<String, Object> request) {
        return Map.of("status", "CREATED", "request", request);
    }
}
```

### 4. Khai bao MQ exposed API

```java
@SharedApi(name = "create-order-mq", protocol = "MQ", topic = "demo.orders")
@KafkaListener(topics = "demo.orders")
public void handleOrder(String payload) {
    // Xu ly message
}
```

### 5. Khai bao outbound client call

```java
@ClientCall(name = "notify-partner", destinationUrl = "https://partner.com/webhook", method = "POST")
public void notifyPartner(Object payload) {
    // Goi he thong ben ngoai
}
```

Voi MQ:

```java
@ClientCall(name = "publish-vdt-client-order-mq", protocol = "MQ", topic = "vdt.client.orders")
public void publishOrder(String payload) {
    // Publish message
}
```

## Cau hinh runtime quan trong

| Key | Y nghia |
| --- | --- |
| `vdt.share.enabled` | Bat/tat auto configuration cua shared-lib |
| `vdt.share.runtime.http-filter-enabled` | Bat filter bao ve HTTP exposed API |
| `vdt.share.runtime.mq-interceptor-enabled` | Bat interceptor bao ve Kafka listener |
| `vdt.share.runtime.fail-open` | Cho phep request di tiep khi runtime config loi/thieu |
| `vdt.share.runtime.hmac-enabled` | Bat xac thuc HMAC |
| `vdt.share.runtime.credential-encryption-key` | Key dung de giai ma secret/credential |
| `vdt.share.audit.enabled` | Bat/tat audit log |
| `vdt.share.audit.kafka.enabled` | Gui audit log len Kafka |
| `vdt.share.audit.kafka.topic` | Topic audit log, mac dinh `security.logs` |

## Luong xu ly tong quat

1. Microservice gan `shared-lib` va khai bao `@SharedApi`/`@ClientCall`.
2. Khi service start, `EndpointScanner` quet endpoint.
3. `RegistrationService` gui registration event len Kafka.
4. Backend quan tri nhan event va luu microservice/API vao PostgreSQL.
5. Admin cau hinh client, credential, permission, access policy va limit tren UI.
6. Backend dong bo cau hinh runtime sang Redis.
7. Khi request HTTP/MQ di vao service, `shared-lib` doc Redis de kiem tra credential, HMAC, permission, policy va rate limit.
8. Ket qua xu ly duoc ghi audit log vao Kafka topic `security.logs`.
9. Logstash day log sang Elasticsearch/Kibana; backend anomaly xu ly metric, rule, baseline va alert.

## Chay test

Chay test tung module:

```powershell
cd shared-lib
.\mvnw.cmd test
```

```powershell
cd management-system\be
.\mvnw.cmd test
```

```powershell
cd vdt-demo
.\mvnw.cmd test
```

```powershell
cd vdt-client
.\mvnw.cmd test
```

Build frontend:

```powershell
cd management-system\fe
npm run build
```

## Troubleshooting

### Backend khong ket noi duoc PostgreSQL

Kiem tra container:

```powershell
cd management-system\be
docker compose ps
```

Dam bao PostgreSQL dang map port `5433:5432` va thong tin trong `application.properties` khop voi compose.

### Frontend khong dang nhap duoc

- Kiem tra Keycloak da chay tai `http://localhost:8080`.
- Kiem tra realm `vdt-shared-lib` da duoc import.
- Kiem tra client `fe-app` trong realm.

### `vdt-demo` khong build duoc vi thieu `shared-lib`

Chay lai:

```powershell
cd shared-lib
.\mvnw.cmd clean install
```

### Service khong dang ky API len backend

- Kiem tra `spring.application.name` da cau hinh.
- Kiem tra `vdt.share.enabled=true`.
- Kiem tra Kafka dang chay tai `localhost:9092`.
- Kiem tra service co annotation `@SharedApi` hoac `@ClientCall`.

### Request bi tu choi

Kiem tra tren UI quan tri:

- Exposed API dang enabled.
- Client dang active.
- Credential con hieu luc.
- Client da duoc gan permission toi API.
- Access policy cho phep request.
- Rate limit/max request size/max response size chua vuot nguong.

## Ghi chu bao mat

Cac secret, password va encryption key trong repo hien dang phuc vu local demo. Khi trien khai moi truong that, can dua chung vao bien moi truong hoac secret manager, thay doi Keycloak admin password, cau hinh TLS va khong commit secret len repository.
