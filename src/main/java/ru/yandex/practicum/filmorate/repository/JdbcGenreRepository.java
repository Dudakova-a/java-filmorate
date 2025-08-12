package ru.yandex.practicum.filmorate.repository;

import lombok.RequiredArgsConstructor;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;
import ru.yandex.practicum.filmorate.model.Genre;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;
import java.util.Optional;

@Repository
@RequiredArgsConstructor
public class JdbcGenreRepository implements GenreStorage {
    private final JdbcTemplate jdbcTemplate;

    @Override
    public List<Genre> findAll() {
        String sql = "SELECT * FROM GENRES ORDER BY id";
        return jdbcTemplate.query(sql, this::mapRowToGenre);
    }

    @Override
    public Optional<Genre> findById(Integer id) {
        String sql = "SELECT * FROM GENRES WHERE id = ?";
        try {
            return Optional.ofNullable(jdbcTemplate.queryForObject(sql, this::mapRowToGenre, id));
        } catch (EmptyResultDataAccessException e) {
            return Optional.empty();
        }
    }

    @Override
    public List<Genre> getFilmGenres(Integer filmId) {
        String sql = "SELECT g.* FROM GENRES g " +
                "JOIN FILM_GENRES fg ON g.id = fg.genre_id " +
                "WHERE fg.film_id = ? " +
                "ORDER BY g.id ASC";
        return jdbcTemplate.query(sql, this::mapRowToGenre, filmId);
    }

    @Override
    public void addGenreToFilm(Integer filmId, Integer genreId) {
        // Проверяем существование жанра
        if (findById(genreId).isEmpty()) {
            throw new IllegalArgumentException("Genre with id " + genreId + " not found");
        }

        // Проверяем, что связь уже не существует
        String checkSql = "SELECT COUNT(*) FROM FILM_GENRES WHERE film_id = ? AND genre_id = ?";
        boolean exists = jdbcTemplate.queryForObject(checkSql, Integer.class, filmId, genreId) > 0;

        if (!exists) {
            String insertSql = "INSERT INTO FILM_GENRES (film_id, genre_id) VALUES (?, ?)";
            jdbcTemplate.update(insertSql, filmId, genreId);
        }
    }

    @Override
    public void removeGenreFromFilm(Integer filmId, Integer genreId) {
        String sql = "DELETE FROM FILM_GENRES WHERE film_id = ? AND genre_id = ?";
        jdbcTemplate.update(sql, filmId, genreId);
    }

    private Genre mapRowToGenre(ResultSet rs, int rowNum) throws SQLException {
        return new Genre(
                rs.getInt("id"),
                rs.getString("name")
        );
    }
}