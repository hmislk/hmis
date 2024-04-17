package com.divudi.bean.common;

import java.io.BufferedReader;
import javax.inject.Named;
import javax.enterprise.context.ApplicationScoped;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.io.IOException;
import java.net.URL;
import java.util.Scanner;

/**
 *
 * @author L C J Samarasekara <lawan.chaamindu1234@gmail.com>
 */
@Named
@ApplicationScoped
public class VersionController {

    private String systemVersion; // Public vareiable to store the system version read from the file
    private String latestVersion;

    public VersionController() {
        readFirstLine(); // Load first line content upon bean instantiation
        fetchLatestVersion(); // Fetch latest version upon bean instantiation
    }
    
    public String navigateToAboutSoftware(){
        return "/about_software?faces-redirect=true";
    }

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
                        systemVersion = null; // Set to a default or indicate unavailable
                    }
                } // InputStream and BufferedReader are auto-closed here
            } else {
                // Handle case where VERSION.txt does not exist or could not be found
                systemVersion = null; // Indicate that the version is unavailable
            }
        } catch (Exception e) {
            e.printStackTrace();
            // Handle any exceptions, e.g., file not found, read errors
            systemVersion = null; // Default version or indicate unavailable
        }
    }
    
    public void fetchLatestVersion() {
        try {
            // Create a URL object pointing to the VERSION.txt file in the GitHub repository
            URL url = new URL("https://raw.githubusercontent.com/hmislk/hmis/master/src/main/resources/VERSION.txt");

            // Open a connection to the URL
            try (Scanner scanner = new Scanner(url.openStream(), StandardCharsets.UTF_8)) {
                // Read the first line which contains the latest version
                if (scanner.hasNextLine()) {
                    latestVersion = scanner.nextLine().trim();
                } else {
                    latestVersion = null; // Set to a default or indicate unavailable
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
            // Handle any exceptions, e.g., URL not found, read errors
            latestVersion = null; // Default version or indicate unavailable
        }
    }

    // Getter for systemVersion (to make it accessible from XHTML)
    public String getSystemVersion() {
        return systemVersion;
    }

    public String getLatestVersion() {
        return latestVersion;
    }
    

}
