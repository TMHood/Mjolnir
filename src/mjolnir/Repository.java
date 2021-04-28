package mjolnir;

public class Repository {
    private String name;
    private String localFolder;

    public Repository(String name, String localFolder) {
        this.name = name;
        this.localFolder = localFolder;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getLocalFolder() {
        return localFolder;
    }

    public void setLocalFolder(String localFolder) {
        this.localFolder = localFolder;
    }
}
