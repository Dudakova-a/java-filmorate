package ru.yandex.practicum.filmorate.repository;

import ru.yandex.practicum.filmorate.model.User;

import java.util.List;
import java.util.Optional;

/**
 * Интерфейс для работы с хранилищем пользователей.
 * Определяет методы для управления пользователями и их дружескими связями.
 */
public interface UserStorage {

    List<User> findAll();

    Optional<User> findById(Integer id);

    User save(User user);

    User update(User user);

    void delete(Integer id);

    boolean existsByEmail(String email);

    boolean existsById(Integer id);

}