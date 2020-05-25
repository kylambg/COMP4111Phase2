package EventHandler;

import DataBaseManager.Manager;
import org.apache.http.HttpException;
import org.apache.http.HttpRequest;
import org.apache.http.HttpResponse;
import org.apache.http.HttpStatus;
import org.apache.http.nio.protocol.*;
import org.apache.http.protocol.HttpContext;

import java.io.IOException;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

public class LogoutHandler implements HttpAsyncRequestHandler<HttpRequest> {
    @Override
    public HttpAsyncRequestConsumer<HttpRequest> processRequest(final HttpRequest request, final HttpContext context) {
        // Buffer request content in memory for simplicity
        return new BasicAsyncRequestConsumer();
    }

    @Override
    public void handle(HttpRequest httpRequest, HttpAsyncExchange httpAsyncExchange, HttpContext httpContext) throws HttpException, IOException {
        final HttpResponse response = httpAsyncExchange.getResponse();
        if (!httpRequest.getRequestLine().getMethod().equals("GET")) {
            System.out.println("Not get");
            response.setStatusCode(HttpStatus.SC_BAD_REQUEST);
            httpAsyncExchange.submitResponse(new BasicAsyncResponseProducer(response));
            return;
        }
        try {
            //For concurrency
            Future<Integer> token = Executors.newSingleThreadExecutor().submit(() -> Manager.getToken(httpRequest.getRequestLine().getUri()));
            if (Structure.validateToken(token.get())) { //true if contain
                Structure.deleteFromToken(token.get());
                if (Structure.containTransaction(token.get())) {
                    Structure.deleteFromAction(Structure.getTransactionIDFromToken(token.get()));
                    Structure.deleteFromTransaction(token.get());
                }
                response.setStatusCode(HttpStatus.SC_OK);
                httpAsyncExchange.submitResponse(new BasicAsyncResponseProducer(response));
                return;
            } else { //false if invalid token
                //System.out.println("invalid token");
                response.setStatusCode(HttpStatus.SC_BAD_REQUEST);
                httpAsyncExchange.submitResponse(new BasicAsyncResponseProducer(response));
                return;
            }
        } catch (NumberFormatException e) {
            e.getMessage();
            System.out.println("CANNOT PARSE LONG");
            response.setStatusCode(HttpStatus.SC_BAD_REQUEST);
            httpAsyncExchange.submitResponse(new BasicAsyncResponseProducer(response));
        } catch (InterruptedException | ExecutionException e) {
            System.out.println("Interrupted Exception/Execution Exception");
            e.printStackTrace();
            response.setStatusCode(HttpStatus.SC_BAD_REQUEST);
            httpAsyncExchange.submitResponse(new BasicAsyncResponseProducer(response));
        } catch (Exception e) {
            System.out.println("Other Exception");
            e.printStackTrace();
            response.setStatusCode(HttpStatus.SC_BAD_REQUEST);
            httpAsyncExchange.submitResponse(new BasicAsyncResponseProducer(response));
        }
    }
}
