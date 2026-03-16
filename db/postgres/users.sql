BEGIN;
INSERT INTO "users" ("id", "username", "phone", "email", "hashed_password")
VALUES (1, 'admin', '13800000000', 'admin@example.com',
        '{bcrypt}$2a$10$IlYJ6qn4gyXUL.CCLzlN4ujjzlfI.3UbB0VQrYSUmiaPKpcnxdU.G');
COMMIT;
