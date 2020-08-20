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

import java.net.URL;

import org.arquillian.cube.openshift.impl.enricher.AwaitRoute;
import org.arquillian.cube.openshift.impl.enricher.RouteURL;
import org.jboss.arquillian.junit.Arquillian;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;

import static io.restassured.RestAssured.given;
import static io.restassured.RestAssured.when;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.is;

@RunWith(Arquillian.class)
public class OpenShiftIT {

    private static final String CLIENT_ID = "demoapp";

    private static final String CLIENT_SECRET = "1daa57a2-b60e-468b-a3ac-25bd2dc2eadc";

    @RouteURL(value = "${app.name}", path = "/api/greeting")
    @AwaitRoute(path = "/actuator/health")
    private URL greetingUrl;

    @RouteURL(value = "secure-sso", path = "/auth/realms/master/protocol/openid-connect/token")
    private URL tokenUrl;

    @Test
    public void shouldNotGetGreetingWithoutToken() {
        when().get(greetingUrl)
                .then().statusCode(HttpStatus.UNAUTHORIZED.value());
    }

    @Test
    public void shouldNotGetGreetingWithUnauthorizedUser() {
        String token = getToken("admin", "admin");
        given().header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                .when().get(greetingUrl)
                .then().statusCode(HttpStatus.FORBIDDEN.value());
    }

    @Test
    public void shouldGetDefaultGreeting() {
        String token = getToken("alice", "password");
        given().header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                .when().get(greetingUrl)
                .then().statusCode(HttpStatus.OK.value())
                .and().body("content", is(equalTo("Hello, World!")));
    }

    @Test
    public void shouldGetCustomGreeting() {
        String token = getToken("alice", "password");
        given().header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                .when().get(greetingUrl + "?name=Scott")
                .then().statusCode(HttpStatus.OK.value())
                .and().body("content", is(equalTo("Hello, Scott!")));
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
                .then().statusCode(200)
                .and().extract().body()
                .jsonPath().getString("access_token");
    }
}
