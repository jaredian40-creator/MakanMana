package com.example.makanmana;

import com.google.firebase.auth.FirebaseAuthInvalidCredentialsException;
import com.google.firebase.auth.FirebaseAuthInvalidUserException;
import com.google.firebase.auth.FirebaseAuthUserCollisionException;
import com.google.firebase.auth.FirebaseAuthWeakPasswordException;

/**
 * Turns raw Firebase auth exceptions into short, friendly messages so users see
 * "Incorrect email or password" instead of a technical stack message.
 */
public final class AuthErrors {

    private AuthErrors() {
    }

    public static String forLogin(Exception e) {
        if (e instanceof FirebaseAuthInvalidUserException
                || e instanceof FirebaseAuthInvalidCredentialsException) {
            return "Incorrect email or password";
        }
        if (isNetwork(e)) return "No internet connection. Please try again.";
        return "Login failed. Please try again.";
    }

    public static String forRegister(Exception e) {
        if (e instanceof FirebaseAuthUserCollisionException) {
            return "That email is already registered. Try logging in.";
        }
        if (e instanceof FirebaseAuthWeakPasswordException) {
            return "Password is too weak (use at least 6 characters).";
        }
        if (e instanceof FirebaseAuthInvalidCredentialsException) {
            return "Please enter a valid email address.";
        }
        if (isNetwork(e)) return "No internet connection. Please try again.";
        return "Registration failed. Please try again.";
    }

    private static boolean isNetwork(Exception e) {
        return e instanceof com.google.firebase.FirebaseNetworkException;
    }
}
