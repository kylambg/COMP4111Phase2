package EventHandler;

import DataBaseManager.LookUpManager;
import Object.Book;

import DataBaseManager.AddToLibraryManager;
import DataBaseManager.Manager;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.apache.http.*;
import org.apache.http.entity.StringEntity;
import org.apache.http.nio.protocol.*;
import org.apache.http.protocol.HttpContext;
import org.apache.http.util.EntityUtils;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URLDecoder;
import java.util.Vector;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

public class BookHandler implements HttpAsyncRequestHandler<HttpRequest> {
    @Override
    public HttpAsyncRequestConsumer<HttpRequest> processRequest(final HttpRequest request, final HttpContext context) {
        // Buffer request content in memory for simplicity
        return new BasicAsyncRequestConsumer();
    }

    @Override
    public void handle(HttpRequest httpRequest, HttpAsyncExchange httpAsyncExchange, HttpContext httpContext) throws HttpException, IOException {
        final HttpResponse response = httpAsyncExchange.getResponse();
        String method = httpRequest.getRequestLine().getMethod();
        switch (method) {
            case "POST": {
                if (httpRequest instanceof HttpEntityEnclosingRequest) {
                    Future<Integer> token = Executors.newSingleThreadExecutor().submit(() -> Manager.getToken(httpRequest.getRequestLine().getUri()));
                    try {
                        if (Structure.validateToken(token.get())) { //true if contain
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
                            //Entity cannot match both Json key
                            JsonNode node = ObjectMap.INSTANCE.getObjectMapper().readTree(data);
                            String title = node.get("Title").asText(null);
                            String author = node.get("Author").asText(null);
                            String publisher = node.get("Publisher").asText(null);
                            int year = node.get("Year").asInt(-1);
                            //Invalid value
                            if (title == null || author == null || publisher == null || year < 0) {
                                response.setStatusCode(HttpStatus.SC_BAD_REQUEST);
                                httpAsyncExchange.submitResponse(new BasicAsyncResponseProducer(response));
                                return;
                            }
                            Future<Integer> LoginFuture = Executors.newSingleThreadExecutor().submit(
                                    () -> AddToLibraryManager.getInstance().addBook(title, author, publisher, year, Structure.getUserFromToken(token.get())));
                            int result = LoginFuture.get();
                            if (result < 0) {
                                response.setHeader("Duplicate record:", "/books/" + (-result));
                                response.setStatusCode(HttpStatus.SC_CONFLICT);
                                httpAsyncExchange.submitResponse(new BasicAsyncResponseProducer(response));
                                return;
                            } else if (result > 0) {
                                response.setHeader("Location:", "/books/" + result);
                                response.setStatusCode(HttpStatus.SC_CREATED);
                                httpAsyncExchange.submitResponse(new BasicAsyncResponseProducer(response));
                                return;
                            } else { //result == 0 //exception/cannot login
                                response.setStatusCode(HttpStatus.SC_BAD_REQUEST);
                                httpAsyncExchange.submitResponse(new BasicAsyncResponseProducer(response));
                                return;
                            }
                        } else { //false if invalid token
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

                } else { //httpRequest not instance ofHttpEntityEnclosingRequest
                    response.setStatusCode(HttpStatus.SC_BAD_REQUEST);
                    httpAsyncExchange.submitResponse(new BasicAsyncResponseProducer(response));
                    return;
                }
            }
            case "GET": {
                if (httpRequest instanceof HttpEntityEnclosingRequest) {
                    Future<Integer> token = Executors.newSingleThreadExecutor().submit(() -> Manager.getToken(httpRequest.getRequestLine().getUri()));
                    try {
                        if (Structure.validateToken(token.get())) {
                            //Reference from :https://stackoverflow.com/questions/13592236/parse-a-uri-string-into-name-value-collection
                            //Simple idea to split to query pair
                            //Much better than previous approach which is using contains and do substring
                            URI uri = new URI(httpRequest.getRequestLine().getUri());
                            String[] queries = uri.getQuery().split("&");
                            ConcurrentHashMap<String, String> queryList = new ConcurrentHashMap<>();
                            for (int i = 0; i < queries.length; i++) {
                                int index = queries[i].indexOf("=");
                                queryList.put(URLDecoder.decode(queries[i].substring(0, index), "UTF-8"),
                                        URLDecoder.decode(queries[i].substring(index + 1), "UTF-8"));
                            }
                            Future<Vector<Book>> bookVector = Executors.newSingleThreadExecutor().submit(
                                    () -> (LookUpManager.getInstance().getBooks(queryList, Structure.getUserFromToken(token.get()))));
                            //Book Handling (TO JSON ARRAY)
                            if (bookVector.get().size() == 0) {
                                response.setStatusCode(HttpStatus.SC_NO_CONTENT);
                                httpAsyncExchange.submitResponse(new BasicAsyncResponseProducer(response));
                                return;
                            } else {
                                ObjectNode jsonResult = ObjectMap.INSTANCE.getObjectMapper().createObjectNode();
                                //https://stackoverflow.com/questions/30997362/how-to-modify-jsonnode-in-java
                                //https://www.codota.com/code/java/methods/com.fasterxml.jackson.databind.node.ObjectNode/putPOJO
                                jsonResult.put("FoundBooks", bookVector.get().size());
                                jsonResult.putPOJO("Results", bookVector.get());
                                response.setEntity(new StringEntity(jsonResult.toString()));
                                response.setStatusCode(HttpStatus.SC_OK);
                                httpAsyncExchange.submitResponse(new BasicAsyncResponseProducer(response));
                                return;
                            }

                        } else { //false if invalid token
                            response.setStatusCode(HttpStatus.SC_BAD_REQUEST);
                            httpAsyncExchange.submitResponse(new BasicAsyncResponseProducer(response));
                            return;
                        }
                    } catch (InterruptedException | ExecutionException | URISyntaxException | UnsupportedEncodingException e) {
                        e.printStackTrace();
                        response.setStatusCode(HttpStatus.SC_BAD_REQUEST);
                        httpAsyncExchange.submitResponse(new BasicAsyncResponseProducer(response));
                        return;
                    }
                } else { //httpRequest not instance ofHttpEntityEnclosingRequest
                    response.setStatusCode(HttpStatus.SC_BAD_REQUEST);
                    httpAsyncExchange.submitResponse(new BasicAsyncResponseProducer(response));
                    return;
                }

            }
            default:
                //wrong method
                response.setStatusCode(HttpStatus.SC_BAD_REQUEST);
                httpAsyncExchange.submitResponse(new BasicAsyncResponseProducer(response));
        }
    }
}
