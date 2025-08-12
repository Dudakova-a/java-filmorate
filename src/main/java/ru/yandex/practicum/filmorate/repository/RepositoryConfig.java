package ru.yandex.practicum.filmorate.repository;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.jdbc.core.JdbcTemplate;

/**
 * Конфигурационный класс для настройки репозиториев.
 */
@Configuration
public class RepositoryConfig {

    @Bean
    public FilmStorage filmStorage(JdbcTemplate jdbcTemplate,
                                         GenreStorage genreStorage,
                                         MpaStorage mpaStorage) {
        return new JdbcFilmRepository(jdbcTemplate, genreStorage, mpaStorage);
    }

    @Bean
    public UserStorage userStorage(JdbcTemplate jdbcTemplate, FriendshipStatusStorage statusStorage) {
        return new JdbcUserRepository(jdbcTemplate, statusStorage);
    }

    @Bean
    public FriendStorage friendStorage(JdbcTemplate jdbcTemplate,
                                             FriendshipStatusStorage statusStorage,
                                             UserStorage userStorage) {
        return new JdbcFriendRepository(jdbcTemplate, statusStorage, userStorage);
    }
}