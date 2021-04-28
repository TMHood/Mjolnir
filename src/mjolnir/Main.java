package mjolnir;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.TableColumnModel;
import javax.swing.table.TableModel;
import javax.swing.text.BadLocationException;
import javax.swing.text.Document;
import javax.swing.text.SimpleAttributeSet;
import javax.swing.text.StyleConstants;
import java.awt.*;
import java.io.File;
import java.io.FileReader;
import java.io.FilenameFilter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;
import java.util.Optional;
import java.util.Vector;
import java.util.concurrent.Executors;
import java.util.stream.Collectors;

import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;

public class Main {
    public static String THOR_HOME_ENV_VAR = "THORHOME";
    public static String THOR_HOME = System.getenv(THOR_HOME_ENV_VAR);
    public static final String THOR_LOGS_DIR = THOR_HOME + "\\logs";
    public static final String THOR_LOGFILE_PREFIX = "thorlog_";
    public static final String THOR_LOGFILE_SUFFIX = ".log";
    public static final String THOR_LOGFILE_REGEXP = "^thorlog_.*\\.log$";
    public static final String THOR_CONNECTIONS_FILE = THOR_HOME + "\\db_connections.json";
    public static final String THOR_REPOSITORIES_FILE = THOR_HOME + "\\repositories.json";
    public static final Color BACKGROUND_COLOR = new Color(230,230,255);

    static JFrame mainFrame;
    static JComboBox cbRepo;
    static JComboBox cbRelease;
    static JComboBox cbEnv;
    static JTable tabLogs;
    static String[] tableColumns;
    static DefaultTableModel tableModel;
    static Object[][] tableData;
    static ButtonGroup bg;
    static JRadioButton rbFull;
    static JRadioButton rbSummary;
    static JTextPane tpLogContent;

    static List<Repository> repoList;

    public static void main(String args[]) {
        if (THOR_HOME == null || THOR_HOME.isBlank()) {
            displayFatal("Cannot find environment variable " + THOR_HOME_ENV_VAR);
        }

        // JFrame
        mainFrame = new JFrame("Mjölnir");
        mainFrame.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
        mainFrame.setSize(1200, 800);
/*
        // menu
        JMenuBar mb = new JMenuBar();
        JMenu m1 = new JMenu("FILE");
        JMenu m2 = new JMenu("Help");
        mb.add(m1);
        mb.add(m2);
        JMenuItem m11 = new JMenuItem("Open");
        JMenuItem m22 = new JMenuItem("Save as");
        m1.add(m11);
        m1.add(m22);
*/
        JLabel lblMjolnir = new JLabel("Mjölnir");
        lblMjolnir.setFont(new Font("Old English Text MT", Font.BOLD, 60));
        lblMjolnir.setAlignmentX(JLabel.CENTER_ALIGNMENT);
        lblMjolnir.setBorder(new EmptyBorder(20, 0, 0, 0));

        // grid
        GridBagConstraints c;

        JPanel pnlGrid = new JPanel(new GridBagLayout());
        pnlGrid.setPreferredSize(new Dimension(500,200));
        pnlGrid.setBackground(BACKGROUND_COLOR);

        JLabel lblRepo = new JLabel("Repository");
        c = new GridBagConstraints();
        c.fill = GridBagConstraints.HORIZONTAL;
        c.gridx = 0;
        c.gridy = 0;
        c.insets = new Insets(10,10,10,10);
        pnlGrid.add(lblRepo, c);

        repoList = readRepositories();
        Vector<String> repoNames = new Vector<String> (repoList.stream().map(x -> x.getName()).collect(Collectors.toList()));
        repoNames.add(0,"");
        cbRepo = new JComboBox(repoNames);
        c = new GridBagConstraints();
        c.fill = GridBagConstraints.HORIZONTAL;
        c.gridx = 1;
        c.gridy = 0;
        pnlGrid.add(cbRepo, c);

        JLabel lblEnv = new JLabel("Environment");
        c = new GridBagConstraints();
        c.fill = GridBagConstraints.HORIZONTAL;
        c.gridx = 0;
        c.gridy = 1;
        c.insets = new Insets(10,10,10,10);
        pnlGrid.add(lblEnv, c);

        List<String> envs = readEnvs();
        envs.add(0, "");
        cbEnv = new JComboBox((Vector) envs);
        cbEnv.addActionListener (e -> repopulateTable());
        c = new GridBagConstraints();
        c.fill = GridBagConstraints.HORIZONTAL;
        c.gridx = 1;
        c.gridy = 1;
        pnlGrid.add(cbEnv, c);

        JLabel lblRelease = new JLabel("Release");
        c = new GridBagConstraints();
        c.fill = GridBagConstraints.HORIZONTAL;
        c.gridx = 0;
        c.gridy = 2;
        c.insets = new Insets(10,10,10,10);
        pnlGrid.add(lblRelease, c);

        List<String> releases = getReleases();
        releases.add(0, "");
        cbRelease = new JComboBox((Vector) releases);
        cbRelease.addActionListener (e -> repopulateTable());
        c = new GridBagConstraints();
        c.fill = GridBagConstraints.HORIZONTAL;
        c.gridx = 1;
        c.gridy = 2;
        pnlGrid.add(cbRelease, c);

        // button panel
        JPanel pnlButtons = new JPanel();
        pnlButtons.setPreferredSize(new Dimension(500,100));
        pnlButtons.setBackground(BACKGROUND_COLOR);

        JButton btnFilecheck = new JButton("filecheck");
        btnFilecheck.addActionListener(x -> runThor(cbRepo.getSelectedItem().toString(), cbRelease.getSelectedItem().toString(),
                cbEnv.getSelectedItem().toString(), "filecheck"));
        pnlButtons.add(btnFilecheck);

        JButton btnPrecheck = new JButton("precheck");
        btnPrecheck.addActionListener(x -> runThor(cbRepo.getSelectedItem().toString(), cbRelease.getSelectedItem().toString(),
                cbEnv.getSelectedItem().toString(), "precheck"));
        pnlButtons.add(btnPrecheck);

        JButton btnDeploy = new JButton("deploy");
        btnDeploy.addActionListener(x -> runThor(cbRepo.getSelectedItem().toString(), cbRelease.getSelectedItem().toString(),
                cbEnv.getSelectedItem().toString(), "deploy"));
        pnlButtons.add(btnDeploy);

        JButton btnPostcheck = new JButton("postcheck");
        btnPostcheck.addActionListener(x -> runThor(cbRepo.getSelectedItem().toString(), cbRelease.getSelectedItem().toString(),
                cbEnv.getSelectedItem().toString(), "postcheck"));
        pnlButtons.add(btnPostcheck);

        JButton btnRevert = new JButton("revert");
        btnRevert.addActionListener(x -> runThor(cbRepo.getSelectedItem().toString(), cbRelease.getSelectedItem().toString(),
                cbEnv.getSelectedItem().toString(), "revert"));
        pnlButtons.add(btnRevert);

        // logs table
        JLabel lblLogs = new JLabel("Log Files");
        lblLogs.setBorder(new EmptyBorder(0, 0, 10, 0));

        lblLogs.setFont(new Font("MS Sans Serif", Font.BOLD, 20));
        lblLogs.setAlignmentX(JLabel.CENTER_ALIGNMENT);
        tableColumns = new String[] {"Release", "Env", "Action", "Date/Time"};
        tableData = logFilesArray(cbRelease.getSelectedItem().toString(), cbEnv.getSelectedItem().toString());
        TableModel tableModel = new DefaultTableModel(tableData, tableColumns);
        tabLogs = new JTable (tableModel);
        TableColumnModel columnModel = tabLogs.getColumnModel();
        columnModel.getColumn(0).setPreferredWidth(150);
        columnModel.getColumn(1).setPreferredWidth(50);
        columnModel.getColumn(2).setPreferredWidth(80);
        columnModel.getColumn(3).setPreferredWidth(150);
        tabLogs.addMouseListener(new java.awt.event.MouseAdapter() {
            @Override
            public void mouseClicked(java.awt.event.MouseEvent evt) {
                displayCurrentLogFile();
            }
        });
        JScrollPane spLogs = new JScrollPane(tabLogs);
        spLogs.setPreferredSize(new Dimension(500,1000));

        // left panel
        JPanel pnlLeft = new JPanel();
        pnlLeft.setLayout(new BoxLayout(pnlLeft, BoxLayout.Y_AXIS));
        pnlLeft.setMaximumSize(new Dimension(1000,9000));
        pnlLeft.setBackground(BACKGROUND_COLOR);
        pnlLeft.add(lblMjolnir);
        pnlLeft.add(pnlGrid);
        pnlLeft.add(pnlButtons);
        pnlLeft.add(lblLogs);
        pnlLeft.add(spLogs);

        // radio buttons
        rbFull = new JRadioButton();
        rbFull.setText("full");
        rbFull.setBackground(BACKGROUND_COLOR);
        rbFull.setSelected(true);
        rbFull.addActionListener(event -> displayCurrentLogFile());
        rbSummary = new JRadioButton();
        rbSummary.setText("summary");
        rbSummary.setBackground(BACKGROUND_COLOR);
        rbSummary.setSelected(false);
        rbSummary.addActionListener(event -> displayCurrentLogFile());

        bg = new ButtonGroup();
        bg.add(rbFull);
        bg.add(rbSummary);

        // radio bar
        JPanel pnlRadio = new JPanel();
        pnlRadio.setLayout(new BoxLayout(pnlRadio, BoxLayout.X_AXIS));
        pnlRadio.setBackground(BACKGROUND_COLOR);
        pnlRadio.add(rbFull);
        pnlRadio.add(rbSummary);

        // log content
        tpLogContent = new JTextPane();
        tpLogContent.setPreferredSize(new Dimension(250, 145));
        tpLogContent.setMinimumSize(new Dimension(100, 100));
        tpLogContent.setBackground(Color.WHITE);

        // scroll pane
        JScrollPane spLogContent = new JScrollPane(tpLogContent);

        // right panel
        JPanel pnlRight = new JPanel();
        pnlRight.setLayout(new BoxLayout(pnlRight, BoxLayout.Y_AXIS));
        pnlRight.setBackground(BACKGROUND_COLOR);
        pnlRight.add(pnlRadio);
        pnlRight.add(spLogContent);

        JPanel pnlMain = new JPanel();
        pnlMain.setLayout(new BoxLayout(pnlMain, BoxLayout.X_AXIS));
        pnlMain.add(pnlLeft);
        pnlMain.add(pnlRight);

        // frame
        mainFrame.getContentPane().add(pnlMain);
        mainFrame.setVisible(true);
    }

    public static List<String> readEnvs () {
        List<String> envList = new Vector<>();
        try (FileReader fileReader = new FileReader(THOR_CONNECTIONS_FILE)) {

            JSONParser jsonParser = new JSONParser();
            JSONObject j = (JSONObject) jsonParser.parse(fileReader);

            JSONArray envs = (JSONArray) j.get("envs");

            Iterator<Object> iter = envs.iterator();
            while(iter.hasNext()){
                JSONObject node = (JSONObject) iter.next();
                String env = (String) node.get("envname");
                envList.add(env);
            }

        } catch (IOException | ParseException e) {
            displayFatal("Cannot read connections file:\n" + THOR_CONNECTIONS_FILE);
        }

        return envList;
    }

    public static List<Repository> readRepositories () {
        List<Repository> repoList = new ArrayList<>();
        try (FileReader fileReader = new FileReader(THOR_REPOSITORIES_FILE)) {

            JSONParser jsonParser = new JSONParser();
            JSONObject j = (JSONObject) jsonParser.parse(fileReader);

            JSONArray repositories = (JSONArray) j.get("repositories");

            Iterator<Object> iter = repositories.iterator();
            while(iter.hasNext()){
                JSONObject node = (JSONObject) iter.next();
                String repo = (String) node.get("name");
                String folder = (String) node.get("localFolder");
                repoList.add(new Repository(repo,folder));
            }

        } catch (IOException | ParseException e) {
            displayFatal("Cannot read repositories file:\n" + THOR_REPOSITORIES_FILE);
        }

        return repoList;
    }

    public static List<ThorLog> getLogFiles(String releaseFilter, String envFilter) {
        FilenameFilter filter = (file, fileName) -> fileName.matches(THOR_LOGFILE_REGEXP);
        String[] logs = new File(THOR_LOGS_DIR).list(filter);

        List<ThorLog> list = Arrays.stream(logs).
                map(s -> new ThorLog(s)).
                filter((l) -> releaseFilter == null || releaseFilter.isBlank() || l.getRelease().equals(releaseFilter)).
                filter((l) -> envFilter == null || envFilter.isBlank() || l.getEnv() == null || l.getEnv().isBlank() || l.getEnv().equals(envFilter)).
                sorted((l1,l2) -> l2.getDateTime().compareTo(l1.getDateTime())).
                collect(Collectors.toList());

        return list;
    }

    public static Object[][] logFilesArray (String releaseFilter, String envFilter) {
        Object[][] array;
        List<ThorLog> logs = getLogFiles(releaseFilter, envFilter);
        array = logs.stream().map(x -> new Object[]{x.getRelease(), x.getEnv(), x.getAction(), x.getDateTime(), x.getFileName()}).toArray(Object[][]::new);
        return array;
    }

    public static List<String> getReleases() {
        List<String> releases = new Vector<>();
        List<ThorLog> logs = getLogFiles(null, null);
        for (ThorLog log : logs) {
            if (!releases.contains(log.getRelease())) {
                releases.add(log.getRelease());
            }
        }
        return releases;
    }

    public static void repopulateTable() {
        tableData = logFilesArray(cbRelease.getSelectedItem().toString(), cbEnv.getSelectedItem().toString());
        tableModel = new DefaultTableModel(tableData, tableColumns);
        tabLogs.setModel(tableModel);
    }

    public static String buildThorCommand (String release, String env, String action) {
        String thorCommand;
        if (action.equals("filecheck")) {
            thorCommand = "thor -r " + release + " -a " + action;
        } else {
            thorCommand = "thor -r " + release + " -a " + action + " -e " + env;
        }
        return thorCommand;
    }

    public static void runThor (String repo, String release, String env, String action) {
        if (repo == null || repo.isBlank()) {
            displayError("You must select a repository.");
            return;
        }

        if (release == null || release.isBlank()) {
            displayError("You must select a Thor release.");
            return;
        }

        String dir = repoPath(repo);
        File dirFile = new File(dir);
        if (!dirFile.exists()) {
            displayError("Cannot find repository drectory:\n" + dir);
            return;
        }

        if (!action.equals("filecheck")) {
            if (env == null || env.isBlank()) {
                displayError("You must select an environment.");
                return;
            }
        }

        String thorFileName = thorFile(repo, release);
        File thorFile = new File(thorFileName);
        if (!thorFile.exists()) {
            displayError("Cannot find Thor file in this repository:\n" + thorFile);
            return;
        }

        try {
            boolean isWindows = System.getProperty("os.name").toLowerCase().startsWith("windows");
            ProcessBuilder builder = new ProcessBuilder();
            if (isWindows) {
                builder.command("cmd.exe", "/c", buildThorCommand(release, env, action));
            } else {
                builder.command("sh", "-c", "ls");
            }

            builder.directory(dirFile);

            Process process = builder.start();
            StreamGobbler streamGobbler =
                    new StreamGobbler(process.getInputStream(), System.out::println);
            Executors.newSingleThreadExecutor().submit(streamGobbler);
            int exitCode = process.waitFor();

            if (exitCode != 0) {
                // do nothing
                System.out.println("exit code " + exitCode);
            }
        } catch (IOException | InterruptedException e) {
            displayError (e.getStackTrace().toString());
            return;
        }

        repopulateTable();
        tabLogs.repaint();
        if (tabLogs.getRowCount() > 0) {
            tabLogs.setRowSelectionInterval(0, 0);
        }
        displayCurrentLogFile();
    }

    public static void displayCurrentLogFile () {
        String fileName = "";
        int row = tabLogs.getSelectedRow();
        if (row >= 0) {
            fileName = tableData[row][4].toString();
        }
        displayLogFile(fileName);
    }

    public static void displayLogFile (String fileName) {
        tpLogContent.setText(null);

        if (fileName != null && !fileName.isBlank()) {
            List<String> lines = new ArrayList<>();
            try {
                lines = Files.readAllLines(Paths.get(THOR_LOGS_DIR + "\\" + fileName));
            } catch (IOException e) {
                e.printStackTrace();
            }

            if (rbSummary.isSelected()) {
                filterLines(lines);
            }

            Color colour;
            for (String s : lines) {
                if (rbSummary.isSelected()) {
                    if (s.startsWith("[=PASS=]") || s.toLowerCase().contains("success")) {
                        colour = Color.GREEN;
                    } else if (s.startsWith("[+FAIL+]") || s.toLowerCase().contains("error")) {
                        colour = Color.RED;
                    } else {
                        colour = Color.BLACK;
                    }
                } else {
                    colour = Color.BLACK;
                }

                Document doc = tpLogContent.getStyledDocument();
                SimpleAttributeSet attributeSet = new SimpleAttributeSet();
                //StyleConstants.setItalic(attributeSet, true);
                StyleConstants.setForeground(attributeSet, colour);
                //StyleConstants.setBackground(attributeSet, Color.blue);
                try {
                    doc.insertString(doc.getLength(), s + "\n", attributeSet);
                } catch (BadLocationException e) {
                    e.printStackTrace();
                }
            }
        }
    }

    public static void filterLines(List<String> lines) {
        //   lines.removeIf(l -> l.isBlank() ||
        //         l.equals("PL/SQL procedure successfully completed.") ||
        //       l.startsWith("Elapsed: "));
        lines.removeIf(l -> !l.startsWith("START") &&
                !l.startsWith("start") &&
                !l.startsWith("END") &&
                !l.startsWith("end") &&
                !l.contains("error") &&
                !l.contains("ERROR") &&
                !l.contains("Error") &&
                !l.startsWith("[+FAIL+]") &&
                !l.startsWith("[=PASS=]") &&
                //  !l.startsWith("**") &&
                !l.contains("STATUS SUMMARY")
        );
    }

    public static String repoPath(String name) {
        Optional<String> path = repoList.stream().filter(x -> x.getName().equals(name)).map(x -> x.getLocalFolder()).findFirst();
        return path.orElse(null);
    }

    public static void displayError (String message) {
        JOptionPane.showMessageDialog (mainFrame, message,"Error", JOptionPane.ERROR_MESSAGE);
    }

    public static void displayFatal (String message) {
        JOptionPane.showMessageDialog (mainFrame, message,"Fatal Error", JOptionPane.ERROR_MESSAGE);
        mainFrame.dispose();
        System.exit(1);
    }

    public static String thorFile (String repo, String release) {
        return repoPath(repo) + "\\thor_releases\\" + release + ".json";
    }
}