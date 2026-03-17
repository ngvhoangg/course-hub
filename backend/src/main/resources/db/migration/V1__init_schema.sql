-- =========================
-- USERS
-- =========================

CREATE TABLE users (
   id BIGSERIAL PRIMARY KEY,
   email VARCHAR(255) NOT NULL UNIQUE,
   password_hash VARCHAR(255) NOT NULL,
   full_name VARCHAR(255),
   role VARCHAR(50) NOT NULL DEFAULT 'STUDENT',
   status VARCHAR(50) NOT NULL DEFAULT 'PENDING',
   created_at TIMESTAMP NOT NULL DEFAULT now()
);

-- =========================
-- CATEGORIES
-- =========================

CREATE TABLE categories (
    id BIGSERIAL PRIMARY KEY,
    name VARCHAR(100) NOT NULL UNIQUE
);

-- =========================
-- COURSES
-- =========================

CREATE TABLE courses (
     id BIGSERIAL PRIMARY KEY,
     title VARCHAR(255) NOT NULL,
     description TEXT,
     price DECIMAL(10,2) NOT NULL,
     instructor_id BIGINT NOT NULL,
     image_url VARCHAR(500),
     status VARCHAR(50) NOT NULL,
     created_at TIMESTAMP NOT NULL DEFAULT now(),
     CONSTRAINT fk_courses_instructor
         FOREIGN KEY (instructor_id) REFERENCES users(id)
);

CREATE TABLE course_categories (
       course_id BIGINT NOT NULL,
       category_id BIGINT NOT NULL,
       PRIMARY KEY (course_id, category_id),
       CONSTRAINT fk_course_categories_course
           FOREIGN KEY (course_id) REFERENCES courses(id),
       CONSTRAINT fk_course_categories_category
           FOREIGN KEY (category_id) REFERENCES categories(id)
);

-- =========================
-- LESSONS
-- =========================

CREATE TABLE lessons (
     id BIGSERIAL PRIMARY KEY,
     title VARCHAR(255) NOT NULL,
     content TEXT,
     order_index INT NOT NULL,
     course_id BIGINT NOT NULL,
     CONSTRAINT fk_lessons_course
         FOREIGN KEY (course_id) REFERENCES courses(id)
);

-- =========================
-- ENROLLMENTS
-- =========================

CREATE TABLE enrollments (
     id BIGSERIAL PRIMARY KEY,
     user_id BIGINT NOT NULL,
     course_id BIGINT NOT NULL,
     status VARCHAR(50) NOT NULL,
     progress INT NOT NULL DEFAULT 0,
     enrolled_at TIMESTAMP NOT NULL DEFAULT now(),
     CONSTRAINT uq_enrollments_user_course UNIQUE (user_id, course_id),
     CONSTRAINT fk_enrollments_user
         FOREIGN KEY (user_id) REFERENCES users(id),
     CONSTRAINT fk_enrollments_course
         FOREIGN KEY (course_id) REFERENCES courses(id)
);

-- =========================
-- REVIEWS
-- =========================

CREATE TABLE reviews (
     id BIGSERIAL PRIMARY KEY,
     rating INT NOT NULL,
     comment TEXT,
     user_id BIGINT NOT NULL,
     course_id BIGINT NOT NULL,
     status VARCHAR(50) NOT NULL,
     created_at TIMESTAMP NOT NULL DEFAULT now(),
     CONSTRAINT uq_reviews_user_course UNIQUE (user_id, course_id),
     CONSTRAINT fk_reviews_user
         FOREIGN KEY (user_id) REFERENCES users(id),
     CONSTRAINT fk_reviews_course
         FOREIGN KEY (course_id) REFERENCES courses(id)
);

-- =========================
-- PAYMENTS
-- =========================

CREATE TABLE payments (
      id BIGSERIAL PRIMARY KEY,
      user_id BIGINT NOT NULL,
      course_id BIGINT NOT NULL,
      amount DECIMAL(10,2) NOT NULL,
      provider VARCHAR(50) NOT NULL,
      status VARCHAR(50) NOT NULL,
      transaction_id VARCHAR(255),
      created_at TIMESTAMP NOT NULL DEFAULT now(),
      CONSTRAINT fk_payments_user
          FOREIGN KEY (user_id) REFERENCES users(id),
      CONSTRAINT fk_payments_course
          FOREIGN KEY (course_id) REFERENCES courses(id)
);

-- =========================
-- EMAIL VERIFICATION TOKENS
-- =========================

CREATE TABLE email_verification_tokens (
       id BIGSERIAL PRIMARY KEY,
       user_id BIGINT NOT NULL,
       token VARCHAR(255) NOT NULL UNIQUE,
       expires_at TIMESTAMP NOT NULL,
       used BOOLEAN NOT NULL DEFAULT false,
       created_at TIMESTAMP NOT NULL DEFAULT now(),
       CONSTRAINT fk_email_tokens_user
           FOREIGN KEY (user_id) REFERENCES users(id)
);

-- =========================
-- REFRESH TOKENS
-- =========================

CREATE TABLE refresh_tokens (
    id BIGSERIAL PRIMARY KEY,
    user_id BIGINT NOT NULL,
    token VARCHAR(255) NOT NULL UNIQUE,
    expires_at TIMESTAMP NOT NULL,
    revoked BOOLEAN NOT NULL DEFAULT false,
    created_at TIMESTAMP NOT NULL DEFAULT now(),
    CONSTRAINT fk_refresh_tokens_user
        FOREIGN KEY (user_id) REFERENCES users(id)
);