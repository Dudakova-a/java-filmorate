package ru.yandex.practicum.filmorate.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import lombok.extern.slf4j.Slf4j;
import org.springframework.transaction.annotation.Transactional;
import ru.yandex.practicum.filmorate.exception.NotFoundException;
import ru.yandex.practicum.filmorate.exception.ValidationException;
import ru.yandex.practicum.filmorate.model.User;
import ru.yandex.practicum.filmorate.repository.FriendStorage;
import ru.yandex.practicum.filmorate.repository.FriendshipStatusStorage;
import ru.yandex.practicum.filmorate.repository.UserStorage;

import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class FriendService {
    private final FriendStorage friendStorage;
    private final UserStorage userStorage;
    private final FriendshipStatusStorage statusStorage;

    @Transactional
    public void addFriend(Integer userId, Integer friendId) {
        log.debug("Attempting to add friend: {} -> {}", userId, friendId);

        // 1. Базовая валидация
        validateUsers(userId, friendId);

        // 2. Проверка существующих отношений
        checkExistingFriendship(userId, friendId);

        // 3. Создание заявки в друзья
        createFriendshipRequest(userId, friendId);

        log.info("Friend request created: {} -> {}", userId, friendId);
    }

    private void checkExistingFriendship(Integer userId, Integer friendId) {
        Optional<Integer> existingStatus = friendStorage.getFriendshipStatus(userId, friendId);
        Optional<Integer> reverseStatus = friendStorage.getFriendshipStatus(friendId, userId);

        if (existingStatus.isPresent() || reverseStatus.isPresent()) {
            String message = "Friendship relation already exists: " + userId + " and " + friendId;
            if (existingStatus.isPresent()) {
                message += " (status: " + getStatusName(existingStatus.get()) + ")";
            }
            if (reverseStatus.isPresent()) {
                message += " (reverse status: " + getStatusName(reverseStatus.get()) + ")";
            }
            throw new ValidationException(message);
        }
    }

    private String getStatusName(int statusId) {
        return statusStorage.findById(statusId)
                .orElseThrow(() -> new IllegalStateException("Status not found: " + statusId))
                .getName();
    }

    private void createFriendshipRequest(Integer userId, Integer friendId) {
        Integer pendingStatusId = statusStorage.findByName("PENDING")
                .orElseThrow(() -> new IllegalStateException("PENDING status not found"))
                .getId();

        friendStorage.addFriend(userId, friendId, pendingStatusId);
    }

    @Transactional
    public void confirmFriendship(Integer userId, Integer friendId) {
        validateUsers(userId, friendId);

        Integer pendingStatusId = getStatusId("PENDING");
        if (!friendStorage.hasPendingRequest(friendId, userId)) {
            throw new ValidationException("No pending friendship request found");
        }

        Integer confirmedStatusId = statusStorage.findByName("CONFIRMED")
                .orElseThrow(() -> new IllegalStateException("CONFIRMED status not found"))
                .getId();

        friendStorage.updateFriendshipStatus(friendId, userId, confirmedStatusId);
        friendStorage.updateFriendshipStatus(userId, friendId, confirmedStatusId);
    }

    @Transactional(readOnly = true)
    public List<User> getFriends(Integer userId) {
        User user = userStorage.findById(userId)
                .orElseThrow(() -> new NotFoundException("User not found: " + userId));

        // Получаем только подтверждённых друзей
        return friendStorage.getFriendsByUserId(userId).stream()
                .filter(Objects::nonNull)
                .peek(friend -> {
                    if (friend.getId() == null) {
                        throw new IllegalStateException("Friend has null ID");
                    }
                })
                .collect(Collectors.toList());
    }

    @Transactional
    public void removeFriend(Integer userId, Integer friendId) {
        validateUsers(userId, friendId);
        friendStorage.removeFriend(userId, friendId);
    }

    @Transactional(readOnly = true)
    public List<User> getCommonFriends(Integer userId, Integer otherId) {
        validateUsers(userId, otherId);
        Integer confirmedStatusId = getStatusId("CONFIRMED");
        return friendStorage.findCommonFriends(userId, otherId);
    }

    private void validateUsers(Integer userId, Integer friendId) {
        if (userId.equals(friendId)) {
            throw new IllegalArgumentException("User cannot add themselves as friend");
        }

        User user = userStorage.findById(userId)
                .orElseThrow(() -> new NotFoundException("User not found with id: " + userId));

        User friend = userStorage.findById(friendId)
                .orElseThrow(() -> new NotFoundException("User not found with id: " + friendId));
    }

    private Integer getStatusId(String statusName) {
        return statusStorage.findByName(statusName)
                .orElseThrow(() -> new IllegalStateException(statusName + " status not found"))
                .getId();
    }
}