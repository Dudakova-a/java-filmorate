# java-filmorate
Template repository for Filmorate project.

![ER Diagram](path/to/er_filmorate.png)

Описание таблиц базы данных Filmorate
1. Пользователи (users)
user_id - уникальный идентификатор
login, email, name - персональные данные
birthday - дата рождения

2. Возрастные рейтинги (mpa)
mpa_id - идентификатор (G, PG, PG-13, R, NC-17)
name - название рейтинга

3. Фильмы (films)
film_id - уникальный ID
name, description - название и описание
release_date, duration - дата выхода и длительность (в минутах)
mpa_id - возрастное ограничение (FK к mpa)

4. Жанры (genres)
genre_id - уникальный ID
name - название (комедия, драма, боевик и др.)

5. Связи фильмов и жанров (film_genres)
film_id + genre_id - составной ключ

6. Лайки фильмов (film_likes) - отметки пользователей о понравившихся фильмах
user_id (FK к users) + film_id (FK к films)

7. Друзья пользователей (user_friends) - дружбы с подтверждением
user_id + friend_id (оба FK к users)
status (requested - запрос на дружбу, confirmed - подтверждённая дружба)
