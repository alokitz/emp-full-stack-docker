package com.example.demo.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.UUID;

@Service
public class FileStorageService {
    @Value("${file.storage.path:${user.home}/uploads}")
    private String storagePath;

    public Path init() throws Exception {
        Path p = Paths.get(storagePath);
        if (!Files.exists(p)) Files.createDirectories(p);
        return p;
    }

    public File save(MultipartFile multipartFile) throws Exception {
        init();
        String filename = UUID.randomUUID().toString() + "_" + multipartFile.getOriginalFilename();
        Path dest = Paths.get(storagePath).resolve(filename);
        multipartFile.transferTo(dest.toFile());
        return dest.toFile();
    }
}
