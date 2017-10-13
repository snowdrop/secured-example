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
import java.security.SecureRandom;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
import java.util.List;

import javax.net.ssl.HostnameVerifier;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLSession;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;
import javax.ws.rs.ForbiddenException;
import javax.ws.rs.client.Client;
import javax.ws.rs.client.ClientBuilder;
import javax.ws.rs.client.ClientRequestFilter;
import javax.ws.rs.client.WebTarget;

import io.fabric8.openshift.api.model.Route;
import io.fabric8.openshift.client.DefaultOpenShiftClient;
import io.fabric8.openshift.client.OpenShiftClient;
import io.openshiftio.booster.service.Greeting;
import org.apache.http.client.HttpClient;
import org.apache.http.conn.ssl.NoopHostnameVerifier;
import org.apache.http.impl.client.HttpClients;
import org.jboss.resteasy.client.jaxrs.ResteasyWebTarget;
import org.junit.BeforeClass;
import org.junit.Test;
import org.keycloak.authorization.client.AuthzClient;
import org.keycloak.authorization.client.Configuration;
import org.keycloak.authorization.client.util.HttpResponseException;
import org.keycloak.representations.AccessTokenResponse;
import org.keycloak.util.JsonSerialization;

import static java.util.stream.Collectors.toList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.fail;

/**
 * @author Heiko Braun
 */
public class SsoIT {
    private static List<String> applicationUrls;

    @BeforeClass
    public static void setup() throws Exception {
        OpenShiftClient oc = new DefaultOpenShiftClient();
        List<Route> routes = oc.routes().inNamespace(oc.getNamespace()).list().getItems();

        String ssoAuthUrl = routes.stream()
                .filter(r -> "secure-sso".equals(r.getMetadata().getName()))
                .findFirst()
                .map(r -> "https://" + r.getSpec().getHost() + "/auth")
                .orElseThrow(() -> new IllegalStateException("Couldn't find secure-sso route"));

        System.setProperty("sso.auth.server.url", ssoAuthUrl);

        authzClient = createAuthzCLient(ssoAuthUrl);

        applicationUrls = routes.stream()
                .filter(r -> r.getMetadata().getName().contains("secured"))
                .map(r -> "http://" + r.getSpec().getHost())
                .collect(toList());

    }

    private static Greeting getGreeting(String url, String token, String from) {
        Client client = ClientBuilder.newClient();
        try {
            WebTarget target = client.target(url);
            System.out.println("URL : " + url);
            target.register((ClientRequestFilter) requestContext -> {
                requestContext.getHeaders().add("Authorization", "Bearer " + token);
            });
            IGreeting greetingClient = ((ResteasyWebTarget) target).proxy(IGreeting.class);
            return greetingClient.greeting(from);
        } finally {
            client.close();
        }
    }

    @Test
    public void defaultUser_defaultFrom() {
        AccessTokenResponse accessTokenResponse = authzClient.obtainAccessToken("alice", "password");
        for (String url : applicationUrls) {
            Greeting greeting = getGreeting(url, accessTokenResponse.getToken(), null);

            assertThat(greeting).isNotNull();
            assertThat(greeting.getContent()).contains("Hello, World!");
        }
    }

    @Test
    public void defaultUser_customFrom() {
        AccessTokenResponse accessTokenResponse = authzClient.obtainAccessToken("alice", "password");
        for (String url : applicationUrls) {
            Greeting greeting = getGreeting(url, accessTokenResponse.getToken(), "Scott");

            assertThat(greeting).isNotNull();
            assertThat(greeting.getContent()).contains("Hello, Scott!");
        }
    }

    // This test checks the "authenticated, but not authorized" flow.
    @Test
    public void adminUser() {
        AccessTokenResponse accessTokenResponse = authzClient.obtainAccessToken("admin", "admin");
        for (String url : applicationUrls) {
            try {
                getGreeting(url, accessTokenResponse.getToken(), null);
                fail("ForbiddenException expected");
            } catch (ForbiddenException e) {
                // expected
            }
        }
    }

    @Test
    public void badPassword() {
        try {
            AccessTokenResponse accessTokenResponse = authzClient.obtainAccessToken("alice", "bad");
            fail("HttpResponseException expected (401)");
        } catch (Throwable t) {
            assertThat(t).isInstanceOf(HttpResponseException.class);
            assertThat(((HttpResponseException) t).getStatusCode()).isEqualTo(401);
        }
    }

    private static AuthzClient authzClient;

    /**
     * We need a simplified setup that allows us to work with self-signed certificates.
     * To support this we need to provide a custom http client.
     */
    private static AuthzClient createAuthzCLient(String ssoAuthUrl) throws Exception {
        InputStream configStream = Thread.currentThread().getContextClassLoader().getResourceAsStream("keycloak.json");
        if (configStream == null) {
            throw new IllegalStateException("Could not find any keycloak.json file in classpath.");
        }

        HttpClient httpClient = HttpClients.custom().setSSLContext(getSSLContext(ssoAuthUrl, true))
                .setSSLHostnameVerifier(new NoopHostnameVerifier())
                .build();

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

    /**
     * If this is a https authServerUrl and noCertCheck is true, create an SSLContext that uses
     * an X509TrustManager that allows any certificate.
     *
     * @return SSLContext with all trusting TrustManager if noCertCheck is true, null otherwise
     */
    public static SSLContext getSSLContext(String authServerUrl, boolean noCertCheck) {
        SSLContext sslContext = null;
        if (authServerUrl.startsWith("https") && noCertCheck) {
            try {
                // Install a TrustManager that ignores certificate checks
                sslContext = SSLContext.getInstance("TLS");
                TrustManager[] trustManagers = {new TrustAllManager()};
                sslContext.init(null, trustManagers, new SecureRandom());
            } catch (Exception e) {
                throw new IllegalStateException("Failed to create HttpsClient", e);
            }
        }

        return sslContext;
    }

    private static class AnyHostnameVerifier implements HostnameVerifier {
        @Override
        public boolean verify(String s, SSLSession sslSession) {
            return true;
        }
    }

    private static class TrustAllManager implements X509TrustManager {

        @Override
        public void checkClientTrusted(X509Certificate[] x509Certificates, String s) throws CertificateException {
            //ignore
        }

        @Override
        public void checkServerTrusted(X509Certificate[] x509Certificates, String s) throws CertificateException {
            //ignore
        }

        @Override
        public X509Certificate[] getAcceptedIssuers() {
            return null;
        }
    }

}
