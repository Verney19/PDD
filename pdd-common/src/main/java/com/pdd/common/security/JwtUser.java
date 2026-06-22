package com.pdd.common.security;

import java.io.Serializable;

public record JwtUser(Long userId, String username, String role) implements Serializable {
}
