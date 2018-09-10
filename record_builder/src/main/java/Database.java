import java.sql.*;
import java.util.*;

/**
 * Class responsible for connecting to the database and database operations
 */
public class Database {

    private String dbusername;
    private String dbpassword;
    private String dburl;
    private Connection connection;
    private static Database instance;

    /**
     * Constructor - takes database user, password, port and database name from config file
     */
    public Database() {
        //retrieve the configuratino properties and set our database class attributes
        Properties properties = Utils.getConfigurationProperties();
        this.dburl = "jdbc:mysql://localhost:" + properties.getProperty("dbport") + "/" + properties.getProperty("database") + "?autoReconnect=true&useSSL=false&useUnicode=true&useJDBCCompliantTimezoneShift=true&useLegacyDatetimeCode=false&serverTimezone=UTC";
        this.dbusername = properties.getProperty("dbusername");
        this.dbpassword = properties.getProperty("dbpassword");
    }

    /**
     * Get Database instance - singleton
     *
     * @return Database instance
     */
    public static Database getInstance() {
        if (instance == null) {
            instance = new Database();
        }
        return instance;
    }

    /**
     * Connect to the database
     *
     * @throws SQLException
     */
    public void connect() throws SQLException {
        try {
            connection = DriverManager.getConnection(dburl, dbusername, dbpassword);
        } catch (SQLException e) {
            throw e;
        }
    }

    /**
     * Close connection to the database
     */
    public void close() {
        try {
            if(connection != null)
                connection.close();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    /**
     * Make a query to the database return a 2-d ArrayList containing rows of data
     *
     * @param sqlStatement SQL statement
     * @return 2-d ArrayList of data
     * @throws SQLException
     */
    private ArrayList<ArrayList<String>> getTableData(String sqlStatement) throws SQLException {
        ArrayList<ArrayList<String>> table = null;
        try {
            PreparedStatement stmt = connection.prepareStatement(sqlStatement);
            ResultSet result = stmt.executeQuery();

            int ncols = result.getMetaData().getColumnCount();
            table = new ArrayList<>();

            ArrayList<String> columnsRow = new ArrayList<>();
            ResultSetMetaData rsmd = result.getMetaData();

            for (int i = 1; i <= ncols; i++) {
                columnsRow.add(rsmd.getColumnName(i));
            }
            table.add(0, columnsRow);

            while (result.next()) {
                ArrayList<String> row = new ArrayList<>();
                for (int i = 1; i <= ncols; i++) {
                    Object obj = result.getObject(i);
                    row.add(i - 1, (obj == null) ? null : obj.toString());
                }
                table.add(row);
            }

            result.close();
            stmt.close();
        } catch (SQLException e) {
            throw e;
        }

        return table;
    }

    /**
     * Insert record data into record table
     *
     * @param recordNumber record number
     * @param timestamp    timestamp
     * @param amount       amount
     * @throws SQLException
     */
    public void insertRecordData(int recordNumber, java.sql.Timestamp timestamp, double amount) throws SQLException {
        try {
            PreparedStatement stmt = connection.prepareStatement("INSERT INTO record VALUES(null, ?, ?, ?)");
            stmt.setInt(1, recordNumber);
            stmt.setTimestamp(2, timestamp);
            stmt.setDouble(3, amount);

            stmt.executeUpdate();
            stmt.close();
        } catch (SQLException e) {
            throw e;
        }
    }

    /**
     * Get total record count
     * @return total record count
     */
    public int getTotalRecordsCount() {
        int count = 0;
        try {
            Statement stmt = connection.createStatement();
            ResultSet result = stmt.executeQuery("SELECT COUNT(*) AS rowcount FROM record");
            result.next();
            count = result.getInt("rowcount");
            result.close();
        } catch (SQLException e) {
            e.printStackTrace();
        }

        return count;
    }

    /**
     * Get amounts grouped by month
     * @return Data table with total amount for each month
     * @throws SQLException
     */
    public ArrayList<ArrayList<String>> getAmountsByMonth() throws SQLException {
        return getTableData("SELECT ANY_VALUE(DATE_FORMAT(timestamp, '%b')) AS 'MONTH', SUM(amount) AS 'TOTAL' FROM record GROUP BY MONTH(timestamp)");
    }

    /**
     * Get all records
     * @return Data table with all records from the database
     * @throws SQLException
     */
    public ArrayList<ArrayList<String>> getAllRecords() throws SQLException {
        return getTableData("SELECT recordNumber, timestamp, amount FROM record");
    }
}