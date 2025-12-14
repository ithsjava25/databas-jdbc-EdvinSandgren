package com.example;

import java.sql.*;
import java.util.Arrays;
import java.util.Scanner;

public class Main {

    static void main(String[] args) {
        if (isDevMode(args)) {
            DevDatabaseInitializer.start();
        }
        new Main().run();
    }

    public void run() {
        // Resolve DB settings with precedence: System properties -> Environment variables
        String jdbcUrl = resolveConfig("APP_JDBC_URL", "APP_JDBC_URL");
        String dbUser = resolveConfig("APP_DB_USER", "APP_DB_USER");
        String dbPass = resolveConfig("APP_DB_PASS", "APP_DB_PASS");

        if (jdbcUrl == null || dbUser == null || dbPass == null) {
            throw new IllegalStateException(
                    "Missing DB configuration. Provide APP_JDBC_URL, APP_DB_USER, APP_DB_PASS " +
                            "as system properties (-Dkey=value) or environment variables.");
        }

        try (Connection connection = DriverManager.getConnection(jdbcUrl, dbUser, dbPass)) {
            Scanner scanner = new Scanner(System.in);
            System.out.println("Enter username:");
            var user = scanner.nextLine();
            System.out.println("Enter password:");
            var password = scanner.nextLine();

            boolean validCredentials = false;

            String query = "select * from account";

            try (PreparedStatement statement = connection.prepareStatement(query)) {
                ResultSet result = statement.executeQuery();
                while (result.next()) {
                    if ((result.getString(2) + " " + result.getString(3))
                            .equals(user + " " + password)) {
                        validCredentials = true;
                    }
                }
            }

            if (!validCredentials) {
                IO.println("Invalid username or password");
            }

            var option = scanner.nextLine();

            switch (option) {
                case "1" -> listMissions(connection);
                case "2" -> listMissionById(connection, scanner);
                case "3" -> listMissionsPerYear(connection, scanner);
                case "4" -> createAccount(connection, scanner);
                case "5" -> updatePassword(connection, scanner);
                case "6" -> deleteAccount(connection, scanner);
                default -> System.out.println("Invalid option");
            }

        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
        //Todo: Starting point for your code
    }

    private void deleteAccount(Connection connection, Scanner scanner) throws SQLException {

        System.out.println("Enter userID:");
        String userId = scanner.nextLine();

        String delete = "delete from account where user_id = ?";

        try (PreparedStatement statement = connection.prepareStatement(delete)) {
            statement.setString(1, userId);
            statement.executeUpdate();

            System.out.println("Deleted account");
        }
    }

    private void updatePassword(Connection connection, Scanner scanner) throws SQLException {

        System.out.println("Enter userID:");
        String userId = scanner.nextLine();

        System.out.println("Enter new password:");
        String password = scanner.nextLine();

        String update = "update account set password = ? where user_id = ?";

        try (PreparedStatement statement = connection.prepareStatement(update)) {
            statement.setString(1, password);
            statement.setString(2, userId);
            statement.executeUpdate();

            System.out.println("Updated password");
        }

    }

    private void createAccount(Connection connection, Scanner scanner) throws SQLException {

        System.out.println("Enter first name:");
        String firstName = scanner.nextLine();

        System.out.println("Enter last name:");
        String lastName = scanner.nextLine();

        System.out.println("Enter SSN:");
        String ssn = scanner.nextLine();

        System.out.println("Enter password:");
        String password = scanner.nextLine();

        String name = firstName.substring(0, 2) + lastName.substring(0, 2);

        String insert = "insert into account (name, password, first_name, last_name, ssn) values (?, ?, ?, ?, ?)";

        try (PreparedStatement statement = connection.prepareStatement(insert)) {
            statement.setString(1, name);
            statement.setString(2, password);
            statement.setString(3, firstName);
            statement.setString(4, lastName);
            statement.setString(5, ssn);
            statement.executeUpdate();

            System.out.println("Account created successfully");
        }
    }

    private void listMissionsPerYear(Connection connection, Scanner scanner) throws SQLException {

        System.out.println("Enter year:");
        int year = scanner.nextInt();

        String query = "select count(launch_date) from moon_mission where year(launch_date) = ?";

        try (PreparedStatement statement = connection.prepareStatement(query)) {
            statement.setInt(1, year);
            ResultSet result = statement.executeQuery();
            while (result.next()) {
                System.out.println("Missions in " + year + ": " +  result.getLong(1));
            }
        }
    }

    private void listMissionById(Connection connection, Scanner scanner) throws SQLException {

        System.out.println("Enter mission ID:");
        var missionId = scanner.nextLine();

        String query = "select * from moon_mission where mission_id = ?";

        try (PreparedStatement statement = connection.prepareStatement(query)) {
            statement.setString(1, missionId);
            ResultSet result = statement.executeQuery();
            while (result.next()) {
                System.out.println(result.getString(2));
            }
        }
    }

    private void listMissions(Connection connection) throws SQLException {

        String query = "select * from moon_mission";

        try (PreparedStatement statement = connection.prepareStatement(query)) {
            ResultSet result = statement.executeQuery();
            while (result.next()) {
                System.out.println(result.getString(2));
            }
        }
    }

    /**
     * Determines if the application is running in development mode based on system properties,
     * environment variables, or command-line arguments.
     *
     * @param args an array of command-line arguments
     * @return {@code true} if the application is in development mode; {@code false} otherwise
     */
    private static boolean isDevMode(String[] args) {
        if (Boolean.getBoolean("devMode"))  //Add VM option -DdevMode=true
            return true;
        if ("true".equalsIgnoreCase(System.getenv("DEV_MODE")))  //Environment variable DEV_MODE=true
            return true;
        return Arrays.asList(args).contains("--dev"); //Argument --dev
    }

    /**
     * Reads configuration with precedence: Java system property first, then environment variable.
     * Returns trimmed value or null if neither source provides a non-empty value.
     */
    private static String resolveConfig(String propertyKey, String envKey) {
        String v = System.getProperty(propertyKey);
        if (v == null || v.trim().isEmpty()) {
            v = System.getenv(envKey);
        }
        return (v == null || v.trim().isEmpty()) ? null : v.trim();
    }
}
