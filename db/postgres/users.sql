BEGIN;
INSERT INTO "users" ("id", "username", "hashed_password")
VALUES (1, 'admin', '{bcrypt}$2a$10$IlYJ6qn4gyXUL.CCLzlN4ujjzlfI.3UbB0VQrYSUmiaPKpcnxdU.G');
COMMIT;