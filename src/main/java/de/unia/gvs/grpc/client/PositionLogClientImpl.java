package de.unia.gvs.grpc.client;

import de.unia.gvs.grpc.Coordinate;
import de.unia.gvs.grpc.DeleteUserRequest;
import de.unia.gvs.grpc.LengthReply;
import de.unia.gvs.grpc.ListUsersRequest;
import de.unia.gvs.grpc.LogPositionRequest;
import de.unia.gvs.grpc.LogPositionRequestOrBuilder;
import de.unia.gvs.grpc.PointsRequest;
import de.unia.gvs.grpc.PointsRequestOrBuilder;
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
        PointsRequest.Builder builder = PointsRequest.newBuilder().setUserId(userId);
        PointsRequest points = builder.build();
        return stub.getPoints(points);
    }

    @Override
    public List<Integer> listUsers() {
        ListUsersRequest.Builder builder = ListUsersRequest.newBuilder();
        ListUsersRequest listUsersRequest = builder.build();
        return stub.listUsers(listUsersRequest).getUsersIdsList();
    }

    @Override
    public void removeUser(int userId) {
        DeleteUserRequest.Builder builder = DeleteUserRequest.newBuilder().setUserId(userId);
        DeleteUserRequest deleteUserRequest = builder.build();
        stub.deleteUser(deleteUserRequest);
    }

    @Override
    public void logPoints(LogPositionRequest request) {
        if(!listUsers().contains(new Integer(request.getUserId())));
            
        stub.logPosition(request);
    }

    @Override
    public LengthReply getTrackLength(int userId) {
        return LengthReply.getDefaultInstance();
    }
}
