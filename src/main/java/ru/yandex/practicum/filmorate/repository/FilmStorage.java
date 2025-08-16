package ru.yandex.practicum.filmorate.repository;

import ru.yandex.practicum.filmorate.model.Film;

import java.util.List;
import java.util.Optional;

/**
 * Интерфейс для работы с хранилищем фильмов.
 * Определяет основные CRUD-операции и специфичные методы для работы с фильмами.
 */
public interface FilmStorage {

    List<Film> findAll();

    Optional<Film> findById(Integer id);

    Film save(Film film);

    Film update(Film film);

    void delete(Integer id);

    void addLike(Integer filmId, Integer userId);

    void removeLike(Integer filmId, Integer userId);

    List<Film> getPopularFilms(int count);

    boolean hasLike(Integer filmId, Integer userId);

    boolean existsById(Integer filmId);
}