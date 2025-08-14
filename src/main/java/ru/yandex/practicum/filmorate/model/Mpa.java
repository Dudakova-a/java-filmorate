package ru.yandex.practicum.filmorate.model;

import com.fasterxml.jackson.annotation.JsonGetter;
import lombok.*;

/**
 * Класс представляет возрастной рейтинг фильма по системе MPA (Motion Picture Association).
 **/
@Data
@Builder
@NoArgsConstructor(access = AccessLevel.PUBLIC)

public class Mpa {
    private Integer id;              // Уникальный идентификатор рейтинга
    private String code;         // Краткий код рейтинга
    private String description;  // Описание возрастных ограничений

    public Mpa(Integer id, String code, String description) {
        this.id = id;
        this.code = code;
        this.description = description;
    }

    @JsonGetter("name")
    public String getName() {
        return this.code;
    }
}