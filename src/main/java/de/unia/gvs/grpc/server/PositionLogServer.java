package de.unia.gvs.grpc.server;

import io.grpc.Server;
import io.grpc.ServerBuilder;

import java.io.IOException;
import java.util.logging.Logger;

/**
 * gRPC server for the Position Log service.
 */
public class PositionLogServer {
    public static final int GRPC_PORT = 4711;

    private static final Logger log = Logger.getLogger(PositionLogServer.class.getSimpleName());

    private Server server;

    public void start() throws IOException {
        log.info("Starting server on port " + GRPC_PORT);
        server = ServerBuilder.forPort(GRPC_PORT)
                .addService(new PositionLogServiceImpl())
                .build();
        server.start();

        // Automatically shut down the server when the JVM stops
        Runtime.getRuntime().addShutdownHook(new Thread(this::stop));
    }

    private void stop() {
        if (server != null) {
            log.info("Shutting down server");
            server.shutdown();
            log.info("Done.");
        }
    }
}
