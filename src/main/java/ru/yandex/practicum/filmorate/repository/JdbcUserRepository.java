package ru.yandex.practicum.filmorate.repository;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.jdbc.support.KeyHolder;
import org.springframework.stereotype.Repository;
import ru.yandex.practicum.filmorate.model.FriendshipStatus;
import ru.yandex.practicum.filmorate.model.User;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.*;

/**
 * JDBC-реализация репозитория для работы с пользователями.
 */
@Slf4j
@Repository
@RequiredArgsConstructor
public class JdbcUserRepository implements UserStorage {
    private final JdbcTemplate jdbcTemplate;
    private final FriendshipStatusStorage statusStorage;

    @Override
    public List<User> findAll() {
        String sql = "SELECT * FROM users";
        List<User> users = jdbcTemplate.query(sql, this::mapRowToUser);
        users.forEach(this::loadFriends);
        return users;
    }

    @Override
    public Optional<User> findById(Integer id) {
        String sql = "SELECT * FROM users WHERE id = ?";
        try {
            User user = jdbcTemplate.queryForObject(sql, this::mapRowToUser, id);
            loadFriends(user);
            return Optional.of(user);
        } catch (EmptyResultDataAccessException e) {
            return Optional.empty();
        }
    }

    @Override
    public User save(User user) {
        String sql = "INSERT INTO users (email, login, name, birthday) VALUES (?, ?, ?, ?)";
        KeyHolder keyHolder = new GeneratedKeyHolder();

        jdbcTemplate.update(connection -> {
            PreparedStatement ps = connection.prepareStatement(sql, new String[]{"id"});
            ps.setString(1, user.getEmail());
            ps.setString(2, user.getLogin());
            ps.setString(3, user.getName());
            ps.setDate(4, java.sql.Date.valueOf(user.getBirthday()));  // Используем java.sql.Date
            return ps;
        }, keyHolder);

        user.setId(Objects.requireNonNull(keyHolder.getKey()).intValue());
        return user;
    }

    @Override
    public User update(User user) {
        String sql = "UPDATE users SET email = ?, login = ?, name = ?, birthday = ? WHERE id = ?";
        int updated = jdbcTemplate.update(sql,
                user.getEmail(),
                user.getLogin(),
                user.getName(),
                user.getBirthday(),
                user.getId());

        if (updated == 0) {
            throw new IllegalArgumentException("User with id " + user.getId() + " not found");
        }

        return findById(user.getId()).orElseThrow();
    }

    @Override
    public void delete(Integer id) {
        // Сначала удаляем связи дружбы
        jdbcTemplate.update("DELETE FROM friendship WHERE user_id = ? OR friend_id = ?", id, id);
        // Затем удаляем пользователя
        jdbcTemplate.update("DELETE FROM users WHERE id = ?", id);
    }

    private User mapRowToUser(ResultSet rs, int rowNum) throws SQLException {
        return User.builder()
                .id(rs.getInt("id"))
                .email(rs.getString("email"))
                .login(rs.getString("login"))
                .name(rs.getString("name"))
                .birthday(rs.getDate("birthday").toLocalDate())
                .build();
    }

    private void loadFriends(User user) {
        if (user == null || user.getId() == null) return;

        String sql = "SELECT f.friend_id, fs.name as status_name " +
                "FROM friendship f " +
                "JOIN friendship_status fs ON f.status_id = fs.id " +
                "WHERE f.user_id = ?";

        if (user.getFriends() == null) {
            user.setFriends(new HashMap<>());
        } else {
            user.getFriends().clear();
        }

        jdbcTemplate.query(sql, (rs, rowNum) -> {
            int friendId = rs.getInt("friend_id");
            String statusName = rs.getString("status_name");

            if (!rs.wasNull() && statusName != null) {
                FriendshipStatus status = statusStorage.findByName(statusName)
                        .orElseThrow(() -> new IllegalStateException("Status not found: " + statusName));
                user.addFriend(friendId, status);
            }
            return null;
        }, user.getId());
    }

    @Override
    public boolean existsByEmail(String email) {
        String sql = "SELECT EXISTS(SELECT 1 FROM users WHERE email = ?)";
        return Boolean.TRUE.equals(jdbcTemplate.queryForObject(sql, Boolean.class, email));
    }

    @Override
    public boolean existsById(Integer id) {
        String sql = "SELECT EXISTS(SELECT 1 FROM users WHERE id = ?)";
        return Boolean.TRUE.equals(jdbcTemplate.queryForObject(sql, Boolean.class, id));
    }
}