package de.unia.gvs.grpc;

import de.unia.gvs.grpc.client.PositionLogClient;
import de.unia.gvs.grpc.client.PositionLogClientImpl;
import de.unia.gvs.grpc.rest.UserEndpoint;
import de.unia.gvs.grpc.server.PositionLogServer;
import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;
import io.undertow.Undertow;
import io.undertow.server.handlers.resource.ClassPathResourceManager;
import io.undertow.server.handlers.resource.ResourceHandler;
import io.undertow.server.handlers.resource.ResourceManager;
import org.jboss.logging.Logger;
import org.jboss.resteasy.plugins.server.undertow.UndertowJaxrsServer;

import javax.ws.rs.core.Application;
import java.io.IOException;
import java.util.Collections;
import java.util.Random;
import java.util.Set;

/**
 * Main entry point for the application.
 * <p>
 * Manages the following tasks:
 * <ul>
 * <li>Instantiate the gRPC server on port 4711</li>
 * <li>Instantiate a gRPC client for this server</li>
 * <li>Seed the gRPC service with some test data</li>
 * <li>Provide a REST web service on port 8080, which accesses the gRPC service through the above client</li>
 * </ul>
 */
public class App {
    private static final Logger log = Logger.getLogger(App.class);
    private static final int HTTP_PORT = 8080;

    private PositionLogClient client;

    public static void main(String[] args) throws IOException {
        new App().run();
    }

    private void run() throws IOException {
        log.info("Starting gRPC server");
        final PositionLogServer positionLogServer = new PositionLogServer();
        positionLogServer.start();

        log.info("Starting gRPC client");
        final ManagedChannel channel = ManagedChannelBuilder.forAddress("localhost", PositionLogServer.GRPC_PORT)
                .usePlaintext()
                .build();
        client = new PositionLogClientImpl(channel);

        // Add some sample data
        log.info("Adding sample data");
        createSampleData(client);

        log.info("Starting Undertow HTTP server");
        initUndertow();
        log.info("HTTP server running on http://localhost:" + HTTP_PORT);
    }

    /**
     * Create sample data and add it via the supplied gRPC client
     *
     * @param client
     */
    private void createSampleData(PositionLogClient client) {
        for (int userId = 1; userId < 5; ++userId) {
            final double centroid_lat = 48.333889;
            final double centroid_lon = 10.898333;
            final Random random = new Random();

            final LogPositionRequest.Builder builder = LogPositionRequest.newBuilder().setUserId(userId);
            for (int i = 0; i < 15; ++i) {
                final Coordinate point = Coordinate.newBuilder()
                        .setLatitude(centroid_lat + random.nextGaussian() * 0.01)
                        .setLongitude(centroid_lon + random.nextGaussian() * 0.01)
                        .build();
                builder.addPoints(point);
            }
            client.logPoints(builder.build());
        }
    }

    /**
     * Launch an Undertow HTTP service which provides the REST web service and serves static pages (under <code>/web</code>)
     */
    private void initUndertow() {
        // Configure the JAX-RS REST web service and add the endpoint implementation
        final UndertowJaxrsServer rest = new UndertowJaxrsServer();
        rest.deploy(new Application() {
            @Override
            public Set<Object> getSingletons() {
                return Collections.singleton(new UserEndpoint(client));
            }
        });

        // Launch an Undertow instance for, listens on :8080
        final Undertow.Builder undertow = Undertow.builder()
                .addHttpListener(HTTP_PORT, "0.0.0.0");

        // Static resources served under /web
        final ResourceManager resourceManager = new ClassPathResourceManager(App.class.getClassLoader(), "web");
        final ResourceHandler resourceHandler = new ResourceHandler(resourceManager).addWelcomeFiles("map.html");
        rest.addResourcePrefixPath("/web", resourceHandler);

        rest.start(undertow);
    }
}
