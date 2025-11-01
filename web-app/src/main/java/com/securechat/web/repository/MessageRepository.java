package com.securechat.web.repository;

import com.securechat.web.model.WebMessage;
import org.springframework.context.annotation.Profile;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Repository;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.List;

@Repository
@Profile("db")
public class MessageRepository {
    private static final RowMapper<WebMessage> ROW_MAPPER = new WebMessageRowMapper();
    private final JdbcTemplate jdbcTemplate;

    public MessageRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public void save(WebMessage message) {
        jdbcTemplate.update(
                "INSERT INTO messages(author, content, created_at) VALUES (?, ?, ?)",
                message.user(),
                message.text(),
                Timestamp.from(message.timestamp()));
    }

    public List<WebMessage> findRecent(int limit) {
        return jdbcTemplate.query(
                "SELECT author, content, created_at FROM messages ORDER BY created_at DESC LIMIT ?",
                ROW_MAPPER,
                limit);
    }

    private static class WebMessageRowMapper implements RowMapper<WebMessage> {
        @Override
        public WebMessage mapRow(ResultSet rs, int rowNum) throws SQLException {
            String user = rs.getString("author");
            String text = rs.getString("content");
            Timestamp ts = rs.getTimestamp("created_at");
            Instant instant = ts != null ? ts.toInstant() : Instant.now();
            return new WebMessage(user, text, instant, false, "msg");
        }
    }
}
