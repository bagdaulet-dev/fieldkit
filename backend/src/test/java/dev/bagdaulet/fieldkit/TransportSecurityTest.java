package dev.bagdaulet.fieldkit;

import static org.testng.Assert.*;

import java.net.*;
import java.net.http.*;
import java.util.UUID;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.testng.annotations.Test;

public class TransportSecurityTest {
  @Test
  public void httpsSessionUsesSecureCookieAndHsts() throws Exception {
    var folder = java.nio.file.Files.createTempDirectory("fieldkit-tls-test-");
    var keyStore = folder.resolve("test.p12");
    String password = "fieldkit-test-only";
    try {
      String keytool =
          java.nio.file.Path.of(System.getProperty("java.home"), "bin", "keytool").toString();
      var generated =
          new ProcessBuilder(
                  keytool,
                  "-genkeypair",
                  "-alias",
                  "local-test",
                  "-keyalg",
                  "RSA",
                  "-keysize",
                  "2048",
                  "-validity",
                  "1",
                  "-storetype",
                  "PKCS12",
                  "-keystore",
                  keyStore.toString(),
                  "-storepass",
                  password,
                  "-keypass",
                  password,
                  "-dname",
                  "CN=localhost",
                  "-ext",
                  "SAN=IP:127.0.0.1,DNS:localhost",
                  "-noprompt")
              .redirectOutput(ProcessBuilder.Redirect.DISCARD)
              .redirectError(ProcessBuilder.Redirect.DISCARD)
              .start();
      assertEquals(generated.waitFor(), 0, "Could not generate isolated test TLS material.");
      var trust = java.security.KeyStore.getInstance("PKCS12");
      try (var input = java.nio.file.Files.newInputStream(keyStore)) {
        trust.load(input, password.toCharArray());
      }
      var trustManager =
          javax.net.ssl.TrustManagerFactory.getInstance(
              javax.net.ssl.TrustManagerFactory.getDefaultAlgorithm());
      trustManager.init(trust);
      var tls = javax.net.ssl.SSLContext.getInstance("TLS");
      tls.init(null, trustManager.getTrustManagers(), null);
      try (var app =
              new SpringApplicationBuilder(FieldkitApplication.class)
                  .profiles("preview")
                  .run(
                      "--server.port=0",
                      "--fieldkit.secure-cookies=true",
                      "--debug=false",
                      "--logging.level.root=ERROR",
                      "--server.ssl.enabled=true",
                      "--server.ssl.key-store=" + keyStore.toUri(),
                      "--server.ssl.key-store-type=PKCS12",
                      "--server.ssl.key-store-password=" + password,
                      "--spring.datasource.url=jdbc:h2:mem:tls-positive-"
                          + UUID.randomUUID()
                          + ";MODE=MySQL");
          var http = HttpClient.newBuilder().sslContext(tls).build()) {
        String base = "https://127.0.0.1:" + app.getEnvironment().getProperty("local.server.port");
        var response =
            http.send(
                HttpRequest.newBuilder(URI.create(base + "/api/session")).GET().build(),
                HttpResponse.BodyHandlers.ofString());
        assertEquals(response.statusCode(), 200);
        String cookie = response.headers().firstValue("set-cookie").orElse("");
        assertTrue(cookie.contains("Secure"));
        assertTrue(cookie.contains("HttpOnly"));
        assertTrue(cookie.contains("SameSite=Strict"));
        assertTrue(cookie.contains("Path=/"));
        assertTrue(
            response
                .headers()
                .firstValue("strict-transport-security")
                .orElse("")
                .contains("max-age=31536000"));
      }
    } finally {
      java.nio.file.Files.deleteIfExists(keyStore);
      java.nio.file.Files.delete(folder);
    }
  }

  @Test
  public void secureModeRejectsHttpAndSpoofedProxyHeadersWithoutCreatingSessions()
      throws Exception {
    try (var app =
            new SpringApplicationBuilder(FieldkitApplication.class)
                .profiles("preview")
                .run(
                    "--server.port=0",
                    "--fieldkit.secure-cookies=true",
                    "--debug=false",
                    "--logging.level.root=ERROR",
                    "--spring.datasource.url=jdbc:h2:mem:tls-"
                        + UUID.randomUUID()
                        + ";MODE=MySQL");
        var http = HttpClient.newHttpClient()) {
      String base = "http://127.0.0.1:" + app.getEnvironment().getProperty("local.server.port");
      for (String path :
          new String[] {"/api/session", "/api/export", "/", "/api/health?not-public-data"}) {
        var response =
            http.send(
                HttpRequest.newBuilder(URI.create(base + path))
                    .header("X-Forwarded-Proto", "https")
                    .header("Forwarded", "proto=https;host=trusted.example")
                    .GET()
                    .build(),
                HttpResponse.BodyHandlers.ofString());
        if (path.startsWith("/api/health")) {
          assertEquals(response.statusCode(), 200);
        } else {
          assertEquals(response.statusCode(), 426);
          assertTrue(response.body().contains("HTTPS_REQUIRED"));
        }
        assertTrue(response.headers().firstValue("set-cookie").isEmpty());
        assertTrue(response.headers().firstValue("strict-transport-security").isEmpty());
        assertTrue(response.headers().firstValue("content-security-policy").isPresent());
      }
    }
  }
}
