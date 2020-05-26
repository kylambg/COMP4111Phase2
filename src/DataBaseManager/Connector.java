package DataBaseManager;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

import java.util.ArrayList;
import java.util.List;

public class Connector {
    final static String URL = "jdbc:mysql://localhost:3306/Comp4111"; //DBMS schema name
    final static String USER = "root";
    final static String PASSWD = "123";
    final static String Driver = "com.mysql.jdbc.Driver";
    final static int MAX_CONN = 100;

    private List<Connection> connections;

    public Connector() throws ClassNotFoundException {
        connections = new ArrayList<Connection>();
        Class.forName(Driver);
    }

    /**
     * Reference from Another team: (Opponent team in phase 1)
     * If there does not have any connection, use the first 1
     * Else, get the connection from pool
     *
     * @return type Connection: A new connection
     * @throws SQLException
     */
    synchronized Connection getConnection() throws SQLException {
        if (connections.size() == 0) {
            return DriverManager.getConnection(URL, USER, PASSWD);
        } else {
            int lastIndex = connections.size() - 1;
            return connections.remove(lastIndex);
        }
    }

    /**
     * When closing the connection, add it to the connection pool for reuse
     */
    public synchronized void closeConnection(Connection conn) throws SQLException {
        if (connections.size() == MAX_CONN) {
            conn.close();
        } else {
            connections.add(conn);
        }
    }
}
