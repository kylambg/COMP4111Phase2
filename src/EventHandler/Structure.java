package EventHandler;

import java.util.Random;
import java.util.Vector;
import java.util.concurrent.ConcurrentHashMap;

import Object.Action;

public class Structure {
    //Switch to ConcurrentHashMap, which is thread safe
    private static ConcurrentHashMap<String, String> userPasswordMap = new ConcurrentHashMap<>();
    //Token, username
    private static ConcurrentHashMap<Integer, String> tokenMap = new ConcurrentHashMap<>();
    //token, transaction id
    private static ConcurrentHashMap<Integer, Integer> transactionMap = new ConcurrentHashMap<>();
    //transaction id, action
    private static ConcurrentHashMap<Integer, Vector<Action>> actionMap = new ConcurrentHashMap<>();
    private static Random random = new Random();

    //User Part
    public static synchronized void addToUserPassword(String user, String password) {
        userPasswordMap.put(user, password);
    }

    public static synchronized boolean validateUserExist(String user, String password) {
        return userPasswordMap.containsKey(user) && userPasswordMap.containsValue(password);
    }

    public static synchronized void deleteFromUserPassword(String user) {
        userPasswordMap.remove(user);
    }

    //token part
    public static synchronized int addToToken(String user) {
        int token=-1;
        do {
            token = random.nextInt();
        }while(tokenMap.containsValue(token));
        tokenMap.put(token, user);
        return token;
    }

    public static synchronized boolean validateToken(Integer token) {
        return tokenMap.containsKey(token);
    }

    //Won't check whether the token is inside or not, be careful
    public static synchronized void deleteFromToken(int token) {
        tokenMap.remove(token);
    }

    public static synchronized String getUserFromToken(int token) {
        return tokenMap.get(token);
    }

    //transaction part
    public static synchronized int addToTransaction(Integer token) {
        int transactionID=-1;
        do{
            transactionID = random.nextInt();
        }while(transactionMap.containsValue(transactionID));
        transactionMap.put(token, transactionID);
        return transactionID;
    }

    public static synchronized boolean validateTransaction(int transactionID) {
        return transactionMap.containsValue(transactionID);
    }

    public static synchronized boolean containTransaction(int token) {
        return transactionMap.containsKey(token);
    }

    public static synchronized void deleteFromTransaction(int token) {
        transactionMap.remove(token);
    }

    public static synchronized int getTransactionIDFromToken(int token) {
        return transactionMap.get(token);
    }

    public static synchronized void addToActionMap(Integer transactionID, Vector<Action> actions) {
        actionMap.put(transactionID, actions);
    }

    public static synchronized Vector<Action> getAction(Integer transactionID) {
        return actionMap.get(transactionID);
    }

    public static synchronized boolean containTransactionIDinAction(Integer transactionID) {
        return actionMap.containsKey(transactionID);
    }

    public static synchronized void deleteFromAction(int transactionID) {
        actionMap.remove(transactionID);
    }
}
