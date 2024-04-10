package com.divudi.bean.common;

import java.io.BufferedReader;
import javax.inject.Named;
import javax.enterprise.context.ApplicationScoped;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import javax.annotation.PostConstruct;

/**
 *
 * @author L C J Samarasekara <lawan.chaamindu1234@gmail.com>
 */
@Named
@ApplicationScoped
public class VersionController {

    private String systemVersion; // Public vareiable to store the system version read from the file

    public VersionController() {
//        readFirstLine(); // Load first line content upon bean instantiation
    }

    @PostConstruct
    public void readFirstLine() {
        try {
            // Use getClassLoader() to load the VERSION.txt file from src/main/resources
            InputStream inputStream = getClass().getClassLoader().getResourceAsStream("VERSION.txt");
            if (inputStream != null) {
                try ( BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream, StandardCharsets.UTF_8))) {
                    String firstLine = reader.readLine();
                    if (firstLine != null && !firstLine.isEmpty()) {
                        systemVersion = firstLine.trim();
                    } else {
                        systemVersion = "0.0.0.0"; // Set to a default or indicate unavailable
                    }
                } // InputStream and BufferedReader are auto-closed here
            } else {
                // Handle case where VERSION.txt does not exist or could not be found
                systemVersion = "0.0.0.0"; // Indicate that the version is unavailable
            }
        } catch (Exception e) {
            e.printStackTrace();
            // Handle any exceptions, e.g., file not found, read errors
            systemVersion = "0.0.0.0"; // Default version or indicate unavailable
        }
    }

    // Getter for systemVersion (to make it accessible from XHTML)
    public String getSystemVersion() {
        return systemVersion;
    }

}
