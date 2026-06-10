package com.parkvision.cps.repository;

import com.parkvision.cps.domain.auth.AppUser;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Repository;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/**
 * Standalone JDBC store for back-office / owner accounts. Kept independent from
 * {@link ParkVisionRepository} so authentication does not couple to the business
 * persistence profile.
 */
@Repository
public class AuthUserRepository {
    private final JdbcTemplate jdbcTemplate;

    public AuthUserRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    private static final String COLUMNS =
            "id, username, display_name, role, password_hash, salt, status, owner_id, last_login, created_at";

    private static final RowMapper<AppUser> MAPPER = (ResultSet rs, int rowNum) -> new AppUser(
            rs.getLong("id"),
            rs.getString("username"),
            rs.getString("display_name"),
            rs.getString("role"),
            rs.getString("password_hash"),
            rs.getString("salt"),
            rs.getString("status"),
            toLocalDateTime(rs.getTimestamp("last_login")),
            toLocalDateTime(rs.getTimestamp("created_at")),
            rs.getString("owner_id")
    );

    public long count() {
        Integer total = jdbcTemplate.queryForObject("select count(*) from app_user", Integer.class);
        return total == null ? 0 : total;
    }

    public Optional<AppUser> findByUsername(String username) {
        List<AppUser> rows = jdbcTemplate.query(
                "select " + COLUMNS + " from app_user where username = ?",
                MAPPER,
                username
        );
        return rows.stream().findFirst();
    }

    public Optional<AppUser> findById(long id) {
        List<AppUser> rows = jdbcTemplate.query(
                "select " + COLUMNS + " from app_user where id = ?",
                MAPPER,
                id
        );
        return rows.stream().findFirst();
    }

    public boolean existsByUsername(String username) {
        Integer total = jdbcTemplate.queryForObject(
                "select count(*) from app_user where username = ?", Integer.class, username);
        return total != null && total > 0;
    }

    public List<AppUser> findAll() {
        return jdbcTemplate.query(
                "select " + COLUMNS + " from app_user order by id",
                MAPPER
        );
    }

    public void insert(AppUser user) {
        jdbcTemplate.update(
                "insert into app_user (username, display_name, role, password_hash, salt, status, owner_id, created_at) "
                        + "values (?, ?, ?, ?, ?, ?, ?, ?)",
                user.username(),
                user.displayName(),
                user.role(),
                user.passwordHash(),
                user.salt(),
                user.status() == null ? "ACTIVE" : user.status(),
                user.ownerId(),
                Timestamp.valueOf(user.createdAt() == null ? LocalDateTime.now() : user.createdAt())
        );
    }

    public void updateStatus(long id, String status) {
        jdbcTemplate.update("update app_user set status = ? where id = ?", status, id);
    }

    public void updateRole(long id, String role) {
        jdbcTemplate.update("update app_user set role = ? where id = ?", role, id);
    }

    public void updateDisplayName(long id, String displayName) {
        jdbcTemplate.update("update app_user set display_name = ? where id = ?", displayName, id);
    }

    public void updatePassword(long id, String passwordHash, String salt) {
        jdbcTemplate.update(
                "update app_user set password_hash = ?, salt = ? where id = ?",
                passwordHash, salt, id);
    }

    public void touchLastLogin(String username) {
        jdbcTemplate.update(
                "update app_user set last_login = ? where username = ?",
                Timestamp.valueOf(LocalDateTime.now()),
                username
        );
    }

    private static LocalDateTime toLocalDateTime(Timestamp timestamp) {
        return timestamp == null ? null : timestamp.toLocalDateTime();
    }
}
