package EventHandler;

import DataBaseManager.DeleteManager;
import DataBaseManager.LoanReturnManager;
import DataBaseManager.Manager;
import com.fasterxml.jackson.databind.JsonNode;
import org.apache.http.*;
import org.apache.http.nio.protocol.*;
import org.apache.http.protocol.HttpContext;

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
                try {
                    if (Structure.validateToken(token.get())) {
                        String uri = httpRequest.getRequestLine().getUri();
                        String[] path = uri.split("/");
                        int id = Integer.parseInt(path[path.length - 1].substring(0, path[path.length - 1].indexOf("?")));
                        //System.out.println("ID: " + id);
                        JsonNode node = ObjectMap.INSTANCE.getObjectMapper().createObjectNode();
                        int action = node.get("Avaliable").asInt(-1);
                        Future<Integer> loanReturnResult = Executors.newSingleThreadExecutor().submit(
                                () -> LoanReturnManager.getInstance().loanReturn(action, id));
                        switch (loanReturnResult.get()) {
                            case 0:
                                response.setStatusCode(HttpStatus.SC_NOT_FOUND);
                                response.setReasonPhrase("NO BOOK RECORD");
                                httpAsyncExchange.submitResponse(new BasicAsyncResponseProducer(response));
                                return;
                            case 1:
                                response.setStatusCode(HttpStatus.SC_OK);
                                httpAsyncExchange.submitResponse(new BasicAsyncResponseProducer(response));
                                return;
                            default:
                                response.setStatusCode(HttpStatus.SC_BAD_REQUEST);
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
                    response.setStatusCode(HttpStatus.SC_BAD_REQUEST);
                    httpAsyncExchange.submitResponse(new BasicAsyncResponseProducer(response));
                    return;
                } catch (Exception e) {
                    e.getMessage();
                    response.setStatusCode(HttpStatus.SC_BAD_REQUEST);
                    httpAsyncExchange.submitResponse(new BasicAsyncResponseProducer(response));
                    return;
                }
            case "DELETE":
                //Future<Integer> token = Executors.newSingleThreadExecutor().submit(()-> Manager.getToken(httpRequest.getRequestLine().getUri()));
                try {
                    if (Structure.validateToken(token.get())) {
                        String uri = httpRequest.getRequestLine().getUri();
                        String[] path = uri.split("/");
                        int id = Integer.parseInt(path[path.length - 1].substring(0, path[path.length - 1].indexOf("?")));
                        //System.out.println("ID: " + id);
                        Future<Boolean> deleteResult = Executors.newSingleThreadExecutor().submit(
                                () -> DeleteManager.getInstance().deleteBook(id));
                        if (deleteResult.get()) {
                            response.setStatusCode(HttpStatus.SC_OK);
                            httpAsyncExchange.submitResponse(new BasicAsyncResponseProducer(response));
                            return;
                        } else {
                            response.setStatusCode(HttpStatus.SC_NOT_FOUND);
                            response.setReasonPhrase("NO BOOK RECORD");
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
                    response.setStatusCode(HttpStatus.SC_BAD_REQUEST);
                    httpAsyncExchange.submitResponse(new BasicAsyncResponseProducer(response));
                    return;
                } catch (Exception e) {
                    e.getMessage(); //SQLException
                    response.setStatusCode(HttpStatus.SC_BAD_REQUEST);
                    httpAsyncExchange.submitResponse(new BasicAsyncResponseProducer(response));
                    return;
                }

        }

    }

}
