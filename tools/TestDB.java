package tools;

public class TestDB {
    public static void main(String[] args) {
        String r = common.DBUtil.testConnection();
        System.out.println(r == null ? "OK" : r);
    }
}
