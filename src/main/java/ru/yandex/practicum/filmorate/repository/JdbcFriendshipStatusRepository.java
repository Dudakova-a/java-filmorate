package ru.yandex.practicum.filmorate.repository;

import lombok.RequiredArgsConstructor;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;
import ru.yandex.practicum.filmorate.model.FriendshipStatus;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;
import java.util.Optional;

@Repository
@RequiredArgsConstructor
public class JdbcFriendshipStatusRepository implements FriendshipStatusStorage {
    private final JdbcTemplate jdbcTemplate;

    @Override
    public Optional<FriendshipStatus> findById(int id) {
        String sql = "SELECT * FROM friendship_status WHERE id = ?";
        try {
            return Optional.ofNullable(jdbcTemplate.queryForObject(sql, this::mapRowToStatus, id));
        } catch (EmptyResultDataAccessException e) {
            return Optional.empty();
        }
    }

    @Override
    public Optional<FriendshipStatus> findByName(String name) {
        String sql = "SELECT * FROM friendship_status WHERE name = ?";
        try {
            return Optional.ofNullable(jdbcTemplate.queryForObject(sql, this::mapRowToStatus, name));
        } catch (EmptyResultDataAccessException e) {
            return Optional.empty();
        }
    }

    @Override
    public List<FriendshipStatus> findAll() {
        String sql = "SELECT * FROM friendship_status";
        return jdbcTemplate.query(sql, this::mapRowToStatus);
    }

    private FriendshipStatus mapRowToStatus(ResultSet rs, int rowNum) throws SQLException {
        return FriendshipStatus.builder()
                .id(rs.getInt("id"))
                .name(rs.getString("name"))
                .build();
    }
}