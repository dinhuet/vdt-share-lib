# Hướng dẫn sử dụng Agent - Agent Workflow Guide

## Cấu trúc thư mục (Directory Structure)

```
.opencode/
├── AGENTS.md         # Hướng dẫn này (this guide)
├── agents/           # Agent definition files (định nghĩa các agent)
├── doc/              # Technical documentation (tài liệu kỹ thuật sau implement)
├── plan/             # Implementation plans (kế hoạch triển khai)
├── spec/             # Feature specifications (đặc tả yêu cầu)
└── skills/           # Loadable skills (kỹ năng chuyên sâu có thể tải)
```

---

## Quy trình làm việc (Workflow)

```
Yêu cầu → [Business Analyst] → spec/
  → [Planner] → plan/
    → [Fullstack Developer] → code + doc/
      → [Tester] → tests
        → [Code Reviewer] → review
          → [Git Manager] → commit/PR
```

---

## Vai trò và cách dùng từng Agent

### 1. Business Analyst
- **Khi nào dùng:** Khi có yêu cầu nghiệp vụ mới, cần phân tích và viết spec.
- **Đầu vào:** Yêu cầu từ người dùng.
- **Đầu ra:** File `.md` trong `spec/`.
- **Cách dùng:** Mô tả yêu cầu, BA sẽ hỏi làm rõ nếu cần, sau đó tạo spec.

### 2. Planner
- **Khi nào dùng:** Sau khi có spec, trước khi bắt đầu code.
- **Đầu vào:** `spec/<feature>.md`
- **Đầu ra:** File `.md` trong `plan/`.
- **Cách dùng:** Yêu cầu planner đọc spec và lên kế hoạch triển khai chi tiết.

### 3. Fullstack Developer
- **Khi nào dùng:** Khi đã có plan và spec, cần implement.
- **Đầu vào:** `plan/<feature>.md` + `spec/<feature>.md`
- **Đầu ra:** Code + `doc/<feature>.md`
- **Cách dùng:** Gọi agent này với tên feature để nó đọc plan và spec rồi code.

### 4. Tester
- **Khi nào dùng:** Sau khi implement xong, cần viết test.
- **Đầu vào:** `spec/<feature>.md` + `doc/<feature>.md`
- **Đầu ra:** Test files + test report.
- **Cách dùng:** Cung cấp feature name hoặc module cần test.

### 5. Code Reviewer
- **Khi nào dùng:** Khi có code changes cần review trước khi merge.
- **Tính chất:** Read-only — không sửa code, chỉ nhận xét.
- **Đầu vào:** Code files + spec để đối chiếu.
- **Đầu ra:** Review report (trả về conversation, không ghi file).

### 6. Debugger
- **Khi nào dùng:** Khi có bug, error, hoặc test fail.
- **Cách dùng:** Cung cấp error message/stack trace/hành vi lỗi, debugger sẽ chẩn đoán và sửa.

### 7. Git Manager
- **Khi nào dùng:** Khi cần commit, tạo branch, hoặc chuẩn bị PR.
- **Nguyên tắc:** Không `git add .` mù quáng, commit theo Conventional Commits.

### 8. Researcher
- **Khi nào dùng:** Khi cần nghiên cứu thư viện, API, best practices trước khi quyết định kỹ thuật.
- **Tính chất:** Research only — không code.

---

## Quy tắc chung

| Thư mục  | Ai ghi             | Ai đọc                          |
|----------|--------------------|---------------------------------|
| `spec/`  | Business Analyst   | Planner, Developer, Tester, Reviewer |
| `plan/`  | Planner            | Developer                       |
| `doc/`   | Developer          | Tester, Reviewer                |
| `agents/`| Người dùng (cấu hình) | Tất cả agent                |

- **Luôn đọc spec/plan trước khi code.**
- **Luôn ghi doc/ sau khi implement.**
- **Không sửa file trong `agents/` trừ khi có yêu cầu rõ ràng.**
- **Skills trong `skills/` có thể được load bằng tool `skill` để có hướng dẫn chuyên sâu.**
- **Cấu hình agent và permission được định nghĩa trong `opencode.jsonc`.**

---

## Cách gọi Agent (Prompt template)

### Cú pháp `dinhuet:<agent-name>` và `dinhuet:<skill-name>`

- `dinhuet:<tên-agent>` — Đọc định nghĩa agent trong `agents/<tên-agent>.md` để biết role, input/output, cách dùng, rồi thực hiện theo.
  - Ví dụ: `dinhuet:planner` → đọc `agents/planner.md` và làm theo.
- `dinhuet:<tên-skill>` — Load skill từ `skills/<tên-skill>/` bằng tool `skill`.
  - Ví dụ: `dinhuet:brainstorming` → load skill brainstorming.

### Cú pháp `@agent-name` (mặc định)

### Ví dụ gọi Business Analyst:
> `@business-analyst Tôi cần tính năng đăng nhập bằng Google. Phân tích và viết spec.`

### Ví dụ gọi Planner:
> `@planner Đọc spec/auth-google.md và lên kế hoạch triển khai.`

### Ví dụ gọi Developer:
> `@fullstack-developer Implement feature auth-google theo plan.`

### Ví dụ gọi Tester:
> `@tester Viết test cho feature auth-google.`
