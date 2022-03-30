package org.jedkos.ceptic.client;

import org.junit.Test;

public class CepticClientTests {

    @Test
    public void createClient_Unsecure() {
        CepticClient client = new CepticClientBuilder()
                .secure(false)
                .build();
    }

}
