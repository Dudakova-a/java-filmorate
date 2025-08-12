package ru.yandex.practicum.filmorate.model;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;

/** Класс представляет жанр фильма.**/

@AllArgsConstructor
@Getter
@Setter
public class Genre {
    private Integer id;
    private String name;

}