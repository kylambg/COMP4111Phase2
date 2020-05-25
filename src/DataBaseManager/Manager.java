package DataBaseManager;

import java.net.URI;
import java.net.URISyntaxException;

abstract public class Manager {
    protected static final String BOOK = "books";
    protected static final String USER = "users";
    protected static final String USERNAME = "userName";
    protected static final String PASSWORD = "Passwords";
    protected static final String ID = "BookID";
    protected static final String TITLE = "Title";
    protected static final String AUTHOR = "Author";
    protected static final String PUBLISHER = "Publisher";
    protected static final String YEAR = "Year";
    protected static final String STATUS = "Statuses";

    public static int getToken(String uri) throws URISyntaxException {
        //this may be able to optimize to use string only
        if (uri == null)
            return -1;
        else {
            URI u = new URI(uri);
            String[] query = u.getQuery().split("&");
            //for loop has a better performance than for each
            for (int i = 0; i < query.length; ++i) {
                if (query[i].contains("token=")) {
                    try {
                        return Integer.parseInt(query[i].substring(6)); //assign token to value}
                    } catch (Exception e) {
                        return -1; //invalid number format
                    }
                }
            }
        }
        return -1;
    }
}


