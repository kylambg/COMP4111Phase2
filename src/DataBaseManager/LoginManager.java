package DataBaseManager;

import EventHandler.Structure;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;

public class LoginManager extends Manager {
    private Connector connector;

    //One and only one Connector object will be created
    private LoginManager() {
        try {
            connector = new Connector();
        } catch (ClassNotFoundException e) {
            e.printStackTrace();
        }
    }

    //Using Bill Pugh Singleton design pattern, good performance, thread safe
    //Reason not to use ENUM approach because extends have to use in this class
    private static class Singleton {
        private static final LoginManager instance = new LoginManager();
    }

    public static LoginManager getInstance() {
        return Singleton.instance;
    }

    public int login(String user, String password) {
        //System.out.println("USER: " + user+ " PASSWORD "+ password);
        //System.out.println(IDStructure.validateUser(user));
        if (Structure.validateUserExist(user, password)) {
            //System.out.println("Already Login");
            return -1;
        } else {
            try {
                Connection connection = connector.getConnection();
                //success(does not throw exception)
                String prepareStmt = "SELECT UserID FROM " + USER + " WHERE "
                        + USERNAME + "= ? AND " + PASSWORD + " = ?";
                PreparedStatement stmt = connection.prepareStatement(prepareStmt);
                stmt.setString(1, user);
                stmt.setString(2, password);
                ResultSet rs = stmt.executeQuery();
                if (!rs.next()) {//wrong password/user
                    stmt.close();
                    rs.close();
                    connector.closeConnection(connection);
                    return 0;
                }
                Structure.addToUserPassword(user, password);
                Structure.addToToken(user);
                //System.out.println(Structure.addToToken(user)); //(print out token)
                stmt.close();
                rs.close();
                connector.closeConnection(connection);
                return 1;
            } catch (Exception e) {
                e.printStackTrace();
                //Throw SC.BadRequest
                System.out.println("Wrong password/username");
                return 0;
            }
        }
    }

}
