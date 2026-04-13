package id.ac.ui.cs.advprog.backend.auth.service;

import id.ac.ui.cs.advprog.backend.auth.model.Role;

public interface PersonalKeyService {
    String generateRawKey();

    String buildDownloadFilename(String username);

    String buildDownloadContents(String username, String legalName, Role role, String rawKey, String issuedAtIso);
}
