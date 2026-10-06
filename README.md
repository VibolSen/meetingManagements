# Meeting Management System (MMS) - Enterprise Backend API (v1.5.0)

A high-performance, centralized Spring Boot REST API service engineered to manage the entire lifecycle of enterprise corporate meetings: room reservations, automated double-booking prevention, equipment logistics, support personnel scheduling, multi-tier approvals, recurring meeting series, meeting minutes (MOM), personal action items, digital room signage kiosk integration, and multi-channel notification dispatching.

---

## 🚀 Key Features & Enterprise Capabilities

### 1. Real-Time Zero-Conflict Engine
- Validates room reservation intervals (`[startTime, endTime)`) against all existing active meetings.
- Live room availability search with capacity, equipment, and equipment filters (`/api/rooms/available`).
- Instant conflict diagnosis with actionable error feedback and next-available slot suggestions.

### 2. Intelligent Approval & Governance Workflow
- **Standard Meetings** (Room capacity $< 20$): Auto-confirmed instantly for friction-free scheduling.
- **Executive / Boardroom Meetings** (Room capacity $\ge 20$): Automatically routed to `PENDING` state awaiting Admin authorization (`PATCH /api/meetings/{id}/approve`).

### 3. Equipment & Material Inventory Allocation
- Checks live quantity availability before confirming bookings.
- Automatically decrements available stock upon meeting confirmation.
- **Auto-Restoration Guarantee**: Automatically returns allocated equipment and catering items back to active inventory when meetings are cancelled or completed early.

### 4. Support Staff Scheduling & Roster Management
- Allocates certified technicians, receptionists, and facilitators.
- Queries concurrent personnel schedules to eliminate staff double-booking.

### 5. Recurring Meeting Series Engine
- Supports automated recurrence patterns: `DAILY`, `WEEKLY`, `BI_WEEKLY`, and `MONTHLY`.
- **Pre-Flight Conflict Preview**: Evaluates all recurring occurrences in advance (`POST /api/meetings/recurring/preview`), identifying potential slot collisions before committing the series.

### 6. Meeting Minutes (MOM) & Action Items Checklist
- Complete official Meeting Minutes capture (`agenda`, `discussionSummary`, `decisionsMade`).
- Granular action item assignment with due dates, assignees, and real-time status tracking (`PENDING`, `IN_PROGRESS`, `COMPLETED`, `CANCELLED`).

### 7. Digital Signage Kiosk & Room Display Gateway
- Dedicated display endpoints for wall-mounted tablet displays outside meeting rooms (`GET /api/rooms/{id}/display-status`).
- Returns live countdown to the next meeting, current meeting title, host details, and occupancy state (`AVAILABLE`, `OCCUPIED`, `STARTING_SOON`).

### 8. Facility Maintenance & Room Issue Incident Tracking
- In-room hardware and facility reporting (`AUDIO_VISUAL`, `FURNITURE`, `AIR_CONDITIONING`, `NETWORK`, `CLEANLINESS`).
- Incident status tracking (`REPORTED`, `UNDER_INSPECTION`, `RESOLVED`, `CANCELLED`) with priority levels (`LOW`, `MEDIUM`, `HIGH`, `CRITICAL`).

### 9. Multi-Channel Notification Dispatcher
- **Telegram Bot Integration**: Dispatches formatted Markdown reminders and lifecycle alerts to corporate channels and attendees via the Telegram Bot API (`RestClient`).
- **Notification Templates**: Dynamic template engine with variable substitution (`{organizerName}`, `{roomName}`, `{startTime}`, etc.).
- **SMTP Email Notifications**: HTML/Text corporate email notifications for meeting confirmation, updates, and cancellations.

### 10. Automated Life-Cycle Background Scheduler
- `MeetingAutoReleaseScheduler`: Automatically releases reserved rooms if organizers do not check in within the configured grace period, maximizing room availability.

### 11. Comprehensive Facility Analytics & Heatmaps
- Analytics engine calculating facility utilization rates, peak booking hours, department-level room consumption, and issue turnaround metrics.

---

## 🛠️ Technology Stack

| Layer | Technology | Version / Specification | Rationale |
| :--- | :--- | :--- | :--- |
| **Language** | Java | JDK 25 | Latest enterprise Java runtime performance |
| **Framework** | Spring Boot | 4.1.1 | Enterprise application framework |
| **Security** | Spring Security & JWT | JWT Bearer Authentication | Role-based authorization (`ADMIN`, `ORGANIZER`, `EMPLOYEE`) |
| **Persistence** | Spring Data JPA / Hibernate | 6.x | High-throughput ORM with optimized JPQL queries |
| **Database** | MySQL | 8.0+ | Relational data integrity with foreign key constraints |
| **Validation** | Jakarta Bean Validation | 3.0 | Strict payload schema enforcement |
| **Utilities** | Project Lombok | Latest | Clean boilerplate reduction |
| **HTTP Client** | Spring RestClient | Modern non-blocking API | Telegram Bot API dispatching |
| **Monitoring** | Spring Boot Actuator | Integrated | Live health telemetry at `/actuator/health` |

---

## 🗄️ Database Architecture (21 Core Entities)

The system is structured around 21 normalized relational entities:

1. **`departments`**: Corporate departments (`department_id`, `name`).
2. **`users`**: System users (`user_id`, `name`, `email`, `role`, `department_id`, `status`, `booking_access_level`).
3. **`rooms`**: Physical conference rooms (`room_id`, `name`, `location`, `capacity`, `status`).
4. **`meetings`**: Core booking records (`meeting_id`, `title`, `purpose`, `organizer_id`, `room_id`, `start_time`, `end_time`, `status`).
5. **`recurring_series`**: Recurring meeting parent series configuration.
6. **`meeting_attendees`**: Participant RSVP junction (`meeting_id`, `user_id`, `response_status`).
7. **`meeting_minutes`**: Formal records of meeting discussions and decisions.
8. **`action_items`**: Trackable tasks linked to meetings and users (`due_date`, `status`, `assigned_to`).
9. **`meeting_attachments`**: Shared meeting files, decks, and agendas (`file_name`, `file_url`, `file_size`).
10. **`materials`**: Inventory items and equipment (`material_id`, `name`, `type`, `quantity_available`).
11. **`meeting_materials`**: Allocated items junction (`meeting_id`, `material_id`, `quantity_requested`).
12. **`staff`**: Operational staff roster (`staff_id`, `name`, `role`, `skill`, `availability_status`).
13. **`meeting_staff`**: Allocated staff junction (`meeting_id`, `staff_id`, `assigned_role`).
14. **`room_issues`**: Maintenance and incident reports (`room_id`, `category`, `priority`, `status`).
15. **`notifications`**: User alert inbox (`recipient_id`, `type`, `message`, `status`).
16. **`notification_templates`**: Customizable alert templates (`template_code`, `template_body`).
17. **`system_settings`**: Dynamic runtime system governance key-value properties.
18. **`audit_logs`**: Immutable audit logs of administrative actions.

---

## 📡 REST API Reference

Base URL: `http://localhost:8080`

### 1. Authentication & System Identity (`/api/auth`, `/api/system`)
| Method | Endpoint | Access | Description |
| :--- | :--- | :--- | :--- |
| `POST` | `/api/auth/login` | **Public** | Authenticate credentials and retrieve JWT bearer token |
| `GET` | `/api/auth/me` | Authenticated | Retrieve authenticated user profile and permissions |
| `POST` | `/api/auth/register` | Disabled | Public registration is disabled; accounts are provisioned by Admins |
| `GET` | `/api/system/info` | **Public** | System version (`1.5.0`), environment, builder info, and uptime |
| `GET` | `/api/system/settings` | **Public** | Public system configurations (booking windows, grace periods) |
| `GET` | `/api/system/settings/all` | Admin Only | Full system governance settings |
| `PUT` | `/api/system/settings/batch` | Admin Only | Batch update system settings |

### 2. Meetings & Calendar Management (`/api/meetings`)
| Method | Endpoint | Access | Description |
| :--- | :--- | :--- | :--- |
| `GET` | `/api/meetings` | Authenticated | Retrieve meetings with optional filters |
| `GET` | `/api/meetings/{id}` | Authenticated | Get complete meeting details |
| `POST` | `/api/meetings` | Authenticated | **Schedule Meeting**: Conflict validation, stock deduction, notifications |
| `POST` | `/api/meetings/recurring/preview` | Authenticated | Preview recurring occurrences and check for collisions |
| `POST` | `/api/meetings/recurring` | Authenticated | Create recurring meeting series |
| `PUT` | `/api/meetings/{id}` | Organizer / Admin | Update meeting details with conflict re-validation |
| `PATCH`| `/api/meetings/{id}/approve` | Admin Only | Approve large-room `PENDING` booking |
| `PATCH`| `/api/meetings/{id}/cancel` | Organizer / Admin | Cancel meeting, **restores materials stock**, dispatches alerts |
| `PATCH`| `/api/meetings/{id}/check-in` | Organizer / Admin | Check in to room |
| `PATCH`| `/api/meetings/{id}/end-early` | Organizer / Admin | End meeting early, liberating the room for other bookings |
| `PATCH`| `/api/meetings/{id}/attendee/{userId}/rsvp` | Attendee | Update participant RSVP (`ACCEPTED`, `DECLINED`) |
| `GET` | `/api/meetings/{id}/minutes` | Authenticated | Retrieve formal meeting minutes |
| `POST` | `/api/meetings/{id}/minutes` | Organizer / Admin | Save/publish meeting minutes |
| `GET` | `/api/meetings/{id}/actions` | Authenticated | List action items assigned in this meeting |
| `POST` | `/api/meetings/{id}/actions` | Organizer / Admin | Create and assign a new action item |
| `GET` | `/api/meetings/{id}/attachments` | Authenticated | List attachments for meeting |
| `POST` | `/api/meetings/{id}/attachments` | Organizer / Admin | Attach file/document to meeting |

### 3. Personal Action Items (`/api/actions`)
| Method | Endpoint | Access | Description |
| :--- | :--- | :--- | :--- |
| `GET` | `/api/actions/user/{userId}` | Authenticated | Get all action items assigned to user |
| `PATCH`| `/api/actions/{id}/status` | Assignee / Admin | Update item status (`IN_PROGRESS`, `COMPLETED`, `CANCELLED`) |
| `DELETE`| `/api/actions/{id}` | Admin / Creator | Delete action item |

### 4. Meeting Rooms & Kiosks (`/api/rooms`)
| Method | Endpoint | Access | Description |
| :--- | :--- | :--- | :--- |
| `GET` | `/api/rooms` | Authenticated | List all rooms |
| `GET` | `/api/rooms/{id}` | Authenticated | Get room details |
| `POST` | `/api/rooms` | Admin Only | Create new meeting room |
| `PUT` | `/api/rooms/{id}` | Admin Only | Update room details |
| `DELETE`| `/api/rooms/{id}` | Admin Only | Remove meeting room |
| `GET` | `/api/rooms/{id}/display-status` | **Public** | **Signage Kiosk**: Live room occupancy & countdown |
| `GET` | `/api/rooms/{id}/availability` | Authenticated | Real-time interval conflict verification |
| `GET` | `/api/rooms/available` | Authenticated | Query rooms available for time window |
| `POST` | `/api/rooms/{id}/issues` | Authenticated | Report an equipment or facility issue in room |

### 5. Facility Incident Tracking (`/api/issues`)
| Method | Endpoint | Access | Description |
| :--- | :--- | :--- | :--- |
| `GET` | `/api/issues` | Authenticated | List all room issues |
| `GET` | `/api/issues/{id}` | Authenticated | Get issue details |
| `PUT` | `/api/issues/{id}` | Admin / Tech | Update issue resolution notes |
| `PATCH`| `/api/issues/{id}/status` | Admin Only | Update status (`UNDER_INSPECTION`, `RESOLVED`, `CANCELLED`) |

### 6. Materials & Equipment Logistics (`/api/materials`)
| Method | Endpoint | Access | Description |
| :--- | :--- | :--- | :--- |
| `GET` | `/api/materials` | Authenticated | List all equipment & material inventory |
| `POST` | `/api/materials` | Admin Only | Add new inventory item |
| `PUT` | `/api/materials/{id}` | Admin Only | Update stock quantities |
| `DELETE`| `/api/materials/{id}` | Admin Only | Delete inventory item |

### 7. Support Staff Roster (`/api/staff`)
| Method | Endpoint | Access | Description |
| :--- | :--- | :--- | :--- |
| `GET` | `/api/staff` | Authenticated | List all support personnel |
| `GET` | `/api/staff/available` | Authenticated | Query staff free during specific time slot |
| `POST` | `/api/staff` | Admin Only | Register support staff member |
| `PUT` | `/api/staff/{id}` | Admin Only | Update staff profile |
| `DELETE`| `/api/staff/{id}` | Admin Only | Remove support staff member |

### 8. User Management & Departments (`/api/users`, `/api/departments`)
| Method | Endpoint | Access | Description |
| :--- | :--- | :--- | :--- |
| `GET` | `/api/users` | Authenticated | List all users |
| `POST` | `/api/users` | Admin Only | Provision corporate staff account |
| `PUT` | `/api/users/{id}` | Admin Only | Update user profile |
| `PATCH`| `/api/users/{id}/status` | Admin Only | Toggle user status (`ACTIVE`, `SUSPENDED`) |
| `PATCH`| `/api/users/{id}/booking-access` | Admin Only | Set booking access level (`FULL_ACCESS`, `VIEW_ONLY`) |
| `GET` | `/api/departments` | Authenticated | List all departments |
| `POST` | `/api/departments` | Admin Only | Create department |

### 9. Telegram Bot Integration (`/api/telegram`)
| Method | Endpoint | Access | Description |
| :--- | :--- | :--- | :--- |
| `GET` | `/api/telegram/status` | Authenticated | Check Telegram bot configuration and connectivity |
| `POST` | `/api/telegram/test` | Admin Only | Dispatch test notification to target Chat ID |
| `GET` | `/api/telegram/templates` | Admin Only | List all notification templates |
| `PUT` | `/api/telegram/templates/{code}` | Admin Only | Update notification template text |

### 10. Facility Analytics (`/api/analytics`)
| Method | Endpoint | Access | Description |
| :--- | :--- | :--- | :--- |
| `GET` | `/api/analytics/summary` | Admin Only | Overall utilization, total booking hours, and capacity statistics |
| `GET` | `/api/analytics/rooms` | Admin Only | Per-room utilization rates |
| `GET` | `/api/analytics/departments` | Admin Only | Department usage distribution |
| `GET` | `/api/analytics/heatmap` | Admin Only | Peak booking hour heatmap matrix |

---

## ⚙️ Configuration & Environment Variables

Configuration is managed via `src/main/resources/application.properties` with environment variable overrides:

```properties
spring.application.name=meetingManagements
server.port=8080

# Database Configuration
spring.datasource.url=jdbc:mysql://localhost:3306/meetingmanagements?createDatabaseIfNotExist=true&useSSL=false&serverTimezone=UTC&allowPublicKeyRetrieval=true
spring.datasource.username=root
spring.datasource.password=${DB_PASSWORD:your_db_password}
spring.datasource.driver-class-name=com.mysql.cj.jdbc.Driver

# JPA & Hibernate
spring.jpa.hibernate.ddl-auto=update
spring.jpa.show-sql=true
spring.jpa.properties.hibernate.format_sql=true
spring.jpa.open-in-view=false

# JWT Security
jwt.secret=${JWT_SECRET:9a7c3b2e5f8a1d4c7b0e3f6a9c2d5e8b1a4f7c0d3e6a9b2c5d8e1f4a7b0c3d6e}
jwt.expiration-ms=86400000

# Application Metadata
info.app.name=Meeting Management System
info.app.version=1.5.0
info.app.builder=Vibol SEN
info.app.environment=production

# Telegram Bot Multi-Channel Alert Configuration
telegram.bot.enabled=${TELEGRAM_BOT_ENABLED:true}
telegram.bot.token=${TELEGRAM_BOT_TOKEN:}
telegram.bot.username=${TELEGRAM_BOT_USERNAME:MMS_Meeting_Alert_Bot}
telegram.bot.default-chat-id=${TELEGRAM_DEFAULT_CHAT_ID:1035574371}
telegram.reminder.default-minutes=10

# Mail SMTP Configuration
mail.enabled=${MAIL_ENABLED:false}
mail.from=noreply@meetinghub.internal
spring.mail.host=smtp.gmail.com
spring.mail.port=587
spring.mail.properties.mail.smtp.auth=true
spring.mail.properties.mail.smtp.starttls.enable=true
```

> **Security Note:** Never commit raw API tokens or credentials into source control. Store local overrides in `application-local.properties` (which is included in `.gitignore`) or set system environment variables (`TELEGRAM_BOT_TOKEN`, `DB_PASSWORD`).

---

## 🔨 Build & Run Instructions

### Prerequisites
- **JDK 25** installed and set in `JAVA_HOME`.
- **MySQL 8.0+** running locally on port `3306`.

### Running the Application
From the `meetingManagements` directory:

```powershell
# Windows
.\mvnw.cmd spring-boot:run

# Linux / macOS
./mvnw spring-boot:run
```

Once started:
- API Root: `http://localhost:8080`
- System Info: `http://localhost:8080/api/system/info`
- Health Check: `http://localhost:8080/actuator/health`
