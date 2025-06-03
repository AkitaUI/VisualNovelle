package com.example.visualstudio;

public class UserProfile {
    public String email;
    // добавляйте другие поля, например:
    // public String displayName;
    // public String avatarUrl;

    // Конструктор без аргументов обязателен для Firebase
    public UserProfile() { }

    public UserProfile(String email) {
        this.email = email;
    }
}

