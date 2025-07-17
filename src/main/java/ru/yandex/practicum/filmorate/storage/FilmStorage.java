package ru.yandex.practicum.filmorate.storage;

import ru.yandex.practicum.filmorate.model.Film;
import java.util.List;

public interface FilmStorage {
    List<Film> getAllFilms();

    Film addFilm(Film film);

    Film updateFilm(Film film);

    Film getFilmById(int id);

    boolean containsFilm(int id);

    List<Film> getPopularFilms(int count);
}