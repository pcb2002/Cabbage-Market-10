package com.example.cabbagemarket10.global.security.jwt;

public record AuthenticatedClient(
        Long clientId,
        String email) {}
