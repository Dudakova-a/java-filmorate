package ru.yandex.practicum.filmorate.model;

import lombok.*;

/**
 * Класс для статусов дружеских отношений между пользователями.
 */
@Data
@Builder
@NoArgsConstructor(access = AccessLevel.PUBLIC)
@AllArgsConstructor(access = AccessLevel.PUBLIC)
public class FriendshipStatus {
    private int id;
    private String name;
}
