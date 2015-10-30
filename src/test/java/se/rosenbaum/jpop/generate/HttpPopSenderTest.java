package se.rosenbaum.jpop.generate;

import org.junit.Before;
import org.junit.Test;

import java.io.Reader;
import java.io.StringReader;

import static org.junit.Assert.assertEquals;

public class HttpPopSenderTest {

    private HttpPopSender sut;

    @Before
    public void setup() {
        sut = new HttpPopSender(null);
    }

    @Test
    public void testReadReplyAndGetResultReplyTooLong() throws Exception {
        StringBuffer reply = getMaxReply();
        testReadReply(reply.toString() + "g", PopSender.Result.INVALID_POP, reply.substring("invalid\n".length()));
    }

    @Test
    public void testReadReplyAndGetResultReplyExactlyLimit() throws Exception {
        StringBuffer reply = getMaxReply();
        testReadReply(reply.toString(), PopSender.Result.INVALID_POP, reply.substring("invalid\n".length()));
    }

    @Test
    public void testReadReplyAndGetResultReplyEmptyMessage() throws Exception {
        testReadReply("invalid\n", PopSender.Result.INVALID_POP, null);
    }

    @Test
    public void testReadReplyAndGetResultReplyNullMessage() throws Exception {
        testReadReply("invalid", PopSender.Result.INVALID_POP, null);
    }

    @Test
    public void testReadReplyAndGetResultReplyNormalMessage() throws Exception {
        testReadReply("invalid\nWrong transaction!", PopSender.Result.INVALID_POP, "Wrong transaction!");
    }

    @Test
    public void testOk() {
        testReadReply("valid", PopSender.Result.OK, null);
    }

    @Test
    public void testBadReply() {
        testReadReply("valid\n", PopSender.Result.PROTOCOL_ERROR, null);
    }

    @Test
    public void testBadReply2() {
        testReadReply("vali", PopSender.Result.PROTOCOL_ERROR, null);
    }

    private StringBuffer getMaxReply() {
        StringBuffer reply = new StringBuffer(HttpPopSender.REPLY_SIZE_LIMIT);
        reply.append("invalid\nab");
        for (int i = reply.length(); i < HttpPopSender.REPLY_SIZE_LIMIT; i++) {
            reply.append("1");
        }
        return reply;
    }

    private void testReadReply(String reply, PopSender.Result expectedResult, String expectedMessage) {
        Reader reader = new StringReader(reply);
        sut.readReplyAndSetResult(reader);
        PopSender.Result result = sut.getResult();
        assertEquals(expectedResult, result);
        assertEquals(expectedMessage, sut.errorMessage());
    }
}