package dev.bagdaulet.fieldkit;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.csrf.HttpSessionCsrfTokenRepository;

@Configuration
@org.springframework.session.jdbc.config.annotation.web.http.EnableJdbcHttpSession(
    maxInactiveIntervalInSeconds = 604800)
public class SecurityConfiguration {
  public static final String CSP =
      "default-src 'self'; script-src 'self'; style-src 'self'; font-src 'self'; img-src 'self' data:; connect-src 'self'; frame-ancestors 'none'; base-uri 'none'; form-action 'self'; object-src 'none'";

  @Bean
  org.springframework.session.web.http.DefaultCookieSerializer cookieSerializer(
      @org.springframework.beans.factory.annotation.Value("${fieldkit.secure-cookies}")
          boolean secure) {
    var cookie = new org.springframework.session.web.http.DefaultCookieSerializer();
    cookie.setCookieName("FIELDKIT_SESSION");
    cookie.setCookiePath("/");
    cookie.setUseHttpOnlyCookie(true);
    cookie.setSameSite("Strict");
    cookie.setUseSecureCookie(secure);
    cookie.setCookieMaxAge(604800);
    return cookie;
  }

  @Bean
  org.springframework.boot.jackson.autoconfigure.JsonMapperBuilderCustomizer strictJson() {
    return builder ->
        builder
            .disable(tools.jackson.databind.DeserializationFeature.ACCEPT_FLOAT_AS_INT)
            .disable(tools.jackson.databind.MapperFeature.ALLOW_COERCION_OF_SCALARS)
            .withCoercionConfig(
                tools.jackson.databind.type.LogicalType.Textual,
                config -> {
                  for (var shape :
                      java.util.List.of(
                          tools.jackson.databind.cfg.CoercionInputShape.Integer,
                          tools.jackson.databind.cfg.CoercionInputShape.Float,
                          tools.jackson.databind.cfg.CoercionInputShape.Boolean))
                    config.setCoercion(shape, tools.jackson.databind.cfg.CoercionAction.Fail);
                });
  }

  @Bean
  org.springframework.security.core.userdetails.UserDetailsService users() {
    return new org.springframework.security.provisioning.InMemoryUserDetailsManager();
  }

  @Bean
  SecurityFilterChain security(HttpSecurity http) throws Exception {
    http.authorizeHttpRequests(a -> a.anyRequest().permitAll())
        .csrf(c -> c.csrfTokenRepository(new HttpSessionCsrfTokenRepository()))
        .headers(
            h ->
                h.contentSecurityPolicy(c -> c.policyDirectives(CSP))
                    .referrerPolicy(
                        r ->
                            r.policy(
                                org.springframework.security.web.header.writers
                                    .ReferrerPolicyHeaderWriter.ReferrerPolicy.SAME_ORIGIN))
                    .permissionsPolicyHeader(
                        p -> p.policy("camera=(), microphone=(), geolocation=()")))
        .exceptionHandling(
            e ->
                e.accessDeniedHandler(
                    (request, response, denied) -> {
                      response.setStatus(403);
                      response.setContentType("application/json");
                      response
                          .getWriter()
                          .write(
                              "{\"error\":{\"code\":\"CSRF_FAILED\",\"message\":\"Refresh this page and try again.\"}}");
                    }));
    return http.build();
  }
}
