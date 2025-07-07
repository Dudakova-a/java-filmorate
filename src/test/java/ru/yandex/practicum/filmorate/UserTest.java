package ru.yandex.practicum.filmorate;

import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import jakarta.validation.ValidatorFactory;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import ru.yandex.practicum.filmorate.model.*;

import java.time.LocalDate;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

class UserTest {
    private static Validator validator;

    @BeforeAll
    static void setup() {
        ValidatorFactory factory = Validation.buildDefaultValidatorFactory();
        validator = factory.getValidator();
    }

    // Тест для корректного пользователя
    @Test
    void shouldPassValidUser() {
        User user = User.builder()
                .email("valid@example.com")
                .login("valid_login")
                .birthday(LocalDate.of(1990, 1, 1))
                .build();

        Set<ConstraintViolation<User>> violations = validator.validate(user);
        assertTrue(violations.isEmpty(), "Не должно быть нарушений валидации");
    }

    // Тест для пустого email
    @Test
    void shouldFailWhenEmailIsEmpty() {
        User user = User.builder()
                .email("")  // Пустой email
                .login("valid_login")
                .birthday(LocalDate.of(1990, 1, 1))
                .build();

        Set<ConstraintViolation<User>> violations = validator.validate(user);
        assertFalse(violations.isEmpty(), "Должны быть нарушения валидации");
        assertEquals("Email не может быть пустым", violations.iterator().next().getMessage());
    }

    // Тест для email без @
    @Test
    void shouldFailWhenEmailInvalid() {
        User user = User.builder()
                .email("invalid-email")  // Нет символа @
                .login("valid_login")
                .birthday(LocalDate.of(1990, 1, 1))
                .build();

        Set<ConstraintViolation<User>> violations = validator.validate(user);
        assertFalse(violations.isEmpty());
        assertEquals("Email должен содержать символ @", violations.iterator().next().getMessage());
    }


    // Тест для даты рождения в будущем
    @Test
    void shouldFailWhenBirthdayInFuture() {
        User user = User.builder()
                .email("valid@example.com")
                .login("valid_login")
                .birthday(LocalDate.now().plusDays(1))  // Завтра
                .build();

        Set<ConstraintViolation<User>> violations = validator.validate(user);
        assertFalse(violations.isEmpty());
        assertEquals("Дата рождения не может быть в будущем", violations.iterator().next().getMessage());
    }

    // Тест для пустого имени (должно пройти валидацию)
    @Test
    void shouldPassWhenNameIsEmpty() {
        User user = User.builder()
                .email("valid@example.com")
                .login("valid_login")
                .name("")  // Пустое имя
                .birthday(LocalDate.of(1990, 1, 1))
                .build();

        Set<ConstraintViolation<User>> violations = validator.validate(user);
        assertTrue(violations.isEmpty(), "Пустое имя должно быть допустимо");
    }

}