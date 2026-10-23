package com.scc;

import com.scc.dao.DatabaseInitializer;
import com.scc.dao.UserDao;
import com.scc.ui.Authentication;

import java.util.Scanner;

public class Main {
    public static void main(String[] args) {
        // Initialize Database & tables
        DatabaseInitializer.initializeDatabase();

        // Instantiate shared DAOs and utilities
        UserDao userDao = new UserDao();
        Scanner scanner = new Scanner(System.in);

        // Delegate execution to Authentication UI class
        Authentication auth = new Authentication(userDao, scanner);
        auth.start();
    }
}