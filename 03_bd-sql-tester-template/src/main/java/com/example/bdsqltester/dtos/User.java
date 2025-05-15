package com.example.bdsqltester.dtos;

import java.sql.ResultSet;
import java.sql.SQLException;

public class User {
    public long id;
    public String username; // Ubah tipe data menjadi String
    public String password;
    public String role;

    public User(String username, String password, String role) { // Sesuaikan konstruktor
        this.id = id;
        this.username = username;
        this.password = password;
        this.role = role;
    }

    public User(ResultSet rs) throws SQLException {
        this.id = rs.getLong("id");
        this.username = rs.getString("username"); // Baca sebagai String dari ResultSet
        this.password = rs.getString("password");
        this.role = rs.getString("role");
    }

    public long getId() {
        return id;
    }

    public void setId(long id) {
        this.id = id;
    }

    public String getUsername() { // Ubah tipe data getter
        return username;
    }

    public void setUsername(String username) { // Ubah tipe data setter
        this.username = username;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public String getRole() {
        return role;
    }

    public void setRole(String role) {
        this.role = role;
    }
}