package ru.yandex.practicum.filmorate.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.yandex.practicum.filmorate.exception.NotFoundException;
import ru.yandex.practicum.filmorate.exception.ValidationException;
import ru.yandex.practicum.filmorate.model.Film;
import ru.yandex.practicum.filmorate.model.Genre;
import ru.yandex.practicum.filmorate.repository.FilmStorage;
import ru.yandex.practicum.filmorate.repository.UserStorage;

import java.time.LocalDate;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Сервисный класс для работы с фильмами.
 * Обеспечивает бизнес-логику приложения для операций с фильмами.**/
@Service
@RequiredArgsConstructor
public class FilmService {
    private final FilmStorage filmStorage;
    private final UserService userService;
    private final MpaService mpaService;
    private final GenreService genreService;
    private final UserStorage userStorage;

    public List<Film> findAll() {
        return filmStorage.findAll();
    }

    public Film findById(Integer id) {
        return filmStorage.findById(id)
                .orElseThrow(() -> new NotFoundException("Film not found with id: " + id));
    }

    public Film create(Film film) {
        if (film.getReleaseDate().isBefore(LocalDate.of(1895, 12, 28))) {
            throw new ValidationException("Дата релиза не может быть раньше 28 декабря 1895 года");
        }
        // Проверяем MPA
        mpaService.findById(film.getMpa().getId());

        if (film.getGenres() != null && !film.getGenres().isEmpty()) {
            checkGenresExist(film.getGenres());
        }

        return filmStorage.save(film);
    }

    private void checkGenresExist(Set<Genre> genres) {
        // Собираем уникальные ID жанров
        Set<Integer> uniqueGenreIds = genres.stream()
                .map(Genre::getId)
                .collect(Collectors.toSet());

        // Проверяем каждый уникальный ID
        uniqueGenreIds.forEach(genreId -> {
            if (genreService.findById(genreId) == null) {
                throw new NotFoundException("Жанр с ID " + genreId + " не найден");
            }
        });
    }

    public Film update(Film film) {
        findById(film.getId()); // Проверка существования фильма
        return filmStorage.update(film);
    }

    public void delete(Integer id) {
        filmStorage.delete(id);
    }

    @Transactional
    public void addLike(Integer filmId, Integer userId) {
        // Проверяем существование фильма и пользователя
        findById(filmId);
        userService.findById(userId);

        // Проверяем, не ставил ли уже пользователь лайк
        if (filmStorage.hasLike(filmId, userId)) {
            throw new ValidationException("User " + userId + " already liked film " + filmId);
        }

        // Добавляем лайк
        filmStorage.addLike(filmId, userId);
    }

    @Transactional
    public void removeLike(Integer filmId, Integer userId) {
        // Проверяем существование фильма и пользователя
        if (!filmStorage.existsById(filmId)) {
            throw new NotFoundException("Фильм с ID " + filmId + " не найден");
        }
        if (!userStorage.existsById(userId)) {
            throw new NotFoundException("Пользователь с ID " + userId + " не найден");
        }

        // Проверяем, ставил ли пользователь лайк
        if (!filmStorage.hasLike(filmId, userId)) {
            throw new NotFoundException("Like from user " + userId + " to film " + filmId + " not found");
        }

        // Удаляем лайк
        filmStorage.removeLike(filmId, userId);
    }

    public List<Film> getPopularFilms(int count) {
        return filmStorage.getPopularFilms(count);
    }
}