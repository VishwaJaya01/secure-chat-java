package com.securechat.web.repository;

import com.securechat.web.model.UserView;
import org.springframework.context.annotation.Profile;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Repository;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;

@Repository
@Profile("db")
public class UserRepository {
    private static final RowMapper<UserView> ROW_MAPPER = new UserViewRowMapper();
    private final JdbcTemplate jdbcTemplate;

    public UserRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public void upsert(String username) {
        jdbcTemplate.update(
                "MERGE INTO chat_users(user_id, display_name) KEY(user_id) VALUES (?, ?)",
                username,
                username);
    }

    public List<UserView> findAll() {
        return jdbcTemplate.query("SELECT user_id, display_name FROM chat_users ORDER BY user_id", ROW_MAPPER);
    }

    private static class UserViewRowMapper implements RowMapper<UserView> {
        @Override
        public UserView mapRow(ResultSet rs, int rowNum) throws SQLException {
            String user = rs.getString("user_id");
            return new UserView(user, "online");
        }
    }
}
