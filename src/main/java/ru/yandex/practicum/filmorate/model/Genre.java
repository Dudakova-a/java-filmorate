package ru.yandex.practicum.filmorate.model;

import lombok.*;

/** Класс представляет жанр фильма.**/

@NoArgsConstructor(access = AccessLevel.PUBLIC)
@AllArgsConstructor(access = AccessLevel.PUBLIC)
@Getter
@Setter
public class Genre {
    private Integer id;
    private String name;

}