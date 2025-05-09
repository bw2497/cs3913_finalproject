package com.mycompany.universityguessgame;

import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.io.*;
import java.sql.*;
import java.util.*;
import java.util.List;

public class UniversityGuessGame {
    private JFrame frame;
    private JTextField guessInput;
    private JTextArea resultArea;
    private JLabel timerLabel;
    private GameGraphicsPanel graphicsPanel;

    private List<University> universities;
    private University mysteryUniversity;

    private int guessesLeft = 6;
    private int timeLeft = 60;
    private final Object lock = new Object();
    private TimerThread timerThread;

    public static void main(String[] args) {
        SwingUtilities.invokeLater(UniversityGuessGame::new);
    }

    public UniversityGuessGame() {
        universities = loadUniversities("top100_universities.csv");
        Collections.shuffle(universities);
        mysteryUniversity = universities.get(0);
        buildUI();
        startTimer();
    }

    private void buildUI() {
        frame = new JFrame("Guess the University");
        frame.setSize(900, 600);
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setLayout(new BorderLayout());

        graphicsPanel = new GameGraphicsPanel();
        graphicsPanel.setPreferredSize(new Dimension(200, 0));
        frame.add(graphicsPanel, BorderLayout.WEST);

        guessInput = new JTextField();
        JButton submitButton = new JButton("Submit Guess");
        submitButton.addActionListener(e -> checkGuess());

        JButton leaderboardButton = new JButton("Leaderboard");
        leaderboardButton.addActionListener(e -> {
            List<String> top = DatabaseManager.getTopResults(10);
            JOptionPane.showMessageDialog(frame,
                String.join("\n", top),
                "Top 10 Players",
                JOptionPane.INFORMATION_MESSAGE);
        });

        JPanel topPanel = new JPanel(new BorderLayout(5,5));
        topPanel.add(leaderboardButton, BorderLayout.WEST);
        topPanel.add(guessInput, BorderLayout.CENTER);
        topPanel.add(submitButton, BorderLayout.EAST);
        frame.add(topPanel, BorderLayout.NORTH);

        resultArea = new JTextArea();
        resultArea.setEditable(false);
        resultArea.setFont(new Font("Monospaced", Font.PLAIN, 14));
        frame.add(new JScrollPane(resultArea), BorderLayout.CENTER);

        timerLabel = new JLabel("Time left: 60s");
        frame.add(timerLabel, BorderLayout.SOUTH);

        frame.setVisible(true);
    }

    private void startTimer() {
        timerThread = new TimerThread();
        timerThread.start();
    }

    private void checkGuess() {
        String userGuess = guessInput.getText().trim().toLowerCase();
        guessInput.setText("");
        Optional<University> guessOpt = universities.stream()
                .filter(u -> u.getName().toLowerCase().contains(userGuess))
                .findFirst();

        if (guessOpt.isPresent()) {
            University guessed = guessOpt.get();
            guessesLeft--;

            StringBuilder sb = new StringBuilder();
            sb.append("Guess ").append(6 - guessesLeft)
              .append(": ").append(guessed.getName()).append("\n");

            int guessRank = guessed.getCurrentRank();
            int targetRank = mysteryUniversity.getCurrentRank();
            int guessHigh = guessed.getHighestRank();
            int targetHigh = mysteryUniversity.getHighestRank();

            String arrowRank = guessRank > targetRank ? " ↓" : guessRank < targetRank ? " ↑" : " ✅";
            String arrowHigh = guessHigh > targetHigh ? " ↓" : guessHigh < targetHigh ? " ↑" : " ✅";

            sb.append("Rank: ").append(guessRank).append(arrowRank)
              .append(" (highest: ").append(guessHigh).append(arrowHigh).append(")\n");

            String stateMatch = guessed.getState().equalsIgnoreCase(mysteryUniversity.getState()) ? " (correct)" : "";
            sb.append("State: ").append(guessed.getState()).append(stateMatch).append("\n");

            String[] guessedTags = guessed.getTags().split(";");
            String[] targetTags = mysteryUniversity.getTags().split(";");
            Set<String> targetTagSet = new HashSet<>();
            for (String t : targetTags) targetTagSet.add(t.trim().toLowerCase());

            sb.append("Tags: ");
            for (int i = 0; i < guessedTags.length; i++) {
                String tag = guessed.getTags().split(";")[i].trim();
                if (targetTagSet.contains(tag.toLowerCase())) {
                    sb.append(tag).append(" (correct)");
                } else {
                    sb.append(tag);
                }
                if (i < guessedTags.length - 1) sb.append("; ");
            }
            sb.append("\n\n");

            if (guessed.getName().equalsIgnoreCase(mysteryUniversity.getName())) {
                sb.append("✅ Congratulations! You guessed correctly!\n");
                sb.append("Official Website: ").append(mysteryUniversity.getWebsite()).append("\n");
                resultArea.append(sb.toString());
                promptAndSave(true);
                endGame();
            } else {
                resultArea.append(sb.toString());
                if (guessesLeft == 0) {
                    resultArea.append("❌ Game over. The university was: "
                        + mysteryUniversity.getName() + "\nRank: "
                        + mysteryUniversity.getCurrentRank() + "\n");
                    promptAndSave(false);
                    endGame();
                } else {
                    resetTimer();
                }
            }
        } else {
            resultArea.append("❗ University not found. Try FULL Name\n");
        }
        graphicsPanel.repaint();
    }

    private void promptAndSave(boolean completed) {
        String name = JOptionPane.showInputDialog(frame,
            "Enter your name for the leaderboard:");
        if (name != null && !name.isBlank()) {
            DatabaseManager.saveResult(name.trim(), 6 - guessesLeft, completed);
        }
    }

    private void resetTimer() {
        synchronized (lock) { timeLeft = 60; }
        graphicsPanel.repaint();
    }

    private void endGame() {
        guessInput.setEnabled(false);
        timerThread.interrupt();
    }

    private class TimerThread extends Thread {
        public void run() {
            while (timeLeft > 0 && guessesLeft > 0) {
                try {
                    Thread.sleep(1000);
                    synchronized (lock) {
                        timeLeft--;
                        SwingUtilities.invokeLater(() -> timerLabel.setText("Time left: " + timeLeft + "s"));
                        graphicsPanel.repaint();
                        if (timeLeft == 0) {
                            guessesLeft--;
                            resultArea.append("⏰ Time's up! Remaining: " + guessesLeft + "\n");
                            resetTimer();
                            if (guessesLeft == 0) {
                                resultArea.append("❌ Game over. The university was: " + mysteryUniversity.getName() + "\n");
                                promptAndSave(false);
                                endGame();
                            }
                        }
                    }
                } catch (InterruptedException e) { break; }
            }
        }
    }

    private class GameGraphicsPanel extends JPanel {
        @Override
        protected void paintComponent(Graphics g) {
            super.paintComponent(g);
            int w = getWidth(), h = getHeight();
            int barW = (int)((timeLeft/60.0)*w);
            g.setColor(Color.GREEN);
            g.fillRect(0,0,barW,20);
            g.setColor(Color.BLACK);
            g.drawRect(0,0,w-1,20);
            int r=15, s=10, y=40;
            for(int i=0;i<6;i++){
                int x=s+i*(2*r+s);
                g.setColor(i<guessesLeft?Color.BLUE:Color.RED);
                g.fillOval(x,y,2*r,2*r);
            }
        }
    }


        private static class University {
            private final String name;
            private final int currentRank;
            private final int highestRank;
            private final String state;
            private final String tags;
            private final String website;

            public University(String name, int currentRank, int highestRank, String state, String tags, String website) {
                this.name = name;
                this.currentRank = currentRank;
                this.highestRank = highestRank;
                this.state = state;
                this.tags = tags;
                this.website = website;
            }

            public String getName() { return name; }
            public int getCurrentRank() { return currentRank; }
            public int getHighestRank() { return highestRank; }
            public String getState() { return state; }
            public String getTags() { return tags; }
            public String getWebsite() { return website; }
        }

        private List<University> loadUniversities(String filePath) {
            List<University> list = new ArrayList<>();
            try (BufferedReader br = new BufferedReader(new FileReader(filePath))) {
                String line = br.readLine();
                while ((line = br.readLine()) != null) {
                    String[] tokens = line.split(",(?=([^\"]*\"[^\"]*\")*[^\"]*$)", -1);
                    if (tokens.length >= 6) {
                        String name = tokens[0].trim();
                        int rank = Integer.parseInt(tokens[1].trim());
                        int highRank = Integer.parseInt(tokens[2].trim());
                        String state = tokens[3].trim();
                        String tags = tokens[4].trim();
                        String website = tokens[5].trim();
                        list.add(new University(name, rank, highRank, state, tags, website));
                    }
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
            return list;
        }
    }

    class DatabaseManager {
        private static final String URL = "jdbc:mariadb://127.0.0.1:3306/university_guess_game";
        private static final String USER = "root";
        private static final String PASSWORD = "";

        public static void saveResult(String username, int guessesUsed, boolean completed) {
            String sql = "INSERT INTO leaderboard (username, guesses_used, completed) VALUES (?, ?, ?)";
            try (Connection conn = DriverManager.getConnection(URL, USER, PASSWORD);
                 PreparedStatement stmt = conn.prepareStatement(sql)) {
                stmt.setString(1, username);
                stmt.setInt(2, guessesUsed);
                stmt.setBoolean(3, completed);
                stmt.executeUpdate();
            } catch (SQLException e) {
                e.printStackTrace();
            }
        }

        public static List<String> getTopResults(int limit) {
            List<String> leaderboard = new ArrayList<>();
            String sql = "SELECT username, guesses_used, completed FROM leaderboard ORDER BY guesses_used ASC LIMIT ?";
            try (Connection conn = DriverManager.getConnection(URL, USER, PASSWORD);
                 PreparedStatement stmt = conn.prepareStatement(sql)) {
                stmt.setInt(1, limit);
                ResultSet rs = stmt.executeQuery();
                while (rs.next()) {
                    leaderboard.add(String.format("%s - %d guesses - %s",
                            rs.getString("username"),
                            rs.getInt("guesses_used"),
                            rs.getBoolean("completed") ? "✔ Completed" : "✘ Incomplete"));
                }
            } catch (SQLException e) {
                e.printStackTrace();
            }
            return leaderboard;
        }
}
