package com.leadflow.config;

import org.hibernate.SessionFactory;
import org.hibernate.cfg.Configuration;

public class DatabaseConfig {
    public static final String JDBC_URL = "jdbc:postgresql://ep-round-salad-be5rjzht-pooler.c-9.us-east-2.aws.neon.tech/neondb?sslmode=require";
    public static final String DB_USER = "neondb_owner";
    public static final String DB_PASSWORD = "npg_SdgB8X1eyQha";

    private static SessionFactory sessionFactory;

    /**
     * Initializes and returns the singleton Hibernate SessionFactory.
     *
     * @return configured SessionFactory instance
     */
    public static synchronized SessionFactory getSessionFactory() {
        if (sessionFactory == null) {
            try {
                Configuration cfg = new Configuration().configure("hibernate.cfg.xml");
                cfg.setProperty("hibernate.connection.url", JDBC_URL);
                cfg.setProperty("hibernate.connection.username", DB_USER);
                cfg.setProperty("hibernate.connection.password", DB_PASSWORD);
                sessionFactory = cfg.buildSessionFactory();
            } catch (Exception e) {
                throw new RuntimeException("Failed to initialize Hibernate SessionFactory: " + e.getMessage(), e);
            }
        }
        return sessionFactory;
    }

    /**
     * Shuts down the Hibernate SessionFactory gracefully on application exit.
     */
    public static void shutdown() {
        if (sessionFactory != null && !sessionFactory.isClosed()) {
            sessionFactory.close();
        }
    }
}