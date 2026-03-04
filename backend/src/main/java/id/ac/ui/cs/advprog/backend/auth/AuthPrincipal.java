package id.ac.ui.cs.advprog.backend.auth;

import id.ac.ui.cs.advprog.backend.auth.Role;

public record AuthPrincipal(long userId, String username, Role role) {

}