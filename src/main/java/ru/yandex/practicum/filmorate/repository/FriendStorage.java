package ru.yandex.practicum.filmorate.repository;

import ru.yandex.practicum.filmorate.model.User;

import java.util.List;
import java.util.Optional;

/**
 * Интерфейс хранилища для работы с дружескими связями между пользователями.
 * Управляет отношениями "дружба" и их статусами.
 */
public interface FriendStorage {

    void addFriend(Integer userId, Integer friendId, Integer statusId);

    void removeFriend(Integer userId, Integer friendId);


    boolean friendshipExists(int userId, int friendId);

    void confirmFriendship(Integer userId, Integer friendId);

    Optional<Integer> getFriendshipStatus(int userId, int friendId);

    List<User> findFriendsByStatus(Integer userId, String statusName);

    boolean hasPendingRequest(Integer userId, Integer friendId);


    void updateFriendshipStatus(Integer userId, Integer friendId, Integer newStatusId);

    List<User> findCommonFriends(Integer userId, Integer otherId);

    List<User> getFriends(Integer userId);

    List<User> getFriendsByStatus(Integer userId, String statusName);

    List<User> getCommonFriends(Integer userId, Integer otherId);

    List<User> getFriendsByUserId(Integer userId);
}