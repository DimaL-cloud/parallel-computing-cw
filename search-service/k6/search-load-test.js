import http from 'k6/http';
import { check, sleep } from 'k6';
import { Trend, Rate } from 'k6/metrics';

const searchDuration = new Trend('search_duration');
const searchErrors = new Rate('search_errors');

export const options = {
    scenarios: {
        search_load: {
            executor: 'ramping-vus',
            startVUs: 0,
            stages: [
                { duration: '20s', target: 100 },
                { duration: '20s', target: 500 },
                { duration: '20s', target: 1000 },
                { duration: '1m', target: 5000 },
                { duration: '20s', target: 500 },
                { duration: '20s', target: 0 },
            ],
            gracefulRampDown: '5s',
        },
    },
    thresholds: {
        http_req_failed: ['rate<0.01'],
        http_req_duration: ['p(95)<300'],
        search_duration: ['p(95)<300'],
        search_errors: ['rate<0.01'],
    },
};

const BASE_URL = __ENV.BASE_URL || 'http://localhost:8080';
const SEARCH_TERMS = (__ENV.SEARCH_TERMS || 'good,bad,film,story,love,hate').split(',');

export default function () {
    const term = SEARCH_TERMS[Math.floor(Math.random() * SEARCH_TERMS.length)];
    const res = http.get(`${BASE_URL}/filePaths?search=${encodeURIComponent(term)}`);

    const ok = check(res, {
        'status is 200': (r) => r.status === 200,
        'content-type is json': (r) =>
            String(r.headers['Content-Type'] || '').includes('application/json'),
    });

    searchDuration.add(res.timings.duration);
    searchErrors.add(!ok);

    sleep(1);
}
