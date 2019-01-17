package de.unia.gvs.grpc.client;

import com.google.protobuf.Empty;
import de.unia.gvs.grpc.*;
import de.unia.gvs.grpc.PositionLogServiceGrpc.PositionLogServiceImplBase;
import io.grpc.ManagedChannel;
import io.grpc.inprocess.InProcessChannelBuilder;
import io.grpc.inprocess.InProcessServerBuilder;
import io.grpc.stub.StreamObserver;
import io.grpc.testing.GrpcCleanupRule;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;
import org.mockito.ArgumentCaptor;
import org.mockito.ArgumentMatchers;

import java.io.IOException;

import static org.junit.Assert.assertEquals;
import static org.mockito.AdditionalAnswers.delegatesTo;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

/**
 * Integration test for the gRPC service client.
 * <p>
 * Validates the correct invocation of gRPC service endpoints by the client for all operations.
 * <p>
 * <strong>TODO</strong>: Your code should pass all these tests. Consider if additional test cases or assertions are needed.
 */
@RunWith(JUnit4.class)
public class ClientTest {
    /**
     * This rule manages automatic graceful shutdown for the registered servers and channels at the
     * end of test.
     */
    @Rule
    public final GrpcCleanupRule grpcCleanup = new GrpcCleanupRule();

    private final PositionLogServiceImplBase serviceImpl = mock(PositionLogServiceImplBase.class, delegatesTo(new MockPositionLogServiceImpl()));

    private PositionLogClient client;

    @Before
    public void setUp() throws IOException {
        // Generate a unique in-process serviceImpl name.
        final String serverName = InProcessServerBuilder.generateName();

        // Create a server, add service, start, and register for automatic graceful shutdown.
        grpcCleanup.register(InProcessServerBuilder
                .forName(serverName).directExecutor().addService(serviceImpl).build().start());

        // Create a client channel and register for automatic graceful shutdown.
        final ManagedChannel channel = grpcCleanup.register(
                InProcessChannelBuilder.forName(serverName).directExecutor().build());

        client = new PositionLogClientImpl(channel);
    }

    @Test
    public void clientImpl_listUsers() {
        final ArgumentCaptor<ListUsersRequest> captor = ArgumentCaptor.forClass(ListUsersRequest.class);

        client.listUsers();
        verify(serviceImpl).listUsers(captor.capture(), ArgumentMatchers.<StreamObserver<ListUsersReply>>any());
    }

    @Test
    public void clientImpl_deleteUser() {
        final int userId = 1234;
        final ArgumentCaptor<DeleteUserRequest> captor = ArgumentCaptor.forClass(DeleteUserRequest.class);

        client.removeUser(userId);
        verify(serviceImpl).deleteUser(captor.capture(), ArgumentMatchers.<StreamObserver<Empty>>any());
        assertEquals("Must invoke delete operation for same user ID", userId, captor.getValue().getUserId());
    }

    @Test
    public void clientImpl_getUserPoints() {
        final int userId = 1234;
        final ArgumentCaptor<PointsRequest> captor = ArgumentCaptor.forClass(PointsRequest.class);

        client.getPoints(userId);
        verify(serviceImpl).getPoints(captor.capture(), ArgumentMatchers.<StreamObserver<Coordinate>>any());
        assertEquals("Must fetch points for same user ID", userId, captor.getValue().getUserId());
    }

    @Test
    public void clientImpl_getTrackLength() {
        final int userId = 1234;
        final ArgumentCaptor<LengthRequest> captor = ArgumentCaptor.forClass(LengthRequest.class);

        client.getTrackLength(userId);
        verify(serviceImpl).getTrackLength(captor.capture(), ArgumentMatchers.<StreamObserver<LengthReply>>any());
        assertEquals("Must fetch track length for same user ID", userId, captor.getValue().getUserId());
    }

    @Test
    public void clientImpl_logPoints() {
        final int userId = 1234;
        final LogPositionRequest request = LogPositionRequest.newBuilder().setUserId(userId).build();
        client.logPoints(request);
        verify(serviceImpl).logPosition(eq(request), ArgumentMatchers.<StreamObserver<Empty>>any());
    }

    /**
     * No-op implementation of the service interface used as the mocked request endpoint
     */
    private static class MockPositionLogServiceImpl extends PositionLogServiceImplBase {
        @Override
        public void listUsers(ListUsersRequest request, StreamObserver<ListUsersReply> responseObserver) {
            responseObserver.onNext(ListUsersReply.getDefaultInstance());
            responseObserver.onCompleted();
        }

        @Override
        public void deleteUser(DeleteUserRequest request, StreamObserver<Empty> responseObserver) {
            responseObserver.onNext(Empty.getDefaultInstance());
            responseObserver.onCompleted();
        }

        @Override
        public void logPosition(LogPositionRequest request, StreamObserver<Empty> responseObserver) {
            responseObserver.onNext(Empty.getDefaultInstance());
            responseObserver.onCompleted();
        }

        @Override
        public void getPoints(PointsRequest request, StreamObserver<Coordinate> responseObserver) {
            responseObserver.onCompleted();
        }

        @Override
        public void getTrackLength(LengthRequest request, StreamObserver<LengthReply> responseObserver) {
            responseObserver.onNext(LengthReply.getDefaultInstance());
            responseObserver.onCompleted();
        }
    }
}
