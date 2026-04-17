package id.ac.ui.cs.advprog.backend.auth.service;

public interface PersonalKeyService {
    String generateRawKey();

    String buildDownloadFilename(String username);

    String buildDownloadContents(PersonalKeyDocument document);
}