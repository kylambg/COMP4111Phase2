package DataBaseManager;

import Object.Book;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Vector;
import java.util.concurrent.ConcurrentHashMap;

public class LookUpManager extends Manager {
    private Connector connector;

    //One and only one Connector object will be created
    private LookUpManager() {
        try {
            connector = new Connector();
        } catch (ClassNotFoundException e) {
            e.printStackTrace();
        }
    }

    //Using Bill Pugh Singleton design pattern, good performance, thread safe
    //Reason not to use ENUM approach because extends have to use in this class
    private static class Singleton {
        private static final LookUpManager instance = new LookUpManager();
    }

    public static LookUpManager getInstance() {
        return Singleton.instance;
    }

    //using Vector to return multiple records
    //Vector is thread-safe
    //better than using a int string to store multiple records in original version
    public Vector<Book> getBooks(ConcurrentHashMap<String, String> query, String user) {
        StringBuffer executeStatement = new StringBuffer().append("SELECT Title, Author, Publisher, Year FROM ").append(BOOK);
        Vector<Book> books = new Vector<>();
        try {
            Connection connection = connector.getConnection();
            if (query.size() == 1) { //only tokens query
                Statement stmtCheck = connection.createStatement();
                ResultSet rs = stmtCheck.executeQuery(executeStatement.toString());
                while (rs.next()) {
                    Book book = new Book(rs.getString(1), rs.getString(2), rs.getString(3), rs.getInt(4));
                    System.out.println(book.toString());
                    books.add(book);
                }
                rs.close();
                stmtCheck.close();
                connector.closeConnection(connection);
                return books;
            } else {
                executeStatement.append(" WHERE ");
                String sortby = null;
                String limit = null;
                String orderby = null;
                int flag = 0;
                //Always use Entry to iterate all key-value pair for performance
                //https://blog.jooq.org/2015/02/05/top-10-easy-performance-optimisations-in-java/
                for (ConcurrentHashMap.Entry<String, String> set : query.entrySet()) {
                    //System.out.println("key: " + set.getKey());
                    //System.out.println("query: " + set.getValue());
                    if (set.getKey().toLowerCase().equalsIgnoreCase("title")) {
                        executeStatement.append(TITLE).append(" LIKE '%").append(set.getValue()).append("%'").append(" AND ");
                        flag = 1;
                    } else if (set.getKey().toLowerCase().equalsIgnoreCase("author")) {
                        executeStatement.append(AUTHOR).append(" LIKE '%").append(set.getValue()).append("%'").append(" AND ");
                        flag = 1;
                    } else if (set.getKey().toLowerCase().equalsIgnoreCase("publisher")) {
                        executeStatement.append(PUBLISHER).append(" LIKE '%").append(set.getValue()).append("%'").append(" AND ");
                        flag = 1;
                    } else if (set.getKey().toLowerCase().equalsIgnoreCase("year")) {
                        executeStatement.append(YEAR).append(" LIKE '%").append(set.getValue()).append("%'").append(" AND ");
                        flag = 1;
                    } else if (set.getKey().toLowerCase().equalsIgnoreCase("id")) {
                        try {
                            if (Integer.parseInt(set.getValue()) <= 0)
                                return null;
                        } catch (NumberFormatException e) {
                            e.getMessage();
                            return null;
                        }
                        executeStatement.append(ID).append(" LIKE '%").append(set.getValue()).append("%'").append(" AND ");
                        flag = 1;
                    } else if (set.getKey().toLowerCase().equalsIgnoreCase("sortby")) {
                        //support id/title/author/publisher/year
                        sortby = set.getValue();
                    } else if (set.getKey().toLowerCase().equalsIgnoreCase("limit")) {
                        //support single integer
                        limit = set.getValue();
                    } else if (set.getKey().toLowerCase().equalsIgnoreCase("order")) {
                        //support ASEC/DSEC
                        orderby = set.getValue();
                    }
                }
                //System.out.println(executeStatement.toString());
                if (flag == 1)
                    //Remove last AND if we use title, year, publisher and author to perform sorting
                    executeStatement.delete(executeStatement.length() - 4, executeStatement.length());
                else if (flag == 0)
                    //Remove WHERE
                    executeStatement.delete(executeStatement.length() - 6, executeStatement.length());
                if (sortby != null) {
                    if (sortby.equalsIgnoreCase("id"))
                        executeStatement.append(" ORDER BY ").append(ID);
                    else
                        executeStatement.append(" ORDER BY ").append(sortby);
                }
                if (orderby != null) {
                    executeStatement.append(" ").append(orderby);
                }
                if (limit != null) {
                    try {
                        if (Integer.parseInt(limit) < 0)
                            return null;
                    } catch (NumberFormatException e) {
                        e.getMessage();
                    }
                    executeStatement.append(" LIMIT ").append(limit);
                }
                executeStatement.append(";");
                //check the statement
                //System.out.println(executeStatement);
                //System.out.println("");
                Statement stmt = connection.createStatement();
                ResultSet rs = stmt.executeQuery(executeStatement.toString());
                while (rs.next()) {
                    Book book = new Book(rs.getString(1), rs.getString(2), rs.getString(3), rs.getInt(4));
                    //System.out.println(book.toString());
                    books.add(book);
                }
                rs.close();
                stmt.close();
                connector.closeConnection(connection);
                return books;
            }
        } catch (SQLException e) {
            e.printStackTrace();
            return books; //size == 0(NO RECORD)
        }
    }
}
