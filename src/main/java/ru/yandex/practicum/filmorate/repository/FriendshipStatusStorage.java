package ru.yandex.practicum.filmorate.repository;

import ru.yandex.practicum.filmorate.model.FriendshipStatus;

import java.util.List;
import java.util.Optional;

public interface FriendshipStatusStorage {
    Optional<FriendshipStatus> findById(int id);

    Optional<FriendshipStatus> findByName(String name);

    List<FriendshipStatus> findAll();
}