package tools;

public class InitDBRunner {
    public static void main(String[] args) {
        System.out.println("Initializing DB schema...");
        common.DBUtil.initSchema();
        System.out.println("Init finished.");
    }
}
