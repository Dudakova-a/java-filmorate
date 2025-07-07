package ru.yandex.practicum.filmorate.model;

import jakarta.validation.constraints.*;

import lombok.Data;
import lombok.Builder;

import java.time.LocalDate;

/**
 * Класс, представляющий фильм в системе.
 * Содержит основные характеристики фильма и используется для хранения и передачи данных о фильмах.
 */
@Data
@Builder
public class Film {
    private Integer id;            // Уникальный идентификатор фильма

    @NotBlank(message = "Название фильма не может быть пустым")
    private String name;           // Название фильма

    @Size(max = 200, message = "Описание не должно превышать 200 символов")
    private String description;    // Описание фильма

    @NotNull(message = "Дата релиза обязательна")
    private LocalDate releaseDate; // Дата выхода фильма

    @Positive(message = "Продолжительность должна быть положительным числом")
    private Integer duration;      // Продолжительность фильма в минутах
}