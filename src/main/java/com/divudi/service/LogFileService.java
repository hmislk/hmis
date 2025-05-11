package com.divudi.service;

/*  LogFileService.java
    Contributed by ChatGPT on 2025-05-12  */
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.*;
import java.util.*;
import java.util.stream.*;
import javax.ejb.Stateless;
import org.primefaces.model.DefaultStreamedContent;
import org.primefaces.model.StreamedContent;

@Stateless
public class LogFileService {

    public List<Path> listAll(Path dir) throws IOException {
        System.out.println("[LOG-DL] listAll() scanning: " + dir);
        try (Stream<Path> s = Files.list(dir)) {
            List<Path> result = s.filter(Files::isRegularFile)
                    .sorted(Comparator.comparing(Path::getFileName).reversed())
                    .collect(Collectors.toList());
            System.out.println("[LOG-DL] listAll() found " + result.size() + " regular files");
            return result;
        }
    }

    public StreamedContent download(Path p) throws IOException {
        InputStream in = Files.newInputStream(p);
        return DefaultStreamedContent.builder()
                .name(p.getFileName().toString())
                .contentType("application/octet-stream")
                .stream(() -> in)
                .build();
    }
}
