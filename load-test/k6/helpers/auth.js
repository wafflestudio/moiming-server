import http from 'k6/http';
import { check } from 'k6';
import { BASE_URL, DEFAULT_HEADERS } from '../config.js';

/**
 * 로그인 후 JWT 토큰을 반환한다.
 * @param {string} email
 * @param {string} password
 * @returns {string} JWT token
 */
export function login(email, password) {
  const res = http.post(
    `${BASE_URL}/api/auth/login`,
    JSON.stringify({ email, password }),
    { headers: DEFAULT_HEADERS },
  );

  check(res, { 'login 200': (r) => r.status === 200 });

  return res.json('token');
}

/**
 * 로그아웃 (토큰 블랙리스트 처리)
 * @param {string} token
 */
export function logout(token) {
  const res = http.post(
    `${BASE_URL}/api/auth/logout`,
    null,
    { headers: { Authorization: `Bearer ${token}` } },
  );

  check(res, { 'logout 204': (r) => r.status === 204 });
}