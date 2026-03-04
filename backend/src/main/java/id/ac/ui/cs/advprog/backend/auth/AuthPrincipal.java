package id.ac.ui.cs.advprog.backend.auth;

/*
Tanggung jawab: representasi identitas user immutable di SecurityContext.
 */
public record AuthPrincipal(long userId, String username, Role role) {}