package DataBaseManager;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

public class AddToLibraryManager extends Manager {
    private Connector connector;

    //One and only one Connector object will be created
    private AddToLibraryManager() {
        try {
            connector = new Connector();
        } catch (ClassNotFoundException e) {
            e.printStackTrace();
        }
    }

    //Using Bill Pugh Singleton design pattern, good performance, thread safe
    //Reason not to use ENUM approach because extends have to use in this class
    //Reference:https://www.geeksforgeeks.org/java-singleton-design-pattern-practices-examples/
    private static class Singleton {
        private static final AddToLibraryManager instance = new AddToLibraryManager();
    }

    public static AddToLibraryManager getInstance() {
        return AddToLibraryManager.Singleton.instance;
    }

    public int getBookID(String title, String author, String publisher, int year, String user) {
        try {
            //because only single instance is created, put getConnection within method allows a thread
            //inside connection pool be called instead of using single thread if putting getConnection
            //within constructor
            Connection connection = connector.getConnection();
            //Use prepared statements to prevent SQL injection, it is more elegant than first edition
            String preparedStatement = "SELECT " + ID + " FROM " + BOOK + " WHERE "
                    + TITLE + "= % ? % AND " + AUTHOR + "= % ? % AND " + PUBLISHER + "= % ? % AND " + YEAR + "= % ? %";
            PreparedStatement stmt = connection.prepareStatement(preparedStatement);
            stmt.setString(1, title);
            stmt.setString(2, author);
            stmt.setString(3, publisher);
            stmt.setInt(4, year);
            ResultSet rs = stmt.executeQuery();
            // Zero or one book, check the first record is suffix to tell whether the book is duplicate
            if (rs.first()) {
                // contains book
                int result = rs.getInt(ID);
                rs.close();
                stmt.close();
                connector.closeConnection(connection);
                return result;
            }
            // no that book
            else {
                rs.close();
                stmt.close();
                connector.closeConnection(connection);
                return -1;
            }
        } catch (SQLException e) {
            e.printStackTrace();
            //Statement fail, cannot login
            return 0;
        }
    }

    public int addBook(String title, String author, String publisher, int year, String user) {
        try {
            //because only single instance is created, put getConnection within method allows a thread
            //inside connection pool be called instead of using single thread if putting getConnection
            //within constructor
            Connection connection = connector.getConnection();
            //If already has that book, ID will be positive
            int bookID = this.getBookID(title, author, publisher, year, user);
            if (bookID > 0) {
                //duplicate book
                //compare to first implementation, this approach reduce invoking getBookID method again
                //to retrieve BookID
                //idea reference from opponent team in phase 1
                return -bookID;
            } else if (bookID == -1) {
                //Use prepared statements to prevent SQL injection, it is more elegant than first edition
                String updateStatement = "INSERT INTO " + BOOK + " (" + TITLE + "," + AUTHOR + "," + PUBLISHER + "," + YEAR + ")"
                        + " VALUES (?, ?, ?, ?)";
                PreparedStatement update = connection.prepareStatement(updateStatement);
                update.setString(1, title);
                update.setString(2, author);
                update.setString(3, publisher);
                update.setInt(4, year);
                update.executeUpdate();
                //Reference: https://www.programcreek.com/java-api-examples/?class=java.sql.PreparedStatement&method=getGeneratedKeys
                //Can get key directly instead of doing a new search, can boost the performance
                ResultSet rs = update.getGeneratedKeys();
                int newBookID = rs.getInt(ID);
                rs.close();
                update.close();
                connector.closeConnection(connection);
                return newBookID;
            } else {
                //Throw SC.BadRequest, cannot login
                return 0;
            }
        } catch (SQLException e) {
            e.printStackTrace();
            //Throw SC.BadRequest, cannot login
            return 0;
        }
    }

}
