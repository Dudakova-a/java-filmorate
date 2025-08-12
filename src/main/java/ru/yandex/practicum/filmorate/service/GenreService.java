package ru.yandex.practicum.filmorate.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import ru.yandex.practicum.filmorate.exception.NotFoundException;
import ru.yandex.practicum.filmorate.model.Genre;
import ru.yandex.practicum.filmorate.repository.GenreStorage;

import java.util.List;

/**
 * Сервисный класс для работы с жанрами фильмов.
 **/

@Service
@RequiredArgsConstructor
public class GenreService {
    private final GenreStorage genreStorage;


    public List<Genre> findAll() {
        return genreStorage.findAll();
    }

    public Genre findById(Integer id) {
        return genreStorage.findById(id)
                .orElseThrow(() -> new NotFoundException("Genre not found with id: " + id));
    }

    public List<Genre> getFilmGenres(Integer filmId) {
        return genreStorage.getFilmGenres(filmId);
    }
}