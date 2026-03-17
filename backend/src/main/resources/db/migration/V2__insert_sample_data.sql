-- =========================
-- USERS
-- =========================

INSERT INTO users (email, password_hash, full_name, role, status)
VALUES
    ('alice@student.com', 'hashed_pw_1', 'Alice Student', 'STUDENT', 'ACTIVE'),
    ('bob@instructor.com', 'hashed_pw_2', 'Bob Instructor', 'INSTRUCTOR', 'ACTIVE'),
    ('admin@platform.com', 'hashed_pw_3', 'Admin User', 'ADMIN', 'ACTIVE');

-- =========================
-- CATEGORIES
-- =========================

INSERT INTO categories (name) VALUES
                                  ('Spring'),
                                  ('Backend'),
                                  ('Java');

-- =========================
-- COURSES
-- =========================

INSERT INTO courses (title, description, price, instructor_id, image_url, status)
VALUES
    ('Spring Boot Fundamentals', 'Learn Spring Boot from scratch', 49.99, 2,
     'https://cdn.example.com/spring-boot.png', 'PUBLISHED');

-- =========================
-- COURSE CATEGORIES
-- =========================

INSERT INTO course_categories (course_id, category_id) VALUES
                                                           (1, 1),
                                                           (1, 2),
                                                           (1, 3);

-- =========================
-- LESSONS
-- =========================

INSERT INTO lessons (title, content, order_index, course_id) VALUES
                                                                 ('Introduction', 'Welcome to the course', 1, 1),
                                                                 ('Spring Boot Basics', 'Core concepts explained', 2, 1),
                                                                 ('REST APIs', 'Building RESTful APIs', 3, 1);

-- =========================
-- ENROLLMENTS
-- =========================

INSERT INTO enrollments (user_id, course_id, status, progress)
VALUES (1, 1, 'ACTIVE', 30);

-- =========================
-- REVIEWS
-- =========================

INSERT INTO reviews (rating, comment, user_id, course_id, status)
VALUES (5, 'Excellent course!', 1, 1, 'VISIBLE');

-- =========================
-- PAYMENTS
-- =========================

INSERT INTO payments (user_id, course_id, amount, provider, status, transaction_id)
VALUES (1, 1, 49.99, 'STRIPE', 'PAID', 'txn_123456');