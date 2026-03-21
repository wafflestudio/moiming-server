-- 부하 테스트 완료 후 seed 유저 정리
DELETE FROM users WHERE email LIKE 'loadtest-%@example.com';