package com.personal.auth.security;

import org.junit.jupiter.api.Test;
import org.springframework.security.authentication.AbstractAuthenticationToken;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.oauth2.jwt.Jwt;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class JwtAuthenticationConverterTest {

    private final JwtAuthenticationConverter converter = new JwtAuthenticationConverter();

    private static Jwt.Builder token() {
        return Jwt.withTokenValue("token").header("alg", "none").subject("keycloak-sub");
    }

    @Test
    void realmRolesAndScopesBecomeAuthorities() {
        Jwt jwt = token()
                .claim("scope", "openid email")
                .claim("realm_access", Map.of("roles", List.of("user", "admin")))
                .build();

        AbstractAuthenticationToken authentication = converter.convert(jwt);

        assertThat(authentication.getName()).isEqualTo("keycloak-sub");
        assertThat(authentication.getAuthorities()).extracting(GrantedAuthority::getAuthority)
                .containsExactlyInAnyOrder("SCOPE_openid", "SCOPE_email", "ROLE_USER", "ROLE_ADMIN");
    }

    @Test
    void tokenWithoutRealmAccessHasOnlyScopes() {
        Jwt jwt = token().claim("scope", "openid").build();

        assertThat(converter.convert(jwt).getAuthorities()).extracting(GrantedAuthority::getAuthority)
                .containsExactly("SCOPE_openid");
    }
}
