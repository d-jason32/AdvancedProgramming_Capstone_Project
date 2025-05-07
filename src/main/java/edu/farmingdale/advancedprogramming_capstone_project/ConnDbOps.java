package edu.farmingdale.advancedprogramming_capstone_project;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;

/**
 * Methods for database use.
 * @author Jason Devaraj
 */
public class ConnDbOps {
    final String MYSQL_SERVER_URL = "jdbc:mysql://csc311serverjason.mysql.database.azure.com";
    final String DB_URL = MYSQL_SERVER_URL + "/" + "new_table";
    final String USERNAME = "eagle";
    final String PASSWORD = "usxCi90RWtiwtt";

    /**
     * Method to connect to a database.
     * @return Registered users
     */
    public  boolean connectToDatabase() {
        boolean hasRegisteredUsers = false;

        //Class.forName("com.mysql.jdbc.Driver");
        try {
            System.out.println("Trying to connect.");
            //First, connect to the MYSQL server and create the database if not created
            Connection conn = DriverManager.getConnection(MYSQL_SERVER_URL, USERNAME, PASSWORD);
            System.out.println("Connected.");
            Statement statement = conn.createStatement();
            statement.executeUpdate("CREATE DATABASE IF NOT EXISTS new_table");
            System.out.println("Database created.");
            statement.close();
            conn.close();
            System.out.println("Closed");
            //Second, connect to the database and create the table "users" if not created
            conn = DriverManager.getConnection(DB_URL, USERNAME, PASSWORD);
            statement = conn.createStatement();
            String sql = "CREATE TABLE IF NOT EXISTS users ("
                    + "id INT( 10 ) NOT NULL PRIMARY KEY,"
                    + "first_name VARCHAR(200) NOT NULL,"
                    + "last_name VARCHAR(200) NOT NULL,"
                    + "email VARCHAR(200),"
                    + "password VARCHAR(200)"
                    + ")";
            statement.executeUpdate(sql);

            //check if we have users in the table users
            statement = conn.createStatement();
            ResultSet resultSet = statement.executeQuery("SELECT COUNT(*) FROM users");

            if (resultSet.next()) {
                int numUsers = resultSet.getInt(1);
                if (numUsers > 0) {
                    hasRegisteredUsers = true;
                }
            }
            statement.close();
            conn.close();

        } catch (Exception e) {
            e.printStackTrace();
        }
        return hasRegisteredUsers;
    }

    /**
     * Method to search for a student using an id number.
     * @param id Person object
     */
    public  void queryUserById(String id) {
        try {
            Connection conn = DriverManager.getConnection(DB_URL, USERNAME, PASSWORD);
            // Select all where the id matches the student
            String sql = "SELECT * FROM users WHERE id = ?";
            PreparedStatement preparedStatement = conn.prepareStatement(sql);
            preparedStatement.setString(1, id);

            ResultSet resultSet = preparedStatement.executeQuery();
            while (resultSet.next()) {
                int idNum = resultSet.getInt("id");
                String firstName = resultSet.getString("first_name");
                String lastName = resultSet.getString("last_name");
                String email = resultSet.getString("email");
                String password = resultSet.getString("password");
                System.out.println("ID: " + idNum + ", First name: " + firstName + "Last name" + lastName + ", Email: " + email + ", Password: " + password);
            }

            preparedStatement.close();
            conn.close();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    /**
     * Method to search for a student using their email.
     * @param accEmail Person object
     */
    public String queryPasswordByEmail(String accEmail) {
        try (Connection conn = DriverManager.getConnection(DB_URL, USERNAME, PASSWORD);
             PreparedStatement preparedStatement = conn.prepareStatement(
                     "SELECT password FROM users WHERE email = ?")) {

            preparedStatement.setString(1, accEmail);
            ResultSet resultSet = preparedStatement.executeQuery();

            if (resultSet.next()) {
                System.out.println(resultSet.getString("password"));
                return resultSet.getString("password");
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return null;
    }

    /**
     * Method to search for a student using an id number.
     * @param id Person object
     * @return String
     */
    public String queryUser(String id) {
        String completeString = "";

        try {
            Connection conn = DriverManager.getConnection(DB_URL, USERNAME, PASSWORD);
            // Select all where the id matches the student
            String sql = "SELECT * FROM users WHERE id = ?";
            PreparedStatement preparedStatement = conn.prepareStatement(sql);
            preparedStatement.setString(1, id);

            ResultSet resultSet = preparedStatement.executeQuery();

            while (resultSet.next()) {
                int idNum = resultSet.getInt("id");
                String firstName = resultSet.getString("first_name");
                String lastName = resultSet.getString("last_name");
                String email = resultSet.getString("email");
                String password = resultSet.getString("password");
                completeString = "ID: " + idNum + ", First name: " + firstName + ", Last name: " + lastName + ", Email: " + email + ", Password: " + password;
            }
            preparedStatement.close();
            conn.close();
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return completeString;
    }

    /**
     * Method to delete a user.
     * @param id Person object
     */
    public void delete(String id) {

        try {
            Connection conn = DriverManager.getConnection(DB_URL, USERNAME, PASSWORD);
            // Delete a specific user whose id matches a student.
            String sql = "DELETE FROM users WHERE id = ?";
            PreparedStatement preparedStatement = conn.prepareStatement(sql);
            preparedStatement.setString(1, id);
            int deleted = preparedStatement.executeUpdate();

            preparedStatement.close();
            conn.close();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    /**
     * Method to list all the users.
     */
    public void listAllUsers() {
        try {
            Connection conn = DriverManager.getConnection(DB_URL, USERNAME, PASSWORD);
            String sql = "SELECT * FROM users ";
            PreparedStatement preparedStatement = conn.prepareStatement(sql);

            ResultSet resultSet = preparedStatement.executeQuery();

            while (resultSet.next()) {
                int idNum = resultSet.getInt("id");
                String firstName = resultSet.getString("first_name");
                String lastName = resultSet.getString("last_name");
                String email = resultSet.getString("email");
                String password = resultSet.getString("password");
                System.out.println("ID: " + idNum + ", First name: " + firstName + "Last name" + lastName + ", Email: " + email + ", Password: " + password);
            }

            preparedStatement.close();
            conn.close();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }


    /**
     * Method to return a list that contains all users in the database.
     * @return A list of type person
     */
    public List<Person> displayAllUsers() {
        List<Person> users = new ArrayList<>();
        try {
            Connection conn = DriverManager.getConnection(DB_URL, USERNAME, PASSWORD);
            String sql = "SELECT * FROM users ";
            PreparedStatement preparedStatement = conn.prepareStatement(sql);

            ResultSet resultSet = preparedStatement.executeQuery();

            while (resultSet.next()) {
                int idNum = resultSet.getInt("id");
                String firstName = resultSet.getString("first_name");
                String lastName = resultSet.getString("last_name");
                String email = resultSet.getString("email");
                String password = resultSet.getString("password");
                users.add(new Person(idNum, firstName, lastName, email, password));
            }

            preparedStatement.close();
            conn.close();
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return users;
    }

    /**
     * Inserts a user into the database.
     * @param id The unique identifier for the user
     * @param first_name The user's first name
     * @param last_name The user's last name
     * @param email The user's email address
     * @param password The user's password
     */
    public  void insertUser(String id, String first_name, String last_name, String email, String password) {
        try {
            Connection conn = DriverManager.getConnection(DB_URL, USERNAME, PASSWORD);
            String sql = "INSERT INTO users (id, first_name, last_name, email, password) VALUES (?, ?, ?, ?, ?)";
            PreparedStatement preparedStatement = conn.prepareStatement(sql);
            preparedStatement.setString(1, id);
            preparedStatement.setString(2, first_name);
            preparedStatement.setString(3, last_name);
            preparedStatement.setString(4, email);
            preparedStatement.setString(5, password);

            int row = preparedStatement.executeUpdate();
            if (row > 0) {
                System.out.println("A new user was inserted successfully.");
            }
            preparedStatement.close();
            conn.close();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    /**
     * Edits a student in the database if given the id number.
     * @param idNum Unique Identifier For Person
     * @param firstName User's First Name
     * @param lastName User's Last Name
     * @param email User's Email
     * @param password User's Password
     * @throws SQLException SQL Exception For Errors
     */
    public void editUser(String idNum, String firstName, String lastName, String email, String password) throws SQLException {
        String sql = "UPDATE users SET first_name = ?, last_name = ?, email = ?, password = ? WHERE id = ?";
        try{
            Connection conn = DriverManager.getConnection(DB_URL, USERNAME, PASSWORD);
            PreparedStatement preparedStatement = conn.prepareStatement(sql);
            preparedStatement.setString(5, idNum);
            preparedStatement.setString(1, firstName);
            preparedStatement.setString(2, lastName);
            preparedStatement.setString(3, email);
            preparedStatement.setString(4, password);

            int rowsUpdated = preparedStatement.executeUpdate();
        }
        catch (SQLException e) {
            e.printStackTrace();
        }
    }

    public class AuthService {
        private static List<String> authDB = new ArrayList<>();

        public static List<String> getAuthDB() {
            return authDB;
        }

        public void setAuthDB(List<String> authDB) {
            this.authDB = authDB;
        }


        public static void initializeAuthDB(ConnDbOps cdbop) {
            authDB.clear();
            List<Person> users = cdbop.displayAllUsers();

            for (Person user : users) {
                String email = user.getEmail();
                if (email != null && !email.trim().isEmpty()) {
                    authDB.add(email);
                }
            }

            System.out.println("Initialized authDB with " + authDB.size() + " emails");
        }
    }
}