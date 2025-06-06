package com.divudi.service;

/*  DataAdministrationController.java
    Contributed by ChatGPT on 2025-05-12  */
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.*;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.regex.*;
import java.util.stream.*;
import javax.ejb.Stateless;
import org.primefaces.model.DefaultStreamedContent;
import org.primefaces.model.StreamedContent;

@Stateless
public class LogFileService {

    private static final Pattern ROTATED
            = Pattern.compile("server\\.log_(\\d{4}-\\d{2}-\\d{2})T(\\d{2}-\\d{2}-\\d{2})");
    private static final SimpleDateFormat TS
            = new SimpleDateFormat("yyyy-MM-dd'T'HH-mm-ss");

    public List<Path> list(Path dir, Date from, Date to) throws IOException {
        try (Stream<Path> s = Files.list(dir)) {
            return s.filter(Files::isRegularFile)
                    .filter(p -> dateOf(p.getFileName().toString())
                    .map(d -> !d.before(from) && !d.after(to))
                    .orElse(true)) // include live server.log
                    .sorted(Comparator.comparing(Path::getFileName).reversed())
                    .collect(Collectors.toList());
        }
    }

    public StreamedContent download(Path p) throws IOException {
        return DefaultStreamedContent.builder()
                .name(p.getFileName().toString())
                .contentType("application/octet-stream")
                .stream(() -> {
                    try {
                        return Files.newInputStream(p);
                    } catch (IOException e) {
                        throw new RuntimeException("Error opening log file: " + p.getFileName(), e);
                    }
                })
                .build();
    }

    private Optional<Date> dateOf(String name) {
        Matcher m = ROTATED.matcher(name);
        if (!m.matches()) {
            return Optional.empty();
        }
        try {
            return Optional.of(TS.parse(m.group(1) + "T" + m.group(2)));
        } catch (ParseException e) {
            return Optional.empty();
        }
    }
}
