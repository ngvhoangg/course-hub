# Course Hub

A backend system for an online course platform built with Spring Boot.

---

## Core Features

- JWT Authentication (Access + Refresh Token, rotation, httpOnly cookie)
- Email Verification (SendGrid, token-based)
- Role-based Authorization (STUDENT, INSTRUCTOR, ADMIN)
- Course / Lesson / Enrollment / Review / Payment APIs
- Rate Limiting (Bucket4j)
- Background Jobs (token cleanup)
- Dockerized environment (PostgreSQL with pgvector, Kafka)

---

## Tech Stack

- **Backend:** Spring Boot, Spring Security, JPA (Hibernate)
- **Database:** PostgreSQL 17 (+ pgvector)
- **Message Queue:** Apache Kafka
- **Email:** SendGrid
- **Testing:** JUnit, Testcontainers
- **CI/CD:** GitHub Actions
- **Containerization:** Docker, Docker Compose