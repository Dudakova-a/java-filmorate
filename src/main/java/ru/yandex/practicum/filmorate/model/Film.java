package ru.yandex.practicum.filmorate.model;

import jakarta.validation.constraints.*;
import lombok.*;

import java.time.LocalDate;
import java.util.HashSet;
import java.util.Objects;
import java.util.Set;

/**
 * Класс содержит основные характеристики фильма, используется для хранения и передачи данных о фильмах.
 */
@Data
@Builder
@NoArgsConstructor(access = AccessLevel.PUBLIC)
@AllArgsConstructor(access = AccessLevel.PUBLIC)
public class Film {
    private Integer id;            // Уникальный идентификатор фильма

    @NotBlank(message = "Название фильма не может быть пустым")
    private String name;           // Название фильма

    @Size(max = 200, message = "Описание не должно превышать 200 символов")
    private String description;    // Описание фильма

    @NotNull(message = "Дата релиза обязательна")
    @PastOrPresent(message = "Дата релиза не может быть в будущем")
    private LocalDate releaseDate; // Дата выхода фильма

    @Positive(message = "Продолжительность должна быть положительным числом")
    private Integer duration;      // Продолжительность фильма в минутах

    private final Set<Integer> likes = new HashSet<>();

    private Mpa mpa;
    private Set<Genre> genres = new HashSet<>();

    public void addGenre(Integer genreId) {
        genres.add(new Genre(genreId, ""));
    }

    public void removeGenre(Integer genreId) {
        genres.removeIf(g -> Objects.equals(g.getId(), genreId));
    }
}