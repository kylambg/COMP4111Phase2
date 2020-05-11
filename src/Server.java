import org.apache.http.*;
import org.apache.http.config.SocketConfig;
import org.apache.http.entity.ContentType;
import org.apache.http.impl.nio.bootstrap.HttpServer;
import org.apache.http.impl.nio.bootstrap.ServerBootstrap;
import org.apache.http.impl.nio.reactor.IOReactorConfig;
import org.apache.http.nio.entity.NFileEntity;
import org.apache.http.nio.entity.NStringEntity;
import org.apache.http.nio.protocol.*;
import org.apache.http.protocol.HttpContext;
import org.apache.http.protocol.HttpCoreContext;
import org.apache.http.ssl.SSLContexts;

import javax.net.ssl.SSLContext;
import java.net.SocketTimeoutException;
import java.net.URL;
import java.sql.*;
import java.util.concurrent.TimeUnit;

import EventHandler.*;

public class Server {
    static final String ROOT = "/BookManagementService";

    static class StdErrorExceptionLogger implements ExceptionLogger {

        @Override
        public void log(final Exception ex) {
            if (ex instanceof SocketTimeoutException) {
                System.err.println("Connection timed out");
            } else if (ex instanceof ConnectionClosedException) {
                System.err.println(ex.getMessage());
            } else {
                ex.printStackTrace();
            }
        }

    }

    public static void main(String[] args) throws Exception {

        int port = 8080; //Modify this line if using different port

        SSLContext sslContext = null;
        if (port == 8443) {
            // Initialize SSL context
            URL url = Server.class.getResource("/my.keystore");
            if (url == null) {
                System.out.println("Keystore not found");
                System.exit(1);
            }
            sslContext = SSLContexts.custom()
                    .loadKeyMaterial(url, "secret".toCharArray(), "secret".toCharArray())
                    .build();
        }

        //Reference:https://www.programcreek.com/java-api-examples/index.php?api=org.apache.http.protocol.UriHttpRequestHandlerMapper
        //Easier then using request.getURI() in the first version
        //check the URI directly
        UriHttpAsyncRequestHandlerMapper uriHttpRequestHandlerMapper = new UriHttpAsyncRequestHandlerMapper();
        LoginHandler loginHandler = new LoginHandler();
        LogoutHandler logoutHandler = new LogoutHandler();
        BookStatusHandler bookStatusHandler = new BookStatusHandler();
        BookHandler BookHandler = new BookHandler();
        TransactionHandler transactionHandler = new TransactionHandler();

        uriHttpRequestHandlerMapper.register(ROOT + "/login", loginHandler);
        uriHttpRequestHandlerMapper.register(ROOT + "/logout", logoutHandler);
        uriHttpRequestHandlerMapper.register(ROOT + "/books", BookHandler);
        //uriHttpRequestHandlerMapper.register(ROOT + "/books/*", bookStatusHandler);
        //uriHttpRequestHandlerMapper.register(ROOT + "/transaction", transactionHandler);

        final IOReactorConfig config = IOReactorConfig.custom()
                .setSoTimeout(15000)
                .setTcpNoDelay(true)
                .build();

        final HttpServer server = ServerBootstrap.bootstrap()
                .setListenerPort(port)
                .setServerInfo("Test/1.1")
                .setIOReactorConfig(config)
                .setSslContext(sslContext)
                .setExceptionLogger(ExceptionLogger.STD_ERR)
                .setHandlerMapper(uriHttpRequestHandlerMapper)
                .create();

        server.start();
        server.awaitTermination(Long.MAX_VALUE, TimeUnit.DAYS);

        Runtime.getRuntime().addShutdownHook(new Thread() {
            @Override
            public void run() {
                server.shutdown(5, TimeUnit.SECONDS);
            }
        });
    }
}
