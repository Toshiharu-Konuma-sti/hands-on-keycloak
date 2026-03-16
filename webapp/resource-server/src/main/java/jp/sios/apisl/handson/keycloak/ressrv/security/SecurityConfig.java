package jp.sios.apisl.handson.keycloak.ressrv.security;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.AuthenticationManagerResolver;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationProvider;
import org.springframework.security.oauth2.server.resource.authentication.OpaqueTokenAuthenticationProvider;
import org.springframework.security.oauth2.server.resource.introspection.SpringOpaqueTokenIntrospector;
import org.springframework.security.web.SecurityFilterChain;

@Configuration
public class SecurityConfig {

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http,
            AuthenticationManagerResolver<HttpServletRequest> authManagerResolver) throws Exception {
        http
            .authorizeHttpRequests(auth -> auth
                .requestMatchers("/api/**").authenticated()
                .anyRequest().permitAll()
            )
            .oauth2ResourceServer(oauth -> oauth.authenticationManagerResolver(authManagerResolver));

        return http.build();
    }

    @Bean
    AuthenticationManagerResolver<HttpServletRequest> tokenAuthenticationManagerResolver(
            @Value("${spring.security.oauth2.resourceserver.jwt.issuer-uri}") String issuerUri,
            @Value("${spring.security.oauth2.resourceserver.opaquetoken.introspection-uri}") String introspectionUri,
            @Value("${spring.security.oauth2.resourceserver.opaquetoken.client-id}") String clientId,
            @Value("${spring.security.oauth2.resourceserver.opaquetoken.client-secret}") String clientSecret) {

        // 1. JWT検証用マネージャー
        // fromIssuerUriの代わりに withIssuerLocation().build() を使用してデコーダーを生成します
        var jwtDecoder = NimbusJwtDecoder.withIssuerLocation(issuerUri).build();
        AuthenticationManager jwtManager = new JwtAuthenticationProvider(jwtDecoder)::authenticate;

        // 2. Opaque Token検証用マネージャー
        // 警告を回避するため、あえて明示的に SpringOpaqueTokenIntrospector を使用します
        @SuppressWarnings("removal")
        var introspector = new SpringOpaqueTokenIntrospector(introspectionUri, clientId, clientSecret);
        AuthenticationManager opaqueManager = new OpaqueTokenAuthenticationProvider(introspector)::authenticate;

        // 3. 判別ロジック (Bearer eyJ で始まればJWT、それ以外はOpaque)
        return request -> {
            String header = request.getHeader("Authorization");
            if (header != null && header.startsWith("Bearer eyJ")) {
                return jwtManager;
            }
            return opaqueManager;
        };
    }
}