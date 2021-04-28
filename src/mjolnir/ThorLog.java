package mjolnir;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

public class ThorLog {
    private String fileName;
    private String release;
    private String env;
    private String action;
    private LocalDateTime dateTime;

    public ThorLog (String fileName) {
        this.fileName = fileName;

        int sep3 = fileName.lastIndexOf("_");
        int sep2 = fileName.lastIndexOf("_", sep3-1);
        int sep1 = fileName.lastIndexOf("_", sep2-1);

        this.dateTime = LocalDateTime.parse(fileName.substring(sep3+1, fileName.length()-Main.THOR_LOGFILE_SUFFIX.length()), DateTimeFormatter.ofPattern("yyyyMMdd-HHmmss"));
        this.action = fileName.substring(sep2 + 1, sep3);

        if (this.action.equals("filecheck")) {
            this.env = "";
            if (sep1 < Main.THOR_LOGFILE_PREFIX.length()) {
                release = (fileName.substring(Main.THOR_LOGFILE_PREFIX.length(), sep2));
            } else {
                release = (fileName.substring(Main.THOR_LOGFILE_PREFIX.length(), sep1));
            }
        } else {
            this.env = fileName.substring(sep1+1, sep2);
            release= (fileName.substring(Main.THOR_LOGFILE_PREFIX.length(), sep1));
        }
    }

    public String getFileName() {
        return fileName;
    }

    public void setFileName(String fileName) {
        this.fileName = fileName;
    }

    public String getEnv() {
        return env;
    }

    public void setEnv(String env) {
        this.env = env;
    }

    public String getAction() {
        return action;
    }

    public void setAction(String action) {
        this.action = action;
    }

    public LocalDateTime getDateTime() {
        return dateTime;
    }

    public void setDateTime(LocalDateTime dateTime) {
        this.dateTime = dateTime;
    }

    public String getRelease() {
        return release;
    }

    public void setRelease(String release) {
        this.release = release;
    }
}
