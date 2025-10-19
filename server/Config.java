package server;

import java.io.*;
import java.util.Properties;

public class Config {
    private static final String FILE = "config.properties";

    public int port = 5000;
    public String password = "SuperSecret123";
    public boolean dbLogging = true;
    public String dbUrl = "jdbc:mysql://localhost:3306/file_server";
    public String dbUser = "root";
    public String dbPass = "yourpassword";

    public static Config load() {
        Config c = new Config();
        Properties p = new Properties();
        File f = new File(FILE);
        if (f.exists()) {
            try (FileInputStream fis = new FileInputStream(f)) {
                p.load(fis);
                c.port = Integer.parseInt(p.getProperty("port", "5000"));
                c.password = p.getProperty("password", c.password);
                c.dbLogging = Boolean.parseBoolean(p.getProperty("dbLogging", "true"));
                c.dbUrl = p.getProperty("dbUrl", c.dbUrl);
                c.dbUser = p.getProperty("dbUser", c.dbUser);
                c.dbPass = p.getProperty("dbPass", c.dbPass);
            } catch (Exception e) {
                // ignore, use defaults
            }
        }
        return c;
    }

    public void save() throws IOException {
        Properties p = new Properties();
        p.setProperty("port", Integer.toString(port));
        p.setProperty("password", password == null ? "" : password);
        p.setProperty("dbLogging", Boolean.toString(dbLogging));
        p.setProperty("dbUrl", dbUrl == null ? "" : dbUrl);
        p.setProperty("dbUser", dbUser == null ? "" : dbUser);
        p.setProperty("dbPass", dbPass == null ? "" : dbPass);
        try (FileOutputStream fos = new FileOutputStream(FILE)) {
            p.store(fos, "FILETRANS server configuration");
        }
    }
}
