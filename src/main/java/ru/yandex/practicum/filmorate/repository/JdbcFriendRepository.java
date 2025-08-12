package ru.yandex.practicum.filmorate.repository;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;
import ru.yandex.practicum.filmorate.model.FriendshipStatus;
import ru.yandex.practicum.filmorate.model.User;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.List;
import java.util.Optional;

@Slf4j
@Repository
@RequiredArgsConstructor
public class JdbcFriendRepository implements FriendStorage {
    private final JdbcTemplate jdbcTemplate;
    private final FriendshipStatusStorage statusStorage;
    private final UserStorage userStorage;

    @Transactional
    @Override
    public void addFriend(Integer userId, Integer friendId, Integer statusId) {
        // Проверка существования пользователей
        if (userStorage.findById(userId).isEmpty()) {
            throw new IllegalArgumentException("User not found with id: " + userId);
        }
        if (userStorage.findById(friendId).isEmpty()) {
            throw new IllegalArgumentException("User not found with id: " + friendId);
        }

        // Проверка на добавление самого себя
        if (userId.equals(friendId)) {
            throw new IllegalArgumentException("User cannot add themselves as friend");
        }

        // Проверка на существующую дружбу
        if (friendshipExists(userId, friendId)) {
            throw new IllegalArgumentException("Friendship already exists");
        }

        // Получаем ID статуса PENDING
        Integer pendingStatusId = statusStorage.findByName("PENDING")
                .orElseThrow(() -> new IllegalStateException("PENDING status not found"))
                .getId();

        // Создаем запись о дружбе
        String sql = "INSERT INTO friendship (user_id, friend_id, status_id) VALUES (?, ?, ?)";
        jdbcTemplate.update(sql, userId, friendId, statusId);
    }

    @Transactional
    @Override
    public void removeFriend(Integer userId, Integer friendId) {
        // Удаляем только одно направление дружбы
        String sql = "DELETE FROM friendship WHERE user_id = ? AND friend_id = ?";
        int deleted = jdbcTemplate.update(sql, userId, friendId);
        log.debug("Deleted {} friendship record from {} to {}", deleted, userId, friendId);
    }

    @Override
    public boolean friendshipExists(int userId, int friendId) {
        String sql = "SELECT COUNT(*) > 0 FROM friendship " +
                "WHERE (user_id = ? AND friend_id = ?) OR " +
                "      (user_id = ? AND friend_id = ?)";

        return Boolean.TRUE.equals(jdbcTemplate.queryForObject(
                sql,
                Boolean.class,
                userId, friendId,
                friendId, userId
        ));
    }

    @Override
    public Optional<Integer> getFriendshipStatus(int userId, int friendId) {
        String sql = "SELECT status_id FROM friendship WHERE user_id = ? AND friend_id = ?";
        try {
            return Optional.ofNullable(jdbcTemplate.queryForObject(sql, Integer.class, userId, friendId));
        } catch (EmptyResultDataAccessException e) {
            return Optional.empty();
        }
    }

    @Override
    public List<User> findFriendsByStatus(Integer userId, String statusName) {
        Integer statusId = statusStorage.findByName(statusName)
                .orElseThrow(() -> new IllegalStateException(statusName + " status not found"))
                .getId();

        String sql = "SELECT u.* FROM users u " +
                "JOIN friendship f ON u.id = f.friend_id " +
                "WHERE f.user_id = ? AND f.status_id = ?";
        return jdbcTemplate.query(sql, this::mapRowToUser, userId, statusId);
    }

    @Override
    @Transactional
    public void confirmFriendship(Integer userId, Integer friendId) {
        // Получаем ID статусов
        Integer confirmedStatusId = statusStorage.findByName("CONFIRMED")
                .orElseThrow(() -> new IllegalStateException("CONFIRMED status not found"))
                .getId();

        // Обновляем существующую заявку
        String updateSql = "UPDATE friendship SET status_id = ? WHERE user_id = ? AND friend_id = ?";
        jdbcTemplate.update(updateSql, confirmedStatusId, friendId, userId);

        // Создаем обратную запись о дружбе
        String insertSql = "INSERT INTO friendship (user_id, friend_id, status_id) VALUES (?, ?, ?)";
        jdbcTemplate.update(insertSql, userId, friendId, confirmedStatusId);
    }

    @Override
    public boolean hasPendingRequest(Integer userId, Integer friendId) {
        String sql = "SELECT COUNT(*) > 0 FROM friendship f " +
                "JOIN friendship_status fs ON f.status_id = fs.id " +
                "WHERE f.user_id = ? AND f.friend_id = ? AND fs.name = 'PENDING'";
        return Boolean.TRUE.equals(jdbcTemplate.queryForObject(
                sql, Boolean.class, userId, friendId));
    }

    @Override
    public void updateFriendshipStatus(Integer userId, Integer friendId, Integer newStatusId) {
        String sql = "UPDATE friendship SET status_id = ? " +
                "WHERE user_id = ? AND friend_id = ?";
        jdbcTemplate.update(sql, newStatusId, userId, friendId);
    }

    @Override
    public List<User> findCommonFriends(Integer userId, Integer otherId) {
        String sql = "SELECT u.* FROM users u " +
                "JOIN friendship f1 ON u.id = f1.friend_id AND f1.user_id = ? " +
                "JOIN friendship f2 ON u.id = f2.friend_id AND f2.user_id = ? " +
                // Убрали проверку статуса, чтобы учитывать все связи
                "WHERE f1.status_id IS NOT NULL AND f2.status_id IS NOT NULL";

        return jdbcTemplate.query(sql, this::mapRowToUser, userId, otherId);
    }


    @Override
    public List<User> getFriends(Integer userId) {
        String sql = "SELECT u.* FROM users u " +
                "JOIN friendship f ON u.id = f.friend_id " +
                "WHERE f.user_id = ? AND f.status_id = (" +
                "   SELECT id FROM friendship_status WHERE name = 'CONFIRMED'" +
                ")";

        return jdbcTemplate.query(sql, this::mapRowToUser, userId);
    }

    @Override
    public List<User> getFriendsByStatus(Integer userId, String statusName) {
        String sql = "SELECT u.* FROM users u " +
                "JOIN friendship f ON u.id = f.friend_id " +
                "JOIN friendship_status fs ON f.status_id = fs.id " +
                "WHERE f.user_id = ? AND fs.name = ?";

        return jdbcTemplate.query(sql, this::mapRowToUser, userId, statusName);
    }


    @Override
    public List<User> getCommonFriends(Integer userId, Integer otherId) {
        String sql = "SELECT u.* FROM users u " +
                "WHERE u.id IN (" +
                "   SELECT f1.friend_id FROM friendship f1 " +
                "   WHERE f1.user_id = ? AND f1.status_id = (" +
                "       SELECT id FROM friendship_status WHERE name = 'CONFIRMED'" +
                "   )" +
                ") AND u.id IN (" +
                "   SELECT f2.friend_id FROM friendship f2 " +
                "   WHERE f2.user_id = ? AND f2.status_id = (" +
                "       SELECT id FROM friendship_status WHERE name = 'CONFIRMED'" +
                "   )" +
                ")";

        return jdbcTemplate.query(sql, this::mapRowToUser, userId, otherId);
    }

    @Override
    public List<User> getFriendsByUserId(Integer userId) {
        String sql = "SELECT u.*, fs.name as status_name FROM users u " +
                "JOIN friendship f ON u.id = f.friend_id " +
                "JOIN friendship_status fs ON f.status_id = fs.id " +
                "WHERE f.user_id = ?";

        return jdbcTemplate.query(sql, (rs, rowNum) -> {
            User user = mapRowToUser(rs, rowNum);
            String statusName = rs.getString("status_name");

            return user;
        }, userId);
    }

    private User mapRowToUser(ResultSet rs, int rowNum) throws SQLException {
        User user = User.builder()
                .id(rs.getInt("id"))
                .email(rs.getString("email"))
                .login(rs.getString("login"))
                .name(rs.getString("name"))
                .birthday(rs.getDate("birthday").toLocalDate())
                .friends(new HashMap<>())
                .build();


        try {
            String statusName = rs.getString("status_name");
            if (statusName != null) {
                FriendshipStatus status = statusStorage.findByName(statusName)
                        .orElseThrow(() -> new IllegalStateException("Status not found: " + statusName));
                user.getFriends().put(user.getId(), status);
            }
        } catch (SQLException ignored) {

        }

        return user;
    }

}