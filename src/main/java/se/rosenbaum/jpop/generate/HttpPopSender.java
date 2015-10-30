package se.rosenbaum.jpop.generate;

import se.rosenbaum.jpop.Pop;
import se.rosenbaum.jpop.PopRequestURI;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.Reader;
import java.io.UnsupportedEncodingException;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.nio.CharBuffer;

/**
 * This will send the pop through http or https.
 */
public class HttpPopSender implements PopSender {
    public static final int REPLY_SIZE_LIMIT = 1024;
    private PopRequestURI popRequestURI;
    private Result result;
    private String message;

    public HttpPopSender(PopRequestURI popRequestURI) {
        this.popRequestURI = popRequestURI;
    }

    public Result getResult() {
        if (result == null) {
            throw new UnsupportedOperationException("sendPop has not been called yet.");
        }
        return result;
    }

    public void sendPop(Pop signedPop) {
        URL url;
        try {
            url = new URL(popRequestURI.getP());
        } catch (MalformedURLException e) {
            result(Result.LOCAL_ERROR, "Invalid Url: " + popRequestURI.getP());
            return;
        }
        HttpURLConnection urlConnection;
        try {
            urlConnection = (HttpURLConnection) url.openConnection();
        } catch (IOException e) {
            result(Result.COMMUNICATION_ERROR, "Cannot connect to " + url + ": " + e.getMessage());
            return;
        }
        try {
            urlConnection.setDoOutput(true);
            urlConnection.setRequestProperty("Content-Type", "application/bitcoin-pop");

            byte[] bytes = signedPop.bitcoinSerialize();
            OutputStream out = null;
            try {
                out = new BufferedOutputStream(urlConnection.getOutputStream());
            } catch (IOException e) {
                result(Result.COMMUNICATION_ERROR, "Cannot get OutputStream on " + url + ": " + e.getMessage());
                return;
            }
            try {
                out.write(bytes);
                out.close();
            } catch (IOException e) {
                result(Result.COMMUNICATION_ERROR, "Cannot write to " + url + ": " + e.getMessage());
                return;
            }
            try {
                int responseCode = urlConnection.getResponseCode();
                if (responseCode != HttpURLConnection.HTTP_OK) {
                    result(Result.COMMUNICATION_ERROR, "Got response code: " + responseCode);
                    return;
                }
            } catch (IOException e) {
                result(Result.COMMUNICATION_ERROR, "Cannot get response code: " + e.getMessage());
                return;
            }

            InputStream in = null;
            try {
                in = new BufferedInputStream(urlConnection.getInputStream());
            } catch (IOException e) {
                result(Result.COMMUNICATION_ERROR, "Cannot get InputStream on " + url + ": " + e.getMessage());
                return;
            }
            InputStreamReader inputStreamReader = null;
            try {
                inputStreamReader = new InputStreamReader(in, "US-ASCII");
                readReplyAndSetResult(inputStreamReader);
                inputStreamReader.close();
                return;
            } catch (UnsupportedEncodingException e) {
                result(Result.LOCAL_ERROR, "Unknown encoding 'US-ASCII':" + e.getMessage());
            } catch (IOException e) {
                result(Result.COMMUNICATION_ERROR, "Cannot close InputStreamReader");
            }
        } finally {
            urlConnection.disconnect();
        }
    }

    void readReplyAndSetResult(Reader reader) {
        try {
            String reply = getReplyFromReader(reader);
            if (reply.equals("valid")) {
                result = Result.OK;
                return;
            }
            if (!reply.startsWith("invalid")) {
                result = Result.PROTOCOL_ERROR;
                return;
            }
            result = Result.INVALID_POP;

            int newLineIndex = reply.indexOf("\n");
            if (newLineIndex == -1) {
                return;
            }
            setMessage(reply.substring(newLineIndex + 1));
        } catch (IOException e) {
            result(Result.COMMUNICATION_ERROR, "Could not read reply: " + e.getMessage());
            return;
        }
    }

    private static String getReplyFromReader(final Readable input) throws IOException {
        final StringBuilder text = new StringBuilder();
        final CharBuffer buffer = CharBuffer.allocate(128);
        while (true) {
            final int n = input.read(buffer);
            if (n == -1) {
                break;
            }
            buffer.flip();
            text.append(buffer,0,n);
            if (text.length() > REPLY_SIZE_LIMIT) {
                return text.substring(0, REPLY_SIZE_LIMIT);
            }
        }
        return text.toString();
    }

    private void result(Result result, String message) {
        this.result = result;
        setMessage(message);
    }

    public String errorMessage() {
        if (result == null) {
            throw new UnsupportedOperationException("sendPop has not been called yet.");
        }
        return this.message;
    }

    public void setMessage(String message) {
        if (message != null && message.length() > 0) {
            this.message = message;
        } else {
            this.message = null;
        }
    }
}
