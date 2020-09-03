/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package dev.snowdrop.example;

import java.security.KeyFactory;
import java.security.NoSuchAlgorithmException;
import java.security.interfaces.RSAPublicKey;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.X509EncodedKeySpec;

import dev.snowdrop.example.jwt.JwtProperties;
import dev.snowdrop.example.jwt.KeycloakAuthenticationConverter;
import org.apache.commons.codec.binary.Base64;
import org.springframework.http.HttpStatus;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.WebSecurityConfigurerAdapter;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;
import org.springframework.security.web.authentication.HttpStatusEntryPoint;
import org.springframework.stereotype.Component;

@Component
public class SecurityConfiguration extends WebSecurityConfigurerAdapter {

    private final JwtProperties jwtProperties;

    public SecurityConfiguration(JwtProperties jwtProperties) {
        this.jwtProperties = jwtProperties;
    }

    @Override
    protected void configure(HttpSecurity http) throws Exception {
        http
                .authorizeRequests(registry -> registry
                        // Require authentication for REST API access
                        .antMatchers("/api/*").authenticated()
                        // Only allow example-admin (e.g. Alice) to access the REST API
                        .antMatchers("/api/*").hasRole("example-admin")
                        // Allow free access to the rest of the resources
                        .anyRequest().permitAll()
                )
                .oauth2ResourceServer(configurer -> configurer
                        .jwt(customizer -> customizer
                                // Setup a custom Keycloak role converter
                                .jwtAuthenticationConverter(new KeycloakAuthenticationConverter())
                                // Setup a JWT decoder that uses public key to verify it
                                // Alternatively Spring Security could verify the token by contacting Keycloak server
                                .decoder(NimbusJwtDecoder.withPublicKey(getJwtPublicKey()).build())
                        )
                )
                .exceptionHandling(configurer -> configurer
                        .authenticationEntryPoint(new HttpStatusEntryPoint(HttpStatus.UNAUTHORIZED))
                );
    }

    private RSAPublicKey getJwtPublicKey() {
        try {
            byte[] encoded = Base64.decodeBase64(jwtProperties.getPublicKey());
            KeyFactory keyFactory = KeyFactory.getInstance("RSA");
            X509EncodedKeySpec keySpec = new X509EncodedKeySpec(encoded);
            return (RSAPublicKey) keyFactory.generatePublic(keySpec);
        } catch (NoSuchAlgorithmException | InvalidKeySpecException e) {
            throw new RuntimeException(e);
        }
    }
}
