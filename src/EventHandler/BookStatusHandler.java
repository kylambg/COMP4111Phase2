package EventHandler;

import DataBaseManager.DeleteManager;
import DataBaseManager.LoanReturnManager;
import DataBaseManager.Manager;
import com.fasterxml.jackson.databind.JsonNode;
import org.apache.http.*;
import org.apache.http.nio.protocol.*;
import org.apache.http.protocol.HttpContext;
import org.apache.http.util.EntityUtils;

import java.io.IOException;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

/**
 * Handle delete, loan, and return
 */
public class BookStatusHandler implements HttpAsyncRequestHandler<HttpRequest> {
    //delete/loan/return
    @Override
    public HttpAsyncRequestConsumer<HttpRequest> processRequest(HttpRequest httpRequest, HttpContext httpContext) throws HttpException, IOException {
        // Buffer request content in memory for simplicity
        return new BasicAsyncRequestConsumer();
    }

    @Override
    public void handle(HttpRequest httpRequest, HttpAsyncExchange httpAsyncExchange, HttpContext httpContext) throws HttpException, IOException {
        final HttpResponse response = httpAsyncExchange.getResponse();
        String method = httpRequest.getRequestLine().getMethod();
        Future<Integer> token = Executors.newSingleThreadExecutor().submit(() -> Manager.getToken(httpRequest.getRequestLine().getUri()));
        switch (method) {
            case "PUT":
                HttpEntity entity = null;
                if (httpRequest instanceof HttpEntityEnclosingRequest) {
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
                    try {
                        if (Structure.validateToken(token.get())) {
                            String uri = httpRequest.getRequestLine().getUri();
                            String[] path = uri.split("/");
                            int id = Integer.parseInt(path[path.length - 1].substring(0, path[path.length - 1].indexOf("?")));
                            //System.out.println("ID: " + id);
                            JsonNode node = ObjectMap.INSTANCE.getObjectMapper().readTree(data);
                            int action = node.get("Available").asInt(-1);
                            //System.out.println(action);
                            Future<Integer> loanReturnResult = Executors.newSingleThreadExecutor().submit(
                                    () -> LoanReturnManager.getInstance().loanReturn(action, id));
                            switch (loanReturnResult.get()) {
                                case 0:
                                    response.setStatusCode(HttpStatus.SC_NOT_FOUND);
                                    response.setReasonPhrase("No book record");
                                    httpAsyncExchange.submitResponse(new BasicAsyncResponseProducer(response));
                                    return;
                                case 1:
                                    response.setStatusCode(HttpStatus.SC_OK);
                                    httpAsyncExchange.submitResponse(new BasicAsyncResponseProducer(response));
                                    return;
                                default:
                                    //System.out.println("Exception raise/Already loan/return/Wrong available value");
                                    response.setStatusCode(HttpStatus.SC_BAD_REQUEST);
                                    httpAsyncExchange.submitResponse(new BasicAsyncResponseProducer(response));
                                    return;
                            }

                        } else {
                            System.out.println("Invalid token");
                            response.setStatusCode(HttpStatus.SC_BAD_REQUEST);
                            httpAsyncExchange.submitResponse(new BasicAsyncResponseProducer(response));
                            return;
                        }


                    } catch (InterruptedException | ExecutionException e) {
                        System.out.println("Interrupted/Execution Exception");
                        e.printStackTrace();
                        response.setStatusCode(HttpStatus.SC_BAD_REQUEST);
                        httpAsyncExchange.submitResponse(new BasicAsyncResponseProducer(response));
                        return;
                    } catch (Exception e) {
                        System.out.println("Other Exception");
                        e.printStackTrace();
                        response.setStatusCode(HttpStatus.SC_BAD_REQUEST);
                        httpAsyncExchange.submitResponse(new BasicAsyncResponseProducer(response));
                        return;
                    }
                } else {
                    //No entity
                    response.setStatusCode(HttpStatus.SC_BAD_REQUEST);
                    httpAsyncExchange.submitResponse(new BasicAsyncResponseProducer(response));
                }
            case "DELETE":
                //Future<Integer> token = Executors.newSingleThreadExecutor().submit(()-> Manager.getToken(httpRequest.getRequestLine().getUri()));
                try {
                    if (Structure.validateToken(token.get())) {
                        String uri = httpRequest.getRequestLine().getUri();
                        //System.out.println(uri);
                        String[] path = uri.split("/");
                        int id = Integer.parseInt(path[path.length - 1].substring(0, path[path.length - 1].indexOf("?")));
                 /*       if (uri.getQuery() != null) {
                            query = uri.getQuery().toLowerCase().split("&");
                            path = uri.getPath().toLowerCase().split("/");
                            if(check_digit_valid(path[path.length-1]))
                                id = Integer.valueOf(path[path.length-1]);
                        }*/
                        //System.out.println("ID: " + id);
                        Future<Boolean> deleteResult = Executors.newSingleThreadExecutor().submit(
                                () -> DeleteManager.getInstance().deleteBook(id));
                        if (deleteResult.get()) {
                            response.setStatusCode(HttpStatus.SC_OK);
                            httpAsyncExchange.submitResponse(new BasicAsyncResponseProducer(response));
                            return;
                        } else {
                            response.setStatusCode(HttpStatus.SC_NOT_FOUND);
                            response.setReasonPhrase("No book record");
                            httpAsyncExchange.submitResponse(new BasicAsyncResponseProducer(response));
                            return;
                        }

                    } else {
                        response.setStatusCode(HttpStatus.SC_BAD_REQUEST);
                        httpAsyncExchange.submitResponse(new BasicAsyncResponseProducer(response));
                        return;
                    }


                } catch (InterruptedException | ExecutionException e) {
                    e.printStackTrace();
                    System.out.println("Interrupted/Execution Exception");
                    response.setStatusCode(HttpStatus.SC_BAD_REQUEST);
                    httpAsyncExchange.submitResponse(new BasicAsyncResponseProducer(response));
                    return;
                } catch (Exception e) {
                    System.out.println("Other Exception");
                    e.printStackTrace(); //SQLException
                    response.setStatusCode(HttpStatus.SC_BAD_REQUEST);
                    httpAsyncExchange.submitResponse(new BasicAsyncResponseProducer(response));
                    return;
                }

        }

    }

}
