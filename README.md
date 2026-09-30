# Meeting Management System (MMS) - Backend API

A centralized Spring Boot REST API service designed to manage the entire lifecycle of organizational meetings: room reservations, equipment/materials tracking, support staff scheduling, double-booking prevention, approval workflows, and utilization reporting.

---

## 🚀 Key Features & Business Logic

- **Real-Time Double-Booking Prevention**: Validates room time intervals (`[startTime, endTime)`) against existing active meetings to prevent overlapping bookings.
- **Intelligent Approval Workflow**:
  - **Standard Meetings** (Room capacity $< 20$): Auto-confirmed immediately for fast scheduling.
  - **Large / Boardroom Meetings** (Room capacity $\ge 20$): Routed to `PENDING` status awaiting administrative approval (`/api/meetings/{id}/approve`).
- **Material & Equipment Inventory Tracking**:
  - Automatically verifies stock availability before booking.
  - Decrements available stock upon meeting confirmation.
  - **Auto-Restoration**: Releases allocated stock back to inventory whenever a meeting is cancelled.
- **Support Staff Scheduling**:
  - Assigns technicians, receptionists, and facilitators.
  - Queries real-time staff availability to prevent scheduling staff into overlapping concurrent meetings.
- **Participant RSVP & Notifications**:
  - Tracks individual attendee response status (`ACCEPTED`, `DECLINED`, `PENDING`).
  - Broadcasts in-app alerts on booking, updates, and cancellations.
- **System Metrics & Dashboard**: Aggregated counts of meetings by status, room utilization, and upcoming meetings timeline.

---

## 🛠️ Technology Stack

| Layer | Technology | Details |
| :--- | :--- | :--- |
| **Framework** | Spring Boot | Java 25 (release 25) |
| **Persistence** | Spring Data JPA / Hibernate | Object-relational mapping & custom JPQL queries |
| **Database** | MySQL | Database: `meetingmanagements` |
| **Validation** | Jakarta Bean Validation | `@Valid`, `@NotBlank`, `@NotNull`, `@Min`, `@Email` |
| **Boilerplate Reduction** | Project Lombok | Getters, setters, builders |
| **Monitoring** | Spring Boot Actuator | Health and metrics monitoring (`/actuator/health`) |
| **CORS Support** | Spring WebMvcConfigurer | Configured for Next.js frontend (`localhost:3000`) |

---

## 🗄️ Database Design (10 Core Entities)

The system is built on 10 relational tables as modeled in the database schema:

![Entity-Relationship Diagram](ERD%20Diagram.png)

1. **`departments`**: Organizational departments (`department_id`, `name`).
2. **`users`**: System users (`user_id`, `name`, `email`, `role`, `department_id`).
3. **`rooms`**: Meeting spaces (`room_id`, `name`, `location`, `capacity`, `status`).
4. **`meetings`**: Meeting records (`meeting_id`, `title`, `purpose`, `organizer_id`, `room_id`, `start_time`, `end_time`, `status`).
5. **`meeting_attendees`**: Junction table for participants (`meeting_id`, `user_id`, `response_status`).
6. **`materials`**: Equipment and refreshment inventory (`material_id`, `name`, `type`, `quantity_available`).
7. **`meeting_materials`**: Junction table for requested items (`meeting_id`, `material_id`, `quantity_requested`).
8. **`staff`**: Support personnel roster (`staff_id`, `name`, `role`, `skill`, `availability_status`).
9. **`meeting_staff`**: Junction table for assigned personnel (`meeting_id`, `staff_id`, `assigned_role`).
10. **`notifications`**: Alert records (`notification_id`, `meeting_id`, `recipient_id`, `type`, `message`, `sent_at`, `status`).

---

## 📡 REST API Reference

Base URL: `http://localhost:8080`

### 1. Meetings (`/api/meetings`)
| Method | Endpoint | Description |
| :--- | :--- | :--- |
| `GET` | `/api/meetings` | Retrieve all meetings |
| `GET` | `/api/meetings/{id}` | Get meeting details (attendees, materials, staff) |
| `GET` | `/api/meetings/organizer/{organizerId}` | Filter meetings by organizer |
| `GET` | `/api/meetings/status/{status}` | Filter by status (`PENDING`, `CONFIRMED`, `CANCELLED`, `COMPLETED`) |
| `GET` | `/api/meetings/calendar?roomId=&start=&end=` | Calendar view for room availability and time slots |
| `POST` | `/api/meetings` | **Create Meeting**: Checks conflicts, deducts inventory, assigns staff, auto-confirms or sets `PENDING`, sends alerts |
| `PUT` | `/api/meetings/{id}` | Update meeting details with conflict re-validation |
| `PATCH`| `/api/meetings/{id}/approve` | Approve a `PENDING` large-room meeting |
| `PATCH`| `/api/meetings/{id}/cancel?reason=` | Cancel meeting, **restores materials stock**, and notifies participants |
| `PATCH`| `/api/meetings/{id}/attendee/{userId}/rsvp?status=` | Participant RSVP update (`ACCEPTED`, `DECLINED`) |

### 2. Meeting Rooms (`/api/rooms`)
| Method | Endpoint | Description |
| :--- | :--- | :--- |
| `GET` | `/api/rooms` | Retrieve all rooms |
| `GET` | `/api/rooms/{id}` | Get room details |
| `POST` | `/api/rooms` | Register a new meeting room |
| `PUT` | `/api/rooms/{id}` | Update room details |
| `DELETE`| `/api/rooms/{id}` | Delete a room |
| `GET` | `/api/rooms/{id}/availability?start=&end=` | **Live room conflict check**: returns `available: true/false`, conflict reason, next available time |
| `GET` | `/api/rooms/available?start=&end=&minCapacity=` | Find all available rooms for a given time slot |

### 3. Materials & Equipment (`/api/materials`)
| Method | Endpoint | Description |
| :--- | :--- | :--- |
| `GET` | `/api/materials` | List all inventory items |
| `GET` | `/api/materials/{id}` | Get item details |
| `GET` | `/api/materials/type/{type}` | Filter by `EQUIPMENT`, `STATIONERY`, `CATERING` |
| `POST` | `/api/materials` | Add new material/equipment |
| `PUT` | `/api/materials/{id}` | Update inventory quantities |
| `DELETE`| `/api/materials/{id}` | Remove inventory item |

### 4. Support Staff (`/api/staff`)
| Method | Endpoint | Description |
| :--- | :--- | :--- |
| `GET` | `/api/staff` | List all support personnel |
| `GET` | `/api/staff/{id}` | Get staff details |
| `GET` | `/api/staff/role/{role}` | Filter by `TECHNICIAN`, `RECEPTIONIST`, `FACILITATOR` |
| `GET` | `/api/staff/available?start=&end=` | **Find available staff** not booked in concurrent meetings |
| `POST` | `/api/staff` | Register new staff member |
| `PUT` | `/api/staff/{id}` | Update staff details |
| `DELETE`| `/api/staff/{id}` | Delete staff member |

### 5. Users & Departments (`/api/users`, `/api/departments`)
| Method | Endpoint | Description |
| :--- | :--- | :--- |
| `GET` | `/api/users` | List all users |
| `POST` | `/api/users` | Create user with role (`ADMIN`, `ORGANIZER`, `EMPLOYEE`) |
| `GET` | `/api/departments` | List departments |
| `POST` | `/api/departments` | Create department |

### 6. Notifications & Dashboard (`/api/notifications`, `/api/dashboard`)
| Method | Endpoint | Description |
| :--- | :--- | :--- |
| `GET` | `/api/notifications/user/{userId}` | User notifications inbox |
| `PATCH`| `/api/notifications/{id}/status?status=` | Update notification status (`SENT`, `FAILED`, `PENDING`) |
| `GET` | `/api/dashboard/summary` | Aggregated metrics: meeting counts, room & staff utilization, upcoming timeline |

---

## ⚙️ Configuration & Setup

### Prerequisites
- **Java**: JDK 25 or higher
- **Database**: MySQL running on `localhost:3306`

### 1. Database Connection (`application.properties`)
Located at `src/main/resources/application.properties`:
```properties
spring.application.name=meetingManagements
server.port=8080

spring.datasource.url=jdbc:mysql://localhost:3306/meetingmanagements?createDatabaseIfNotExist=true&useSSL=false&serverTimezone=UTC&allowPublicKeyRetrieval=true
spring.datasource.username=root
spring.datasource.password=Vibol@2020
spring.datasource.driver-class-name=com.mysql.cj.jdbc.Driver

spring.jpa.hibernate.ddl-auto=update
spring.jpa.show-sql=true
spring.jpa.properties.hibernate.format_sql=true
spring.jpa.open-in-view=false

management.endpoints.web.exposure.include=health,info,metrics
```

### 2. Build & Run
From the `meetingManagements` directory:

```powershell
# Compile without running tests
.\mvnw.cmd compile -DskipTests

# Start the Spring Boot Application
.\mvnw.cmd spring-boot:run
```

Once started, the service will be available at:
`http://localhost:8080`

Health check endpoint:
`http://localhost:8080/actuator/health`

---

## 🧪 Testing with Postman

A ready-to-import Postman collection and testing walkthrough are included:

- **Postman Collection**: `Meeting_Management_System.postman_collection.json` (located in the project root)
- **Step-by-Step Testing Guide**: `POSTMAN_TESTING_GUIDE.md`

### Initial Seed Data Available for Testing
On startup, `DataInitializer.java` automatically creates:
- **Users**: Admin (`admin@meeting.com`, ID 1), Organizer (`organizer@meeting.com`, ID 2), Employee (`alice@meeting.com`, ID 3).
- **Rooms**: Executive Boardroom (Cap 20, ID 1), Conference Hall Orion (Cap 12, ID 2), Huddle Room 1 (Cap 6, ID 3), Innovation Lab (Cap 30, ID 4).
- **Materials**: Projector (ID 1), Digital Whiteboard (ID 2), Speakerphone (ID 3), Stationery (ID 4), Catering (ID 5).
- **Staff**: John Miller (Technician, ID 1), Sophia Davis (Receptionist, ID 2), Michael Chang (Facilitator, ID 3).
