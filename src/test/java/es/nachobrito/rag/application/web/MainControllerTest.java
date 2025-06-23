/*
 *    Copyright 2025 Nacho Brito
 *
 *    Licensed under the Apache License, Version 2.0 (the "License");
 *    you may not use this file except in compliance with the License.
 *    You may obtain a copy of the License at
 *
 *        http://www.apache.org/licenses/LICENSE-2.0
 *
 *    Unless required by applicable law or agreed to in writing, software
 *    distributed under the License is distributed on an "AS IS" BASIS,
 *    WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *    See the License for the specific language governing permissions and
 *    limitations under the License.
 */

package es.nachobrito.rag.application.web;

import static org.junit.jupiter.api.Assertions.*;

import io.micronaut.context.annotation.Property;
import io.micronaut.http.HttpRequest;
import io.micronaut.http.client.HttpClient;
import io.micronaut.http.client.annotation.Client;
import io.micronaut.test.extensions.junit5.annotation.MicronautTest;
import jakarta.inject.Inject;
import org.junit.jupiter.api.Test;

/**
 * @author nacho
 */
@MicronautTest(rebuildContext = true)
class MainControllerTest {

  @Inject
  @Client("/")
  HttpClient client;

  @Test
  @Property(name = "micronaut.http.client.follow-redirects", value = "false")
  void redirectHome() {
    var request = HttpRequest.GET("/");
    var response = client.toBlocking().exchange(request);

    assertEquals(301, response.code());
    assertEquals("/index.html", response.getHeaders().get("Location"));
  }
}
