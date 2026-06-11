package com.gtalent.util;

public class Validator {
    public static boolean isValidName(String name) {
        return name != null && !name.trim().isEmpty();
    }

    public static boolean isValidPhone(String phone) {
        if (phone == null || phone.isEmpty()) return false;
        return phone.matches("^09\\d{8}$");
    }

    public static boolean isValidLineAccount(String line) {
        return line != null && !line.trim().isEmpty();
    }
}
