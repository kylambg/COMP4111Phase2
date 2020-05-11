package EventHandler;

import java.util.Random;
import java.util.Vector;
import java.util.concurrent.ConcurrentHashMap;

import Object.Action;

public class Structure {
    //Switch to ConcurrentHashMap, which is thread safe
    private static ConcurrentHashMap<Integer, String> tokenMap = new ConcurrentHashMap<>();
    private static ConcurrentHashMap<Integer, Integer> transactionMap = new ConcurrentHashMap<>();
    private static ConcurrentHashMap<Integer, Vector<Action>> actionMap = new ConcurrentHashMap<>();
    private static Random random = new Random();

    public static synchronized int addToToken(String user) {
        int token = random.nextInt();
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

    public static synchronized int addToTransaction(Integer token) {
        int transactionID = random.nextInt();
        transactionMap.put(token, transactionID);
        return transactionID;
    }

    public static synchronized boolean validateTransaction(int transactionID) {
        return transactionMap.containsValue(transactionID);
    }

    public static synchronized boolean deleteFromTransaction(int transactionID) {
        if (!validateTransaction(transactionID))
            return false;
        else {
            transactionMap.remove(transactionID);
            return true;
        }
    }

    public static synchronized boolean validateUser(String username) {

        return tokenMap.containsValue(username);
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
