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
            response.setStatusCode(HttpStatus.SC_BAD_REQUEST);
            httpAsyncExchange.submitResponse(new BasicAsyncResponseProducer(response));
            return;
        }
        try {
            //For concurrency
            Future<Integer> token = Executors.newSingleThreadExecutor().submit(() -> Manager.getToken(httpRequest.getRequestLine().getUri()));
            if (Structure.validateToken(token.get())) { //true if contain
                Structure.deleteFromToken(token.get());
                response.setStatusCode(HttpStatus.SC_OK);
                httpAsyncExchange.submitResponse(new BasicAsyncResponseProducer(response));
                return;
            } else { //false if invalid token
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
            e.printStackTrace();
            response.setStatusCode(HttpStatus.SC_BAD_REQUEST);
            httpAsyncExchange.submitResponse(new BasicAsyncResponseProducer(response));
        } catch (Exception e) {
            e.getMessage();
            response.setStatusCode(HttpStatus.SC_BAD_REQUEST);
            httpAsyncExchange.submitResponse(new BasicAsyncResponseProducer(response));
        }
    }
}
