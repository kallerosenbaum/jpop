package se.rosenbaum.jpop;

import org.bitcoinj.core.Base58;
import org.junit.Test;

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.util.Arrays;
import java.util.Random;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
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
    public void testCreateOnlyPButR() {
        new PopRequestURI("btcpop:?p=a&r=b");
    }

    @Test
    public void testCreateOnlyNButR() {
        new PopRequestURI("btcpop:?n=a&r=b");
    }

    @Test
    public void testCreateOnlyR() {
        new PopRequestURI("btcpop:?r=b");
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
        String encoded = URLEncoder.encode("http://a.example.com/Å/ä/ö", "UTF-8");
        encoded += "/http://";
        PopRequestURI uri = new PopRequestURI("btcpop:?label=a text&n=111&p=" + encoded);

        assertEquals("a text", uri.getLabel());
        assertEquals("http://a.example.com/Å/ä/ö/http://", uri.getP());
    }

}