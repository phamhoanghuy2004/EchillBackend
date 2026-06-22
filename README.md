<div align="center">
  <br />
  <h1>🌟 E-Chill Backend 🌟</h1>
  <p>
    <strong>A robust, scalable Spring Boot REST API for the E-Chill Online Course Platform.</strong>
  </p>
  <br />
</div>

## 📖 Introduction

**E-Chill Backend** is the core server-side application powering the E-Chill online learning platform. Designed with modularity and scalability in mind, it provides robust APIs for course management, adaptive learning tests, real-time chat, and secure authentication. 

---

## ✨ Main Features

- 🔐 **Authentication & Authorization:** Secure JWT-based authentication using OAuth2 Resource Server. Role-based access control (Admin, Teacher, Student).
- 📚 **Course Management:** Full CRUD operations for courses, lessons, and documents. Includes review and enrollment management.
- 📝 **Testing & Assessment:** Features Placement Tests, Adaptive Tests, and comprehensive test result tracking to analyze student skills.
- 💬 **Real-time Chat:** Seamless communication via WebSocket for real-time messaging.
- ☁️ **Media Management:** Integrated with Cloudinary for fast and secure image, video, and document hosting.
- 💳 **Payments & Transactions:** Management of Vouchers, Coin Packages, and detailed transaction histories.
- 📊 **Analytics Dashboard:** Advanced analytics and statistics for Admins, Teachers, and individual Student study progress.
- 🤖 **AI Integration:** Leveraging Spring AI with Google GenAI for intelligent features (like adaptive testing and personalized skill profiling).
- 📂 **Excel Processing:** Batch import and export capabilities utilizing Apache POI.

---

## 🛠️ Technologies Used

### Core Framework
- **Java 21**
- **Spring Boot 3.3.0** (Web, Data JPA, Validation, Mail)
- **Spring Security** (OAuth2 Resource Server)
- **Spring Boot WebSocket**

### Database & Caching
- **MySQL** (Relational Database)
- **Redis** (In-memory Data Structure Store & Caching)
- **Elasticsearch** (Search Engine & Full-text search)

### Utilities & Integrations
- **Lombok** & **MapStruct** (Boilerplate reduction & Object Mapping)
- **Spring AI** (Google GenAI Integration)
- **Cloudinary HTTP44** (Media Storage)
- **Apache POI** (Excel File Processing)
- **TSID Creator** (Unique ID Generation)

---

## 🚀 Installation & Running

### Prerequisites
Make sure you have the following installed on your machine:
- **Java 21** (JDK 21)
- **Maven**
- **MySQL** Database Server
- **Redis** Server
- **Elasticsearch** Server (Optional depending on your active profile)

### Setup Instructions

1. **Clone the repository:**
   ```bash
   git clone <repository_url>
   cd echill-backend
   ```

2. **Configure Environment Variables:**
   Create a `.env` file in the root directory (or configure via `application.yml`/`application.properties`) with your respective credentials for Database, Redis, Cloudinary, Google AI, and Mail server.

3. **Build the Application:**
   Using Maven wrapper:
   ```bash
   ./mvnw clean install -DskipTests
   ```

4. **Run the Application:**
   ```bash
   ./mvnw spring-boot:run
   ```
   *The server will typically start on port `8080`.*

---

## 📡 List of Main APIs

The application exposes a wide variety of RESTful endpoints. Here is a high-level overview of the primary resource prefixes:

| Prefix | Description |
|---|---|
| `/api/v1/auth` | Login, Registration, Token verification, Password reset |
| `/api/v1/users` | User profile management |
| `/api/v1/courses` | Course browsing, details, and modifications |
| `/api/v1/lessons` | Lesson content and progression |
| `/api/v1/enrollments` | Managing student course enrollments |
| `/api/v1/tests` | Standard and Placement exams |
| `/api/v1/adaptive-tests`| AI-driven personalized assessments |
| `/api/v1/chat` | WebSocket endpoints and chat history retrieval |
| `/api/v1/payments` | Transactions, Coin packages, and Voucher application |
| `/api/v1/admin/*` | Dedicated admin endpoints for users, analytics, and platform control |

*(Note: Detailed API documentation is typically accessible via Swagger UI if configured in the environment.)*

---

## 🔑 Default Accounts

For development and testing purposes, use the following administrator credentials:

- **Username:** `hoanghuy`
- **Password:** `17102004Huy@`

---
*Created with ❤️ by the E-Chill Development Team.*
