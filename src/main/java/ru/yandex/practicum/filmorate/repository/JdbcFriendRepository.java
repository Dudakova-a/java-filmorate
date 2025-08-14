package ru.yandex.practicum.filmorate.repository;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;
import ru.yandex.practicum.filmorate.model.FriendshipStatus;
import ru.yandex.practicum.filmorate.model.User;

import java.util.List;
import java.util.Optional;

@Slf4j
@Repository
@RequiredArgsConstructor
public class JdbcFriendRepository implements FriendStorage {
    private final JdbcTemplate jdbcTemplate;

    private static final RowMapper<User> USER_MAPPER = (rs, rowNum) -> User.builder()
            .id(rs.getInt("id"))
            .email(rs.getString("email"))
            .login(rs.getString("login"))
            .name(rs.getString("name"))
            .birthday(rs.getDate("birthday").toLocalDate())
            .build();

    @Transactional
    @Override
    public void addFriend(Integer userId, Integer friendId, Integer statusId) {
        validateUsers(userId, friendId);
        checkStatusExists(statusId);

        if (friendshipExists(userId, friendId)) {
            throw new IllegalArgumentException("Friendship already exists between users: " + userId + " and " + friendId);
        }

        String sql = "INSERT INTO friendship (user_id, friend_id, status_id) VALUES (?, ?, ?)";
        jdbcTemplate.update(sql, userId, friendId, statusId);
        log.debug("Added friendship: user {} → friend {} with status {}", userId, friendId, statusId);
    }

    @Transactional
    @Override
    public void removeFriend(Integer userId, Integer friendId) {
        String sql = "DELETE FROM friendship WHERE user_id = ? AND friend_id = ?";
        int rowsAffected = jdbcTemplate.update(sql, userId, friendId);
        if (rowsAffected == 0) {
            log.warn("No friendship found to delete: user {} → friend {}", userId, friendId);
        } else {
            log.debug("Removed friendship: user {} → friend {}", userId, friendId);
        }
    }

    @Override
    public boolean friendshipExists(int userId, int friendId) {
        String sql = "SELECT COUNT(*) FROM friendship WHERE user_id = ? AND friend_id = ?";
        Integer count = jdbcTemplate.queryForObject(sql, Integer.class, userId, friendId);
        return count != null && count > 0;
    }

    @Transactional
    @Override
    public void confirmFriendship(Integer userId, Integer friendId) {
        Integer confirmedStatusId = getStatusIdByName("CONFIRMED")
                .orElseThrow(() -> new IllegalStateException("CONFIRMED status not found"));

        // Обновляем существующую заявку
        String updateSql = "UPDATE friendship SET status_id = ? WHERE user_id = ? AND friend_id = ?";
        jdbcTemplate.update(updateSql, confirmedStatusId, friendId, userId);

        // Создаем обратную запись о дружбе
        String insertSql = "INSERT INTO friendship (user_id, friend_id, status_id) VALUES (?, ?, ?)";
        jdbcTemplate.update(insertSql, userId, friendId, confirmedStatusId);
        log.debug("Confirmed friendship between {} and {}", userId, friendId);
    }

    @Override
    public Optional<Integer> getFriendshipStatus(int userId, int friendId) {
        String sql = "SELECT status_id FROM friendship WHERE user_id = ? AND friend_id = ?";
        try {
            return Optional.of(jdbcTemplate.queryForObject(sql, Integer.class, userId, friendId));
        } catch (EmptyResultDataAccessException e) {
            return Optional.empty();
        }
    }

    @Override
    public List<User> findFriendsByStatus(Integer userId, String statusName) {
        return getFriendsByStatus(userId, statusName);
    }

    @Override
    public boolean hasPendingRequest(Integer userId, Integer friendId) {
        String sql = "SELECT COUNT(*) FROM friendship f " +
                "JOIN friendship_status fs ON f.status_id = fs.id " +
                "WHERE f.user_id = ? AND f.friend_id = ? AND fs.name = 'PENDING'";
        Integer count = jdbcTemplate.queryForObject(sql, Integer.class, userId, friendId);
        return count != null && count > 0;
    }

    @Override
    public void updateFriendshipStatus(Integer userId, Integer friendId, Integer newStatusId) {
        checkStatusExists(newStatusId);

        String sql = "UPDATE friendship SET status_id = ? WHERE user_id = ? AND friend_id = ?";
        int rowsAffected = jdbcTemplate.update(sql, newStatusId, userId, friendId);
        if (rowsAffected == 0) {
            throw new IllegalArgumentException("Friendship not found between users: " + userId + " and " + friendId);
        }
        log.debug("Updated friendship status: user {} → friend {} to status {}", userId, friendId, newStatusId);
    }

    @Override
    public List<User> findCommonFriends(Integer userId, Integer otherId) {
        String sql = "SELECT u.* FROM users u " +
                "JOIN friendship f1 ON u.id = f1.friend_id AND f1.user_id = ? " +
                "JOIN friendship f2 ON u.id = f2.friend_id AND f2.user_id = ? " +
                "JOIN friendship_status fs ON f1.status_id = fs.id AND fs.name = 'CONFIRMED'";

        return jdbcTemplate.query(sql, USER_MAPPER, userId, otherId);
    }

    @Override
    public List<User> getFriends(Integer userId) {
        String sql = "SELECT u.*, fs.id as status_id, fs.name as status_name FROM users u " +
                "JOIN friendship f ON u.id = f.friend_id " +
                "JOIN friendship_status fs ON f.status_id = fs.id " +
                "WHERE f.user_id = ?";

        return jdbcTemplate.query(sql, (rs, rowNum) -> {
            User user = USER_MAPPER.mapRow(rs, rowNum);
            FriendshipStatus status = new FriendshipStatus(
                    rs.getInt("status_id"),
                    rs.getString("status_name")
            );
            user.addFriend(userId, status);
            return user;
        }, userId);
    }

    @Override
    public List<User> getFriendsByStatus(Integer userId, String statusName) {
        String sql = "SELECT u.* FROM users u " +
                "JOIN friendship f ON u.id = f.friend_id " +
                "JOIN friendship_status fs ON f.status_id = fs.id " +
                "WHERE f.user_id = ? AND fs.name = ?";

        return jdbcTemplate.query(sql, USER_MAPPER, userId, statusName);
    }

    @Override
    public List<User> getCommonFriends(Integer userId, Integer otherId) {
        return findCommonFriends(userId, otherId);
    }

    @Override
    public List<User> getFriendsByUserId(Integer userId) {
        return getFriends(userId);
    }

    // Вспомогательные методы
    private void validateUsers(Integer userId, Integer friendId) {
        if (userId.equals(friendId)) {
            throw new IllegalArgumentException("User cannot be friend with themselves");
        }
        if (!userExists(userId)) {
            throw new IllegalArgumentException("User not found with id: " + userId);
        }
        if (!userExists(friendId)) {
            throw new IllegalArgumentException("User not found with id: " + friendId);
        }
    }

    private boolean userExists(Integer userId) {
        String sql = "SELECT COUNT(*) FROM users WHERE id = ?";
        Integer count = jdbcTemplate.queryForObject(sql, Integer.class, userId);
        return count != null && count > 0;
    }

    private Optional<Integer> getStatusIdByName(String statusName) {
        String sql = "SELECT id FROM friendship_status WHERE name = ?";
        try {
            return Optional.of(jdbcTemplate.queryForObject(sql, Integer.class, statusName));
        } catch (EmptyResultDataAccessException e) {
            return Optional.empty();
        }
    }

    private void checkStatusExists(Integer statusId) {
        String sql = "SELECT COUNT(*) FROM friendship_status WHERE id = ?";
        Integer count = jdbcTemplate.queryForObject(sql, Integer.class, statusId);
        if (count == null || count == 0) {
            throw new IllegalArgumentException("Status not found with id: " + statusId);
        }
    }
}