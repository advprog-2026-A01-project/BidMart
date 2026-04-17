package id.ac.ui.cs.advprog.backend.auth.service;

import id.ac.ui.cs.advprog.backend.auth.model.Role;
import org.springframework.web.multipart.MultipartFile;

public record IdentityRegistrationCommand(
        String username,
        String password,
        String confirmPassword,
        String legalName,
        Role role,
        String documentType,
        String documentExtractedText,
        MultipartFile documentImage
) {}