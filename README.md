![Java](https://img.shields.io/badge/Java-ED8B00?style=for-the-badge&logo=java&logoColor=white)
![Spring Boot](https://img.shields.io/badge/Spring_Boot-6DB33F?style=for-the-badge&logo=springboot&logoColor=white)
![Spring Security](https://img.shields.io/badge/Spring_Security-6DB33F?style=for-the-badge&logo=springsecurity&logoColor=white)
![Spring Cloud](https://img.shields.io/badge/Spring_Cloud-6DB33F?style=for-the-badge&logo=spring&logoColor=white)
![Microservices](https://img.shields.io/badge/Microservices-000000?style=for-the-badge&logo=googlecloud&logoColor=white)
![MySQL](https://img.shields.io/badge/MySQL-005C84?style=for-the-badge&logo=mysql&logoColor=white)
![Kafka](https://img.shields.io/badge/Apache_Kafka-000000?style=for-the-badge&logo=apachekafka&logoColor=white)
![Docker](https://img.shields.io/badge/Docker-2496ED?style=for-the-badge&logo=docker&logoColor=white)
![Eureka](https://img.shields.io/badge/Eureka_Server-4B0082?style=for-the-badge)
![API Gateway](https://img.shields.io/badge/API_Gateway-FF6F00?style=for-the-badge)
![Jenkins](https://img.shields.io/badge/Jenkins-CF404D?style=for-the-badge&logo=jenkins&logoColor=white)


# Hotel Management System - Backend

A microservices-based hotel management system built with Spring Boot that handles hotel operations, bookings, and notifications.

## Architecture

The system uses microservices architecture with the following components:

- **API Gateway** - Single entry point for all client requests
- **Config Server** - Centralized configuration management
- **Eureka Server** - Service discovery and registration
- **5 Business Services** - Auth, Hotel, Booking, Notification, and their databases


 ## Setup
- Clone the repo
  ```bash
  mvn package -DskipTests
  docker compose up --build -d
  ```

## Architecture
<img width="1456" height="1469" alt="image" src="https://github.com/user-attachments/assets/ee69ce34-2733-441e-b44c-0895af6fd5d8" />

## Eureka
<img width="1910" height="907" alt="image" src="https://github.com/user-attachments/assets/8c140560-92b3-487b-8644-086468756071" />

## Dockerized and Jenkins pipeline ran
<img width="1280" height="619" alt="image" src="https://github.com/user-attachments/assets/6c6dc1a6-0d27-482c-9a05-1361f5b4b95f" />

## Notifications  Reminders
--

<img width="1545" height="803" alt="image" src="https://github.com/user-attachments/assets/c874c86e-e202-481f-b0db-055e371a70fe" />
--
<img width="1564" height="804" alt="image" src="https://github.com/user-attachments/assets/2a3a61b0-aedf-4923-b128-31163888304f" />
--
<img width="1566" height="778" alt="image" src="https://github.com/user-attachments/assets/055944ae-3735-4e3c-8f3e-1ee3cf419dd7" />
--

##  Frontend Screens
<img width="1912" height="938" alt="image" src="https://github.com/user-attachments/assets/d068a756-2b3a-43f7-9e36-5a8fcf01f235" />
<img width="1918" height="933" alt="image" src="https://github.com/user-attachments/assets/c65fff0f-ac2c-46c4-9b4f-7c01cb606041" />
<img width="1910" height="937" alt="image" src="https://github.com/user-attachments/assets/bf39fb72-81ba-4d01-af47-4eecec99efba" />
<img width="1913" height="940" alt="image" src="https://github.com/user-attachments/assets/a763349f-5042-4c25-a513-c0787a4d7081" />
<img width="1911" height="941" alt="image" src="https://github.com/user-attachments/assets/e108ebd9-434a-425d-afa6-582791bc9745" />
<img width="1917" height="936" alt="image" src="https://github.com/user-attachments/assets/017f3e56-7f29-4e6c-9aec-4aee69a9cf03" />
<img width="1913" height="943" alt="image" src="https://github.com/user-attachments/assets/35310c1f-f02e-4698-a9dc-ade940e11b43" />
<img width="1906" height="941" alt="image" src="https://github.com/user-attachments/assets/eadcabe6-fc5c-42d1-bb70-b9da1043f188" />
<img width="1901" height="936" alt="image" src="https://github.com/user-attachments/assets/5bdbbe01-e1ad-46b4-9f9e-475d1909096e" />
<img width="1908" height="931" alt="image" src="https://github.com/user-attachments/assets/57b26e88-3ae9-4ce2-9942-ad9ba0d79c90" />
<img width="1917" height="934" alt="image" src="https://github.com/user-attachments/assets/402581fc-6ca3-429a-a137-1b0dfabaa0d0" />
<img width="603" height="854" alt="image" src="https://github.com/user-attachments/assets/ad77805b-2c68-40f5-8edb-7daad00495cb" />


## Services

### 1. Auth Service (Port 8081)

Handles user authentication and authorization.

**Database:** `auth_db`

**Features:**
- User registration (guests can self-register)
- JWT token generation and validation
- User management (admin creates staff accounts)
- Role-based access control
- Password management

**Roles:**
- `ADMIN` - Full system access, seeded at startup
- `MANAGER` - Manages assigned hotel
- `RECEPTIONIST` - Manages assigned hotel operations
- `GUEST` - Books rooms and manages own bookings

**Key Endpoints:**
- `POST /api/auth/register` - Guest registration
- `POST /api/auth/login` - User login
- `POST /api/auth/create-user` - Admin creates staff (requires role + hotelId)
- `PUT /api/auth/me/username` - Update username
- `PUT /api/auth/me/password` - Change password

**Security:**
- Passwords hashed with BCrypt
- JWT tokens contain: userId, username, email, role, hotelId (for staff)
- Token expiry: 24 hours

---

### 2. API Gateway (Port 9090)

Routes requests and handles cross-cutting concerns.

**Features:**
- JWT token validation
- Request routing to microservices
- Rate limiting (Redis-based)
- CORS configuration
- Circuit breaker (Resilience4j)
- User context propagation via headers

**Request Flow:**
1. Client sends request with JWT token
2. Gateway validates token
3. Gateway extracts user info and adds headers:
   - `X-User-Id`
   - `X-User-Role`
   - `X-User-Email`
   - `X-Hotel-Id` (for staff)
4. Request forwarded to target service
5. Response returned to client

**Routes:**
- `/api/auth/**` → Auth Service (8081)
- `/api/hotels/**` → Hotel Service (8082)
- `/api/bookings/**` → Booking Service (8083)

**Public Routes (no authentication):**
- `/api/auth/register`
- `/api/auth/login`
- `/actuator/**`

---

### 3. Hotel Service (Port 8082)

Manages hotels and rooms.

**Database:** `hotel_db`

**Entities:**
- **Hotel** - Properties: name, address, city, star rating, amenities, status, room counts
- **Room** - Properties: room number, type, price, occupancy, floor, bed type, size, amenities, status

**Room Types:**
- SINGLE, DOUBLE, SUITE, DELUXE, EXECUTIVE, PRESIDENTIAL

**Room Status:**
- AVAILABLE, OCCUPIED, MAINTENANCE, CLEANING, OUT_OF_ORDER

**Hotel Status:**
- ACTIVE, INACTIVE, UNDER_MAINTENANCE

**Key Endpoints:**

Hotels:
- `POST /api/hotels` - Create hotel (admin only)
- `PUT /api/hotels/{id}` - Update hotel (admin/manager of that hotel)
- `GET /api/hotels/{id}` - Get hotel details
- `GET /api/hotels` - Get all hotels (admin only)
- `GET /api/hotels/active` - Get active hotels
- `GET /api/hotels/search?city={city}` - Search hotels by city
- `GET /api/hotels/my-hotel` - Get manager's assigned hotel

Rooms:
- `POST /api/hotels/rooms` - Create room
- `PUT /api/hotels/rooms/{id}` - Update room
- `PATCH /api/hotels/rooms/{id}/status` - Update room status
- `GET /api/hotels/rooms/{id}` - Get room details
- `GET /api/hotels/rooms/hotel/{hotelId}` - Get all rooms for hotel
- `GET /api/hotels/rooms/hotel/{hotelId}/available` - Get available rooms
- `GET /api/hotels/rooms/hotel/{hotelId}/search` - Search rooms with filters
- `DELETE /api/hotels/rooms/{id}` - Delete room (admin/manager only)

**Authorization:**
- Admin: Full access to all hotels
- Manager/Receptionist: Only their assigned hotel (hotelId must match)
- Guest: Read-only access

**Automatic Room Count Management:**
- Hotel's `totalRooms` and `availableRooms` updated automatically when rooms are added/removed/status changed

---

### 4. Booking Service (Port 8083)

Handles room bookings and availability.

**Database:** `booking_db`

**Entity:**
- **Booking** - Properties: user, hotel, room, dates, amount, status, payment details, guest info

**Booking Status:**
- PENDING → CONFIRMED → CHECKED_IN → CHECKED_OUT
- CANCELLED (can cancel from PENDING or CONFIRMED)

**Payment Status:**
- PENDING, PAID, REFUNDED, FAILED

**Key Features:**
- Date-based availability checking
- Automatic room assignment by type
- Pessimistic locking to prevent double bookings
- Kafka events for notifications
- OpenFeign calls to hotel-service for room data

**Key Endpoints:**

Search & Availability:
- `GET /api/bookings/search-hotels?city={city}&checkInDate={date}&checkOutDate={date}` - Search hotels with availability
- `GET /api/bookings/availability?hotelId={id}&checkInDate={date}&checkOutDate={date}` - Check room availability
- `GET /api/bookings/room-types?hotelId={id}&checkInDate={date}&checkOutDate={date}` - Get available room types

Booking Management:
- `POST /api/bookings` - Create booking (guest only, backend assigns specific room)
- `GET /api/bookings/{id}` - Get booking details
- `GET /api/bookings/my-bookings` - Get user's bookings (guest)
- `GET /api/bookings/hotel/{hotelId}` - Get hotel bookings (staff)
- `PATCH /api/bookings/{id}/cancel?reason={text}` - Cancel booking
- `PATCH /api/bookings/{id}/check-in` - Check in guest (staff only)
- `PATCH /api/bookings/{id}/check-out` - Check out guest (staff only)
- `PATCH /api/bookings/{id}/payment?status={status}&method={method}` - Update payment

Staff Operations:
- `GET /api/bookings/hotel/{hotelId}/today-checkins` - Get today's check-ins
- `GET /api/bookings/hotel/{hotelId}/today-checkouts` - Get today's check-outs

**Concurrency Handling:**
- Uses `@Lock(LockModeType.PESSIMISTIC_WRITE)` on database queries
- Prevents race conditions when multiple users book the same room
- If conflict detected, transaction fails and user must retry

**Booking Flow:**
1. Guest searches hotels by city and dates
2. Guest selects hotel and room type
3. Backend finds available room of that type
4. Pessimistic lock acquired to check conflicts
5. If no conflicts, booking created with status CONFIRMED
6. Kafka event published for email notification
7. Guest receives confirmation email

**Authorization:**
- Guests: Can create and view own bookings, cancel own bookings
- Staff: Can view hotel bookings, check-in/check-out guests, update payment
- Admin: Full access

**Integration:**
- Calls hotel-service via OpenFeign to get hotel/room details
- Publishes events to Kafka for notifications
- Updates room status in hotel-service during check-in/check-out

---

### 5. Notification Service (Port 8085)

Sends email notifications based on booking events.

**Database:** `notification_db` (stores notification history)

**Kafka Topics:**
- `booking-created` - New booking confirmation
- `checkin-reminder` - 24 hours before check-in
- `guest-checked-in` - Check-in completed
- `guest-checked-out` - Check-out completed

<img width="1545" height="803" alt="image" src="https://github.com/user-attachments/assets/c874c86e-e202-481f-b0db-055e371a70fe" />
<img width="1564" height="804" alt="image" src="https://github.com/user-attachments/assets/2a3a61b0-aedf-4923-b128-31163888304f" />
<img width="1566" height="778" alt="image" src="https://github.com/user-attachments/assets/055944ae-3735-4e3c-8f3e-1ee3cf419dd7" />

**Email Templates:**
1. **Booking Confirmation** - Sent when booking created
   - Booking ID, hotel name, room type, dates, amount, guest details

2. **Check-in Reminder** - Sent 24 hours before check-in
   - Reminder with booking details

3. **Check-in Confirmation** - Sent when guest checks in
   - Welcome message, room number, check-out date

4. **Check-out Confirmation** - Sent when guest checks out
   - Thank you message, booking summary

**Email Configuration:**
- SMTP: Gmail (smtp.gmail.com:587)
- Uses app-specific password
- TLS encryption enabled

**Consumer Groups:**
- `notification-group` - Processes all booking events

**Error Handling:**
- Failed emails logged but don't block Kafka processing
- Can retry failed notifications manually

---

### 6. Config Server (Port 8888)

Centralized configuration management.

**Source:** Git repository (https://github.com/coosmos/hotel-config-server)

**Configuration Files:**
- `{service-name}.properties` - Base config
- `{service-name}-local.properties` - Local development
- `{service-name}-docker.properties` - Docker deployment

**Profile Usage:**
- Local: Uses localhost URLs
- Docker: Uses container names (mysql, kafka, eureka-server, etc.)

All services fetch configuration on startup with:
```properties
spring.config.import=optional:configserver:http://config-server:8888
```

---

### 7. Eureka Server (Port 8761)

Service discovery and registration.

**Features:**
- All services register themselves on startup
- Services discover each other via Eureka
- Health checks and heartbeats
- Load balancing support

**Dashboard:** http://localhost:8761

---

## Database Schema
<img width="990" height="741" alt="image" src="https://github.com/user-attachments/assets/aa924092-f194-4664-9f25-e337725114b9" />

### auth_db
```sql
users (
    id, username, email, password, role, hotel_id,
    full_name, phone_number, active, created_at, updated_at
)
```

### hotel_db
```sql
hotels (
    id, name, description, address, city, state, country, pincode,
    contact_number, email, star_rating, amenities, status,
    total_rooms, available_rooms, created_at, updated_at
)

rooms (
    id, hotel_id, room_number, room_type, price_per_night,
    max_occupancy, floor_number, bed_type, room_size,
    amenities, description, status, is_active, created_at, updated_at
)
```

### booking_db
```sql
bookings (
    id, user_id, hotel_id, room_id, check_in_date, check_out_date,
    total_amount, status, payment_status, payment_method,
    guest_name, guest_email, guest_phone, number_of_guests,
    hotel_name, checked_in_at, checked_out_at, cancelled_at,
    paid_at, created_at, updated_at, created_by, updated_by
)
```

### notification_db
```sql
notifications (
    id, booking_id, recipient_email, notification_type,
    status, sent_at, error_message, created_at
)
```

## Security

### Authentication Flow
1. User logs in with username/password
2. Auth service validates credentials
3. JWT token generated with user claims
4. Client includes token in Authorization header: `Bearer <token>`
5. Gateway validates token on each request
6. Gateway extracts user info and adds to request headers
7. Services use headers for authorization

### JWT Claims
```json
{
  "userId": 1,
  "username": "john.doe",
  "email": "john@example.com",
  "role": "GUEST",
  "hotelId": null,
  "iat": 1704067200,
  "exp": 1704153600
}
```

### Authorization Rules

**ADMIN:**
- Create/update/delete hotels
- Create staff users (manager/receptionist)
- View all bookings
- Full system access

**MANAGER:**
- View/update assigned hotel
- Create/update/delete rooms in assigned hotel
- View hotel bookings
- Check-in/check-out guests
- Update payment status

**RECEPTIONIST:**
- View assigned hotel
- View/create/update rooms in assigned hotel (cannot delete)
- View hotel bookings
- Check-in/check-out guests
- Update payment status

**GUEST:**
- Self-register
- Search hotels and rooms
- Create bookings
- View own bookings
- Cancel own bookings

## Inter-Service Communication

### Synchronous (OpenFeign)
- Booking Service → Hotel Service: Get hotel/room details

### Asynchronous (Kafka)
- Booking Service → Notification Service: Booking events
- Scheduled Service → Notification Service: Check-in reminders

## Running the Application

### Prerequisites
- Docker Desktop
- Java 17
- Maven 3.8+
- Git

### Local Development
1. Start infrastructure:
```bash
docker-compose up -d mysql kafka zookeeper redis config-server eureka-server
```

2. Run services with local profile:
```bash
mvn spring-boot:run -Dspring-boot.run.profiles=local
```

### Docker Deployment
1. Build JARs:
```bash
mvn clean package -DskipTests
```

2. Start all services:
```bash
docker-compose up -d
```

3. Check status:
```bash
docker-compose ps
```

4. View logs:
```bash
docker-compose logs -f
```

### Accessing Services
- API Gateway: http://localhost:9090
- Eureka Dashboard: http://localhost:8761
- MySQL: localhost:3307 (Docker) or localhost:3306 (Local)

## Environment Variables (Docker)

All services use environment variables for configuration:
- `SPRING_PROFILES_ACTIVE=docker`
- `SPRING_CONFIG_IMPORT=optional:configserver:http://config-server:8888`
- Database URLs use container names: `mysql:3306`
- Kafka URL: `kafka:29092`
- Eureka URL: `http://eureka-server:8761/eureka/`

## API Gateway Routes

All requests go through gateway at http://localhost:9090:
- `http://localhost:9090/api/auth/**` → Auth Service
- `http://localhost:9090/api/hotels/**` → Hotel Service
- `http://localhost:9090/api/bookings/**` → Booking Service

## Error Handling

All services return consistent error responses:
```json
{
  "message": "Error description",
  "timestamp": "2024-01-06T12:00:00Z"
}
```

HTTP Status Codes:
- 200 OK - Success
- 201 Created - Resource created
- 400 Bad Request - Validation error
- 401 Unauthorized - Missing/invalid token
- 403 Forbidden - Insufficient permissions
- 404 Not Found - Resource not found
- 409 Conflict - Duplicate resource
- 500 Internal Server Error - Server error

## Testing

Unit tests included for:
- Repositories (H2 in-memory database)
- Services (Mockito)
- Controllers (MockMvc)

Run tests:
```bash
mvn test
```

## Data Seeding

Admin user seeded on startup:
- Username: `admin`
- Password: `admin123`
- Role: `ADMIN`
