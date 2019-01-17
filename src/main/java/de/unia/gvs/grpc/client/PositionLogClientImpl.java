package de.unia.gvs.grpc.client;

import de.unia.gvs.grpc.Coordinate;
import de.unia.gvs.grpc.LengthReply;
import de.unia.gvs.grpc.LogPositionRequest;
import de.unia.gvs.grpc.PositionLogServiceGrpc;
import io.grpc.ManagedChannel;

import java.util.Collections;
import java.util.Iterator;
import java.util.List;

/**
 * Implementation of the gRPC service client for the Position Log service.
 * <p>
 * <strong>TODO</strong>: Implement all missing methods to make {@link de.unia.gvs.grpc.client.ClientTest} pass.
 */
public class PositionLogClientImpl implements PositionLogClient {
    private PositionLogServiceGrpc.PositionLogServiceBlockingStub stub;

    public PositionLogClientImpl(ManagedChannel channel) {
    }

    @Override
    public Iterator<Coordinate> getPoints(int userId) {
        return Collections.emptyIterator();
    }

    @Override
    public List<Integer> listUsers() {
        return Collections.emptyList();
    }

    @Override
    public void removeUser(int userId) {
    }

    @Override
    public void logPoints(LogPositionRequest request) {
    }

    @Override
    public LengthReply getTrackLength(int userId) {
        return LengthReply.getDefaultInstance();
    }
}
