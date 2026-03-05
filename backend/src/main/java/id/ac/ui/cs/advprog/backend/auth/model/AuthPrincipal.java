package id.ac.ui.cs.advprog.backend.auth.model;

/*
Tanggung jawab: representasi identitas user immutable di SecurityContext.
role disimpan sebagai STRING agar mendukung custom role runtime.
 */

public record AuthPrincipal(long userId, String username, String role) {}