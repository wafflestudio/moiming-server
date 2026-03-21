import { sleep } from 'k6';
import { THRESHOLDS } from '../config.js';
import { login, logout } from '../helpers/auth.js';

export const options = {
  thresholds: THRESHOLDS,
  stages: [
    { duration: '15s', target: 25 }, // 램프업
    { duration: '1m',  target: 25 }, // 유지
    { duration: '15s', target: 0  }, // 램프다운
  ],
};

// 테스트 전 DB에 미리 생성해둔 계정 사용
// seed: loadtest-user-0@example.com ~ loadtest-user-49@example.com / password123!
export default function () {
  const index = __VU % 50;
  const email = `loadtest-user-${index}@example.com`;
  const password = 'password123!';

  const token = login(email, password);
  sleep(1);

  logout(token);
  sleep(1);
}
