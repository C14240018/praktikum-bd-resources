package com.example.bdsqltester.datasources;

import com.example.bdsqltester.dtos.Assignment;
import com.zaxxer.hikari.*;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class MainDataSource {
    private static HikariConfig config = new HikariConfig();
    private static HikariDataSource ds;
    private static int currentUserId; // Untuk menyimpan ID user yang sedang login

    static {
        config.setJdbcUrl("jdbc:postgresql://localhost:5432/postgres");
        config.setUsername("postgres");
        config.setPassword("9SaltySpies");
        config.addDataSourceProperty("cachePrepStmts", "true");
        config.addDataSourceProperty("prepStmtCacheSize", "250");
        config.addDataSourceProperty("prepStmtCacheSqlLimit", "2048");
        ds = new HikariDataSource(config);
    }

    public static Connection getConnection() throws SQLException {
        return ds.getConnection();
    }

    private MainDataSource() {}

    public static void setCurrentUserId(int userId) {
        currentUserId = userId;
    }

    public static int getCurrentUserId() {
        return currentUserId;
    }

    public static List<Assignment> getCompletedAssignments() throws SQLException {
        List<Assignment> assignments = new ArrayList<>();

        String query = "SELECT a.id, a.name, g.grade_value " +
                "FROM assignments a " +
                "JOIN grades g ON a.id = g.assignment_id " +
                "WHERE g.user_id = ? AND a.status = 'COMPLETED' " +
                "ORDER BY a.completion_date DESC";

        try (Connection conn = getConnection();
             PreparedStatement stmt = conn.prepareStatement(query)) {

            stmt.setInt(1, getCurrentUserId());

            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    assignments.add(new Assignment(
                            rs.getLong("id"),
                            rs.getString("name"),
                            rs.getInt("grade_value")
                    ));
                }
            }
        }

        return assignments;
    }

    public static double getAverageGrade() throws SQLException {
        String query = "SELECT AVG(grade_value) as average " +
                "FROM grades " +
                "WHERE user_id = ?";

        try (Connection conn = getConnection();
             PreparedStatement stmt = conn.prepareStatement(query)) {

            stmt.setInt(1, getCurrentUserId());

            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    return rs.getDouble("average");
                }
            }
        }

        return 0.0;
    }

    public static AssignmentStats getAssignmentStats() throws SQLException {
        String query = "SELECT " +
                "COUNT(*) as total, " +
                "SUM(CASE WHEN grade_value >= 70 THEN 1 ELSE 0 END) as passed, " +
                "AVG(grade_value) as average " +
                "FROM grades " +
                "WHERE user_id = ?";

        try (Connection conn = getConnection();
             PreparedStatement stmt = conn.prepareStatement(query)) {

            stmt.setInt(1, getCurrentUserId());

            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    return new AssignmentStats(
                            rs.getInt("total"),
                            rs.getInt("passed"),
                            rs.getDouble("average")
                    );
                }
            }
        }

        return new AssignmentStats(0, 0, 0.0);
    }

    public static class AssignmentStats {
        public final int totalAssignments;
        public final int passedAssignments;
        public final double averageGrade;

        public AssignmentStats(int totalAssignments, int passedAssignments, double averageGrade) {
            this.totalAssignments = totalAssignments;
            this.passedAssignments = passedAssignments;
            this.averageGrade = averageGrade;
        }
    }

}
