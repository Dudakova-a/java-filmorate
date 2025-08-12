package ru.yandex.practicum.filmorate.repository;

import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;
import ru.yandex.practicum.filmorate.model.FriendshipStatus;

import java.util.List;

@Component
@RequiredArgsConstructor
public class FriendshipStatusInitializer {
    private final FriendshipStatusStorage statusRepository;
    private final JdbcTemplate jdbcTemplate;

    @PostConstruct
    public void init() {
        List<FriendshipStatus> defaultStatuses = List.of(
                new FriendshipStatus(1, "PENDING"),
                new FriendshipStatus(2, "CONFIRMED")
        );

        List<FriendshipStatus> existingStatuses = statusRepository.findAll();
        List<FriendshipStatus> statusesToInsert = defaultStatuses.stream()
                .filter(status -> existingStatuses.stream()
                        .noneMatch(existing -> existing.getId() == status.getId()))
                .toList();

        if (!statusesToInsert.isEmpty()) {

            jdbcTemplate.batchUpdate(
                    "INSERT INTO friendship_status (id, name) VALUES (?, ?)",
                    statusesToInsert,
                    statusesToInsert.size(),
                    (ps, status) -> {
                        ps.setInt(1, status.getId());
                        ps.setString(2, status.getName());
                    }
            );
        }
    }
}