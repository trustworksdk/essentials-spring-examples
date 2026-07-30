const activityLog = document.getElementById("activity-log");
const csrfToken = document.querySelector('meta[name="_csrf"]')?.content;
const csrfHeader = document.querySelector('meta[name="_csrf_header"]')?.content || "X-CSRF-TOKEN";
const currentUser = document.querySelector('meta[name="current-user"]')?.content || "";
const toastHost = document.getElementById("toast-host");

const state = { allTasks: [], myTasks: [], unassignedTasks: [] };

function errorMessage(error) {
  return error instanceof Error ? error.message : String(error);
}

function logActivity(message, payload) {
  const time = new Date().toLocaleTimeString();
  const line = `[${time}] ${message}`;
  activityLog.textContent = payload === undefined
    ? `${line}\n${activityLog.textContent}`
    : `${line}\n${JSON.stringify(payload, null, 2)}\n${activityLog.textContent}`;
}

function toast(message, type = "ok") {
  if (!toastHost) return;
  const item = document.createElement("div");
  item.className = `toast ${type}`;
  item.textContent = message;
  toastHost.prepend(item);
  window.setTimeout(() => item.remove(), 3200);
}

function openTaskView(taskId) {
  window.location.assign(`/tasks/${encodeURIComponent(taskId)}/view`);
}

async function api(path, options = {}) {
  const method = options.method || "GET";
  const headers = { ...(options.headers || {}) };
  if (options.body !== undefined) headers["Content-Type"] = "application/json";
  if (method !== "GET" && method !== "HEAD" && csrfToken) headers[csrfHeader] = csrfToken;

  const response = await fetch(path, {
    method,
    headers,
    body: options.body === undefined ? undefined : JSON.stringify(options.body),
    credentials: "same-origin"
  });

  const text = await response.text();
  let payload = text;
  try { payload = text ? JSON.parse(text) : {}; } catch (_) {}
  if (!response.ok) throw new Error(typeof payload === "string" ? payload : JSON.stringify(payload));
  return payload;
}

function statusActions(status) {
  if (status === "OPEN") return ["start"];
  if (status === "IN_PROGRESS") return ["complete"];
  if (status === "DONE") return ["reopen"];
  return [];
}

function shortDate(iso) {
  return iso ? new Date(iso).toLocaleString() : "-";
}

function updateMetrics() {
  document.getElementById("my-task-count").textContent = String(state.myTasks.length);
  document.getElementById("unassigned-count").textContent = String(state.unassignedTasks.length);
  document.getElementById("done-count").textContent = String(state.myTasks.filter(t => t.status === "DONE").length);
}

function renderMyTasks() {
  const body = document.getElementById("my-tasks-body");
  if (!state.myTasks.length) {
    body.innerHTML = `<tr><td colspan="5" class="muted-cell">No assigned tasks yet.</td></tr>`;
    return;
  }

  body.innerHTML = state.myTasks.map(task => {
    const buttons = statusActions(task.status)
      .map(action => `<button class="mini" data-task-action="${action}" data-task-id="${task.taskId}">${action}</button>`)
      .join("");
    return `<tr>
      <td>
        <div class="queue-title">${task.title}</div>
        <div class="tiny">${task.taskId}</div>
        <div><button class="mini" data-open-task-view="${task.taskId}">Open Task View</button></div>
      </td>
      <td><span class="status status-${task.status}">${task.status}</span></td>
      <td>${task.priority}</td>
      <td>${shortDate(task.updatedAt)}</td>
      <td>${buttons || '<span class="tiny">No action</span>'}</td>
    </tr>`;
  }).join("");
}

function renderUnassignedTasks() {
  const container = document.getElementById("unassigned-list");
  const term = document.getElementById("unassigned-search").value.trim().toLowerCase();
  const filtered = state.unassignedTasks.filter(task =>
    !term || task.title.toLowerCase().includes(term) || task.taskId.toLowerCase().includes(term)
  );

  if (!filtered.length) {
    container.innerHTML = `<p class="muted">No matching unassigned tasks.</p>`;
    return;
  }

  container.innerHTML = filtered.map(task => `
    <article class="queue-item">
      <div>
        <p class="queue-title">${task.title}</p>
        <p class="tiny">${task.taskId} · ${task.priority} · ${task.status}</p>
      </div>
      <div class="queue-actions">
        <button class="mini" data-open-task-view="${task.taskId}">Open Task View</button>
        <button class="mini solid" data-assign-me="${task.taskId}">Assign To Me</button>
      </div>
    </article>
  `).join("");
}

async function refreshDashboard() {
  state.allTasks = await api("/tasks/dashboard");
  if (!Array.isArray(state.allTasks)) throw new Error("Dashboard response was not a task list.");
  state.myTasks = state.allTasks.filter(task => task.assignee === currentUser);
  state.unassignedTasks = state.allTasks.filter(task => !task.assignee);
  renderMyTasks();
  renderUnassignedTasks();
  updateMetrics();
  logActivity("Dashboard refreshed");
}

async function runTaskAction(taskId, action) {
  await api(`/tasks/${encodeURIComponent(taskId)}/${action}`, { method: "POST" });
  toast(`Task ${action} updated`, "ok");
  logActivity(`Action '${action}' applied on task ${taskId}`);
  await refreshDashboard();
}

async function assignToMe(taskId) {
  await api(`/tasks/${encodeURIComponent(taskId)}/assign`, { method: "POST", body: { assignee: currentUser } });
  toast(`Assigned task to ${currentUser}`, "ok");
  logActivity(`Assigned task ${taskId} to ${currentUser}`);
  await refreshDashboard();
}

function bindForms() {
  document.getElementById("create-task-form").addEventListener("submit", async (event) => {
    event.preventDefault();
    const formEl = event.currentTarget;
    const submitButton = formEl.querySelector('button[type="submit"]');
    try {
      if (submitButton) submitButton.disabled = true;
      const data = Object.fromEntries(new FormData(formEl).entries());
      const created = await api("/tasks", {
        method: "POST",
        body: {
          title: data.title,
          description: data.description || null,
          assignee: data.assignee || null,
          priority: data.priority
        }
      });
      formEl.reset();
      toast("Task created", "ok");
      logActivity(`Task created: ${created.taskId ?? "unknown"}`, created);
      await refreshDashboard();
    } catch (error) {
      toast("Could not create task", "error");
      logActivity(`ERROR creating task: ${errorMessage(error)}`);
    } finally {
      if (submitButton) submitButton.disabled = false;
    }
  });

}

function bindActions() {
  document.addEventListener("click", async (event) => {
    try {
      const target = event.target;
      if (!(target instanceof HTMLElement)) return;

      const openTaskId = target.getAttribute("data-open-task-view");
      if (openTaskId) {
        openTaskView(openTaskId);
        return;
      }

      const assignTaskId = target.getAttribute("data-assign-me");
      if (assignTaskId) {
        await assignToMe(assignTaskId);
        return;
      }

      const action = target.getAttribute("data-task-action");
      const actionTaskId = target.getAttribute("data-task-id");
      if (action && actionTaskId) {
        await runTaskAction(actionTaskId, action);
      }
    } catch (error) {
      toast("Action failed", "error");
      logActivity(`ERROR action handling: ${errorMessage(error)}`);
    }
  });
}

document.getElementById("refresh-my-tasks").addEventListener("click", async () => {
  try { await refreshDashboard(); } catch (error) { logActivity(`ERROR refreshing dashboard: ${errorMessage(error)}`); }
});
document.getElementById("unassigned-search").addEventListener("input", renderUnassignedTasks);

bindForms();
bindActions();
window.addEventListener("unhandledrejection", (event) => logActivity(`ERROR unhandled promise: ${errorMessage(event.reason)}`));
window.addEventListener("error", (event) => logActivity(`ERROR runtime: ${event.message}`));
refreshDashboard().catch(error => logActivity(`ERROR bootstrapping: ${errorMessage(error)}`));
