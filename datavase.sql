CREATE DATABASE university_guess_game;

USE university_guess_game;

CREATE TABLE leaderboard (
    id INT AUTO_INCREMENT PRIMARY KEY,
    username VARCHAR(50),
    guesses_used INT,
    completed BOOLEAN,
    timestamp DATETIME DEFAULT CURRENT_TIMESTAMP
);
