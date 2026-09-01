const activityLog = document.getElementById("activity-log");
const toastHost = document.getElementById("toast-host");
const csrfToken = document.querySelector('meta[name="_csrf"]')?.content;
const csrfHeader = document.querySelector('meta[name="_csrf_header"]')?.content || "X-CSRF-TOKEN";
const taskId = document.querySelector('meta[name="task-id"]')?.content || "";
const currentUser = document.querySelector('meta[name="current-user"]')?.content || "";

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

function format(iso) {
  if (!iso) return "-";
  return new Date(iso).toLocaleString();
}

function renderTask(task) {
  document.getElementById("task-title").textContent = task.title;
  document.getElementById("task-id-label").textContent = task.taskId;
  document.getElementById("task-status").textContent = task.status;
  document.getElementById("task-priority").textContent = task.priority;
  document.getElementById("task-assignee").textContent = task.assignee || "unassigned";
  document.getElementById("task-created").textContent = format(task.createdAt);
  document.getElementById("task-updated").textContent = format(task.updatedAt);
  document.getElementById("task-last-comment").textContent = format(task.lastCommentAt);
  document.getElementById("description-input").value = task.description || "";
  updateActionStates(task);

  const tagsList = document.getElementById("tags-list");
  if (!task.tags || !task.tags.length) {
    tagsList.innerHTML = `<p class="muted">No tags.</p>`;
  } else {
    tagsList.innerHTML = task.tags.map(tag => `<p class="queue-item">${tag}</p>`).join("");
  }
}

function updateActionStates(task) {
  const transitions = {
    OPEN: { start: true, complete: false, reopen: false },
    IN_PROGRESS: { start: false, complete: true, reopen: false },
    DONE: { start: false, complete: false, reopen: true }
  };
  const allowed = transitions[task.status] || { start: false, complete: false, reopen: false };
  for (const button of document.querySelectorAll(".editor-actions button[data-action]")) {
    const action = button.getAttribute("data-action");
    button.disabled = !allowed[action];
  }

  const assignBtn = document.getElementById("assign-to-me-btn");
  const unassignBtn = document.getElementById("unassign-btn");
  const hint = document.getElementById("assignment-hint");

  const assignee = task.assignee || "";
  if (!assignee) {
    assignBtn.hidden = false;
    assignBtn.disabled = false;
    unassignBtn.hidden = true;
    hint.textContent = "Task is currently unassigned.";
    return;
  }

  assignBtn.hidden = true;
  unassignBtn.hidden = false;
  unassignBtn.disabled = false;
  hint.textContent = assignee === currentUser ? `Assigned to you (${currentUser}).` : `Assigned to ${assignee}.`;
}

function renderComments(comments) {
  const list = document.getElementById("comments-list");
  if (!comments.length) {
    list.innerHTML = `<p class="muted">No comments yet.</p>`;
    return;
  }
  list.innerHTML = comments.map(c => `
    <article class="queue-item">
      <p class="queue-title">${c.content}</p>
      <p class="tiny">${format(c.createdAt)} · ${c.commentId}</p>
    </article>
  `).join("");
}

async function refresh() {
  const [task, comments] = await Promise.all([
    api(`/tasks/${encodeURIComponent(taskId)}`),
    api(`/tasks/${encodeURIComponent(taskId)}/comments`)
  ]);
  renderTask(task);
  renderComments(comments);
  return { task, comments };
}

async function refreshUntil(predicate, maxAttempts = 16, delayMs = 160) {
  let lastError = null;
  for (let i = 0; i < maxAttempts; i++) {
    try {
      const snapshot = await refresh();
      if (predicate(snapshot)) {
        return snapshot;
      }
    } catch (error) {
      lastError = error;
    }
    await new Promise(resolve => setTimeout(resolve, delayMs));
  }
  throw lastError || new Error("Task view did not reflect latest write in time.");
}

document.getElementById("description-form").addEventListener("submit", async (event) => {
  event.preventDefault();
  try {
    const formEl = event.currentTarget;
    const description = String(new FormData(formEl).get("description") || "").trim();
    await api(`/tasks/${encodeURIComponent(taskId)}/description`, { method: "POST", body: { description } });
    await refreshUntil(({ task }) => (task.description || "") === (description || ""));
    toast("Description saved", "ok");
    logActivity("Description updated");
  } catch (error) {
    toast("Could not save description", "error");
    logActivity(`ERROR description: ${error.message}`);
  }
});

document.getElementById("comment-form").addEventListener("submit", async (event) => {
  event.preventDefault();
  try {
    const formEl = event.currentTarget;
    const content = String(new FormData(formEl).get("content") || "");
    const before = await refresh();
    const expectedCount = (before.comments?.length || 0) + 1;
    await api(`/tasks/${encodeURIComponent(taskId)}/comments`, { method: "POST", body: { content } });
    formEl.reset();
    await refreshUntil(({ comments }) => (comments?.length || 0) >= expectedCount);
    toast("Comment added", "ok");
    logActivity("Comment added");
  } catch (error) {
    toast("Could not add comment", "error");
    logActivity(`ERROR comment: ${error.message}`);
  }
});

document.getElementById("tag-form").addEventListener("submit", async (event) => {
  event.preventDefault();
  try {
    const formEl = event.currentTarget;
    const data = new FormData(formEl);
    const tag = String(data.get("tag") || "").trim().toLowerCase();
    const mode = String(data.get("mode") || "tags");
    await api(`/tasks/${encodeURIComponent(taskId)}/${mode}`, { method: "POST", body: { tag } });
    await refreshUntil(({ task }) => {
      const tags = new Set(task.tags || []);
      return mode === "tags" ? tags.has(tag) : !tags.has(tag);
    });
    toast("Tag updated", "ok");
    logActivity(`Tag mode '${mode}' applied`);
  } catch (error) {
    toast("Could not update tag", "error");
    logActivity(`ERROR tag: ${error.message}`);
  }
});

document.getElementById("assign-to-me-btn").addEventListener("click", async () => {
  try {
    await api(`/tasks/${encodeURIComponent(taskId)}/assign`, { method: "POST", body: { assignee: currentUser } });
    await refreshUntil(({ task }) => task.assignee === currentUser);
    toast(`Assigned to ${currentUser}`, "ok");
    logActivity(`Assigned to ${currentUser}`);
  } catch (error) {
    toast("Could not assign", "error");
    logActivity(`ERROR assign: ${error.message}`);
  }
});

document.getElementById("unassign-btn").addEventListener("click", async () => {
  try {
    await api(`/tasks/${encodeURIComponent(taskId)}/unassign`, { method: "POST" });
    await refreshUntil(({ task }) => !task.assignee);
    toast("Task unassigned", "ok");
    logActivity("Task unassigned");
  } catch (error) {
    toast("Could not unassign", "error");
    logActivity(`ERROR unassign: ${error.message}`);
  }
});

for (const button of document.querySelectorAll(".editor-actions button[data-action]")) {
  button.addEventListener("click", async () => {
    try {
      const action = button.getAttribute("data-action");
      const expectedStatus = action === "start" ? "IN_PROGRESS" : action === "complete" ? "DONE" : "OPEN";
      await api(`/tasks/${encodeURIComponent(taskId)}/${action}`, { method: "POST" });
      await refreshUntil(({ task }) => task.status === expectedStatus);
      toast(`Task ${action} updated`, "ok");
      logActivity(`Lifecycle action '${action}' applied`);
    } catch (error) {
      toast(`Could not ${button.getAttribute("data-action")}`, "error");
      logActivity(`ERROR lifecycle: ${error.message}`);
    }
  });
}

refresh()
  .then(() => logActivity("Task loaded"))
  .catch(error => {
    toast("Could not load task", "error");
    logActivity(`ERROR loading task: ${error.message}`);
  });
