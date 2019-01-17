package de.unia.gvs.grpc.server;

import de.unia.gvs.grpc.*;
import de.unia.gvs.grpc.PositionLogServiceGrpc.PositionLogServiceBlockingStub;
import io.grpc.ManagedChannel;
import io.grpc.Server;
import io.grpc.Status;
import io.grpc.StatusRuntimeException;
import io.grpc.inprocess.InProcessChannelBuilder;
import io.grpc.inprocess.InProcessServerBuilder;
import io.grpc.testing.GrpcCleanupRule;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

import java.io.IOException;
import java.util.Iterator;
import java.util.List;
import java.util.Random;

import static org.hamcrest.CoreMatchers.hasItem;
import static org.junit.Assert.*;

/**
 * Unit/integration tests for the gRPC service implementation.
 *
 * <strong>TODO</strong>: Your code should pass all tests. Additional test cases are needed to achieve full code coverage.
 */
@RunWith(JUnit4.class)
public class ServerTest {
    /**
     * This rule manages automatic graceful shutdown for the registered servers and channels at the
     * end of test.
     */
    @Rule
    public final GrpcCleanupRule grpcCleanup = new GrpcCleanupRule();

    private ManagedChannel channel;
    private PositionLogServiceBlockingStub stub;

    @Before
    public void setUp() throws IOException {
        final String name = InProcessServerBuilder.generateName();

        // Create a server, add service, start, and register for automatic graceful shutdown.
        final PositionLogServiceImpl service = new PositionLogServiceImpl();
        final Server server = grpcCleanup.register(InProcessServerBuilder.forName(name)
                .directExecutor()
                .addService(service)
                .build()
                .start());

        // Create a client channel and register for automatic graceful shutdown.
        channel = grpcCleanup.register(InProcessChannelBuilder.forName(name).directExecutor().build());
        stub = createStub();
    }

    @Test
    public void serviceImpl_listUsers_empty() {
        final ListUsersReply reply = stub.listUsers(ListUsersRequest.getDefaultInstance());
        assertEquals("Newly created service should not return any users", 0, reply.getUsersIdsCount());
    }

    private PositionLogServiceBlockingStub createStub() {
        return PositionLogServiceGrpc.newBlockingStub(channel);
    }

    @Test
    public void serviceImpl_deleteUser_nonexisting() {
        try {
            stub.deleteUser(DeleteUserRequest.newBuilder().setUserId(12345).build());
            fail("Deleting a non-existing user should yield NOT_FOUND status");
        } catch (StatusRuntimeException expected) {
            assertEquals(Status.NOT_FOUND, expected.getStatus());
        }
    }

    @Test
    public void serviceImpl_deleteUser_existing() {
        final int userId = 1;

        stub.logPosition(LogPositionRequest.newBuilder().setUserId(userId).addPoints(Coordinate.getDefaultInstance()).build());

        stub.deleteUser(DeleteUserRequest.newBuilder().setUserId(userId).build());
        final int count = stub.listUsers(ListUsersRequest.getDefaultInstance()).getUsersIdsCount();
        assertEquals("Deleting a user should remove it from the user list", 0, count);
    }

    @Test
    public void serviceImpl_logPosition() {
        final Random random = new Random();
        final Coordinate point = Coordinate.newBuilder()
                .setLatitude(random.nextDouble() * 90)
                .setLongitude(random.nextDouble() * 180)
                .build();
        final LogPositionRequest request = LogPositionRequest.newBuilder()
                .setUserId(1234)
                .addPoints(point)
                .build();

        final PositionLogServiceBlockingStub stub = createStub();
        stub.logPosition(request);

        final List<Integer> idList = stub.listUsers(ListUsersRequest.getDefaultInstance()).getUsersIdsList();
        assertThat("User must exist after adding a track", idList, hasItem(request.getUserId()));

        final Iterator<Coordinate> points = stub.getPoints(PointsRequest.newBuilder().setUserId(request.getUserId()).build());
        assertThat("Track for user must contain logged point", () -> points, hasItem(point));
    }

    @Test
    public void serviceImpl_getLength_nonExisting() {
        final LengthRequest request = LengthRequest.newBuilder()
                .setUserId(12345)
                .build();
        try {
            final LengthReply reply = stub.getTrackLength(request);
            fail("Calculating track length for non-existing user should yield NOT_FOUND status");
        } catch (StatusRuntimeException expected) {
            assertEquals(Status.NOT_FOUND, expected.getStatus());
        }
    }

    @Test
    public void serviceImpl_getLength_nonEmpty() {
        final int userId = 1234;

        // Create track
        {
            final Coordinate point1 = Coordinate.newBuilder()
                    .setLatitude(0).setLongitude(0).build();
            final Coordinate point2 = Coordinate.newBuilder()
                    .setLatitude(1).setLongitude(1).build();
            final Coordinate point3 = Coordinate.newBuilder()
                    .setLatitude(0).setLongitude(1).build();

            final LogPositionRequest request = LogPositionRequest.newBuilder()
                    .setUserId(userId)
                    .addPoints(point1)
                    .addPoints(point2)
                    .addPoints(point3)
                    .build();

            stub.logPosition(request);
        }

        final LengthRequest request = LengthRequest.newBuilder()
                .setUserId(userId)
                .build();
        final LengthReply reply = stub.getTrackLength(request);
        assertEquals(3, reply.getNumPoints());
        assertEquals(156.9e3 + 110.57e3, reply.getLength(), 1e1); // expected accuracy +/- 10m
    }

    @Test
    public void serviceImpl_getPoints_nonExisting() {
        final PointsRequest request = PointsRequest.newBuilder()
                .setUserId(12345)
                .build();
        try {
            final Iterator<Coordinate> reply = stub.getPoints(request);
            assertTrue("Fetching points for non-existing user should not return an empty iterator", reply.hasNext());
            reply.next(); // should throw exception
            fail("Fetching points for non-existing user should yield NOT_FOUND status");
        } catch (StatusRuntimeException expected) {
            assertEquals(Status.NOT_FOUND, expected.getStatus());
        }
    }
}
