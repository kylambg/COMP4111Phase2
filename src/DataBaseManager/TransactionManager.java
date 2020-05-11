package DataBaseManager;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Vector;

import Object.Action;

public class TransactionManager extends Manager {
    private Connector connector;

    //One and only one Connector object will be created
    private TransactionManager() {
        try {
            connector = new Connector();
        } catch (ClassNotFoundException e) {
            e.printStackTrace();
        }
    }

    //Using Bill Pugh Singleton design pattern, good performance, thread safe
    //Reason not to use ENUM approach because extends have to use in this class
    private static class Singleton {
        private static final TransactionManager instance = new TransactionManager();
    }

    public static TransactionManager getInstance() {
        return Singleton.instance;
    }

    public boolean validateCommit(Vector<Action> actions) {
        try {
            Connection connection = connector.getConnection();
            Statement stmt = connection.createStatement();
            connection.setAutoCommit(false);
            for (int i = 0; i < actions.size(); ++i) {
                if (actions.get(i).getAction().equalsIgnoreCase("loan")) {
                    ResultSet rs = stmt.executeQuery("SELECT " + STATUS + " from " + BOOK + " where " + ID + " = " + actions.get(i).getId() + ";");
                    int status = -1;
                    if (rs.next())
                        status = rs.getInt("Statuses");
                    else { // no book record
                        connection.rollback();
                        connector.closeConnection(connection);
                        rs.close();
                        stmt.close();
                        return false;
                    }
                    if (status == 1) {
                        stmt.executeUpdate("UPDATE " + BOOK + " SET " + STATUS + " = " + 0 + " WHERE " + ID + " = " + actions.get(i).getId() + ";");
                        rs.close();
                    } else { //already loan
                        connection.rollback();
                        connector.closeConnection(connection);
                        rs.close();
                        stmt.close();
                        return false;
                    }
                } else if (actions.get(i).getAction().equalsIgnoreCase("return")) {
                    ResultSet rs = stmt.executeQuery("SELECT " + STATUS + " from " + BOOK + " where " + ID + " = " + actions.get(i).getId() + ";");
                    int status = -1;
                    if (rs.next())
                        status = rs.getInt("Statuses");
                    else { // no book record
                        connection.rollback();
                        connector.closeConnection(connection);
                        rs.close();
                        stmt.close();
                        return false;
                    }
                    if (status == 0) {
                        stmt.executeUpdate("UPDATE " + BOOK + " SET " + STATUS + " = " + 1 + " WHERE " + ID + " = " + actions.get(i).getId() + ";");
                        rs.close();
                    } else { //already loan
                        connection.rollback();
                        connector.closeConnection(connection);
                        rs.close();
                        stmt.close();
                        return false;
                    }
                }
            }
            connection.commit();
            connector.closeConnection(connection);
            stmt.close();
            return true;
        } catch (Exception e) {
            e.getMessage();

            return false;
        }
    }

}
