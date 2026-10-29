package dev.bagdaulet.fieldkit;

import jakarta.servlet.*;
import jakarta.servlet.http.*;
import java.io.IOException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

@Component
@Order(Ordered.HIGHEST_PRECEDENCE)
public class TransportSecurity extends OncePerRequestFilter {
  private final boolean httpsRequired;

  public TransportSecurity(@Value("${fieldkit.secure-cookies}") boolean httpsRequired) {
    this.httpsRequired = httpsRequired;
  }

  @Override
  protected void doFilterInternal(
      HttpServletRequest req, HttpServletResponse res, FilterChain chain)
      throws ServletException, IOException {
    res.setHeader("Content-Security-Policy", SecurityConfiguration.CSP);
    res.setHeader("X-Frame-Options", "DENY");
    res.setHeader("X-Content-Type-Options", "nosniff");
    res.setHeader("Referrer-Policy", "same-origin");
    res.setHeader("Permissions-Policy", "camera=(), microphone=(), geolocation=()");
    if (req.isSecure()) res.setHeader("Strict-Transport-Security", "max-age=31536000");
    if (req.getRequestURI().startsWith("/api/")) res.setHeader("Cache-Control", "no-store");
    boolean health =
        req.getRequestURI().equals("/api/health")
            && (req.getMethod().equals("GET") || req.getMethod().equals("HEAD"));
    if (httpsRequired && !req.isSecure() && !health) {
      res.setStatus(426);
      res.setContentType("application/json");
      res.setHeader("Cache-Control", "no-store");
      res.getWriter()
          .write(
              "{\"error\":{\"code\":\"HTTPS_REQUIRED\",\"message\":\"Use HTTPS to access this workspace.\"}}");
      return;
    }
    chain.doFilter(req, res);
  }
}
