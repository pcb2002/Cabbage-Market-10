import http from 'k6/http';
import { check, sleep } from 'k6';

const BASE_URL = __ENV.BASE_URL || 'http://localhost:8080';

export const options = {
    stages: [
        { duration: '30s', target: 5 },
        { duration: '1m', target: 20 },
        { duration: '30s', target: 0 },
    ],

    thresholds: {
        'http_req_failed{endpoint:popular_search}': ['rate<0.01'],
        'http_req_duration{endpoint:popular_search}': ['p(95)<500'],
        'checks{endpoint:popular_search}': ['rate>0.99'],
    },
};

export default function () {
    const res = http.get(`${BASE_URL}/api/search/popular`, {
        headers: {
            Accept: 'application/json',
        },
        tags: {
            endpoint: 'popular_search',
            name: 'GET /api/search/popular',
        },
    });

    check(
        res,
        {
            'popular search status is 200': (r) => r.status === 200,
            'popular search response is JSON': (r) =>
                (r.headers['Content-Type'] || '').includes('application/json'),
            'popular search response has keywords array': (r) => {
                try {
                    const body = JSON.parse(r.body);
                    return Array.isArray(body.data.keywords);
                } catch (e) {
                    return false;
                }
            },
        },
        {
            endpoint: 'popular_search',
        }
    );

    sleep(0.5 + Math.random() * 1.5);
}
