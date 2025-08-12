package ru.yandex.practicum.filmorate.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Класс для статусов дружеских отношений между пользователями.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class FriendshipStatus {
    private int id;
    private String name;
}
