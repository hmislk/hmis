package com.divudi.bean.common;

import javax.inject.Named;
import javax.enterprise.context.ApplicationScoped;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.io.IOException;

/**
 *
 * @author L C J Samarasekara <lawan.chaamindu1234@gmail.com>
 */
@Named
@ApplicationScoped
public class VersionController {

    private final String fileName = "VERSION.txt";
    private String systemVersion; // Public vareiable to store the system version read from the file

    public VersionController() {
        readFirstLine(); // Load first line content upon bean instantiation
    }

    import java.io.File ;

    public void readFirstLine() {
        try {
            // Adjust the file path as necessary to match your deployment environment
            String filePath = System.getProperty("user.dir") + File.separator + fileName;
            java.nio.file.Path path = Paths.get(filePath);

            if (!Files.exists(path)) {
                Files.createFile(path);
            }

            String firstLine = Files.lines(path).findFirst().orElse("0.0.0.0");
            systemVersion = firstLine.trim();
        } catch (IOException e) {
            e.printStackTrace();
            systemVersion = "0.0.0.0"; // Default version in case of error
        }
    }

    // Getter for systemVersion (to make it accessible from XHTML)
    public String getSystemVersion() {
        return systemVersion;
    }

}
