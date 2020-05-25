package DataBaseManager;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

public class LoanReturnManager extends Manager {
    private Connector connector;

    //One and only one Connector object will be created
    private LoanReturnManager() {
        try {
            connector = new Connector();
        } catch (ClassNotFoundException e) {
            e.printStackTrace();
        }
    }

    //Using Bill Pugh Singleton design pattern, good performance, thread safe
    //Reason not to use ENUM approach because extends have to use in this class
    private static class Singleton {
        private static final LoanReturnManager instance = new LoanReturnManager();
    }

    public static LoanReturnManager getInstance() {
        return Singleton.instance;
    }

    public int loanReturn(int action, int id) {
        if (id <= 0)
            return -1;
        try {
            Connection connection = connector.getConnection();
            switch (action) {
                case 0: //loan
                    String prepareStmt0 = "SELECT " + STATUS + " FROM " + BOOK + " WHERE " + ID + " = ?";
                    PreparedStatement stmt0 = connection.prepareStatement(prepareStmt0);
                    stmt0.setInt(1, id);
                    ResultSet rs0 = stmt0.executeQuery();
                    if (rs0.next()) { // only one result as ID is unique
                        if (rs0.getInt(1) == 1) {
                            stmt0.executeUpdate("UPDATE " + BOOK + " set " + STATUS + "= 0 where " + ID + " = " + id + ";");
                            stmt0.close();
                            rs0.close();
                            connector.closeConnection(connection);
                            return 1;
                        } else if (rs0.getInt(1) == 0) {
                            stmt0.close();
                            rs0.close();
                            connector.closeConnection(connection);
                            return -1; //return SC_BAD_REQUEST (Already loan)
                        }
                    } else { //NO BOOK RECORD
                        stmt0.close();
                        rs0.close();
                        connector.closeConnection(connection);
                        return 0;
                    }
                    break;
                case 1: //return
                    String prepareStmt1 = "SELECT " + STATUS + " FROM " + BOOK + " WHERE " + ID + " = ?";
                    PreparedStatement stmt1 = connection.prepareStatement(prepareStmt1);
                    stmt1.setInt(1, id);
                    ResultSet rs1 = stmt1.executeQuery();
                    if (rs1.next()) { // only one result as ID is unique
                        if (rs1.getInt(1) == 0) {
                            stmt1.executeUpdate("UPDATE " + BOOK + " set " + STATUS + "= 1 where " + ID + " = " + id + ";");
                            stmt1.close();
                            rs1.close();
                            connector.closeConnection(connection);
                            return 1;
                        } else if (rs1.getInt(1) == 0) {
                            stmt1.close();
                            rs1.close();
                            connector.closeConnection(connection);
                            return -1; //return SC_BAD_REQUEST(Already return)
                        }
                    } else { //NO BOOK RECORD
                        stmt1.close();
                        rs1.close();
                        connector.closeConnection(connection);
                        return 0;
                    }
                    break;
                default:
                    return -1;

                case -1: //wrong available value
                    return -1;
            }
        } catch (SQLException e) {
            System.out.println("SQL exception");
            e.printStackTrace();
            return -1;
        }
        return -1;
    }
}
