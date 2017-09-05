package com.rationaleemotions.server;

import org.openqa.grid.internal.TestSession;

/**
 * Represents the capabilities that a typical selenium server should possess.
 */
public interface ISeleniumServer {
    
    /**
     * @return - <code>true</code> if the server is running.
     */
    boolean isServerRunning();

    /**
     * Helps start a selenium server.
     *
     * @param session - the capabilities the client requested.
     * @return - The port on which the server was spun off.
     * @throws ServerException - In case of problems.
     */
    int startServer(TestSession session) throws ServerException;

    /**
     * @return - The port on which the server was spun off.
     */
    int getPort();

    /**
     *
     * @return - The host on which the server is running.
     */
    String getHost();

    /**
     * Shutsdown the server.
     *
     * @throws ServerException - In case of problems.
     */
    void shutdownServer() throws ServerException;

    /**
     * Represents all exceptions that can arise out of attempts to manipulate server.
     */
    class ServerException extends Exception {
        public ServerException(final String message) {
            super(message);
        }
        public ServerException(final String message, final Throwable e) {
            super(message, e);
        }
    }
}
