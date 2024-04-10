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

    public void readFirstLine() {
        try {
            // Check if the file exists, if not, create it
            java.nio.file.Path path = Paths.get(fileName);
            if (!Files.exists(path)) {
                Files.createFile(path);
            }

            // Read the first line from the file
            String firstLine = Files.lines(path).findFirst().orElse(null);
            if (firstLine != null && !firstLine.isEmpty()) {
                // Set systemVersion to the content of the first line
                systemVersion = firstLine.trim();
            } else {
                // If the first line is empty or the file does not exist, set systemVersion to null
                systemVersion = null;
            }
        } catch (IOException e) {
            // Handle IOException by printing the stack trace
            e.printStackTrace();
            // Set systemVersion to null if an IOException occurs
            systemVersion = null;
        }
    }

    // Getter for systemVersion (to make it accessible from XHTML)
    public String getSystemVersion() {
        return systemVersion;
    }

}
