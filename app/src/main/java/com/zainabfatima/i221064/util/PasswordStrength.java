package com.zainabfatima.i221064.util;

/** Scores a password while it is typed (Weak / Medium / Strong). */
public final class PasswordStrength {

    public static final int NONE = 0, WEAK = 1, MEDIUM = 2, STRONG = 3;

    private PasswordStrength() { }

    public static int of(String p) {
        if (p == null || p.isEmpty()) return NONE;
        int score = 0;
        if (p.length() >= 8) score++;
        if (p.length() >= 12) score++;
        if (p.matches(".*[a-z].*") && p.matches(".*[A-Z].*")) score++;
        if (p.matches(".*\\d.*")) score++;
        if (p.matches(".*[^A-Za-z0-9].*")) score++;
        if (p.length() < 6) return WEAK;
        if (score <= 2) return WEAK;
        if (score == 3) return MEDIUM;
        return STRONG;
    }

    public static String label(int s) {
        switch (s) {
            case WEAK: return "Weak";
            case MEDIUM: return "Medium";
            case STRONG: return "Strong";
            default: return "";
        }
    }

    public static int color(int s) {
        switch (s) {
            case WEAK: return 0xFFBA4929;
            case MEDIUM: return 0xFFC98611;
            case STRONG: return 0xFF2F6B4F;
            default: return 0xFFE3DFD8;
        }
    }
}
