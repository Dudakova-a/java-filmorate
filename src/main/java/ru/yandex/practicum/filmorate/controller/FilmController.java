package ru.yandex.practicum.filmorate.controller;

import lombok.extern.slf4j.Slf4j;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;
import ru.yandex.practicum.filmorate.model.Film;
import ru.yandex.practicum.filmorate.exception.ValidationException;

import java.time.LocalDate;
import java.util.*;

/**
 * Контроллер для обработки HTTP-запросов, связанных с фильмами.
 * Данные хранятся в памяти (HashMap) и возвращаются в формате JSON.
 */
@Slf4j
@RestController
@RequestMapping("/films") // Базовый путь для всех эндпоинтов этого контроллера
public class FilmController {
    private final Map<Integer, Film> films = new HashMap<>(); // Хранилище фильмов в виде HashMap
    private int currentId = 0; // Текущий ID для генерации следующего
    private static final LocalDate CINEMA_BIRTHDAY = LocalDate.of(1895, 12, 28);

    @GetMapping // Обрабатывает HTTP GET запросы по пути /films
    public List<Film> getAllFilms() {
        // Преобразует Map в ArrayList для возврата
        return new ArrayList<>(films.values());
    }

    @PostMapping // Обрабатывает HTTP POST запросы по пути /films
    public Film addFilm(@Valid @RequestBody Film film) {
        // Валидация фильма согласно задания
        validateFilm(film);
        // Устанавливает новый ID
        film.setId(getNextId());
        // Сохраняет в Map
        films.put(film.getId(), film);
        // Возвращает созданный фильм
        return film;
    }

    @PutMapping // Обрабатывает HTTP PUT запросы по пути /films
    public Film updateFilm(@Valid @RequestBody Film film) {
        // Валидация фильма согласно задания
        validateFilm(film);
        // Проверяем существование пользователя
        if (!films.containsKey(film.getId())) {
            throw new ValidationException("Фильм с id=" + film.getId() + " не найден");
        }
        // Обновляем данные о фильме
        films.put(film.getId(), film);
        // Возвращает обновлённый фильм
        return film;
    }

    // Генерирует следующий доступный ID для нового фильма
    private int getNextId() {
        return ++currentId;
    }

    // Метод для валидации фильма согласно задания
    private void validateFilm(Film film) {
        if (film.getReleaseDate().isBefore(CINEMA_BIRTHDAY)) {
            throw new ValidationException("Дата релиза не может быть раньше 28 декабря 1895 года");
        }
    }
}