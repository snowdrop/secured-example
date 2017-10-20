/*
 * Copyright 2016-2017 Red Hat, Inc, and individual contributors.
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *  http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */

package io.openshift.booster;

import java.io.InputStream;
import java.net.URI;
import java.util.concurrent.TimeUnit;

import javax.net.ssl.SSLContext;

import com.jayway.restassured.response.Response;
import io.fabric8.openshift.api.model.Route;
import io.openshift.booster.test.OpenShiftTestAssistant;
import org.apache.http.client.HttpClient;
import org.apache.http.conn.ssl.NoopHostnameVerifier;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.ssl.SSLContexts;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;
import org.keycloak.authorization.client.AuthzClient;
import org.keycloak.authorization.client.Configuration;
import org.keycloak.authorization.client.util.HttpResponseException;
import org.keycloak.representations.AccessTokenResponse;
import org.keycloak.util.JsonSerialization;

import static com.jayway.awaitility.Awaitility.await;
import static com.jayway.restassured.RestAssured.get;
import static com.jayway.restassured.RestAssured.given;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.fail;
import static org.hamcrest.core.Is.is;

/**
 * @author Heiko Braun
 */
public class OpenShiftIT {
    private static final OpenShiftTestAssistant openshift = new OpenShiftTestAssistant();

    private static AuthzClient authzClient;

    private static String applicationUrl;

    @BeforeClass
    public static void setup() throws Exception {
        Route ssoServerRoute = requireRoute("secure-sso");
        String ssoAuthUrl = "https://" + ssoServerRoute.getSpec().getHost() + "/auth";
        authzClient = createAuthzClient(ssoAuthUrl);

        openshift.deployApplication();

        // wait until the pods & routes become available
        openshift.awaitApplicationReadinessOrFail();

        await().atMost(5, TimeUnit.MINUTES).until(() -> {
            try {
                Response response = get();
                return response.getStatusCode() == 200;
            } catch (Exception e) {
                return false;
            }
        });

        Route applicationRoute = requireRoute("secured-springboot-rest");
        applicationUrl = "http://" + applicationRoute.getSpec().getHost();
    }

    @AfterClass
    public static void teardown() throws Exception {
        openshift.cleanup();
    }

    private static void verifyGreeting(String token, String from) {
        given().header("Authorization", "Bearer " + token)
                .get(URI.create(applicationUrl))
                .then()
                .statusCode(200)
                .body("content", is(String.format("Hello, %s!", from)));
    }

    @Test
    public void defaultUser_defaultFrom() {
        AccessTokenResponse accessTokenResponse = authzClient.obtainAccessToken("alice", "password");

        verifyGreeting(accessTokenResponse.getToken(), null);
    }

    @Test
    public void defaultUser_customFrom() {
        AccessTokenResponse accessTokenResponse = authzClient.obtainAccessToken("alice", "password");

        verifyGreeting(accessTokenResponse.getToken(), "Scott");
    }

    // This test checks the "authenticated, but not authorized" flow.
    @Test
    public void adminUser() {
        AccessTokenResponse accessTokenResponse = authzClient.obtainAccessToken("admin", "admin");

        try {
            verifyGreeting(accessTokenResponse.getToken(), null);
            fail("403 Forbidden expected");
        } catch (Exception e) {
            // todo: properly handle
//            assertThat(e.getResponse().getStatus()).isEqualTo(403);
        }
    }

    @Test
    public void badPassword() {
        try {
            authzClient.obtainAccessToken("alice", "bad");
            fail("401 Unauthorized expected");
        } catch (HttpResponseException t) {
            assertThat(t.getStatusCode()).isEqualTo(401);
        }
    }

    private static Route requireRoute(String name) {
        Route route = openshift.client().routes().inNamespace(openshift.project()).withName(name).get();
        if (route == null) {
            throw new IllegalStateException("Couldn't find route " + name);
        }
        return route;
    }

    /**
     * We need a simplified setup that allows us to work with self-signed certificates.
     * To support this we need to provide a custom http client.
     */
    private static AuthzClient createAuthzClient(String ssoAuthUrl) throws Exception {
        InputStream configStream = Thread.currentThread().getContextClassLoader().getResourceAsStream("keycloak.json");
        if (configStream == null) {
            throw new IllegalStateException("Could not find any keycloak.json file in classpath.");
        }

        SSLContext sslContext = SSLContexts.custom()
                .loadTrustMaterial((chain, authType) -> true)
                .build();
        HttpClient httpClient = HttpClients.custom()
                .setSSLContext(sslContext)
                .setSSLHostnameVerifier(NoopHostnameVerifier.INSTANCE)
                .build();

        System.setProperty("sso.auth.server.url", ssoAuthUrl);
        Configuration baseline = JsonSerialization.readValue(
                configStream,
                Configuration.class,
                true // system property replacement
        );

        return AuthzClient.create(
                new Configuration(
                        baseline.getAuthServerUrl(),
                        baseline.getRealm(),
                        baseline.getClientId(),
                        baseline.getClientCredentials(),
                        httpClient
                )
        );
    }
}