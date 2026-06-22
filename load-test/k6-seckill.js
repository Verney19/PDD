import http from 'k6/http';
import { check, sleep } from 'k6';

export const options = {
  scenarios: {
    seckill: {
      executor: 'constant-arrival-rate',
      rate: 500,
      timeUnit: '1s',
      duration: '60s',
      preAllocatedVUs: 200,
      maxVUs: 1000,
    },
  },
  thresholds: {
    http_req_failed: ['rate<0.05'],
    http_req_duration: ['p(95)<500'],
  },
};

const BASE_URL = __ENV.BASE_URL || 'http://localhost:8080';
const TOKEN = __ENV.TOKEN;
const ACTIVITY_ID = Number(__ENV.ACTIVITY_ID || '900001');

export default function () {
  const payload = JSON.stringify({ activityId: ACTIVITY_ID });
  const params = {
    headers: {
      'Content-Type': 'application/json',
      Authorization: `Bearer ${TOKEN}`,
    },
  };

  const res = http.post(`${BASE_URL}/api/seckill/grab`, payload, params);
  check(res, {
    'response received': (r) => r.status === 200,
    'business response': (r) => r.json('code') !== undefined,
  });
  sleep(0.1);
}
