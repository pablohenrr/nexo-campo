package com.nexocampo.modules.evidence;

import java.io.IOException;
import org.springframework.web.multipart.MultipartFile;

public interface FileStorage {
    String save(MultipartFile file) throws IOException;
    void delete(String storageKey) throws IOException;
}
