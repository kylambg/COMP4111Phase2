package EventHandler;

import DataBaseManager.LoginManager;
import com.fasterxml.jackson.databind.JsonNode;
import org.apache.http.*;
import org.apache.http.entity.ContentType;
import org.apache.http.entity.StringEntity;
import org.apache.http.nio.protocol.*;
import org.apache.http.protocol.HttpContext;
import org.apache.http.util.EntityUtils;

import java.io.IOException;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

public class LoginHandler implements HttpAsyncRequestHandler<HttpRequest> {
    @Override
    public HttpAsyncRequestConsumer<HttpRequest> processRequest(HttpRequest httpRequest, HttpContext httpContext) throws HttpException, IOException {
        // Buffer request content in memory for simplicity
        return new BasicAsyncRequestConsumer();
    }

    @Override
    public void handle(HttpRequest httpRequest, HttpAsyncExchange httpAsyncExchange, HttpContext httpContext) throws HttpException, IOException {
        final HttpResponse response = httpAsyncExchange.getResponse();
        //method not equal post
        if (!httpRequest.getRequestLine().getMethod().equals("POST")) {
            response.setStatusCode(HttpStatus.SC_BAD_REQUEST);
            httpAsyncExchange.submitResponse(new BasicAsyncResponseProducer(response));
            return;
        }
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
            //Entity cannot match both Json key
            try {
                JsonNode node = ObjectMap.INSTANCE.getObjectMapper().readTree(data);
                String username = node.get("Username").asText(null);
                String password = node.get("Password").asText(null);
                if (username == null || password == null) {
                    response.setStatusCode(HttpStatus.SC_BAD_REQUEST);
                    httpAsyncExchange.submitResponse(new BasicAsyncResponseProducer(response));
                    return;
                }
                //Original implementation creates a new connection when every user login to the system/logout/do anything
                //This will ruin the thread pool in current construction
                //This implementation will then use the root account to create a connection, then check whether the user
                //match the userID and userPassword
                //Using Future to store the asynchronous result
                //Reference From https://popcornylu.gitbooks.io/java_multithread/content/async/future.html
                Future<Integer> LoginFuture = Executors.newSingleThreadExecutor().submit(() -> LoginManager.getInstance().login(username, password));
                //switch has a sightly better performance than IF-ELSE
                //System.out.println(LoginFuture.get());
                switch (LoginFuture.get()) {
                    case 1:
                        int token = Structure.addToToken(username);
                        //using string instead of objectMapper to save cost
                        StringEntity e = new StringEntity(
                                "{" + "\n" + "\"Token\" : " + token + "\n" + "}",
                                ContentType.create("application/json", "UTF-8"));
                        response.setEntity(e);
                        response.setStatusCode(HttpStatus.SC_OK);
                        httpAsyncExchange.submitResponse(new BasicAsyncResponseProducer(response));
                        return;
                    case -1:
                        response.setStatusCode(HttpStatus.SC_CONFLICT);
                        httpAsyncExchange.submitResponse(new BasicAsyncResponseProducer(response));
                        return;
                    case 0:
                        //System.out.println("WRONG USER/PASSWORD");
                        response.setStatusCode(HttpStatus.SC_BAD_REQUEST);
                        httpAsyncExchange.submitResponse(new BasicAsyncResponseProducer(response));
                        return;
                }
                response.setStatusCode(HttpStatus.SC_BAD_REQUEST);
                httpAsyncExchange.submitResponse(new BasicAsyncResponseProducer(response));
            } catch (Exception e) {
                e.getMessage();
                response.setStatusCode(HttpStatus.SC_BAD_REQUEST);
                httpAsyncExchange.submitResponse(new BasicAsyncResponseProducer(response));
            }
        } else {
            response.setStatusCode(HttpStatus.SC_BAD_REQUEST);
            httpAsyncExchange.submitResponse(new BasicAsyncResponseProducer(response));
        }
    }
}
