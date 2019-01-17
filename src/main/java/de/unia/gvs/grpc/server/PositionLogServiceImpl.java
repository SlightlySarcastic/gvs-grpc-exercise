package de.unia.gvs.grpc.server;

import com.google.common.collect.Multimap;
import com.google.common.collect.MultimapBuilder;
import com.google.protobuf.Empty;
import de.unia.gvs.grpc.*;
import io.grpc.stub.StreamObserver;

/**
 * Implementation of the gPRC Position Log service.
 *
 * <strong>TODO</strong>: Implement all missing functionality to make {@link de.unia.gvs.grpc.server.ServerTest} pass.
 */
class PositionLogServiceImpl extends PositionLogServiceGrpc.PositionLogServiceImplBase {
    // Stores points for each user, identified by their ID
    private final Multimap<Integer, Coordinate> points = MultimapBuilder.treeKeys().arrayListValues().build();

    @Override
    public void listUsers(ListUsersRequest request, StreamObserver<ListUsersReply> responseObserver) {
    }

    @Override
    public void deleteUser(DeleteUserRequest request, StreamObserver<Empty> responseObserver) {
    }

    @Override
    public void logPosition(LogPositionRequest request, StreamObserver<Empty> responseObserver) {
    }

    @Override
    public void getPoints(PointsRequest request, StreamObserver<Coordinate> responseObserver) {
    }

    @Override
    public void getTrackLength(LengthRequest request, StreamObserver<LengthReply> responseObserver) {
    }
}
