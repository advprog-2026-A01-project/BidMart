package id.ac.ui.cs.advprog.backend.auth.service;

import org.springframework.web.multipart.MultipartFile;

public interface IdentityDocumentValidationService {
    VerifiedIdentityDocument validate(String legalName, String documentType, String ocrText, MultipartFile documentImage);

    record VerifiedIdentityDocument(String normalizedLegalName, String normalizedOcrText, String documentType) {
    }
}
