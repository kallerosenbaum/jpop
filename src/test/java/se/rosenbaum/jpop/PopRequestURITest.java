package se.rosenbaum.jpop;

import org.bitcoinj.core.Base58;
import org.bitcoinj.core.Coin;
import org.bitcoinj.core.Sha256Hash;
import org.junit.Test;

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.util.Arrays;
import java.util.Random;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

public class PopRequestURITest {

    @Test
    public void testCreateNoParams() {
        testIllegalURI("btcpop:?");
    }

    private void testIllegalURI(String input) {
        try {
            new PopRequestURI(input);
            fail();
        } catch (IllegalArgumentException e) {
            // Expected
        }
    }

    @Test
    public void testTest() {
        byte[] bytes = new byte[6];
        System.out.println(Base58.encode(bytes));
        bytes[5]++;
        System.out.println(Base58.encode(bytes));
        bytes[4]++;
        System.out.println(Base58.encode(bytes));
        bytes[3]++;
        System.out.println(Base58.encode(bytes));
        bytes[0]++;
        System.out.println(Base58.encode(bytes));

        Arrays.fill(bytes, Byte.MAX_VALUE);
        System.out.println(Base58.encode(bytes));
        Arrays.fill(bytes, Byte.MIN_VALUE);
        System.out.println(Base58.encode(bytes));

        Random random = new Random(1L);

        printRandomBytes(bytes, random);

        printRandomBytes(bytes, random);

        printRandomBytes(bytes, random);
    }

    private void printRandomBytes(byte[] bytes, Random random) {
        random.nextBytes(bytes);
        for (int i = 0; i < 6; i++) {
            System.out.print(String.format("%#x ", bytes[i]));
        }
        System.out.println();
        System.out.println(Base58.encode(bytes));
    }

    @Test
    public void testCreateOnlyNonce() {
        testIllegalURI("btcpop:?n=B");
    }

    @Test
    public void testCreateOnlyP() {
        testIllegalURI("btcpop:?p=a");
    }

    @Test
    public void testCreateEmptyNonce() {
        testIllegalURI("btcpop:?p=a&n=");
    }

    @Test
    public void testCreateMalformedNonce() {
        testIllegalURI("btcpop:?p=a&n=.");
    }

    @Test
    public void testCreateEmptyP() {
        testIllegalURI("btcpop:?p=&n=1");
    }

    @Test
    public void testCreateMalformedTxidBadChar() {
        testIllegalURI("btcpop:?p=a&n=1&txid=Axc9,");
    }

    @Test
    public void testCreateMalformedTxidTooShort() {
        testIllegalURI("btcpop:?p=a&n=1&txid=CLs");
    }

    @Test
    public void testCreateMalformedTxidTooLong() {
        testIllegalURI("btcpop:?p=a&n=1&txid=ggggggggggggggggggggggggggggggggggggggggggggggggggggggggggg");
    }

    @Test
    public void testCreateMalformedAmount() {
        testIllegalURI("btcpop:?p=a&n=1&amount=a");
    }

    @Test
    public void testCreateZeroAmount() {
        PopRequestURI uri = new PopRequestURI("btcpop:?p=a&n=1&amount=0");
        assertEquals(0L, uri.getAmountSatoshis().longValue());
    }

    @Test
    public void testCreateNegativeAmount() {
        testIllegalURI("btcpop:?p=a&n=1&amount=-1");
    }

    @Test
    public void testCreateBadCommaInAmount() {
        testIllegalURI("btcpop:?p=a&n=1&amount=1,1");
    }

    @Test
    public void testCreateTooSmallFraction() {
        testIllegalURI("btcpop:?p=a&n=1&amount=1,000000009");
    }

    @Test
    public void testCreateTooMuchBitcoin() {
        testIllegalURI("btcpop:?p=a&n=1&amount=21000000.00000001");
    }

    @Test
    public void testCreateMaxBitcoin() {
        PopRequestURI uri = new PopRequestURI("btcpop:?p=a&n=1&amount=21000000.00000000");
        assertEquals(2100000000000000L, uri.getAmountSatoshis().longValue());
    }

    @Test
    public void testCreateMinimal() {
        PopRequestURI uri = new PopRequestURI("btcpop:?n=111&p=a");
        assertArrayEquals(new byte[3], uri.getN());
        assertNull(uri.getAmountSatoshis());
        assertNull(uri.getLabel());
        assertNull(uri.getTxid());
    }


    @Test
    public void testCreateFull() {
        String txid="Emt9MPvt1joznqHy5eEHkNtcuQuYWXzYJBQZN6BJm6NL";
        PopRequestURI uri = new PopRequestURI("btcpop:?n=111&p=a&label=atext&amount=10&txid=" + txid);
        assertArrayEquals(new byte[3], uri.getN());
        assertEquals(1000000000, uri.getAmountSatoshis().longValue());
        assertEquals("atext", uri.getLabel());
        assertEquals("cca7507897abc89628f450e8b1e0c6fca4ec3f7b34cccf55f3f531c659ff4d79", uri.getTxid().toString());
    }

    @Test
    public void testCreateUrlDecode() throws UnsupportedEncodingException {
        // We must be able to handle utf-8 encoded strings as well as non-encoded characters, like /, ? and :
        // Note that literal ? is ok, as well as :. No need to %-encode them
        String encoded = URLEncoder.encode("http://a.example.com/Å/ä/ö", "UTF-8");
        encoded += "/http://"; // http%3A%2F%2Fa.example.com%2F%C3%85%2F%C3%A4%2F%C3%B6/http://
        PopRequestURI uri = new PopRequestURI("btcpop:?label=a text&n=111&p=" + encoded);

        assertEquals("a text", uri.getLabel());
        assertEquals("http://a.example.com/Å/ä/ö/http://", uri.getP());
    }

    @Test
    public void testConstructorMinimal() {
        PopRequest popRequest = createPopRequest("dest", b(1, 2, 3), null, null, null, null);
        PopRequestURI sut = new PopRequestURI(popRequest);
        assertEquals("dest", sut.getP());
        assertArrayEquals(b(1, 2, 3), sut.getN());
        assertNull(sut.getMessage());
        assertNull(sut.getAmountSatoshis());
        assertNull(sut.getLabel());
        assertNull(sut.getTxid());
    }

    @Test
    public void testConstructorFull() {
        PopRequest popRequest = createPopRequest("dest", b(1, 2, 3), Sha256Hash.of(b(1)), "a label å", "a message €", 3L);
        PopRequestURI sut = new PopRequestURI(popRequest);
        assertEquals("dest", sut.getP());
        assertArrayEquals(b(1, 2, 3), sut.getN());
        assertEquals(Sha256Hash.of(b(1)), sut.getTxid());
        assertEquals("a label å", sut.getLabel());
        assertEquals("a message €", sut.getMessage());
        assertEquals(3L, sut.getAmountSatoshis().longValue());
    }

    @Test(expected = IllegalArgumentException.class)
    public void testConstructorMinimalMissingDestination() {
        PopRequest popRequest = createPopRequest(null, b(1, 2, 3), null, null, null, null);
        new PopRequestURI(popRequest);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testConstructorMinimalMissingNonce() {
        PopRequest popRequest = createPopRequest("dest", null, null, null, null, null);
        new PopRequestURI(popRequest);
    }

    @Test
    public void testToURIStringMinimal() {
        PopRequest popRequest = createPopRequest("dest", b(1, 2, 3), null, null, null, null);
        PopRequestURI sut = new PopRequestURI(popRequest);
        String uriString = sut.toURIString();

        PopRequestURI parseResult = new PopRequestURI(uriString);
        assertURIsEqual(sut, parseResult);
    }

    @Test
    public void testToURIStringFull() {
        PopRequest popRequest = createPopRequest("dest", b(1, 2, 3), Sha256Hash.of(b(1)), "a label å", "a message €", 3L);
        PopRequestURI sut = new PopRequestURI(popRequest);
        String uriString = sut.toURIString();

        PopRequestURI parseResult = new PopRequestURI(uriString);
        assertURIsEqual(sut, parseResult);
    }

    @Test
    public void testToURIStringOnlyLabel() {
        PopRequest popRequest = createPopRequest("dest", b(1, 2, 3), null, "a label å", null, null);
        PopRequestURI sut = new PopRequestURI(popRequest);
        String uriString = sut.toURIString();

        PopRequestURI parseResult = new PopRequestURI(uriString);
        assertURIsEqual(sut, parseResult);
    }

    private void assertURIsEqual(PopRequestURI expected, PopRequestURI actual) {
        assertTrue((expected == null && actual == null) || (expected != null && actual != null));
        assertEquals(expected.getP(), actual.getP());
        assertArrayEquals(expected.getN(), actual.getN());
        assertEquals(expected.getMessage(), actual.getMessage());
        assertEquals(expected.getTxid(), actual.getTxid());
        assertEquals(expected.getLabel(), actual.getLabel());
        assertEquals(expected.getAmountSatoshis(), actual.getAmountSatoshis());
    }

    private PopRequest createPopRequest(String destination, byte[] nonce, Sha256Hash txid, String label, String message, Long amountSatoshis) {
        PopRequest popRequest = new PopRequest();
        popRequest.setDestination(destination);
        popRequest.setNonce(nonce);
        popRequest.setTxid(txid);
        popRequest.setLabel(label);
        popRequest.setMessage(message);
        popRequest.setAmount(amountSatoshis == null ? null : Coin.valueOf(amountSatoshis));
        return popRequest;
    }

    protected static byte[] b(int... byteValues) {
        byte[] bytes = new byte[byteValues.length];
        for (int i = 0; i < bytes.length; i++) {
            bytes[i] = (byte)byteValues[i];
        }
        return bytes;
    }

}