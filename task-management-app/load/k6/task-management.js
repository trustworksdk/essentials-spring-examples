import http from "k6/http";
import { check, sleep } from "k6";
import { Counter, Rate, Trend } from "k6/metrics";

const BASE_URL = __ENV.BASE_URL || "http://localhost:8080";
const PROFILE = (__ENV.K6_PROFILE || "smoke").toLowerCase();
const SCENARIO = (__ENV.K6_SCENARIO || "mixed").toLowerCase();
const SOAK_DURATION = __ENV.K6_SOAK_DURATION || "1h";
const CONSISTENCY_TIMEOUT_MS = Number(__ENV.K6_CONSISTENCY_TIMEOUT_MS || 4000);
const CONSISTENCY_POLL_MS = Number(__ENV.K6_CONSISTENCY_POLL_MS || 150);

const writeOps = new Counter("app_write_ops_total");
const readOps = new Counter("app_read_ops_total");
const consistencyLagMs = new Trend("app_consistency_lag_ms");
const consistencyTimeouts = new Rate("app_consistency_timeouts");

const profileMap = {
  smoke: { vus: 1, duration: "1m" },
  baseline: { vus: 10, duration: "5m" },
  stress: { vus: 40, duration: "10m" },
  soak: { vus: 10, duration: SOAK_DURATION }
};

const selected = profileMap[PROFILE] || profileMap.smoke;

export const options = {
  vus: selected.vus,
  duration: selected.duration,
  thresholds: {
    http_req_failed: ["rate<0.02"],
    http_req_duration: ["p(95)<1500", "p(99)<3500"],
    app_consistency_lag_ms: ["p(95)<2500"],
    app_consistency_timeouts: ["rate<0.05"]
  }
};

const vuState = {};

function getState() {
  if (!vuState[__VU]) {
    vuState[__VU] = {
      userId: `k6-u-${__VU}`,
      password: `Pass-${__VU}-12345`,
      csrfHeader: "X-CSRF-TOKEN",
      csrfToken: null,
      taskIds: []
    };
  }
  return vuState[__VU];
}

function parseHiddenInputValue(html, name) {
  const pattern = new RegExp(`<input[^>]*name="${name}"[^>]*value="([^"]+)"`, "i");
  const match = html.match(pattern);
  return match ? match[1] : null;
}

function parseMetaContent(html, metaName) {
  const pattern = new RegExp(`<meta[^>]*name="${metaName}"[^>]*content="([^"]+)"`, "i");
  const match = html.match(pattern);
  return match ? match[1] : null;
}

function ensureAuthenticated(state) {
  const loginPage = http.get(`${BASE_URL}/login`);
  check(loginPage, { "GET /login is 200": (r) => r.status === 200 });
  const csrfFieldName = parseHiddenInputValue(loginPage.body, "_csrf") ? "_csrf" : "_csrf";
  const loginCsrf = parseHiddenInputValue(loginPage.body, csrfFieldName);

  const createRes = http.post(
    `${BASE_URL}/users`,
    JSON.stringify({ userId: state.userId, displayName: `Load User ${__VU}` }),
    { headers: { "Content-Type": "application/json" } }
  );
  check(createRes, { "POST /users accepted": (r) => r.status >= 200 && r.status < 300 });

  const registerRes = http.post(
    `${BASE_URL}/users/${encodeURIComponent(state.userId)}/register`,
    JSON.stringify({ email: `${state.userId}@example.com`, password: state.password }),
    { headers: { "Content-Type": "application/json" } }
  );
  check(registerRes, { "POST /users/{id}/register accepted": (r) => r.status >= 200 && r.status < 300 });

  const form = {
    username: state.userId,
    password: state.password
  };
  if (loginCsrf) form._csrf = loginCsrf;

  const loginRes = http.post(`${BASE_URL}/login`, form, {
    headers: { "Content-Type": "application/x-www-form-urlencoded" },
    redirects: 0
  });
  check(loginRes, {
    "POST /login redirects": (r) => r.status === 302,
    "POST /login success redirect": (r) => (r.headers.Location || "").startsWith("/")
  });

  const root = http.get(`${BASE_URL}/`);
  check(root, { "GET / dashboard is 200": (r) => r.status === 200 });
  const token = parseMetaContent(root.body, "_csrf");
  const headerName = parseMetaContent(root.body, "_csrf_header");
  if (token) state.csrfToken = token;
  if (headerName) state.csrfHeader = headerName;
}

function api(state, method, path, body = null) {
  const headers = {};
  if (body !== null) {
    headers["Content-Type"] = "application/json";
    if (state.csrfToken) headers[state.csrfHeader] = state.csrfToken;
  }
  const url = `${BASE_URL}${path}`;
  let res;
  if (method === "GET") {
    res = http.get(url);
    readOps.add(1);
  } else {
    res = http.request(method, url, body === null ? null : JSON.stringify(body), { headers });
    writeOps.add(1);
  }
  return res;
}

function tryJson(response) {
  try {
    return response.json();
  } catch (_) {
    return null;
  }
}

function waitForConsistency(fetchFn, predicate, startedAtMs) {
  const deadline = Date.now() + CONSISTENCY_TIMEOUT_MS;
  let last = null;
  while (Date.now() < deadline) {
    last = fetchFn();
    if (predicate(last)) {
      consistencyTimeouts.add(false);
      consistencyLagMs.add(Date.now() - startedAtMs);
      return true;
    }
    sleep(CONSISTENCY_POLL_MS / 1000);
  }
  consistencyTimeouts.add(true);
  return false;
}

function createTask(state, assigneeOrNull) {
  const res = api(state, "POST", "/tasks", {
    title: `k6 task ${Date.now()}`,
    description: "k6 generated task",
    assignee: assigneeOrNull,
    priority: "MEDIUM"
  });
  check(res, { "POST /tasks accepted": (r) => r.status >= 200 && r.status < 300 });
  const body = tryJson(res);
  const taskId = body && body.taskId;
  if (taskId) state.taskIds.push(taskId);
  return taskId;
}

function ensureOneTask(state) {
  if (!state.taskIds.length) {
    const taskId = createTask(state, state.userId);
    if (taskId) return taskId;
  }
  return state.taskIds[Math.floor(Math.random() * state.taskIds.length)];
}

function addCommentAndWait(state, taskId) {
  const beforeRes = api(state, "GET", `/tasks/${encodeURIComponent(taskId)}/comments`);
  const before = beforeRes.status === 200 ? (tryJson(beforeRes) || []) : [];
  const expected = before.length + 1;

  const started = Date.now();
  const writeRes = api(state, "POST", `/tasks/${encodeURIComponent(taskId)}/comments`, { content: `k6 comment ${started}` });
  check(writeRes, { "POST /comments accepted": (r) => r.status >= 200 && r.status < 300 });

  waitForConsistency(
    () => {
      const r = api(state, "GET", `/tasks/${encodeURIComponent(taskId)}/comments`);
      return r.status === 200 ? (tryJson(r) || []) : [];
    },
    (comments) => Array.isArray(comments) && comments.length >= expected,
    started
  );
}

function toggleTagAndWait(state, taskId) {
  const tag = "k6";
  const beforeRes = api(state, "GET", `/tasks/${encodeURIComponent(taskId)}`);
  if (beforeRes.status !== 200) return;
  const before = tryJson(beforeRes) || {};
  const tags = new Set(before.tags || []);
  const shouldAdd = !tags.has(tag);
  const mode = shouldAdd ? "tags" : "untags";

  const started = Date.now();
  const writeRes = api(state, "POST", `/tasks/${encodeURIComponent(taskId)}/${mode}`, { tag });
  check(writeRes, { "POST /tags|untags accepted": (r) => r.status >= 200 && r.status < 300 });

  waitForConsistency(
    () => {
      const r = api(state, "GET", `/tasks/${encodeURIComponent(taskId)}`);
      return r.status === 200 ? (tryJson(r) || {}) : {};
    },
    (task) => {
      const taskTags = new Set(task.tags || []);
      return shouldAdd ? taskTags.has(tag) : !taskTags.has(tag);
    },
    started
  );
}

function mixedFlow(state) {
  if (Math.random() < 0.3) {
    const maybeCreate = Math.random() < 0.35;
    const taskId = maybeCreate ? createTask(state, Math.random() < 0.5 ? state.userId : null) : ensureOneTask(state);
    if (!taskId) return;
    if (Math.random() < 0.5) addCommentAndWait(state, taskId);
    if (Math.random() < 0.5) toggleTagAndWait(state, taskId);
  } else {
    const dashboard = api(state, "GET", "/tasks/dashboard");
    check(dashboard, { "GET /tasks/dashboard is 200": (r) => r.status === 200 });
    const taskId = ensureOneTask(state);
    if (!taskId) return;
    api(state, "GET", `/tasks/${encodeURIComponent(taskId)}`);
    api(state, "GET", `/tasks/${encodeURIComponent(taskId)}/comments`);
  }
}

function writeHeavyFlow(state) {
  const taskId = ensureOneTask(state);
  if (!taskId) return;
  addCommentAndWait(state, taskId);
  toggleTagAndWait(state, taskId);
  if (Math.random() < 0.5) {
    const action = Math.random() < 0.5 ? "start" : "reopen";
    api(state, "POST", `/tasks/${encodeURIComponent(taskId)}/${action}`);
  }
}

function contentionFlow(state) {
  const dashboard = api(state, "GET", "/tasks/dashboard");
  if (dashboard.status !== 200) return;
  const tasks = tryJson(dashboard) || [];
  const unassigned = tasks.filter((t) => !t.assignee);
  let taskId = null;
  if (unassigned.length) {
    taskId = unassigned[0].taskId;
  } else {
    taskId = createTask(state, null);
  }
  if (!taskId) return;
  const started = Date.now();
  const assignRes = api(state, "POST", `/tasks/${encodeURIComponent(taskId)}/assign`, { assignee: state.userId });
  check(assignRes, { "POST /assign accepted": (r) => r.status >= 200 && r.status < 300 });
  waitForConsistency(
    () => {
      const r = api(state, "GET", `/tasks/${encodeURIComponent(taskId)}`);
      return r.status === 200 ? (tryJson(r) || {}) : {};
    },
    (task) => task.assignee === state.userId || task.assignee !== null,
    started
  );
}

export default function () {
  const state = getState();
  ensureAuthenticated(state);

  if (SCENARIO === "write") {
    writeHeavyFlow(state);
  } else if (SCENARIO === "contention") {
    contentionFlow(state);
  } else {
    mixedFlow(state);
  }

  sleep(0.2);
}
