# cs3913_finalproject
# 🎓 University Guess Game  
A Java-based guessing game where players identify top U.S. universities based on their rank, state, and specialty tags — powered by a graphical interface, timers, and leaderboard tracking.

## 👨‍💻 Creators  
- Bo Wen  

## 🛠️ Technologies Used  
- Java  
- Swing & AWT (custom `JPanel` painting) For Graphics
- Thread Concurrency (with synchronization)  
- File I/O (CSV parsing)  
- JDBC (MySQL/MariaDB)  

## ✨ Features  

### 🖥️ Interactive GUI  
- Uses a **custom `JPanel`** (`GameGraphicsPanel`) that overrides `paintComponent(Graphics g)`.  
- **Live Time Bar**: a green bar at the top shows the remaining seconds (updates every tick).  
- **Guess Indicators**: a row of blue/red circles shows remaining vs. used guesses.  
- Built on Swing’s event-dispatch thread for smooth rendering.

### 🧩 Game Mechanics  
- 🎯 Player has 6 guesses to identify a randomly selected university  
- 📉 Hints include comparison of ranks (⬆️/⬇️), matched states, and shared tags  
- ⏱️ A 60-second timer for each guess, with automatic penalties on timeout  
- 🏫 Over 50 real U.S. universities included, loaded from CSV  

### 💾 Database Integration  
- 🏆 Leaderboard with top 10 player scores  
- ✅ Stores completion status and number of guesses used  
- 💽 Uses MySQL/MariaDB via JDBC  

## 🗄️ Database Setup  
Ensure your local MariaDB/MySQL configuration matches:  
- Port: `3306`  
- Username: `root`  
- Database: `university_guess_game`  

### 🧱 SQL Schema  
Use this schema to create the leaderboard table:  
```sql
CREATE DATABASE university_guess_game;

USE university_guess_game;

CREATE TABLE leaderboard (
    id INT AUTO_INCREMENT PRIMARY KEY,
    username VARCHAR(50),
    guesses_used INT,
    completed BOOLEAN,
    timestamp TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);
```  

## 📚 Dependencies  
You’ll need the **MariaDB JDBC driver**:  
If you're using Maven, add this to your `pom.xml`:
```xml
<dependency>
    <groupId>org.mariadb.jdbc</groupId>
    <artifactId>mariadb-java-client</artifactId>
    <version>3.3.2</version>
</dependency>
```

## 🎯 How to Play  
1. 🚀 Launch the application  
2. ⌨️ Type the full name of a U.S. university  
3. 📊 Get clues about its rank, tags, and state match  
4. 🧠 Use logic to narrow it down within 6 guesses  
5. 🏆 If correct, save your name to the leaderboard!  

---  
📘 Learn. Guess. Win. Become the campus king! 🎓

