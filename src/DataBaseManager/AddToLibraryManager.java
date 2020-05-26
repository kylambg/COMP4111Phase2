package DataBaseManager;

import java.sql.*;

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


    /**
     *
     * @param title title of book to be fetched
     * @param author author of book to be fetched
     * @param publisher publisher of book to be fetched
     * @param year year published of book to be fetched
     * @param user current user initiating the request
     * @return type integer: ID if book found, -1 if not found, 0 if statement fail/can't login
     * @author kylambg
     */
    public int getBookID(String title, String author, String publisher, int year, String user) {
        try {
            //because only single instance is created, put getConnection within method allows a thread
            //inside connection pool be called instead of using single thread if putting getConnection
            //within constructor
            Connection connection = connector.getConnection();
            //Use prepared statements to prevent SQL injection, it is more elegant than first edition
            String preparedStatement = "SELECT " + ID + " FROM " + BOOK + " WHERE "
                    + TITLE + "= ? AND " + AUTHOR + "= ? AND " + PUBLISHER + "= ? AND " + YEAR + "=  ? ";
            PreparedStatement stmt = connection.prepareStatement(preparedStatement);
            stmt.setString(1, title);
            stmt.setString(2, author);
            stmt.setString(3, publisher);
            stmt.setInt(4, year);
            ResultSet rs = stmt.executeQuery();
            //System.out.println(stmt.toString());
            // Zero or one book, check the first record is suffix to tell whether the book is duplicate
            int returnval;
            if (rs.first()) {
                // contains book
                returnval = rs.getInt(ID);
            }
            // no that book
            else returnval=-1;

            rs.close();
            stmt.close();
            connector.closeConnection(connection);
            return returnval;
        } catch (Exception e) {
            e.getMessage();
            //Statement fail, cannot login
            return 0;
        }
    }

    /**
     *
     * @param title title of book to be added
     * @param author author of book to be added
     * @param publisher publisher of book to be added
     * @param year year of publishing of book to be added
     * @param user current user initiating the request
     * @return integer -bookID (negative value) if book already exist, new book ID if successfully added, 0 if error
     */
    public int addBook(String title, String author, String publisher, int year, String user) {
        try {
            //because only single instance is created, put getConnection within method allows a thread
            //inside connection pool be called instead of using single thread if putting getConnection
            //within constructor
            Connection connection = connector.getConnection();
            //If already has that book, ID will be positive
            int bookID = this.getBookID(title, author, publisher, year, user);
            if (bookID > 0) {
                //duplicate book / book already exist in records
                //compare to first implementation, this approach reduce invoking getBookID method again
                //to retrieve BookID
                //idea reference from opponent team in phase 1
                return -bookID;
            } else if (bookID == -1) {
                //Use prepared statements to prevent SQL injection, it is more elegant than first edition
                String updateStatement = "INSERT INTO " + BOOK + " (" + TITLE + "," + AUTHOR + "," + PUBLISHER + "," + YEAR + ")"
                        + " VALUES (?, ?, ?, ?)";
                PreparedStatement update = connection.prepareStatement(updateStatement, Statement.RETURN_GENERATED_KEYS);
                update.setString(1, title);
                update.setString(2, author);
                update.setString(3, publisher);
                update.setInt(4, year);
                update.executeUpdate();
                //Reference: https://blog.csdn.net/weixin_42193004/article/details/94958474?utm_medium=distribute.pc_relevant.none-task-blog-BlogCommendFromMachineLearnPai2-1.nonecase&depth_1-utm_source=distribute.pc_relevant.none-task-blog-BlogCommendFromMachineLearnPai2-1.nonecase
                //Can get key directly instead of doing a new search, can boost the performance
                ResultSet rs = update.getGeneratedKeys();
                int newBookID = 0;
                if (rs.next())
                    newBookID = rs.getInt(1);
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
