package com.example.bdsqltester.scenes.admin;

import com.example.bdsqltester.datasources.GradingDataSource;
import com.example.bdsqltester.datasources.MainDataSource;
import com.example.bdsqltester.dtos.Assignment;
import com.example.bdsqltester.dtos.Grade;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.StackPane;
import javafx.stage.Stage;

import java.sql.*;
import java.util.ArrayList;

public class AdminController {

    @FXML
    private TextArea answerKeyField;

    @FXML
    private ListView<Assignment> assignmentList = new ListView<>();

    @FXML
    private TextField idField;

    @FXML
    private TextArea instructionsField;

    @FXML
    private TextField nameField;

    @FXML
    private Button deleteButton;

    private final ObservableList<Assignment> assignments = FXCollections.observableArrayList();

    @FXML
    void initialize() {
        idField.setEditable(false);
        idField.setMouseTransparent(true);
        idField.setFocusTraversable(false);
        refreshAssignmentList();
        assignmentList.setCellFactory(param -> new ListCell<Assignment>() {
            @Override
            protected void updateItem(Assignment item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setText(null);
                } else {
                    setText(item.name);
                }
            }

            @Override
            public void updateSelected(boolean selected) {
                super.updateSelected(selected);
                if (selected) {
                    onAssignmentSelected(getItem());
                }
            }
        });

        // Atur aksi untuk tombol delete assignment
        deleteButton.setOnAction(this::onDeleteAssignmentClick);
    }

    void refreshAssignmentList() {
        assignments.clear();
        try (Connection c = MainDataSource.getConnection()) {
            Statement stmt = c.createStatement();
            ResultSet rs = stmt.executeQuery("SELECT * FROM assignments");
            while (rs.next()) {
                assignments.add(new Assignment(rs));
            }
        } catch (Exception e) {
            showAlert("Error", "Database Error", e.toString());
        }
        assignmentList.setItems(assignments);
        try {
            if (!idField.getText().isEmpty()) {
                long id = Long.parseLong(idField.getText());
                for (Assignment assignment : assignments) {
                    if (assignment.id == id) {
                        assignmentList.getSelectionModel().select(assignment);
                        break;
                    }
                }
            }
        } catch (NumberFormatException e) {
            // Ignore
        }
    }

    void onAssignmentSelected(Assignment assignment) {
        idField.setText(String.valueOf(assignment.id));
        nameField.setText(assignment.name);
        instructionsField.setText(assignment.instructions);
        answerKeyField.setText(assignment.answerKey);
        // Pastikan tombol delete diaktifkan saat assignment dipilih
        deleteButton.setDisable(assignment == null || idField.getText().isEmpty());
    }

    @FXML
    void onNewAssignmentClick(ActionEvent event) {
        idField.clear();
        nameField.clear();
        instructionsField.clear();
        answerKeyField.clear();
        // Nonaktifkan tombol delete saat tidak ada assignment yang dipilih
        deleteButton.setDisable(true);
    }

    @FXML
    void onSaveClick(ActionEvent event) {
        if (idField.getText().isEmpty()) {
            try (Connection c = MainDataSource.getConnection()) {
                PreparedStatement stmt = c.prepareStatement("INSERT INTO assignments (name, instructions, answer_key) VALUES (?, ?, ?)", Statement.RETURN_GENERATED_KEYS);
                stmt.setString(1, nameField.getText());
                stmt.setString(2, instructionsField.getText());
                stmt.setString(3, answerKeyField.getText());
                stmt.executeUpdate();
                ResultSet rs = stmt.getGeneratedKeys();
                if (rs.next()) {
                    idField.setText(String.valueOf(rs.getLong(1)));
                }
            } catch (Exception e) {
                showAlert("Error", "Database Error", e.toString());
            }
        } else {
            try (Connection c = MainDataSource.getConnection()) {
                PreparedStatement stmt = c.prepareStatement("UPDATE assignments SET name = ?, instructions = ?, answer_key = ? WHERE id = ?");
                stmt.setString(1, nameField.getText());
                stmt.setString(2, instructionsField.getText());
                stmt.setString(3, answerKeyField.getText());
                stmt.setInt(4, Integer.parseInt(idField.getText()));
                stmt.executeUpdate();
            } catch (Exception e) {
                showAlert("Error", "Database Error", e.toString());
            }
        }
        refreshAssignmentList();
    }

    @FXML
    void onShowGradesClick(ActionEvent event) {
        if (idField.getText().isEmpty()) {
            showAlert("Error", "No Assignment Selected", "Please select an assignment to view grades.");
            return;
        }
        Stage gradeStage = new Stage();
        gradeStage.setTitle("Grades");

        TableView<Grade> gradeTable = new TableView<>();

        TableColumn<Grade, Long> userIdColumn = new TableColumn<>("User ID");
        userIdColumn.setCellValueFactory(new PropertyValueFactory<>("userId"));

        TableColumn<Grade, String> usernameColumn = new TableColumn<>("Username");
        usernameColumn.setCellValueFactory(new PropertyValueFactory<>("username"));

        TableColumn<Grade, Long> assignmentColumn = new TableColumn<>("Assignment ID");
        assignmentColumn.setCellValueFactory(new PropertyValueFactory<>("assignmentId"));

        TableColumn<Grade, Double> gradeColumn = new TableColumn<>("Grade");
        gradeColumn.setCellValueFactory(new PropertyValueFactory<>("score"));

        gradeTable.getColumns().addAll(userIdColumn, usernameColumn, assignmentColumn, gradeColumn);

        ObservableList<Grade> gradeList = fetchGradeFromDatabase();
        gradeTable.setItems(gradeList);

        StackPane root = new StackPane();
        root.getChildren().add(gradeTable);
        Scene scene = new Scene(root, 600, 400);
        gradeStage.setScene(scene);
        gradeStage.show();
    }

    private ObservableList<Grade> fetchGradeFromDatabase() {
        ObservableList<Grade> gradeList = FXCollections.observableArrayList();
        try (Connection c = MainDataSource.getConnection()) {
            String query = "SELECT g.user_id, u.username, g.assignment_id, g.grade " +
                    "FROM grades g " +
                    "JOIN users u ON g.user_id = u.id " +
                    "WHERE g.assignment_id = ?";
            PreparedStatement stmt = c.prepareStatement(query);
            stmt.setLong(1, Long.parseLong(idField.getText()));
            ResultSet rs = stmt.executeQuery();

            while (rs.next()) {
                Grade grade = new Grade();
                grade.setUserId(rs.getLong("user_id"));
                grade.setAssignmentId(rs.getLong("assignment_id"));
                grade.setScore(rs.getDouble("grade"));
                grade.setUsername(rs.getString("username")); // Set username
                gradeList.add(grade);
            }

        } catch (SQLException e) {
            e.printStackTrace();
            showAlert("Database Error", "Failed to load grades", "Could not retrieve grades from the database.");
        }
        return gradeList;
    }

    @FXML
    void onTestButtonClick(ActionEvent event) {
        // Display a window containing the results of the answer key query.

        // Create a new window/stage
        Stage stage = new Stage();
        stage.setTitle("Answer Key Results");

        // Display in a table view.
        TableView<ArrayList<String>> tableView = new TableView<>();

        ObservableList<ArrayList<String>> data = FXCollections.observableArrayList();
        ArrayList<String> headers = new ArrayList<>(); // To check if any columns were returned

        // Use try-with-resources for automatic closing of Connection, Statement, ResultSet
        try (Connection conn = GradingDataSource.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(answerKeyField.getText())) {

            ResultSetMetaData metaData = rs.getMetaData();
            int columnCount = metaData.getColumnCount();

            // 1. Get Headers and Create Table Columns
            for (int i = 1; i <= columnCount; i++) {
                final int columnIndex = i - 1; // Need final variable for lambda (0-based index for ArrayList)
                String headerText = metaData.getColumnLabel(i); // Use label for potential aliases
                headers.add(headerText); // Keep track of headers

                TableColumn<ArrayList<String>, String> column = new TableColumn<>(headerText);

                // Define how to get the cell value for this column from an ArrayList<String> row object
                column.setCellValueFactory(cellData -> {
                    ArrayList<String> rowData = cellData.getValue();
                    // Ensure rowData exists and the index is valid before accessing
                    if (rowData != null && columnIndex < rowData.size()) {
                        return new SimpleStringProperty(rowData.get(columnIndex));
                    } else {
                        return new SimpleStringProperty(""); // Should not happen with current logic, but safe fallback
                    }
                });
                column.setPrefWidth(120); // Optional: set a preferred width
                tableView.getColumns().add(column);
            }

            // 2. Get Data Rows
            while (rs.next()) {
                ArrayList<String> row = new ArrayList<>();
                for (int i = 1; i <= columnCount; i++) {
                    // Retrieve all data as String. Handle NULLs gracefully.
                    String value = rs.getString(i);
                    row.add(value != null ? value : ""); // Add empty string for SQL NULL
                }
                data.add(row);
            }

            // 3. Check if any results (headers or data) were actually returned
            if (headers.isEmpty() && data.isEmpty()) {
                // Handle case where query might be valid but returns no results
                Alert infoAlert = new Alert(Alert.AlertType.INFORMATION);
                infoAlert.setTitle("Query Results");
                infoAlert.setHeaderText(null);
                infoAlert.setContentText("The answer key query executed successfully but returned no data.");
                infoAlert.showAndWait();
                return; // Exit the method, don't show the empty table window
            }

            // 4. Set the data items into the table
            tableView.setItems(data);

            // 5. Create layout and scene
            StackPane root = new StackPane();
            root.getChildren().add(tableView);
            Scene scene = new Scene(root, 800, 600); // Adjust size as needed

            // 6. Set scene and show stage
            stage.setScene(scene);
            stage.show();

        } catch (SQLException e) {
            // Log the error and show an alert to the user
            e.printStackTrace(); // Print stack trace to console/log for debugging
            Alert errorAlert = new Alert(Alert.AlertType.ERROR);
            errorAlert.setTitle("Database Error");
            errorAlert.setHeaderText("Failed to execute the answer key query or retrieve results.");
            errorAlert.setContentText("SQL Error: " + e.getMessage());
            errorAlert.showAndWait();
        } catch (Exception e) {
            // Catch other potential exceptions (e.g., class loading if driver not found)
            e.printStackTrace(); // Print stack trace to console/log for debugging
            Alert errorAlert = new Alert(Alert.AlertType.ERROR);
            errorAlert.setTitle("Error");
            errorAlert.setHeaderText("An unexpected error occurred.");
            errorAlert.setContentText(e.getMessage());
            errorAlert.showAndWait();
        }
    } // End of onTestButtonClick method

    @FXML
    void onDeleteClick(ActionEvent event) {
        // Metode ini sudah ada dan terhubung ke tombol delete di bagian atas.
        // Kita akan memindahkan logika penghapusan ke metode onDeleteAssignmentClick.
        onDeleteAssignmentClick(event);
    }

    @FXML
    void onDeleteAssignmentClick(ActionEvent event) {
        // Pastikan ada tugas yang dipilih
        if (idField.getText().isEmpty()) {
            showAlert("Error", "No Assignment Selected", "Please select an assignment to delete.");
            return;
        }

        Alert confirmation = new Alert(Alert.AlertType.CONFIRMATION);
        confirmation.setTitle("Confirmation");
        confirmation.setHeaderText("Delete Assignment");
        confirmation.setContentText("Are you sure you want to delete the assignment: " + nameField.getText() + "?");

        confirmation.showAndWait().ifPresent(response -> {
            if (response == ButtonType.OK) {
                try (Connection c = MainDataSource.getConnection()) {
                    // Hapus nilai terkait terlebih dahulu (opsional, tergantung kebutuhan)
                    PreparedStatement deleteGradesStmt = c.prepareStatement("DELETE FROM grades WHERE assignment_id = ?");
                    deleteGradesStmt.setInt(1, Integer.parseInt(idField.getText()));
                    deleteGradesStmt.executeUpdate();

                    // Hapus tugas
                    PreparedStatement deleteAssignmentStmt = c.prepareStatement("DELETE FROM assignments WHERE id = ?");
                    deleteAssignmentStmt.setInt(1, Integer.parseInt(idField.getText()));
                    int rowsAffected = deleteAssignmentStmt.executeUpdate();

                    if (rowsAffected > 0) {
                        showAlert("Success", "Assignment Deleted", "Successfully deleted the assignment: " + nameField.getText());
                        refreshAssignmentList();
                        onNewAssignmentClick(new ActionEvent()); // Kosongkan form
                    } else {
                        showAlert("Error", "Delete Failed", "Failed to delete the assignment.");
                    }
                } catch (SQLException e) {
                    e.printStackTrace();
                    showAlert("Database Error", "Delete Failed", "Could not delete the assignment from the database.");
                }
            }
        });
    }

    private void showAlert(String title, String header, String content) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle(title);
        alert.setHeaderText(header);
        alert.setContentText(content);
        alert.showAndWait();
    }
}