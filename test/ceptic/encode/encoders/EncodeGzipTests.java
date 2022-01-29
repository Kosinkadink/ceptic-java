package ceptic.encode.encoders;

import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;

import static org.junit.jupiter.api.Assertions.*;

class EncodeGzipTests {

    @Test
    void encodeAndDecode() {
        String input = "someTestString123!@#";

        EncodeGzip encoder = new EncodeGzip();

        byte[] encoded = encoder.encode(input.getBytes());
        byte[] decoded = encoder.decode(encoded);
        String encodedString = new String(encoded, StandardCharsets.UTF_8);
        String output = new String(decoded, StandardCharsets.UTF_8);

        assertEquals(input, output);
        assertNotEquals(input, encodedString);
        assertNotEquals(encoded, decoded);
    }

}