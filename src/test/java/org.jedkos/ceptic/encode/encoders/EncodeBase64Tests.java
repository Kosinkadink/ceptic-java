package org.jedkos.ceptic.encode.encoders;

import org.junit.Test;

import java.nio.charset.StandardCharsets;

import static org.assertj.core.api.Assertions.*;

class EncodeBase64Tests {

    @Test
    public void encodeAndDecode() {
        String input = "someTestString123!@#";

        EncodeBase64 encoder = new EncodeBase64();

        byte[] encoded = encoder.encode(input.getBytes());
        byte[] decoded = encoder.decode(encoded);
        String encodedString = new String(encoded, StandardCharsets.UTF_8);
        String output = new String(decoded, StandardCharsets.UTF_8);

        assertThat(output).isEqualTo(input);
        assertThat(encodedString).isNotEqualTo(input);
        assertThat(decoded).isNotEqualTo(encoded);
    }

}