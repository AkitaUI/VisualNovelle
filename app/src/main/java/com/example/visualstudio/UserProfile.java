package com.example.visualstudio;

public class UserProfile {
    public String email;

    // Конструктор без аргументов обязателен для Firebase
    public UserProfile() { }

    public UserProfile(String email) {
        this.email = email;
    }
}

