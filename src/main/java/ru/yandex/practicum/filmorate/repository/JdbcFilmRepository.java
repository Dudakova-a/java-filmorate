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
import java.util.stream.Collectors;

@Repository
@Primary
@RequiredArgsConstructor
public class JdbcFilmRepository implements FilmStorage {
    private final JdbcTemplate jdbcTemplate;

    @Override
    public List<Film> findAll() {
        String sql = "SELECT f.*, m.CODE AS mpa_name FROM films f JOIN mpa_rating m ON f.mpa_rating_id = m.id";
        List<Film> films = jdbcTemplate.query(sql, this::mapRowToFilm);

        loadGenresForFilms(films);

        return films;
    }

    private void loadGenresForFilms(List<Film> films) {
        if (films.isEmpty()) return;

        String inClause = films.stream()
                .map(f -> String.valueOf(f.getId()))
                .collect(Collectors.joining(","));

        String sql = "SELECT fg.film_id, g.* FROM film_genres fg JOIN genres g ON fg.genre_id = g.id " +
                "WHERE fg.film_id IN (" + inClause + ") ORDER BY fg.film_id, g.id";

        Map<Integer, Set<Genre>> genresByFilmId = new HashMap<>();
        jdbcTemplate.query(sql, rs -> {
            int filmId = rs.getInt("film_id");
            Genre genre = new Genre(rs.getInt("id"), rs.getString("name"));
            genresByFilmId.computeIfAbsent(filmId, k -> new LinkedHashSet<>()).add(genre);
        });

        films.forEach(film ->
                film.setGenres(genresByFilmId.getOrDefault(film.getId(), Set.of()))
        );
    }

    @Override
    public Optional<Film> findById(Integer id) {
        String filmSql = "SELECT f.*, m.CODE AS mpa_name FROM films f JOIN mpa_rating m ON f.mpa_rating_id = m.id WHERE f.id = ?";

        try {
            Film film = jdbcTemplate.queryForObject(filmSql, this::mapRowToFilm, id);

            String genresSql = "SELECT g.* FROM film_genres fg JOIN genres g ON fg.genre_id = g.id WHERE fg.film_id = ? ORDER BY g.id";
            Set<Genre> genres = new LinkedHashSet<>(jdbcTemplate.query(genresSql,
                    (rs, rowNum) -> new Genre(rs.getInt("id"), rs.getString("name")),
                    id));

            film.setGenres(genres);
            return Optional.of(film);
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
            // Убираем дубликаты жанров через Set<Genre> (сравниваем по ID)
            Set<Genre> uniqueGenres = film.getGenres().stream()
                    .collect(Collectors.toCollection(() ->
                            new TreeSet<>(Comparator.comparingInt(Genre::getId))));

            String sql = "INSERT INTO film_genres (film_id, genre_id) VALUES (?, ?)";
            jdbcTemplate.batchUpdate(sql, uniqueGenres.stream()
                    .map(genre -> new Object[]{filmId, genre.getId()})
                    .collect(Collectors.toList()));
        }

        return findById(filmId)
                .orElseThrow(() -> new IllegalStateException("Failed to retrieve saved film"));
    }

    @Override
    @Transactional
    public Film update(Film film) {
        if (!existsById(film.getId())) {
            throw new NotFoundException("Film with id " + film.getId() + " not found");
        }

        String updateSql = "UPDATE films SET name = ?, description = ?, release_date = ?, " +
                "duration = ?, mpa_rating_id = ? WHERE id = ?";
        jdbcTemplate.update(updateSql,
                film.getName(),
                film.getDescription(),
                film.getReleaseDate(),
                film.getDuration(),
                film.getMpa().getId(),
                film.getId());

        jdbcTemplate.update("DELETE FROM film_genres WHERE film_id = ?", film.getId());

        if (film.getGenres() != null && !film.getGenres().isEmpty()) {
            String sql = "INSERT INTO film_genres (film_id, genre_id) VALUES (?, ?)";
            jdbcTemplate.batchUpdate(sql, film.getGenres().stream()
                    .map(genre -> new Object[]{film.getId(), genre.getId()})
                    .collect(Collectors.toList()));
        }

        return findById(film.getId())
                .orElseThrow(() -> new IllegalStateException("Failed to retrieve updated film"));
    }


    private Film mapRowToFilm(ResultSet rs, int rowNum) throws SQLException {
        Film film = new Film();
        film.setId(rs.getInt("id"));
        film.setName(rs.getString("name"));
        film.setDescription(rs.getString("description"));
        film.setReleaseDate(rs.getDate("release_date").toLocalDate());
        film.setDuration(rs.getInt("duration"));

        Mpa mpa = new Mpa();
        mpa.setId(rs.getInt("mpa_rating_id"));
        mpa.setCode(rs.getString("mpa_name"));
        film.setMpa(mpa);

        return film;
    }

    @Override
    public void addLike(Integer filmId, Integer userId) {
        String sql = "INSERT INTO film_likes (film_id, user_id) VALUES (?, ?)";
        jdbcTemplate.update(sql, filmId, userId);
    }

    @Override
    public void delete(Integer id) {
        jdbcTemplate.update("DELETE FROM films WHERE id = ?", id);
    }

    @Override
    public void removeLike(Integer filmId, Integer userId) {
        jdbcTemplate.update(
                "DELETE FROM film_likes WHERE film_id = ? AND user_id = ?",
                filmId, userId
        );
    }

    @Override
    public List<Film> getPopularFilms(int count) {
        String sql = "SELECT f.*, m.CODE AS mpa_name, COUNT(l.user_id) AS likes_count " +
                "FROM films f " +
                "LEFT JOIN film_likes l ON f.id = l.film_id " +
                "JOIN mpa_rating m ON f.mpa_rating_id = m.id " +
                "GROUP BY f.id, m.CODE " +
                "ORDER BY likes_count DESC " +
                "LIMIT ?";
        return jdbcTemplate.query(sql, this::mapRowToFilm, count);
    }

    @Override
    public boolean hasLike(Integer filmId, Integer userId) {
        String sql = "SELECT EXISTS(SELECT 1 FROM film_likes WHERE film_id = ? AND user_id = ?)";
        return Boolean.TRUE.equals(
                jdbcTemplate.queryForObject(sql, Boolean.class, filmId, userId)
        );
    }

    @Override
    public boolean existsById(Integer filmId) {
        String sql = "SELECT EXISTS(SELECT 1 FROM films WHERE id = ?)";
        return Boolean.TRUE.equals(
                jdbcTemplate.queryForObject(sql, Boolean.class, filmId)
        );
    }
}