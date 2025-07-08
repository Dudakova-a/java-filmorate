package ru.yandex.practicum.filmorate.controller;

import lombok.extern.slf4j.*;
import jakarta.validation.Valid;

import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.*;
import ru.yandex.practicum.filmorate.model.User;
import ru.yandex.practicum.filmorate.exception.ValidationException;

import java.util.*;

/**
 * Контроллер для обработки HTTP-запросов, связанных с пользователями.
 */
@Slf4j
@RestController
@RequestMapping("/users")
public class UserController {
    private final Map<Integer, User> users = new HashMap<>(); // Хранилище пользователей в виде HashMap

   @GetMapping // Обрабатывает HTTP GET запросы по пути /users
    public List<User> getAllUsers() {
        // Преобразуем значения Map в ArrayList для возврата
        return new ArrayList<>(users.values());
    }

    @PostMapping // Обрабатывает HTTP POST запросы по пути /users
    public User createUser(@Valid @RequestBody User user) {
        log.debug("Получен запрос на создание пользователя: {}", user);
        if (!StringUtils.hasText(user.getName())) {
            user.setName(user.getLogin());
        }
        user.setId(getNextId()); // Устанавливаем новый ID перед сохранением
        users.put(user.getId(), user);// Сохраняем пользователя в Map
        log.debug("Пользователь успешно создан: {}", user);
        return user; // Возвращаем созданного пользователя
    }

    @PutMapping // Обрабатывает HTTP PUT запросы по пути /users
    public User updateUser(@Valid @RequestBody User user) {
        log.debug("Получен запрос на обновление пользователя с ID {}: {}", user.getId(), user);
        // Проверяем существование пользователя
        if (!users.containsKey(user.getId())) {
            throw new ValidationException("Пользователь с id=" + user.getId() + " не найден");
        }
        // Обновляем данные пользователя
        users.put(user.getId(), user);
        log.debug("Пользователь успешно обновлён: {}", user);
        return user;
    }

    // Метод генерирует следующий доступный ID для нового пользователя.
    private int getNextId() {
        return users.keySet()
                .stream()
                .mapToInt(id -> id)
                .max()
                .orElse(0) + 1;
    }
}
