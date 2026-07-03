import http from 'k6/http';
import { check, sleep } from 'k6';

const BASE_URL = __ENV.BASE_URL || 'http://localhost:8080';

// 실제 테스트 계정으로 변경
const TEST_EMAIL = __ENV.K6_EMAIL || 'user@example.com';
const TEST_PASSWORD = __ENV.K6_PASSWORD || 'password123!';

// API_VERSION: v1(캐시 없음) | v2(Redis 캐시)
const API_VERSION = __ENV.API_VERSION || 'v2';
// ANONYMOUS=true 이면 로그인을 건너뛰고 비로그인 상태로 요청(캐시 적중 경로 검증용)
const ANONYMOUS = __ENV.ANONYMOUS === 'true';

const keywords = [
    '배추',
    '감자',
    '아이폰',
    '의자',
    '노트북',
    '자전거',
    '책상',
    '운동화',
];

export const options = {
    stages: [
        { duration: '30s', target: 5 },
        { duration: '1m', target: 20 },
        { duration: '30s', target: 0 },
    ],

    thresholds: {
        'http_req_failed{endpoint:search}': ['rate<0.01'],
        'http_req_duration{endpoint:search}': ['p(95)<500'],
        'checks{endpoint:search}': ['rate>0.99'],
    },
};

// 부하 테스트 시작 전 1회 로그인 후 Authorization 헤더 값 확보 (ANONYMOUS=true면 생략)
export function setup() {
    if (ANONYMOUS) {
        return { authorization: null };
    }

    const loginResponse = http.post(
        `${BASE_URL}/api/auth/login`,
        JSON.stringify({
            email: TEST_EMAIL,
            password: TEST_PASSWORD,
        }),
        {
            headers: {
                'Content-Type': 'application/json',
                Accept: 'application/json',
            },
            tags: {
                endpoint: 'login',
                name: 'POST /api/auth/login',
            },
        }
    );

    const authorization =
        loginResponse.headers.Authorization ||
        loginResponse.headers.authorization;

    const loginSuccess = check(
        loginResponse,
        {
            'login status is 200': (res) => res.status === 200,
            'Authorization header exists': () => !!authorization,
        },
        {
            endpoint: 'login',
        }
    );

    if (!loginSuccess) {
        throw new Error(
            `로그인 실패\nstatus=${loginResponse.status}\nheaders=${JSON.stringify(loginResponse.headers)}\nbody=${loginResponse.body}`
        );
    }

    // 서버가 "Bearer 토큰" 형태로 내려주면 그대로 사용.
    // 토큰 문자열만 내려줘도 Bearer를 붙여서 사용.
    const bearerToken = authorization.startsWith('Bearer ')
        ? authorization
        : `Bearer ${authorization}`;

    return {
        authorization: bearerToken,
    };
}

export default function (data) {
    const keyword = keywords[Math.floor(Math.random() * keywords.length)];

    const url =
        `${BASE_URL}/api/${API_VERSION}/items/search` +
        `?keyword=${encodeURIComponent(keyword)}` +
        `&tradeStatus=ON_SALE` +
        `&likedOnly=false` +
        `&page=0` +
        `&size=20`;

    const headers = {
        Accept: 'application/json',
    };
    if (data.authorization) {
        headers.Authorization = data.authorization;
    }

    const searchResponse = http.get(url, {
        headers,
        tags: {
            endpoint: 'search',
            name: `GET /api/${API_VERSION}/items/search`,
        },
    });

    check(
        searchResponse,
        {
            'search status is 200': (res) => res.status === 200,
            'search response is JSON': (res) =>
                (res.headers['Content-Type'] || '').includes('application/json'),
            'search response body exists': (res) => res.body.length > 0,
        },
        {
            endpoint: 'search',
        }
    );

    sleep(0.5 + Math.random() * 1.5);
}
