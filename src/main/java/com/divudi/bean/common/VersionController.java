package com.divudi.bean.common;

import java.io.BufferedReader;
import javax.inject.Named;
import javax.enterprise.context.ApplicationScoped;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.time.Duration;
import java.util.Properties;
import javax.ejb.EJB;
import com.divudi.service.ApplicationStartupTimeService;

/**
 *
 * @author L C J Samarasekara <lawan.chaamindu1234@gmail.com>
 */
@Named
@ApplicationScoped
public class VersionController {

    @EJB
    private ApplicationStartupTimeService startupTimeService;

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
                String bt = props.getProperty("git.build.time", null);
                gitBuildTime = convertUtcToServerTimeZone(bt);
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

    /**
     * Converts UTC timestamp from git.properties to server's default time zone
     * @param utcTimestamp The UTC timestamp string (format: "yyyy-MM-dd HH:mm:ss UTC")
     * @return Formatted timestamp in server's default time zone, or null if parsing fails
     */
    private String convertUtcToServerTimeZone(String utcTimestamp) {
        if (utcTimestamp == null || utcTimestamp.isEmpty()) {
            return null;
        }

        try {
            // Parse the UTC timestamp (format: "yyyy-MM-dd HH:mm:ss UTC")
            DateTimeFormatter inputFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss 'UTC'");
            LocalDateTime localDateTime = LocalDateTime.parse(utcTimestamp, inputFormatter);

            // Convert to ZonedDateTime in UTC
            ZonedDateTime utcTime = ZonedDateTime.of(localDateTime, ZoneId.of("UTC"));

            // Convert to server's default time zone
            ZonedDateTime serverTime = utcTime.withZoneSameInstant(ZoneId.systemDefault());

            // Format the output with time zone abbreviation
            DateTimeFormatter outputFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss z");
            return serverTime.format(outputFormatter);
        } catch (Exception e) {
            // If parsing fails, return the original timestamp
            e.printStackTrace();
            return utcTimestamp;
        }
    }

    public String getSystemVersion() {
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

    /**
     * Gets the time elapsed since the build in a human-readable format
     * @return A string like "2 minutes ago", "3 hours ago", "14 days ago", etc.
     */
    public String getGitBuildTimeElapsed() {
        if (gitBuildTime == null || gitBuildTime.isEmpty()) {
            return null;
        }

        try {
            // Parse the formatted build time back to ZonedDateTime
            DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss z");
            ZonedDateTime buildTime = ZonedDateTime.parse(gitBuildTime, formatter);
            ZonedDateTime now = ZonedDateTime.now(buildTime.getZone());

            Duration duration = Duration.between(buildTime, now);
            long totalMinutes = duration.toMinutes();
            long totalHours = duration.toHours();
            long totalDays = duration.toDays();

            if (totalMinutes < 1) {
                return "just now";
            } else if (totalMinutes < 60) {
                return totalMinutes == 1 ? "1 minute ago" : totalMinutes + " minutes ago";
            } else if (totalHours < 24) {
                return totalHours == 1 ? "1 hour ago" : totalHours + " hours ago";
            } else if (totalDays < 30) {
                return totalDays == 1 ? "1 day ago" : totalDays + " days ago";
            } else if (totalDays < 365) {
                long months = totalDays / 30;
                return months == 1 ? "1 month ago" : months + " months ago";
            } else {
                long years = totalDays / 365;
                return years == 1 ? "1 year ago" : years + " years ago";
            }
        } catch (Exception e) {
            // If parsing fails, return null
            e.printStackTrace();
            return null;
        }
    }

    /**
     * Gets the application startup time (when Payara server started or application was deployed)
     * @return Formatted timestamp of when the application started
     */
    public String getApplicationStartupTime() {
        if (startupTimeService == null || startupTimeService.getStartupTime() == null) {
            return null;
        }
        ZonedDateTime startupTime = startupTimeService.getStartupTime();
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss z");
        return startupTime.format(formatter);
    }

    /**
     * Gets the time elapsed since application startup in a human-readable format
     * @return A string like "2 minutes ago", "3 hours ago", "14 days ago", etc.
     */
    public String getApplicationStartupTimeElapsed() {
        if (startupTimeService == null || startupTimeService.getStartupTime() == null) {
            return null;
        }

        try {
            ZonedDateTime startupTime = startupTimeService.getStartupTime();
            ZonedDateTime now = ZonedDateTime.now(startupTime.getZone());
            Duration duration = Duration.between(startupTime, now);
            long totalMinutes = duration.toMinutes();
            long totalHours = duration.toHours();
            long totalDays = duration.toDays();

            if (totalMinutes < 1) {
                return "just now";
            } else if (totalMinutes < 60) {
                return totalMinutes == 1 ? "1 minute ago" : totalMinutes + " minutes ago";
            } else if (totalHours < 24) {
                return totalHours == 1 ? "1 hour ago" : totalHours + " hours ago";
            } else if (totalDays < 30) {
                return totalDays == 1 ? "1 day ago" : totalDays + " days ago";
            } else if (totalDays < 365) {
                long months = totalDays / 30;
                return months == 1 ? "1 month ago" : months + " months ago";
            } else {
                long years = totalDays / 365;
                return years == 1 ? "1 year ago" : years + " years ago";
            }
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }
}
