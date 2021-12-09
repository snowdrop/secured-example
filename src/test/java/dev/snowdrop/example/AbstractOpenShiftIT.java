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

import static io.restassured.RestAssured.given;
import static io.restassured.RestAssured.when;
import static org.apache.commons.lang3.StringUtils.EMPTY;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.is;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.Objects;

import org.awaitility.Awaitility;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;

import com.gargoylesoftware.htmlunit.Page;
import com.gargoylesoftware.htmlunit.SilentCssErrorHandler;
import com.gargoylesoftware.htmlunit.WebClient;
import com.gargoylesoftware.htmlunit.html.HtmlForm;
import com.gargoylesoftware.htmlunit.html.HtmlPage;

import io.fabric8.kubernetes.client.KubernetesClient;
import io.fabric8.openshift.api.model.Route;
import io.fabric8.openshift.client.OpenShiftClient;

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
public abstract class AbstractOpenShiftIT {

    private static final String CLIENT_ID = "demoapp";
    private static final String CLIENT_SECRET = "1daa57a2-b60e-468b-a3ac-25bd2dc2eadc";
    private static final String REALM_REGEXP = ".*(Sign|Log) in to .*";
    private static final String DEFAULT_TEXT = "Invoke the service to see the result.";

    private URL greetingUrl;
    private URL tokenUrl;
    private WebClient webClient;
    private Page page;

    protected abstract KubernetesClient getKubernetesClient();

    @BeforeAll
    public void setup() throws IOException {
        webClient = new WebClient();
        webClient.setCssErrorHandler(new SilentCssErrorHandler());
        webClient.getOptions().setUseInsecureSSL(true);
        webClient.getOptions().setRedirectEnabled(true);

        greetingUrl = getBaseUrlByRouteName("rest-secured", "/api/greeting");
        tokenUrl = getBaseUrlByRouteName("sso", "/auth/realms/master/protocol/openid-connect/token");
        page = webClient.getPage(getBaseUrlByRouteName("rest-secured", ""));
        assertTrue(page instanceof HtmlPage, "Should be in an HTML page");
    }

    @Test
    public void shouldNotGetGreetingWithoutToken() {
        when().get(greetingUrl)
                .then().log().all().statusCode(HttpStatus.UNAUTHORIZED.value());
    }

    @Test
    public void shouldNotGetGreetingWithUnauthorizedUser() {
        String token = getToken("admin", "admin");
        given().header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                .when().get(greetingUrl)
                .then().log().all().statusCode(HttpStatus.FORBIDDEN.value());
    }

    @Test
    public void shouldGetDefaultGreeting() {
        String token = getToken("alice", "password");
        given().header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                .when().get(greetingUrl)
                .then().statusCode(HttpStatus.OK.value())
                .and().log().all().body("content", is(equalTo("Hello, World!")));
    }

    @Test
    public void shouldGetCustomGreeting() {
        String token = getToken("alice", "password");
        given().header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                .when().get(greetingUrl + "?name=Scott")
                .then().statusCode(HttpStatus.OK.value())
                .and().log().all().body("content", is(equalTo("Hello, Scott!")));
    }

    @Test
    public void shouldLoginAndLogout() throws IOException {
        // No login, then we should get 401 when invoke the service
        whenClickOnInvokeButton();
        thenTextShouldContains(DEFAULT_TEXT);
        thenCurlSectionShouldContain(EMPTY);

        // Login
        whenClickOnLogin();
        thenPageIsRedirectedToKeycloakLogin();
        thenLoginInKeycloakAs("alice", "password");
        thenCurlSectionShouldContain("Authorization: Bearer ");

        // Now, we should be able to click on the invoke button
        whenClickOnInvokeButton();
        thenTextShouldContains("Hello, World!");

        // Logout
        whenClickOnLogout();
        whenClickOnInvokeButton();
        thenTextShouldContains(DEFAULT_TEXT);
        thenCurlSectionShouldContain(EMPTY);
    }

    private void whenClickOnLogin() {
        page = Awaitility.await()
                .ignoreExceptions()
                .until(() -> ((HtmlPage) page).getElementById("login").click(), Objects::nonNull);
    }

    private void whenClickOnLogout() {
        page = Awaitility.await()
                .ignoreExceptions()
                .until(() -> ((HtmlPage) page).getElementById("logout").click(), Objects::nonNull);
    }

    private void whenClickOnInvokeButton() throws IOException {
        page = ((HtmlPage) page).getElementById("invoke").click();
    }

    private void thenTextShouldContains(String expected) {
        Awaitility.await().ignoreExceptions()
                .untilAsserted(() -> {
                    String actual = ((HtmlPage) page).getElementById("result").getTextContent();
                    assertTrue(actual.contains(expected),
                            "Unexpected content of the `result` element. Was: '" + actual + "', expected: '" + expected + "'");
                });
    }

    private void thenPageIsRedirectedToKeycloakLogin() {
        String title = ((HtmlPage) page).getTitleText();
        assertTrue(title.matches(REALM_REGEXP),
                "Login page title should display application realm. Title was: '" + title + "'");
    }

    private void thenCurlSectionShouldContain(String expected) {
        Awaitility.await().ignoreExceptions()
                .untilAsserted(() -> {
                    String actual = ((HtmlPage) page).getElementById("curl").getTextContent();
                    assertTrue(actual.contains(expected),
                            "Unexpected content of the `curl` element. Was: '" + actual + "', expected: '" + expected + "'");
                });
    }

    private void thenLoginInKeycloakAs(String user, String pass) throws IOException {
        HtmlForm loginForm = ((HtmlPage) page).getForms().get(0);

        loginForm.getInputByName("username").setValueAttribute(user);
        loginForm.getInputByName("password").setValueAttribute(pass);

        page = loginForm.getInputByName("login").click();
    }

    protected URL getBaseUrlByRouteName(String routeName, String path) throws MalformedURLException {
        // TODO: In Dekorate 1.7, we can inject Routes directly, so we won't need to do this:
        Route route = getKubernetesClient().adapt(OpenShiftClient.class).routes().withName(routeName).get();
        String protocol = route.getSpec().getTls() == null ? "http" : "https";
        int port = "http".equals(protocol) ? 80 : 443;
        return new URL(protocol, route.getSpec().getHost(), port, path);
    }

    private String getToken(String username, String password) {
        return given().relaxedHTTPSValidation()
                .and().formParam("client_id", CLIENT_ID)
                .and().formParam("client_secret", CLIENT_SECRET)
                .and().formParam("grant_type", "password")
                .and().formParam("username", username)
                .and().formParam("password", password)
                .and().contentType(MediaType.APPLICATION_FORM_URLENCODED_VALUE)
                .when().post(tokenUrl)
                .then().log().all().statusCode(200)
                .and().extract().body()
                .jsonPath().getString("access_token");
    }
}
