import http from 'k6/http';
import { check, sleep } from 'k6';
import { BASE_URL, THRESHOLDS, authHeaders } from '../config.js';
import { login } from '../helpers/auth.js';
import { createEvent } from '../helpers/data.js';

export const options = {
  thresholds: THRESHOLDS,
  stages: [
    { duration: '30s', target: 30 }, // 램프업
    { duration: '2m',  target: 30 }, // 유지
    { duration: '15s', target: 0   }, // 램프다운
  ],
};

export function setup() {
  const token = login('loadtest-host@example.com', 'password123!');
  const publicId = createEvent(token);
  return { token, publicId };
}

export default function ({ token, publicId }) {

  // 이벤트 상세 조회
  const detailRes = http.get(
    `${BASE_URL}/api/events/${publicId}`,
    { headers: authHeaders(token) },
  );
  check(detailRes, { 'event detail 200': (r) => r.status === 200 });

  sleep(0.5);

  // 신청 목록 조회
  const regRes = http.get(
    `${BASE_URL}/api/events/${publicId}/registrations`,
    { headers: authHeaders(token) },
  );
  check(regRes, { 'registrations 200': (r) => r.status === 200 });

  sleep(1);
}
