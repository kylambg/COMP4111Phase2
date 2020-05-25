package EventHandler;

import DataBaseManager.Manager;
import DataBaseManager.TransactionManager;
import Object.Action;

import com.fasterxml.jackson.databind.JsonNode;
import org.apache.http.*;
import org.apache.http.entity.ContentType;
import org.apache.http.entity.StringEntity;
import org.apache.http.nio.protocol.*;
import org.apache.http.protocol.HttpContext;
import org.apache.http.util.EntityUtils;

import java.io.IOException;
import java.util.Vector;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

public class TransactionHandler implements HttpAsyncRequestHandler<HttpRequest> {

    @Override
    public HttpAsyncRequestConsumer<HttpRequest> processRequest(HttpRequest httpRequest, HttpContext httpContext) throws HttpException, IOException {
        return new BasicAsyncRequestConsumer();
    }

    @Override
    public void handle(HttpRequest httpRequest, HttpAsyncExchange httpAsyncExchange, HttpContext httpContext) throws HttpException, IOException {
        final HttpResponse response = httpAsyncExchange.getResponse();
        String method = httpRequest.getRequestLine().getMethod();
        Future<Integer> token = Executors.newSingleThreadExecutor().submit(() -> Manager.getToken(httpRequest.getRequestLine().getUri()));
        switch (method) {
            case "POST":
                if (httpRequest instanceof HttpEntityEnclosingRequest) {
                    try {
                        if (Structure.validateToken(token.get())) {
                            HttpEntity entity = null;
                            entity = ((HttpEntityEnclosingRequest) httpRequest).getEntity();
                            //Check whether entity is null
                            byte[] data;
                            if (entity == null) {
                                response.setStatusCode(HttpStatus.SC_BAD_REQUEST);
                                httpAsyncExchange.submitResponse(new BasicAsyncResponseProducer(response));
                                return;
                            } else {
                                data = EntityUtils.toByteArray(entity);
                            }
                            if (data.length == 0) {// request new transaction ID
                                Future transactionID = Executors.newSingleThreadExecutor().submit(
                                        () -> Structure.addToTransaction(token.get()));
                                StringEntity e = new StringEntity(
                                        "{" + "\n" + "\"Transaction\" : " + transactionID.get() + "\n" + "}",
                                        ContentType.create("application/json", "UTF-8"));
                                response.setEntity(e);
                                response.setStatusCode(HttpStatus.SC_OK);
                                httpAsyncExchange.submitResponse(new BasicAsyncResponseProducer(response));
                                return;
                            } else {
                                JsonNode node = ObjectMap.INSTANCE.getObjectMapper().readTree(data);
                                int transactionID = node.get("Transaction").asInt(-1);
                                String operation = node.get("Operation").asText(null);
                                if (operation == null) {
                                    response.setStatusCode(HttpStatus.SC_BAD_REQUEST);
                                    httpAsyncExchange.submitResponse(new BasicAsyncResponseProducer(response));
                                    return;
                                }
                                if (Structure.validateTransaction(transactionID)) { //correct transaction ID
                                    if (Structure.containTransactionIDinAction(transactionID)) {//see whether user have empty action plan
                                        if (operation.equalsIgnoreCase("commit")) {
                                            Future<Boolean> result = Executors.newSingleThreadExecutor().submit(
                                                    () -> TransactionManager.getInstance().validateCommit(Structure.getAction(transactionID)));
                                            if (result.get()) {
                                                response.setStatusCode(HttpStatus.SC_OK);
                                                httpAsyncExchange.submitResponse(new BasicAsyncResponseProducer(response));
                                                return;
                                            } else {
                                                response.setStatusCode(HttpStatus.SC_BAD_REQUEST);
                                                httpAsyncExchange.submitResponse(new BasicAsyncResponseProducer(response));
                                                return;
                                            }
                                        } else if (operation.equalsIgnoreCase("cancel")) {
                                            Structure.deleteFromAction(transactionID);
                                        } else {
                                            response.setStatusCode(HttpStatus.SC_BAD_REQUEST);
                                            httpAsyncExchange.submitResponse(new BasicAsyncResponseProducer(response));
                                            return;
                                        }

                                    } else { //empty action plan
                                        response.setStatusCode(HttpStatus.SC_BAD_REQUEST);
                                        httpAsyncExchange.submitResponse(new BasicAsyncResponseProducer(response));
                                        return;
                                    }
                                } else { //wrong transaction ID
                                    response.setStatusCode(HttpStatus.SC_BAD_REQUEST);
                                    httpAsyncExchange.submitResponse(new BasicAsyncResponseProducer(response));
                                    return;
                                }
                            }

                        } else {//wrong token
                            response.setStatusCode(HttpStatus.SC_BAD_REQUEST);
                            httpAsyncExchange.submitResponse(new BasicAsyncResponseProducer(response));
                            return;
                        }
                    } catch (InterruptedException | ExecutionException e) {
                        e.printStackTrace();
                        response.setStatusCode(HttpStatus.SC_BAD_REQUEST);
                        httpAsyncExchange.submitResponse(new BasicAsyncResponseProducer(response));
                        return;
                    }
                } else { // not instance of Enclosing Request
                    response.setStatusCode(HttpStatus.SC_BAD_REQUEST);
                    httpAsyncExchange.submitResponse(new BasicAsyncResponseProducer(response));
                    return;
                }
                break;
            case "PUT":
                if (httpRequest instanceof HttpEntityEnclosingRequest) {
                    try {
                        if (Structure.validateToken(token.get())) {
                            HttpEntity entity = null;
                            entity = ((HttpEntityEnclosingRequest) httpRequest).getEntity();
                            //Check whether entity is null
                            byte[] data;
                            if (entity == null) { //empty body
                                response.setStatusCode(HttpStatus.SC_BAD_REQUEST);
                                httpAsyncExchange.submitResponse(new BasicAsyncResponseProducer(response));
                                return;
                            } else {
                                data = EntityUtils.toByteArray(entity);
                                JsonNode node = ObjectMap.INSTANCE.getObjectMapper().readTree(data);
                                int transactionID = node.get("Transaction").asInt(-1);
                                int boodID = node.get("Book").asInt(-1);
                                if (Structure.validateTransaction(transactionID)) {
                                    String action = node.get("Action").asText(null);
                                    if (boodID <= 0 || action == null) { //wrong body
                                        response.setStatusCode(HttpStatus.SC_BAD_REQUEST);
                                        httpAsyncExchange.submitResponse(new BasicAsyncResponseProducer(response));
                                        return;
                                    }
                                    if (action.equalsIgnoreCase("loan") || action.equalsIgnoreCase("return")) {
                                        if (!Structure.containTransactionIDinAction(transactionID)) {
                                            //first action
                                            Vector<Action> actions = new Vector<Action>();
                                            actions.add(new Action(boodID, action));
                                            Structure.addToActionMap(transactionID, actions);
                                            response.setStatusCode(HttpStatus.SC_OK);
                                            httpAsyncExchange.submitResponse(new BasicAsyncResponseProducer(response));
                                            return;
                                        } else { //new action
                                            Structure.getAction(transactionID).add(new Action(boodID, action));
                                            response.setStatusCode(HttpStatus.SC_OK);
                                            httpAsyncExchange.submitResponse(new BasicAsyncResponseProducer(response));
                                            return;
                                        }
                                    } else {
                                        //Action other than loan and return
                                        response.setStatusCode(HttpStatus.SC_BAD_REQUEST);
                                        httpAsyncExchange.submitResponse(new BasicAsyncResponseProducer(response));
                                        return;
                                    }
                                } else {
                                    //wrong transaction id
                                    response.setStatusCode(HttpStatus.SC_BAD_REQUEST);
                                    httpAsyncExchange.submitResponse(new BasicAsyncResponseProducer(response));
                                    return;
                                }
                            }
                        } else {//wrong token
                            response.setStatusCode(HttpStatus.SC_BAD_REQUEST);
                            httpAsyncExchange.submitResponse(new BasicAsyncResponseProducer(response));
                            return;
                        }
                    } catch (InterruptedException | ExecutionException e) {
                        e.printStackTrace();
                        response.setStatusCode(HttpStatus.SC_BAD_REQUEST);
                        httpAsyncExchange.submitResponse(new BasicAsyncResponseProducer(response));
                        return;
                    }
                } else { // not instance of Enclosing Request
                    response.setStatusCode(HttpStatus.SC_BAD_REQUEST);
                    httpAsyncExchange.submitResponse(new BasicAsyncResponseProducer(response));
                    return;
                }
            default:
                response.setStatusCode(HttpStatus.SC_BAD_REQUEST);
                httpAsyncExchange.submitResponse(new BasicAsyncResponseProducer(response));
                return;
        }


    }
}
