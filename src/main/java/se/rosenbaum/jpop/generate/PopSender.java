package se.rosenbaum.jpop.generate;

import se.rosenbaum.jpop.Pop;

/**
 * Generic interface for sending a PoP.
 */
public interface PopSender {
    enum Result {
        OK, INVALID_POP, COMMUNICATION_ERROR, PROTOCOL_ERROR, LOCAL_ERROR;
    }

    /**
     * Send the pop
     * @param signedPop the signed pop to send.
     */
    void sendPop(Pop signedPop);

    /**
     * @return Result null if sending was successful and if validation on the receiving end passed. Appropriate Result otherwise.
     * @throws UnsupportedOperationException if sendPop has not yet been called
     */
    Result getResult();

    /**
     * If sending failed for some reason, the server may have provided an error message.
     * @return null if no message was given
     * @throws UnsupportedOperationException if sendPop has not yet been called
     */
    String errorMessage();
}
