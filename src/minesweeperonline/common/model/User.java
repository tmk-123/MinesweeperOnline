package minesweeperonline.common.model;

import java.io.Serializable;

/**
 * Represents a registered user in the database.
 */
public class User implements Serializable {

    private static final long serialVersionUID = 1L;

    private int id;
    private String username;
    private String password;
    private int totalScore;
    private String createdAt;

    public User() {
    }

    public User(String username, String password) {
        this.username = username;
        this.password = password;
        this.totalScore = 0;
    }

    public User(int id, String username, int totalScore) {
        this.id = id;
        this.username = username;
        this.totalScore = totalScore;
    }

    public User(int id, String username, String password, int totalScore, String createdAt) {
        this.id = id;
        this.username = username;
        this.password = password;
        this.totalScore = totalScore;
        this.createdAt = createdAt;
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public int getTotalScore() {
        return totalScore;
    }

    public void setTotalScore(int totalScore) {
        this.totalScore = Math.max(0, totalScore);
    }

    public String getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(String createdAt) {
        this.createdAt = createdAt;
    }

    @Override
    public String toString() {
        return "User{" +
                "id=" + id +
                ", username='" + username + '\'' +
                ", totalScore=" + totalScore +
                '}';
    }
}
