import http from 'k6/http';
import { check } from 'k6';
import { BASE_URL, authHeaders } from '../config.js';

/**
 * 테스트용 유저 자격증명을 생성한다 (실제 가입은 별도 seed 필요).
 * @param {number} index - VU 번호 등 고유 식별자
 * @returns {{ email: string, password: string }}
 */
export function makeUserCredentials(index) {
  return {
    email: `loadtest-user-${index}@example.com`,
    password: 'password123!',
  };
}

/**
 * 이벤트를 생성하고 publicId를 반환한다.
 * @param {string} token - 호스트 JWT 토큰
 * @param {object} overrides - CreateEventRequest 필드 오버라이드
 * @returns {string} publicId
 */
export function createEvent(token, overrides = {}) {
  const now = new Date();
  const body = {
    title: 'Load Test Event',
    capacity: 10,
    waitlistEnabled: true,
    // null = 제한 없음 (EventService: 과거 시각 불가 / RegistrationService: null이면 시작 제한 없음)
    registrationStartsAt: null,
    registrationEndsAt: new Date(now.getTime() + 3_600_000).toISOString(), // 1시간 후
    ...overrides,
  };

  const res = http.post(
    `${BASE_URL}/api/events`,
    JSON.stringify(body),
    { headers: authHeaders(token) },
  );

  check(res, { 'createEvent 200': (r) => r.status === 200 });

  return res.json('publicId');
}
