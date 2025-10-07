import http from 'k6/http';
import { check, sleep } from 'k6';
import { Rate, Trend } from 'k6/metrics';

// Custom metrics
const errorRate = new Rate('errors');
const orderDuration = new Trend('order_duration');

// Test configuration
export const options = {
  stages: [
    { duration: '1m', target: 50 },   // Ramp up to 50 users
    { duration: '2m', target: 50 },   // Stay at 50 users
    { duration: '30s', target: 300 }, // Spike to 300 users
    { duration: '2m', target: 300 },  // Stay at spike
    { duration: '1m', target: 50 },   // Ramp down to 50
    { duration: '1m', target: 0 },    // Ramp down to 0
  ],
  thresholds: {
    http_req_duration: ['p(95)<500'], // 95% of requests under 500ms
    errors: ['rate<0.1'],              // Error rate under 10%
  },
};

const BASE_URL = __ENV.BASE_URL || 'http://localhost:8080';

const products = ['PROD-001', 'PROD-002', 'PROD-003', 'PROD-004', 'PROD-005'];
const paymentMethods = ['credit_card', 'debit_card', 'paypal'];

export default function () {
  const product = products[Math.floor(Math.random() * products.length)];
  const paymentMethod = paymentMethods[Math.floor(Math.random() * paymentMethods.length)];
  const quantity = Math.floor(Math.random() * 5) + 1;

  const payload = JSON.stringify({
    productId: product,
    quantity: quantity,
    paymentMethod: paymentMethod,
  });

  const params = {
    headers: {
      'Content-Type': 'application/json',
    },
  };

  const startTime = new Date().getTime();
  const response = http.post(`${BASE_URL}/api/orders`, payload, params);
  const duration = new Date().getTime() - startTime;

  // Record custom metrics
  orderDuration.add(duration);

  // Check response
  const result = check(response, {
    'status is 201 or 200': (r) => r.status === 201 || r.status === 200,
    'response has orderId': (r) => {
      try {
        const body = JSON.parse(r.body);
        return body.orderId !== undefined;
      } catch (e) {
        return false;
      }
    },
  });

  errorRate.add(!result);

  // Think time
  sleep(Math.random() * 2 + 1); // 1-3 seconds
}

export function handleSummary(data) {
  return {
    'summary.json': JSON.stringify(data),
    stdout: textSummary(data, { indent: ' ', enableColors: true }),
  };
}

function textSummary(data, options) {
  const indent = options.indent || '';
  let output = '\n';
  
  output += `${indent}Test Summary:\n`;
  output += `${indent}  Duration: ${data.state.testRunDurationMs / 1000}s\n`;
  output += `${indent}  VUs: ${data.metrics.vus.values.max}\n`;
  output += `${indent}  Requests: ${data.metrics.http_reqs.values.count}\n`;
  output += `${indent}  Request Rate: ${data.metrics.http_reqs.values.rate.toFixed(2)}/s\n`;
  output += `${indent}  Errors: ${(data.metrics.errors.values.rate * 100).toFixed(2)}%\n`;
  output += `${indent}\n`;
  output += `${indent}Response Times:\n`;

  const getValidMetric = (metric) => {
    return metric !== undefined && metric !== null && !isNaN(metric) ? metric.toFixed(2) : 'N/A';
  }
  output += `${indent}  Min: ${getValidMetric(data.metrics.http_req_duration.values.min)}ms\n`;
  output += `${indent}  Avg: ${getValidMetric(data.metrics.http_req_duration.values.avg)}ms\n`;
  output += `${indent}  P95: ${getValidMetric(data.metrics.http_req_duration.values['p(95)'])}ms\n`;
  output += `${indent}  P99: ${getValidMetric(data.metrics.http_req_duration.values['p(99)'])}ms\n`;
  output += `${indent}  Max: ${getValidMetric(data.metrics.http_req_duration.values.max)}ms\n`;
  
  return output;
}