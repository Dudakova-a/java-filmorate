package ru.yandex.practicum.filmorate.model;


import jakarta.validation.constraints.*;

import lombok.Data;
import lombok.Builder;

import java.time.LocalDate;
import java.util.HashSet;
import java.util.Set;

/**
 * Класс, представляющий пользователя в системе.
 * Содержит основные данные пользователя и используется для хранения и передачи информации о пользователях.
 */
@Data
@Builder
public class User {
    private Integer id;         // Уникальный идентификатор пользователя

    @NotBlank(message = "Email не может быть пустым")
    @Email(message = "Email должен содержать символ @")
    private String email;       // Электронная почта пользователя

    @NotBlank(message = "Логин не может быть пустым")
    @Pattern(regexp = "\\S+", message = "Логин не может содержать пробелы")
    private String login;       // Логин пользователя

    private String name;        // Имя пользователя для отображения

    @PastOrPresent(message = "Дата рождения не может быть в будущем")
    private LocalDate birthday; // Дата рождения пользователя

    private final Set<Integer> friends = new HashSet<>();
}

