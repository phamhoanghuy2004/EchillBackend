package com.echill.event;

public record UserAuthEvent(String email, String fullName, boolean isForgot) {}