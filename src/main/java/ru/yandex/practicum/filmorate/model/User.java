package ru.yandex.practicum.filmorate.model;


import jakarta.validation.constraints.*;
import lombok.Data;
import lombok.Builder;
import ru.yandex.practicum.filmorate.exception.FriendNotFoundException;

import java.time.LocalDate;
import java.util.HashMap;
import java.util.Map;

/**
 * Класс содержит основные данные пользователя и используется для хранения и передачи информации о пользователях.
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

    @Builder.Default
    private Map<Integer, FriendshipStatus> friends = new HashMap<>();

    public void addFriend(int friendId, FriendshipStatus status) {
        if (friends == null) {
            friends = new HashMap<>();
        }
        friends.put(friendId, status);
    }

    public void removeFriend(int friendId) {
        friends.remove(friendId);
    }

    public void updateFriendshipStatus(int friendId, FriendshipStatus status) {
        if (!friends.containsKey(friendId)) {
            throw new FriendNotFoundException("Друг с ID " + friendId + " не найден");
        }
        friends.put(friendId, status);
    }
}