package dk.trustworks.essentials.spring.examples.taskmanagement.readmodel;

import dk.trustworks.essentials.spring.examples.taskmanagement.domain.Priority;
import dk.trustworks.essentials.spring.examples.taskmanagement.domain.TaskStatus;
import dk.trustworks.essentials.spring.examples.taskmanagement.domain.events.CommentAdded;
import dk.trustworks.essentials.spring.examples.taskmanagement.domain.events.TaskAssigned;
import dk.trustworks.essentials.spring.examples.taskmanagement.domain.events.TaskCompleted;
import dk.trustworks.essentials.spring.examples.taskmanagement.domain.events.TaskCreated;
import dk.trustworks.essentials.spring.examples.taskmanagement.domain.events.TaskDescriptionUpdated;
import dk.trustworks.essentials.spring.examples.taskmanagement.domain.events.TaskReopened;
import dk.trustworks.essentials.spring.examples.taskmanagement.domain.events.TaskStarted;
import dk.trustworks.essentials.spring.examples.taskmanagement.domain.events.TaskTagged;
import dk.trustworks.essentials.spring.examples.taskmanagement.domain.events.TaskUnassigned;
import dk.trustworks.essentials.spring.examples.taskmanagement.domain.events.TaskUntagged;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Component
public class TaskReadModels {
    private final JdbcTemplate jdbcTemplate;

    public TaskReadModels(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public Optional<TaskView> findViewByTaskId(String taskId) {
        var result = jdbcTemplate.query("SELECT task_id, title, description, priority, status, assignee, tags_csv, comments_count, created_at, updated_at, last_comment_at " +
                                                "FROM task_view WHERE task_id = ?",
                                        (rs, rowNum) -> mapTaskView(rs),
                                        taskId);
        return result.stream().findFirst();
    }

    public List<TaskDashboardRow> dashboardRows() {
        return jdbcTemplate.query("SELECT task_id, title, status, priority, assignee, updated_at FROM task_dashboard_row ORDER BY updated_at DESC",
                                  (rs, rowNum) -> mapDashboardRow(rs));
    }

    public List<TaskDashboardRow> dashboardRowsForAssignee(String assignee) {
        return jdbcTemplate.query("SELECT task_id, title, status, priority, assignee, updated_at " +
                                          "FROM task_dashboard_row WHERE assignee = ? ORDER BY updated_at DESC",
                                  (rs, rowNum) -> mapDashboardRow(rs),
                                  assignee);
    }

    public List<TaskCommentView> commentsForTask(String taskId) {
        return jdbcTemplate.query("SELECT task_id, comment_id, content, created_at FROM task_comment_view WHERE task_id = ? ORDER BY created_at ASC",
                                  (rs, rowNum) -> new TaskCommentView(rs.getString("task_id"),
                                                                      rs.getString("comment_id"),
                                                                      rs.getString("content"),
                                                                      rs.getTimestamp("created_at").toInstant()),
                                  taskId);
    }

    public void on(TaskCreated event) {
        var taskId = event.taskId().toString();
        jdbcTemplate.update("INSERT INTO task_view(task_id, title, description, priority, status, assignee, tags_csv, comments_count, created_at, updated_at, last_comment_at) " +
                                    "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?) " +
                                    "ON CONFLICT (task_id) DO NOTHING",
                            taskId,
                            event.title(),
                            event.description(),
                            event.priority().name(),
                            TaskStatus.OPEN.name(),
                            event.assignee() != null ? event.assignee().toString() : null,
                            "",
                            0,
                            ts(event.createdAt()),
                            ts(event.createdAt()),
                            null);

        jdbcTemplate.update("INSERT INTO task_dashboard_row(task_id, title, status, priority, assignee, updated_at) " +
                                    "VALUES (?, ?, ?, ?, ?, ?) " +
                                    "ON CONFLICT (task_id) DO NOTHING",
                            taskId,
                            event.title(),
                            TaskStatus.OPEN.name(),
                            event.priority().name(),
                            event.assignee() != null ? event.assignee().toString() : null,
                            ts(event.createdAt()));
    }

    public void on(TaskStarted event) {
        updateStatus(event.taskId().toString(), TaskStatus.IN_PROGRESS, event.startedAt());
    }

    public void on(TaskCompleted event) {
        updateStatus(event.taskId().toString(), TaskStatus.DONE, event.completedAt());
    }

    public void on(TaskReopened event) {
        updateStatus(event.taskId().toString(), TaskStatus.OPEN, event.reopenedAt());
    }

    public void on(TaskAssigned event) {
        var taskId = event.taskId().toString();
        var assignedAt = event.assignedAt();
        var assignee = event.assignee().toString();
        jdbcTemplate.update("UPDATE task_view SET assignee = ?, updated_at = ? WHERE task_id = ?",
                            assignee, ts(assignedAt), taskId);
        jdbcTemplate.update("UPDATE task_dashboard_row SET assignee = ?, updated_at = ? WHERE task_id = ?",
                            assignee, ts(assignedAt), taskId);
    }

    public void on(TaskUnassigned event) {
        var taskId = event.taskId().toString();
        var unassignedAt = event.unassignedAt();
        jdbcTemplate.update("UPDATE task_view SET assignee = NULL, updated_at = ? WHERE task_id = ?",
                            ts(unassignedAt), taskId);
        jdbcTemplate.update("UPDATE task_dashboard_row SET assignee = NULL, updated_at = ? WHERE task_id = ?",
                            ts(unassignedAt), taskId);
    }

    public void on(CommentAdded event) {
        var taskId = event.taskId().toString();
        var createdAt = event.createdAt();
        jdbcTemplate.update("UPDATE task_view SET comments_count = comments_count + 1, last_comment_at = ?, updated_at = ? WHERE task_id = ?",
                            ts(createdAt), ts(createdAt), taskId);
        jdbcTemplate.update("UPDATE task_dashboard_row SET updated_at = ? WHERE task_id = ?",
                            ts(createdAt), taskId);
        jdbcTemplate.update("INSERT INTO task_comment_view(task_id, comment_id, content, created_at) VALUES (?, ?, ?, ?) " +
                                    "ON CONFLICT (task_id, comment_id) DO NOTHING",
                            taskId,
                            event.commentId().toString(),
                            event.content(),
                            ts(createdAt));
    }

    public void on(TaskTagged event) {
        mutateTags(event.taskId().toString(), event.taggedAt(), tags -> tags.add(event.tag()));
    }

    public void on(TaskUntagged event) {
        mutateTags(event.taskId().toString(), event.untaggedAt(), tags -> tags.remove(event.tag()));
    }

    public void on(TaskDescriptionUpdated event) {
        jdbcTemplate.update("UPDATE task_view SET description = ?, updated_at = ? WHERE task_id = ?",
                            event.description(), ts(event.updatedAt()), event.taskId().toString());
        jdbcTemplate.update("UPDATE task_dashboard_row SET updated_at = ? WHERE task_id = ?",
                            ts(event.updatedAt()), event.taskId().toString());
    }

    private void updateStatus(String taskId, TaskStatus newStatus, Instant updatedAt) {
        jdbcTemplate.update("UPDATE task_view SET status = ?, updated_at = ? WHERE task_id = ?",
                            newStatus.name(), ts(updatedAt), taskId);
        jdbcTemplate.update("UPDATE task_dashboard_row SET status = ?, updated_at = ? WHERE task_id = ?",
                            newStatus.name(), ts(updatedAt), taskId);
    }

    private void mutateTags(String taskId, Instant updatedAt, java.util.function.Consumer<Set<String>> mutation) {
        var updated = jdbcTemplate.query("SELECT tags_csv FROM task_view WHERE task_id = ?",
                                         (rs, rowNum) -> {
                                             var tags = parseTagsCsv(rs.getString("tags_csv"));
                                             mutation.accept(tags);
                                             return toTagsCsv(tags);
                                         },
                                         taskId);

        if (!updated.isEmpty()) {
            var tagsCsv = updated.get(0);
            jdbcTemplate.update("UPDATE task_view SET tags_csv = ?, updated_at = ? WHERE task_id = ?",
                                tagsCsv, ts(updatedAt), taskId);
            jdbcTemplate.update("UPDATE task_dashboard_row SET updated_at = ? WHERE task_id = ?",
                                ts(updatedAt), taskId);
        }
    }

    private TaskView mapTaskView(ResultSet rs) throws SQLException {
        var lastCommentTs = rs.getTimestamp("last_comment_at");
        return new TaskView(rs.getString("task_id"),
                            rs.getString("title"),
                            rs.getString("description"),
                            Priority.valueOf(rs.getString("priority")),
                            TaskStatus.valueOf(rs.getString("status")),
                            rs.getString("assignee"),
                            parseTagsCsv(rs.getString("tags_csv")),
                            rs.getInt("comments_count"),
                            rs.getTimestamp("created_at").toInstant(),
                            rs.getTimestamp("updated_at").toInstant(),
                            lastCommentTs != null ? lastCommentTs.toInstant() : null);
    }

    private TaskDashboardRow mapDashboardRow(ResultSet rs) throws SQLException {
        return new TaskDashboardRow(rs.getString("task_id"),
                                    rs.getString("title"),
                                    TaskStatus.valueOf(rs.getString("status")),
                                    Priority.valueOf(rs.getString("priority")),
                                    rs.getString("assignee"),
                                    rs.getTimestamp("updated_at").toInstant());
    }

    private static Set<String> parseTagsCsv(String tagsCsv) {
        if (tagsCsv == null || tagsCsv.isBlank()) {
            return new LinkedHashSet<>();
        }
        return Stream.of(tagsCsv.split(","))
                     .map(String::trim)
                     .filter(s -> !s.isEmpty())
                     .collect(Collectors.toCollection(LinkedHashSet::new));
    }

    private static String toTagsCsv(Set<String> tags) {
        if (tags.isEmpty()) {
            return "";
        }
        return String.join(",", tags);
    }

    private static Timestamp ts(Instant instant) {
        return Timestamp.from(instant);
    }
}
