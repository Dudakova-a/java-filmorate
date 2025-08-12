package ru.yandex.practicum.filmorate.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import ru.yandex.practicum.filmorate.exception.NotFoundException;

import ru.yandex.practicum.filmorate.model.Film;
import ru.yandex.practicum.filmorate.storage.FilmStorage;
import ru.yandex.practicum.filmorate.storage.UserStorage;


import java.util.List;


@Slf4j
@Service
public class FilmService {
    private final FilmStorage filmStorage;
    private final UserStorage userStorage;

    @Autowired
    public FilmService(FilmStorage filmStorage, UserStorage userStorage) {
        this.filmStorage = filmStorage;
        this.userStorage = userStorage;
    }

    public void addLike(int filmId, int userId) {
        log.debug("Попытка добавить лайк фильму id={} от пользователя id={}", filmId, userId);
        Film film = getFilmById(filmId);
        if (!userStorage.containsUser(userId)) {
            log.warn("Пользователь с id={} не найден при попытке поставить лайк фильму id={}", userId, filmId);
            throw new NotFoundException("Пользователь с id=" + userId + " не найден!");
        }
        film.getLikes().add(userId);
        log.debug("Пользователь id={} поставил лайк фильму id={}", userId, filmId);
    }

    public void removeLike(int filmId, int userId) {
        log.debug("Попытка удалить лайк фильму id={} от пользователя id={}", filmId, userId);
        Film film = getFilmById(filmId);
        if (!film.getLikes().remove(userId)) {
            log.warn("Лайк от пользователя id={} не найден у фильма id={}", userId, filmId);
            throw new NotFoundException("Лайк от пользователя с id=" + userId + " не найден");
        }
        log.debug("Пользователь id={} удалил лайк с фильма id={}", userId, filmId);
    }

    public List<Film> getPopularFilms(int count) {
        log.debug("Запрос популярных фильмов");
        List<Film> filmsPop = filmStorage.getPopularFilms(count);
        log.debug("Получено {} поп фильмов", filmsPop.size());
        return filmsPop;
    }

    public Film getFilmById(int id) {
        log.debug("Поиск фильма по id={}", id);
        Film film = filmStorage.getFilmById(id);
        if (film == null) {
            log.warn("Фильм с id={} не найден", id);
            throw new NotFoundException("Фильм с id=" + id + " не найден");
        }
        log.debug("Найден фильм: {}", film);
        return film;
    }

    public List<Film> getAllFilms() {
        log.debug("Запрос всех фильмов");
        List<Film> films = filmStorage.getAllFilms();
        log.debug("Возвращено {} фильмов", films.size());
        return films;
    }

    public Film addFilm(Film film) {
        log.debug("Попытка добавить фильм: {}", film);
        Film createdFilm = filmStorage.addFilm(film);
        log.debug("Фильм добавлен: {}", createdFilm);
        return createdFilm;
    }

    public Film updateFilm(Film film) {
        log.debug("Попытка обновить фильм: {}", film);
        Film updatedFilm = filmStorage.updateFilm(film);
        log.info("Фильм обновлен: {}", updatedFilm);
        return updatedFilm;
    }
}