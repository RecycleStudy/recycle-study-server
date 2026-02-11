/**
 * Review 등록 API 부하 테스트 (k6)
 *
 * 사전 준비:
 *   python perftest/generate-test-data.py api --count 200
 *   -> perftest/k6/users.json 생성됨
 *
 * 실행:
 *   k6 run perftest/k6/review-api-test.js
 *   k6 run --vus 50 --duration 60s perftest/k6/review-api-test.js
 *   k6 run --env LEVEL=2 perftest/k6/review-api-test.js
 *
 * 부하 레벨 (LEVEL 환경변수):
 *   1: 10 VUs,  30s (기준선)
 *   2: 50 VUs,  60s (중간 부하)
 *   3: 100 VUs, 60s (커넥션 풀 압박)
 *   4: 200 VUs, 60s (한계 탐색)
 */

import http from 'k6/http';
import { check, sleep } from 'k6';
import { SharedArray } from 'k6/data';
import { Counter, Rate, Trend } from 'k6/metrics';

const users = new SharedArray('users', function () {
    return JSON.parse(open('./users.json'));
});

const BASE_URL = __ENV.BASE_URL || 'http://localhost:8080';

const levels = {
    '1': { vus: 10, duration: '30s' },
    '2': { vus: 50, duration: '60s' },
    '3': { vus: 100, duration: '60s' },
    '4': { vus: 200, duration: '60s' },
};
const level = levels[__ENV.LEVEL] || null;

export const options = level
    ? { vus: level.vus, duration: level.duration }
    : {};

const reviewCreated = new Counter('reviews_created');
const reviewFailed = new Counter('reviews_failed');
const reviewErrorRate = new Rate('review_error_rate');
const reviewDuration = new Trend('review_duration', true);

export default function () {
    const userIndex = __VU % users.length;
    const user = users[userIndex];
    const uniqueUrl = `https://perftest.example.com/k6/${__VU}/${__ITER}/${Date.now()}`;

    const payload = JSON.stringify({
        targetUrl: uniqueUrl,
        cycle: {
            type: 'DEFAULT',
            code: 'EBBINGHAUS',
        },
    });

    const params = {
        headers: {
            'Content-Type': 'application/json',
            'X-device-Id': user.deviceId,
        },
    };

    const res = http.post(`${BASE_URL}/api/v1/reviews`, payload, params);

    const success = check(res, {
        'status is 201': (r) => r.status === 201,
    });

    if (success) {
        reviewCreated.add(1);
    } else {
        reviewFailed.add(1);
    }
    reviewErrorRate.add(!success);
    reviewDuration.add(res.timings.duration);

    sleep(0.5);
}
