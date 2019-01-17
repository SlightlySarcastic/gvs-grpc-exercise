package de.unia.gvs.grpc.client;

import de.unia.gvs.grpc.Coordinate;
import de.unia.gvs.grpc.LengthReply;
import de.unia.gvs.grpc.LogPositionRequest;

import java.util.Iterator;
import java.util.List;

/**
 * Interface for a client to the gRPC Position Log service.
 */
public interface PositionLogClient {
    /**
     * Retrieve all logged points for a single user.
     *
     * @param userId
     * @return
     */
    Iterator<Coordinate> getPoints(int userId);

    /**
     * Retrieve all users.
     *
     * @return
     */
    List<Integer> listUsers();

    /**
     * Remove a single user.
     *
     * @param userId
     */
    void removeUser(int userId);

    /**
     * Add additional points for a single user, creating the user if necessary.
     *
     * @param request
     */
    void logPoints(LogPositionRequest request);

    /**
     * Calculate the track length for a single user.
     *
     * @param userId
     * @return
     */
    LengthReply getTrackLength(int userId);
}
