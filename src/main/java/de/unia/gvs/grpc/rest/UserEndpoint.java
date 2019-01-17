package de.unia.gvs.grpc.rest;

import com.diffplug.common.base.Errors;
import com.google.common.collect.Streams;
import com.google.protobuf.InvalidProtocolBufferException;
import com.google.protobuf.util.JsonFormat;
import de.unia.gvs.grpc.Coordinate;
import de.unia.gvs.grpc.LengthReply;
import de.unia.gvs.grpc.LogPositionRequest;
import de.unia.gvs.grpc.client.PositionLogClient;
import org.jboss.logging.Logger;

import javax.ws.rs.*;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.util.Iterator;

import static java.util.stream.Collectors.joining;

/**
 * Contains implementations for the REST service endpoints provided via Undertow
 */
@Path("/users")
@Produces(MediaType.APPLICATION_JSON)
public class UserEndpoint {
    private static final Logger log = Logger.getLogger(UserEndpoint.class);

    private final PositionLogClient client;

    public UserEndpoint(PositionLogClient client) {
        this.client = client;
    }

    @GET
    public Response handleUserList() {
        final String json = client.listUsers()
                .stream()
                .map(Object::toString)
                .collect(joining(",\n"));

        return Response.status(Response.Status.OK)
                .entity("[" + json + "]")
                .build();
    }

    @GET
    @Path("/{userId}/points")
    public String handlePointsList(@PathParam("userId") int userId) {
        final Iterator<Coordinate> data = client.getPoints(userId);
        final String json = Streams.stream(data)
                .map(Errors.rethrow().wrap(JsonFormat.printer()::print))
                .collect(joining(",\n"));

        return "[" + json + "]";
    }

    @DELETE
    @Path("/{userId}")
    public void handleDeleteUser(@PathParam("userId") int userId) {
        client.removeUser(userId);
        Response.status(Response.Status.NO_CONTENT);
    }

    @GET
    @Path("/{userId}/trackLength")
    public String handleGetTrackLength(@PathParam("userId") int userId) throws InvalidProtocolBufferException {
        final LengthReply data = client.getTrackLength(userId);
        final String json = JsonFormat.printer().print(data);
        return json;
    }

    @POST
    @Path("/{userId}/points")
    public void handleLogPositions(String body, @PathParam("userId") int userId) {
        final LogPositionRequest.Builder builder = LogPositionRequest.newBuilder().setUserId(userId);
        try {
            JsonFormat.parser().merge(body, builder);
            client.logPoints(builder.build());
            Response.status(Response.Status.CREATED);
        } catch (InvalidProtocolBufferException ex) {
            log.error("Could not deserialize request", ex);
            Response.status(Response.Status.BAD_REQUEST);
        }
    }
}
