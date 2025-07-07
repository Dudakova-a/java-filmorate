package ru.yandex.practicum.filmorate;

import jakarta.validation.Validation;
import jakarta.validation.Validator;
import jakarta.validation.ValidatorFactory;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.util.Set;

import jakarta.validation.ConstraintViolation;
import ru.yandex.practicum.filmorate.model.*;

import static org.junit.jupiter.api.Assertions.*;

class FilmTest {
  private static Validator validator;

  @BeforeAll
  static void setup() {
    ValidatorFactory factory = Validation.buildDefaultValidatorFactory();
    validator = factory.getValidator();
  }

  // Тест для корректного фильма
  @Test
  void shouldPassValidFilm() {
    Film film = Film.builder()
            .name("Inception")
            .description("A thief who steals corporate secrets...")
            .releaseDate(LocalDate.of(2010, 7, 16))
            .duration(148)
            .build();

    Set<ConstraintViolation<Film>> violations = validator.validate(film);
    assertTrue(violations.isEmpty(), "Не должно быть нарушений валидации");
  }

  // Тесты для граничных случаев
  @Test
  void shouldFailWhenNameIsBlank() {
    Film film = Film.builder()
            .name(" ")  // Пробел вместо названия
            .description("Valid description")
            .releaseDate(LocalDate.of(2020, 1, 1))
            .duration(120)
            .build();

    Set<ConstraintViolation<Film>> violations = validator.validate(film);
    assertFalse(violations.isEmpty(), "Должны быть нарушения валидации");
    assertEquals(1, violations.size());
    assertEquals("Название фильма не может быть пустым", violations.iterator().next().getMessage());
  }

  // Слишком длинное описание
  @Test
  void shouldFailWhenDescriptionTooLong() {
    String longDescription = "A".repeat(201);  // 201 символ
    Film film = Film.builder()
            .name("Valid Name")
            .description(longDescription)
            .releaseDate(LocalDate.of(2020, 1, 1))
            .duration(120)
            .build();

    Set<ConstraintViolation<Film>> violations = validator.validate(film);
    assertFalse(violations.isEmpty());
    assertEquals("Описание не должно превышать 200 символов",
            violations.iterator().next().getMessage());
  }

  // Неположительная продолжительность
  @Test
  void shouldFailWhenDurationNotPositive() {
    Film film = Film.builder()
            .name("Valid Name")
            .description("Valid description")
            .releaseDate(LocalDate.of(2020, 1, 1))
            .duration(-10)  // Отрицательное значение
            .build();

    Set<ConstraintViolation<Film>> violations = validator.validate(film);
    assertFalse(violations.isEmpty());
    assertEquals("Продолжительность должна быть положительным числом",
            violations.iterator().next().getMessage());
  }

  // Тест для проверки всех ограничений одновременно
  @Test
  void shouldFailWithMultipleViolations() {
    Film film = Film.builder()
            .name(" ")  // Пустое название
            .description("A".repeat(201))  // Слишком длинное
            .releaseDate(LocalDate.of(1890, 1, 1))  // Слишком ранняя
            .duration(-10)  // Отрицательная
            .build();

    Set<ConstraintViolation<Film>> violations = validator.validate(film);
    assertEquals(3, violations.size(), "Должно быть 3 нарушения валидации");
  }
}