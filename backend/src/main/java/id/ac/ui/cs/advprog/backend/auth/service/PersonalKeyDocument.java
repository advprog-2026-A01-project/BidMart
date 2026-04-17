package id.ac.ui.cs.advprog.backend.auth.service;

import id.ac.ui.cs.advprog.backend.auth.model.Role;

public record PersonalKeyDocument(
        String username,
        String legalName,
        Role role,
        String rawKey,
        String issuedAtIso
) {}