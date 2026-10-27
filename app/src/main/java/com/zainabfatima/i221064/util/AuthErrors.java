package com.zainabfatima.i221064.util;

import com.google.firebase.FirebaseNetworkException;
import com.google.firebase.auth.FirebaseAuthException;
import com.google.firebase.auth.FirebaseAuthInvalidCredentialsException;
import com.google.firebase.auth.FirebaseAuthInvalidUserException;
import com.google.firebase.auth.FirebaseAuthUserCollisionException;
import com.google.firebase.auth.FirebaseAuthWeakPasswordException;

/** Turns Firebase Authentication exceptions into something a person can act on. */
public final class AuthErrors {

    private AuthErrors() { }

    public static String message(Exception e) {
        if (e instanceof FirebaseAuthUserCollisionException) return "That email is already registered. Try logging in.";
        if (e instanceof FirebaseAuthWeakPasswordException) return "That password is too weak. Use at least 6 characters.";
        if (e instanceof FirebaseAuthInvalidUserException) return "No account found for that email.";
        if (e instanceof FirebaseAuthInvalidCredentialsException) return "Incorrect email or password.";
        if (e instanceof FirebaseNetworkException) return "No internet connection.";
        if (e instanceof FirebaseAuthException) return "Authentication failed (" + ((FirebaseAuthException) e).getErrorCode() + ").";
        return e == null || e.getMessage() == null ? "Something went wrong." : e.getMessage();
    }
}
