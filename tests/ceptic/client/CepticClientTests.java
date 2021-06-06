package ceptic.client;

import org.junit.jupiter.api.Test;

public class CepticClientTests {

    @Test
    void createClient_Unsecure() {
        CepticClient client = new CepticClientBuilder()
                .secure(false)
                .build();
    }

}
