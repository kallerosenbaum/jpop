package se.rosenbaum.jpop;

import org.junit.Test;

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;

public class PopURIEncodeDecodeTest {

    @Test
    public void testPopURIEncode() throws Exception {
        testEnc(PopRequestURITest.ALLOWED_CHARS, PopRequestURITest.ALLOWED_CHARS);
        testEnc("", "");
        testEnc("%20", " ");
        testEnc("a", "a");
        testEnc("/", "/");
        testEnc("%3D", "=");
        testEnc("%26", "&");
        testEnc("+", "+");
        testEnc("%25", "%");
        testEnc("%E1%82%A0", "Ⴀ");
        testEnc("%C3%85", "Å");
        testEnc("%5C", "\\");
        testEnc("%23", "#");
        testEnc("%F0%90%8E%81", "\uD800\uDF81");
        testEnc("ab%C3%85%C3%B6%3D%3D%E1%82%A0%25%25%5C", "abÅö==Ⴀ%%\\");
        testEnc(null, null);
    }

    @Test
    public void testPopURIDecode() throws Exception {
        testDec(PopRequestURITest.ALLOWED_CHARS, PopRequestURITest.ALLOWED_CHARS);
        testDec("", "");
        testDec("%20", " ");
        testDec("a", "a");
        testDec("/", "/");
        testDec("%3D", "=");
        testDec("%26", "&");
        testDec("+", "+");
        testDec("%25", "%");
        testDec("%E1%82%A0", "Ⴀ");
        testDec("%C3%85", "Å");
        testDec("%5C", "\\");
        testDec("%25a", "%a");
        testDec("%23", "#");
        testDec("%F0%90%8E%81", "\uD800\uDF81");
        testDec("ab%C3%85%C3%B6%3D%3D%E1%82%A0%25%25%5C", "abÅö==Ⴀ%%\\");
        testDec(null, null);
    }

    @Test
    public void testPopURIDecodeAsciiIllegalCharacter() throws UnsupportedEncodingException {
        for (char c = 0; c < 128; c++) {
            if (PopRequestURITest.ALLOWED_CHARS.indexOf(c) != -1 || c == '%' || c == ' ') {
                continue;
            }
            testEnc(URLEncoder.encode("" + c, "UTF-8"), "" + c);
            testDec(URLEncoder.encode("" + c, "UTF-8"),"" + c);
            try {
                PopURIEncodeDecode.popURIDecode("" + c);
                fail("Fail for input '" + c + "'");
            } catch (IllegalArgumentException e) {

            } catch (Exception e) {
                fail("Wrong exception");
            }
        }
    }

    private void testEnc(String expected, String input) {
        assertEquals(expected, PopURIEncodeDecode.popURIEncode(input));
    }

    private void testDec(String input, String expected) {
        assertEquals(expected, PopURIEncodeDecode.popURIDecode(input));
    }
}
