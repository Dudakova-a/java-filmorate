package ru.yandex.practicum.filmorate.repository;

import ru.yandex.practicum.filmorate.model.Genre;

import java.util.List;
import java.util.Optional;

/**
 * Интерфейс хранилища для работы с жанрами фильмов.
 */
public interface GenreStorage {

    List<Genre> findAll();

    Optional<Genre> findById(Integer id);

    List<Genre> getFilmGenres(Integer filmId);

    void addGenreToFilm(Integer filmId, Integer genreId);

    void removeGenreFromFilm(Integer filmId, Integer genreId);
}