// Step 8 load test — hottest endpoints on the Render free tier (512 MB / 0.1 CPU).
//
// Targets the three endpoints the optimization plan called out:
//   1. Orders list      GET /api/v1/orders
//   2. Admin dashboard  GET /api/v1/dashboard/admin
//   3. Product catalog  GET /api/v1/products   (public, @Cacheable)
//
// Usage:
//   k6 run backend/loadtest/k6-hot-endpoints.js
//
//   # against a deployed instance with real creds:
//   k6 run \
//     -e BASE_URL=https://your-app.onrender.com \
//     -e EMAIL=admin@pkcorporate.com \
//     -e PASSWORD='your-password' \
//     backend/loadtest/k6-hot-endpoints.js
//
// Capturing query counts (plan Step 1 verification):
//   Run the server with HIBERNATE_STATS=true and watch the Hibernate
//   "Session Metrics" log lines while this test runs. Query/collection-fetch
//   counts per request should stay flat as VUs ramp (no N+1 regression).
//
// Record p50/p95 latency + req/s from the k6 end-of-run summary, before and
// after each optimization, to prove the change.

import http from 'k6/http';
import { check, group, sleep } from 'k6';
import { Trend } from 'k6/metrics';

const BASE_URL = __ENV.BASE_URL || 'http://localhost:9090';
const API = `${BASE_URL}/api`;
const EMAIL = __ENV.EMAIL || 'admin@pkcorporate.com';
const PASSWORD = __ENV.PASSWORD || 'admin123';

// Per-endpoint latency so p50/p95 can be read individually, not just aggregate.
const ordersLatency = new Trend('ep_orders_list', true);
const dashboardLatency = new Trend('ep_admin_dashboard', true);
const productsLatency = new Trend('ep_product_catalog', true);

export const options = {
  scenarios: {
    // Gentle ramp — 0.1 CPU saturates fast; this finds the knee without a flood.
    hot_endpoints: {
      executor: 'ramping-vus',
      startVUs: 1,
      stages: [
        { duration: '30s', target: 5 },
        { duration: '1m', target: 10 },
        { duration: '30s', target: 0 },
      ],
      gracefulStop: '10s',
    },
  },
  thresholds: {
    http_req_failed: ['rate<0.01'],
    // Plan targets: keep p95 well under a free-tier-cold-start budget.
    'ep_orders_list': ['p(95)<800'],
    'ep_admin_dashboard': ['p(95)<800'],
    'ep_product_catalog': ['p(95)<400'], // cached, should be fastest
  },
};

// Log in once per VU and reuse the access token for the iteration loop.
export function setup() {
  const res = http.post(
    `${API}/auth/login`,
    JSON.stringify({ email: EMAIL, password: PASSWORD }),
    { headers: { 'Content-Type': 'application/json' } }
  );

  const ok = check(res, {
    'login 200': (r) => r.status === 200,
    'login returned accessToken': (r) => {
      try {
        return !!r.json('data.accessToken');
      } catch (_e) {
        return false;
      }
    },
  });

  if (!ok) {
    throw new Error(
      `Login failed (status ${res.status}). Set -e EMAIL / -e PASSWORD to a valid admin account.`
    );
  }

  return { token: res.json('data.accessToken') };
}

export default function (data) {
  const authHeaders = {
    headers: {
      'Content-Type': 'application/json',
      Authorization: `Bearer ${data.token}`,
    },
  };

  group('orders list', () => {
    const res = http.get(`${API}/v1/orders`, authHeaders);
    ordersLatency.add(res.timings.duration);
    check(res, { 'orders 200': (r) => r.status === 200 });
  });

  group('admin dashboard', () => {
    const res = http.get(`${API}/v1/dashboard/admin`, authHeaders);
    dashboardLatency.add(res.timings.duration);
    check(res, { 'dashboard 200': (r) => r.status === 200 });
  });

  group('product catalog', () => {
    // Public endpoint — no auth required, exercises the Caffeine cache.
    const res = http.get(`${API}/v1/products`);
    productsLatency.add(res.timings.duration);
    check(res, { 'products 200': (r) => r.status === 200 });
  });

  sleep(1);
}
