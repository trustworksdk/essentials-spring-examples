CREATE TABLE IF NOT EXISTS task_view (
    task_id          TEXT PRIMARY KEY,
    title            TEXT        NOT NULL,
    description      TEXT,
    priority         TEXT        NOT NULL,
    status           TEXT        NOT NULL,
    assignee         TEXT,
    tags_csv         TEXT        NOT NULL DEFAULT '',
    comments_count   INTEGER     NOT NULL DEFAULT 0,
    created_at       TIMESTAMPTZ NOT NULL,
    updated_at       TIMESTAMPTZ NOT NULL,
    last_comment_at  TIMESTAMPTZ
);

CREATE TABLE IF NOT EXISTS user_view (
    user_id       TEXT PRIMARY KEY,
    display_name  TEXT        NOT NULL,
    email         TEXT,
    password_hash TEXT,
    registered    BOOLEAN     NOT NULL DEFAULT FALSE,
    created_at    TIMESTAMPTZ NOT NULL,
    updated_at    TIMESTAMPTZ NOT NULL
);

CREATE TABLE IF NOT EXISTS task_dashboard_row (
    task_id     TEXT PRIMARY KEY,
    title       TEXT        NOT NULL,
    status      TEXT        NOT NULL,
    priority    TEXT        NOT NULL,
    assignee    TEXT,
    updated_at  TIMESTAMPTZ NOT NULL
);

CREATE TABLE IF NOT EXISTS task_comment_view (
    task_id     TEXT        NOT NULL,
    comment_id  TEXT        NOT NULL,
    content     TEXT        NOT NULL,
    created_at  TIMESTAMPTZ NOT NULL,
    PRIMARY KEY (task_id, comment_id)
);

CREATE INDEX IF NOT EXISTS idx_task_dashboard_updated_at ON task_dashboard_row(updated_at DESC);
CREATE INDEX IF NOT EXISTS idx_task_dashboard_assignee_updated_at ON task_dashboard_row(assignee, updated_at DESC);
CREATE INDEX IF NOT EXISTS idx_task_comment_task_created_at ON task_comment_view(task_id, created_at ASC);
