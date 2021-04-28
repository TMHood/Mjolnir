package mjolnir;

import javax.swing.*;
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
import java.util.Vector;
import java.util.concurrent.Executors;
import java.util.stream.Collectors;

import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;

public class Main {

    public static final String THOR_HOME = "C:\\Thor";
    public static final String THOR_LOGS_DIR = THOR_HOME + "\\logs";
    public static final String THOR_LOGFILE_PREFIX = "thorlog_";
    public static final String THOR_LOGFILE_SUFFIX = ".log";
    public static final String THOR_LOGFILE_REGEXP = "^thorlog_.*\\.log$";
    public static final String THOR_CONNECTIONS_FILE = THOR_HOME + "\\db_connections.json";

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

    public static void main(String args[]) {
        // JFrame
        JFrame frame = new JFrame("Mjölnir");
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setSize(1200, 800);

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

        // grid
        JPanel pnlGrid = new JPanel(new GridBagLayout());
        pnlGrid.setPreferredSize(new Dimension(500,200));
        GridBagConstraints c;

        JLabel lblEnv = new JLabel("Environment");
        c = new GridBagConstraints();
        c.fill = GridBagConstraints.HORIZONTAL;
        c.gridx = 0;
        c.gridy = 0;
        pnlGrid.add(lblEnv, c);

        List<String> envs = readEnvs();
        envs.add(0, "");
        cbEnv = new JComboBox((Vector) envs);
        cbEnv.addActionListener (e -> repopulateTable());
        c = new GridBagConstraints();
        c.fill = GridBagConstraints.HORIZONTAL;
        c.gridx = 1;
        c.gridy = 0;
        pnlGrid.add(cbEnv, c);

        JLabel lblRelease = new JLabel("Release");
        c = new GridBagConstraints();
        c.fill = GridBagConstraints.HORIZONTAL;
        c.gridx = 0;
        c.gridy = 1;
        pnlGrid.add(lblRelease, c);

        List<String> releases = getReleases();
        releases.add(0, "");
        cbRelease = new JComboBox((Vector) releases);
        cbRelease.addActionListener (e -> repopulateTable());
        c = new GridBagConstraints();
        c.fill = GridBagConstraints.HORIZONTAL;
        c.gridx = 1;
        c.gridy = 1;
        pnlGrid.add(cbRelease, c);

        // button panel
        JPanel pnlButtons = new JPanel();
        pnlButtons.setPreferredSize(new Dimension(500,100));

        JButton btnFilecheck = new JButton("filecheck");
        btnFilecheck.addActionListener(x -> runThor(cbRelease.getSelectedItem().toString(),
                cbEnv.getSelectedItem().toString(), "filecheck"));
        pnlButtons.add(btnFilecheck);

        JButton btnPrecheck = new JButton("precheck");
        btnPrecheck.addActionListener(x -> runThor(cbRelease.getSelectedItem().toString(),
                cbEnv.getSelectedItem().toString(), "precheck"));
        pnlButtons.add(btnPrecheck);

        JButton btnDeploy = new JButton("deploy");
        btnDeploy.addActionListener(x -> runThor(cbRelease.getSelectedItem().toString(),
                cbEnv.getSelectedItem().toString(), "deploy"));
        pnlButtons.add(btnDeploy);

        JButton btnPostcheck = new JButton("postcheck");
        btnPostcheck.addActionListener(x -> runThor(cbRelease.getSelectedItem().toString(),
                cbEnv.getSelectedItem().toString(), "postcheck"));
        pnlButtons.add(btnPostcheck);

        JButton btnRevert = new JButton("revert");
        btnRevert.addActionListener(x -> runThor(cbRelease.getSelectedItem().toString(),
                cbEnv.getSelectedItem().toString(), "revert"));
        pnlButtons.add(btnRevert);

        // logs table
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
        pnlLeft.add(pnlGrid);
        pnlLeft.add(pnlButtons);
        pnlLeft.add(spLogs);

        // radio buttons
        rbFull = new JRadioButton();
        rbFull.setText("full");
        rbFull.setSelected(true);
        rbFull.addActionListener(event -> displayCurrentLogFile());
        rbSummary = new JRadioButton();
        rbSummary.setText("summary");
        rbSummary.setSelected(false);
        rbSummary.addActionListener(event -> displayCurrentLogFile());

        bg = new ButtonGroup();
        bg.add(rbFull);
        bg.add(rbSummary);

        // radio bar
        JPanel pnlRadio = new JPanel();
        pnlRadio.setLayout(new BoxLayout(pnlRadio, BoxLayout.X_AXIS));
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
        pnlRight.add(pnlRadio);
        pnlRight.add(spLogContent);

        JPanel pnlMain = new JPanel();
        pnlMain.setLayout(new BoxLayout(pnlMain, BoxLayout.X_AXIS));
        pnlMain.add(pnlLeft);
        pnlMain.add(pnlRight);

        // frame
        frame.getContentPane().add(pnlMain);
        frame.setVisible(true);
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
            e.printStackTrace();
        }

        return envList;
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

    public static void runThor (String release, String env, String action) {
        try {
            boolean isWindows = System.getProperty("os.name").toLowerCase().startsWith("windows");
            ProcessBuilder builder = new ProcessBuilder();
            if (isWindows) {
                builder.command("cmd.exe", "/c", buildThorCommand(release, env, action));
            } else {
                builder.command("sh", "-c", "ls");
            }
            builder.directory(new File("C:\\GITesure\\tia_db"));

            Process process = builder.start();
            StreamGobbler streamGobbler =
                    new StreamGobbler(process.getInputStream(), System.out::println);
            Executors.newSingleThreadExecutor().submit(streamGobbler);
            int exitCode = process.waitFor();

            assert exitCode == 0;
        } catch (IOException | InterruptedException e) {
            e.printStackTrace();
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
            System.out.println("displaying " +fileName);
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
                //break;
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

}