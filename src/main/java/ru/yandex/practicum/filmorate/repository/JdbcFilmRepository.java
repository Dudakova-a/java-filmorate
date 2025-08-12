package ru.yandex.practicum.filmorate.repository;

import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Primary;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.simple.SimpleJdbcInsert;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;
import ru.yandex.practicum.filmorate.exception.NotFoundException;
import ru.yandex.practicum.filmorate.model.Film;
import ru.yandex.practicum.filmorate.model.Genre;
import ru.yandex.practicum.filmorate.model.Mpa;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.*;

@Repository
@Primary
@RequiredArgsConstructor
public class JdbcFilmRepository implements FilmStorage {
    private final JdbcTemplate jdbcTemplate;
    private final GenreStorage genreStorage;
    private final MpaStorage mpaStorage;

    @Override
    public List<Film> findAll() {
        String sql = "SELECT f.id, f.name, f.description, f.release_date, f.duration, " +
                "f.mpa_rating_id, m.code AS mpa_code, m.description AS mpa_description " +
                "FROM films f JOIN mpa_rating m ON f.mpa_rating_id = m.id " +
                "ORDER BY f.id ASC";
        return jdbcTemplate.query(sql, this::mapRowToFilm);
    }

    @Override
    public Optional<Film> findById(Integer id) {
        String sql = "SELECT f.id, f.name, f.description, f.release_date, f.duration, " +
                "f.mpa_rating_id, m.code, m.description AS mpa_description " +
                "FROM films f JOIN mpa_rating m ON f.mpa_rating_id = m.id " +
                "WHERE f.id = ?";
        try {
            return Optional.ofNullable(jdbcTemplate.queryForObject(sql, this::mapRowToFilm, id));
        } catch (EmptyResultDataAccessException e) {
            return Optional.empty();
        }
    }

    @Override
    @Transactional
    public Film save(Film film) {
        if (film.getMpa() == null || film.getMpa().getId() == null) {
            throw new IllegalArgumentException("MPA rating must be specified");
        }

        SimpleJdbcInsert simpleJdbcInsert = new SimpleJdbcInsert(jdbcTemplate)
                .withTableName("films")
                .usingGeneratedKeyColumns("id");

        Map<String, Object> values = new HashMap<>();
        values.put("name", film.getName());
        values.put("description", film.getDescription());
        values.put("release_date", film.getReleaseDate());
        values.put("duration", film.getDuration());
        values.put("mpa_rating_id", film.getMpa().getId());

        Integer filmId = simpleJdbcInsert.executeAndReturnKey(values).intValue();
        film.setId(filmId);

        if (film.getGenres() != null && !film.getGenres().isEmpty()) {
            Set<Genre> uniqueGenres = new HashSet<>(film.getGenres());
            for (Genre genre : uniqueGenres) {
                if (genre.getId() == null) {
                    throw new IllegalArgumentException("Genre ID must be specified");
                }
                if (genreStorage.findById(genre.getId()).isEmpty()) {
                    throw new IllegalArgumentException("Genre with id " + genre.getId() + " not found");
                }
                genreStorage.addGenreToFilm(filmId, genre.getId());
            }
        }

        return findById(filmId)
                .orElseThrow(() -> new IllegalStateException("Failed to retrieve saved film with id: " + filmId));
    }

    @Override
    @Transactional
    public Film update(Film film) {
        findById(film.getId()).orElseThrow(() ->
                new NotFoundException("Film with id " + film.getId() + " not found"));

        if (film.getMpa() == null) {
            throw new IllegalArgumentException("MPA rating must be specified");
        }

        String updateSql = "UPDATE films SET name = ?, description = ?, release_date = ?, " +
                "duration = ?, mpa_rating_id = ? WHERE id = ?";
        int rowsUpdated = jdbcTemplate.update(updateSql,
                film.getName(),
                film.getDescription(),
                film.getReleaseDate(),
                film.getDuration(),
                film.getMpa().getId(),
                film.getId());

        if (rowsUpdated == 0) {
            throw new IllegalStateException("No film was updated - film with id " + film.getId() + " not found");
        }

        jdbcTemplate.update("DELETE FROM FILM_GENRES WHERE film_id = ?", film.getId());

        if (film.getGenres() != null && !film.getGenres().isEmpty()) {
            film.getGenres().forEach(genre -> {
                if (genre.getId() == null) {
                    throw new IllegalArgumentException("Genre ID must be specified");
                }
                genreStorage.addGenreToFilm(film.getId(), genre.getId());
            });
        }

        return findById(film.getId())
                .orElseThrow(() -> new IllegalStateException("Failed to retrieve updated film"));
    }

    @Override
    public void delete(Integer id) {
        jdbcTemplate.update("DELETE FROM films WHERE id = ?", id);
    }

    @Override
    public void addLike(Integer filmId, Integer userId) {
        String sql = "INSERT INTO FILM_LIKES (film_id, user_id) VALUES (?, ?)";
        jdbcTemplate.update(sql, filmId, userId);
    }

    @Override
    public void removeLike(Integer filmId, Integer userId) {
        String sql = "DELETE FROM FILM_LIKES WHERE film_id = ? AND user_id = ?";
        jdbcTemplate.update(sql, filmId, userId);
    }

    @Override
    public List<Film> getPopularFilms(int count) {
        String sql = "SELECT f.id, f.name, f.description, f.release_date, f.duration, " +
                "f.mpa_rating_id, m.code AS mpa_code, m.description AS mpa_description, " +
                "COUNT(l.user_id) AS likes_count " +
                "FROM films f " +
                "LEFT JOIN FILM_LIKES l ON f.id = l.film_id " +
                "JOIN mpa_rating m ON f.mpa_rating_id = m.id " +
                "GROUP BY f.id, f.name, f.description, f.release_date, f.duration, f.mpa_rating_id, m.code, m.description " +
                "ORDER BY likes_count DESC " +
                "LIMIT ?";

        return jdbcTemplate.query(sql, this::mapRowToFilm, count);
    }

    private Film mapRowToFilm(ResultSet rs, int rowNum) throws SQLException {
        Film film = new Film();
        film.setId(rs.getInt("id"));
        film.setName(rs.getString("name"));
        film.setDescription(rs.getString("description"));
        film.setReleaseDate(rs.getDate("release_date").toLocalDate());
        film.setDuration(rs.getInt("duration"));

        // Загружаем MPA
        Mpa mpa = new Mpa();
        mpa.setId(rs.getInt("mpa_rating_id"));
        mpa.setCode(rs.getString("code")); // Используем code вместо name
        film.setMpa(mpa);

        // Загрузка жанров с сохранением порядка
        List<Genre> genres = genreStorage.getFilmGenres(film.getId());
        film.setGenres(new LinkedHashSet<>(genres));

        return film;
    }

    @Override
    public boolean hasLike(Integer filmId, Integer userId) {
        String sql = "SELECT EXISTS(SELECT 1 FROM FILM_LIKES WHERE film_id = ? AND user_id = ?)";
        return Boolean.TRUE.equals(jdbcTemplate.queryForObject(
                sql,
                Boolean.class,
                filmId,
                userId
        ));
    }

    @Override
    public boolean existsById(Integer filmId) {
        String sql = "SELECT EXISTS(SELECT 1 FROM films WHERE id = ?)";
        return Boolean.TRUE.equals(
                jdbcTemplate.queryForObject(sql, Boolean.class, filmId)
        );
    }
}