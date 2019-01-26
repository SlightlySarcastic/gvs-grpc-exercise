package de.unia.gvs.grpc.server;

import java.util.Collection;
import java.util.Iterator;
import java.util.List;

import com.google.api.Advice.Builder;
import com.google.common.collect.Multimap;
import com.google.common.collect.MultimapBuilder;
import com.google.protobuf.Empty;
import de.unia.gvs.grpc.*;
import io.grpc.Status;
import io.grpc.StatusRuntimeException;
import io.grpc.stub.StreamObserver;
import net.sf.geographiclib.Geodesic;
import net.sf.geographiclib.GeodesicData;

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
        ListUsersReply.Builder builder = ListUsersReply.newBuilder().addAllUsersIds(points.keySet());
        ListUsersReply listUsersReply = builder.build();
        responseObserver.onNext(listUsersReply);
        responseObserver.onCompleted();
    }

    @Override
    public void deleteUser(DeleteUserRequest request, StreamObserver<Empty> responseObserver) {
        if(points.get(request.getUserId()).size()==0)
        {
           responseObserver.onError(new StatusRuntimeException(Status.NOT_FOUND));
           return;
        }
        points.removeAll(request.getUserId());
        responseObserver.onNext(Empty.getDefaultInstance());
        responseObserver.onCompleted();
    }

    @Override
    public void logPosition(LogPositionRequest request, StreamObserver<Empty> responseObserver) {
        points.putAll(request.getUserId(), request.getPointsList());
        responseObserver.onNext(Empty.getDefaultInstance());
        responseObserver.onCompleted();
    }

    @Override
    public void getPoints(PointsRequest request, StreamObserver<Coordinate> responseObserver) {
        if(points.get(request.getUserId()).size()==0)
        {
           responseObserver.onError(new StatusRuntimeException(Status.NOT_FOUND));
           return;
        }
        for(Coordinate coor : points.get(request.getUserId())) {
            responseObserver.onNext(coor);
        }
        responseObserver.onCompleted();

    }

    @Override
    public void getTrackLength(LengthRequest request, StreamObserver<LengthReply> responseObserver) {
        List<Coordinate> points = (List<Coordinate>) this.points.get(request.getUserId());
        if(points.size()==0)
        {
            responseObserver.onError(new StatusRuntimeException(Status.NOT_FOUND));
            return;
        }

        double distance = 0;
        //Die for Schleife ist theoretisch aus dem Skript, ich musste sie aber abändern, damit sie geht.
        for (int i = 0; i < points.size() - 1; ++i) {
            final Coordinate start = points.get(i);
            final Coordinate end = points.get(i + 1);
            final GeodesicData data =
                Geodesic.WGS84.Inverse(start.getLatitude(),
                start.getLongitude(), end.getLatitude(),
                end.getLongitude());
            // s12 field is distance in meters
            distance += data.s12;
        }
        LengthReply.Builder builder = LengthReply.newBuilder().setLength(distance).setNumPoints(points.size());
        LengthReply lengthReply = builder.build();
        responseObserver.onNext(lengthReply);
        responseObserver.onCompleted();
    }
}
