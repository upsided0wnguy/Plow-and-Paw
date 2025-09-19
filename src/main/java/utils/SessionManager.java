package utils;

import main.models.User;

public class SessionManager {
    private static User currentUser;

    public static void setCurrentUser(User user) {
        if (user != null && user.getUsername() != null) {
            user.setUsername(user.getUsername().toLowerCase());
        }
        currentUser = user;
    }

    public static User getCurrentUser() {
        return currentUser;
    }

    public static void clearSession() {
        currentUser = null;
    }

    // Static utility to get the saved username
    public static String getSavedUsername() {
        return currentUser != null ? currentUser.getUsername() : null;
    }
}
