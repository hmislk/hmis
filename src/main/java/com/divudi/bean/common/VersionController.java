package com.divudi.bean.common;

import java.io.BufferedReader;
import javax.inject.Named;
import javax.enterprise.context.ApplicationScoped;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.Properties;

/**
 *
 * @author L C J Samarasekara <lawan.chaamindu1234@gmail.com>
 */
@Named
@ApplicationScoped
public class VersionController {

    private String systemVersion; // Stores the system version read from VERSION.txt

    // Build / Git metadata populated from git.properties (generated at build time)
    private String gitCommitIdAbbrev;
    private String gitBranch;
    private String gitBuildTime;

    public VersionController() {
        readFirstLine();
        loadGitInfo();
    }

    public String navigateToAboutSoftware() {
        return "/about_software?faces-redirect=true";
    }

    public void readFirstLine() {
        try {
            InputStream inputStream = getClass().getClassLoader().getResourceAsStream("VERSION.txt");
            if (inputStream != null) {
                try (BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream, StandardCharsets.UTF_8))) {
                    String firstLine = reader.readLine();
                    if (firstLine != null && !firstLine.isEmpty()) {
                        systemVersion = firstLine.trim();
                    } else {
                        systemVersion = null;
                    }
                }
            } else {
                systemVersion = null;
            }
        } catch (Exception e) {
            e.printStackTrace();
            systemVersion = null;
        }
    }

    private void loadGitInfo() {
        try (InputStream is = getClass().getClassLoader().getResourceAsStream("git.properties")) {
            if (is != null) {
                Properties props = new Properties();
                props.load(is);
                gitCommitIdAbbrev = props.getProperty("git.commit.id.abbrev", null);
                gitBranch = props.getProperty("git.branch", null);
                // Prefer a formatted build time if configured by the build plugin
                String bt = props.getProperty("git.build.time", null);
                gitBuildTime = (bt == null || bt.isEmpty()) ? null : bt;
            } else {
                gitCommitIdAbbrev = null;
                gitBranch = null;
                gitBuildTime = null;
            }
        } catch (Exception e) {
            e.printStackTrace();
            gitCommitIdAbbrev = null;
            gitBranch = null;
            gitBuildTime = null;
        }
    }

    public String getSystemVersion() {
        // Reload in case resource changed across redeploys
        readFirstLine();
        return systemVersion;
    }

    public String getGitCommitIdAbbrev() {
        return gitCommitIdAbbrev;
    }

    public String getGitBranch() {
        return gitBranch;
    }

    public String getGitBuildTime() {
        return gitBuildTime;
    }
}
