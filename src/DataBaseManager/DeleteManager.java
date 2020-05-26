package DataBaseManager;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

public class DeleteManager extends Manager {
    private Connector connector;

    //One and only one Connector object will be created
    private DeleteManager() {
        try {
            connector = new Connector();
        } catch (ClassNotFoundException e) {
            e.printStackTrace();
        }
    }

    //Using Bill Pugh Singleton design pattern, good performance, thread safe
    //Reason not to use ENUM approach because extends have to use in this class
    private static class Singleton {
        private static final DeleteManager instance = new DeleteManager();
    }

    public static DeleteManager getInstance() {
        return Singleton.instance;
    }

    /**
     *
     * @param id id of book to be deleted
     * @return type boolean: true if successful deletion, false otherwise
     * @throws SQLException
     */
    public boolean deleteBook(int id) throws SQLException {
        Statement stmt = connector.getConnection().createStatement();
        ResultSet rs = stmt.executeQuery("SELECT " + ID + " FROM " + BOOK + " WHERE " + ID + " = " + id + ";");
        if (rs.next()) { //only one record as id is unique
            stmt.executeUpdate("DELETE FROM " + BOOK + " where " + ID + " = " + id + ";");
            return true;
        } else
            return false;
    }
}
