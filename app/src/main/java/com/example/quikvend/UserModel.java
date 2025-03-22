package com.example.quikvend;

public class UserModel {
    private String email;
    private String role;

    public UserModel() { }

    public UserModel(String email, String role) {
        this.email = email;
        this.role = role;
    }

    public String getEmail() { return email; }
    public String getRole() { return role; }
}