package edu.farmingdale.advancedprogramming_capstone_project;

/**
 * Represents a Person object.
 * @author Moaath Alrajab
 * @author Jason Devaraj
 */
public class Person {
    private Integer id;
    private String firstName;
    private String lastName;
    private String email;
    private String password;

    /**
     * Default constructor.
     */
    public Person() {
    }

    /**
     * Constructs a new Person object with the specified details.
     * @param id the unique identifier of the person
     * @param firstName the first name of the person
     * @param lastName the last name of the person
     * @param email the email address of the person
     * @param password the password of the person
     */
    public Person(Integer id, String firstName, String lastName, String email, String password) {
        this.id = id;
        this.firstName = firstName;
        this.lastName = lastName;
        this.email = email;
        this.password = password;
    }

    public Person(String email, String password){
        this.email = email;
        this.password = password;
    }


    /**
     * Gets the person's ID.
     * @return the ID
     */
    public int getId() {
        return id;
    }

    /**
     * Sets the person's ID.
     * @param id the ID to set
     */
    public void setId(Integer id) {
        this.id = id;
    }

    /**
     * Gets the person's first name.
     * @return the first name
     */
    public String getFirstName() {
        return firstName;
    }

    /**
     * Sets the person's first name.
     * @param firstName the first name to set
     */
    public void setFirstName(String firstName) {
        this.firstName = firstName;
    }

    /**
     * Gets the person's last name.
     * @return the last name
     */
    public String getLastName() {
        return lastName;
    }

    /**
     * Sets the person's last name.
     * @param lastName the last name to set
     */
    public void setLastName(String lastName) {
        this.lastName = lastName;
    }

    /**
     * Gets the person's email address.
     * @return the email address
     */
    public String getEmail() {
        return email;
    }

    /**
     * Sets the person's email address.
     * @param email the email address to set
     */
    public void setEmail(String email) {
        this.email = email;
    }

    /**
     * Gets the person's password.
     * @return the password
     */
    public String getPassword() {
        return password;
    }

    /**
     * Sets the person's password.
     * @param password the password to set
     */
    public void setPassword(String password) {
        this.password = password;
    }
}