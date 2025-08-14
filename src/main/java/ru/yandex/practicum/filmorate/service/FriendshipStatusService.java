package ru.yandex.practicum.filmorate.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import ru.yandex.practicum.filmorate.model.FriendshipStatus;
import ru.yandex.practicum.filmorate.repository.FriendshipStatusStorage;

import java.util.List;

@Service
@RequiredArgsConstructor
public class FriendshipStatusService {
    private final FriendshipStatusStorage statusStorage;

    public List<FriendshipStatus> getAllFriendshipStatuses() {
        return statusStorage.findAll();
    }
}