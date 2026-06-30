package com.example.cabbagemarket10.global.security.jwt;

import java.security.Principal;

public record AuthenticatedClient(
        Long clientId,
        String email) implements Principal{

    @Override
    public String getName() {
        return this.email;  // TODO: This is temporary and should be refactored
    }
}
