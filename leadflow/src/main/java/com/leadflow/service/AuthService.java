package com.leadflow.service;

import com.leadflow.dao.HibernateRepository;
import com.leadflow.model.Role;
import com.leadflow.model.User;

import java.util.List;
import java.util.Optional;

/**
 * Handles user authentication, account creation, and default seed initialization.
 */
public class AuthService {

    private final HibernateRepository repository;

    /**
     * Constructs AuthService backed by HibernateRepository.
     *
     * @param repository data repository
     */
    public AuthService(HibernateRepository repository) {
        this.repository = repository;
    }

    /**
     * Seeds default Owner and Employee accounts if the database is empty.
     */
    public void seedDefaultUsersIfEmpty() {
        if (repository.findUsersByRole(Role.OWNER).isEmpty()) {
            repository.save(new User("Ali Saad (Owner)", "owner@leadflow.com", "admin123", Role.OWNER, 10));
            repository.save(new User("Hassan Jalil", "hassan@leadflow.com", "emp123", Role.EMPLOYEE, 3));
            repository.save(new User("Bilal Ahmed", "bilal@leadflow.com", "emp123", Role.EMPLOYEE, 2));
            repository.save(new User("Sara Khan", "sara@leadflow.com", "emp123", Role.EMPLOYEE, 1));
        }
    }

    /**
     * Authenticates a user by email and password.
     *
     * @param email    login email
     * @param password plaintext password
     * @return Optional containing authenticated User if credentials match
     */
    public Optional<User> authenticate(String email, String password) {
        return repository.findUserByEmail(email.trim())
                .filter(u -> u.getPasswordHash().equals(password));
    }

    /**
     * Registers a new Employee account.
     *
     * @param name     employee full name
     * @param email    unique email
     * @param password login password
     * @param weight   priority routing weight (1-10)
     * @return created User entity
     */
    public User createEmployee(String name, String email, String password, int weight) {
        User emp = new User(name, email.trim(), password, Role.EMPLOYEE, weight);
        repository.save(emp);
        return emp;
    }

    /**
     * Toggles an employee's availability flag.
     *
     * @param user target employee
     */
    public void toggleEmployeeAvailability(User user) {
        user.toggleAvailability();
        repository.update(user);
    }

    /**
     * Retrieves all employees.
     *
     * @return list of employees
     */
    public List<User> getAllEmployees() {
        return repository.findUsersByRole(Role.EMPLOYEE);
    }
}