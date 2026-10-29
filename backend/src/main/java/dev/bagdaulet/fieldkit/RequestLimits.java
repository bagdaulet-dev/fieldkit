package dev.bagdaulet.fieldkit;

import jakarta.servlet.*;
import jakarta.servlet.http.*;
import java.io.*;
import java.util.concurrent.ConcurrentHashMap;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

@Component
@Order(Ordered.HIGHEST_PRECEDENCE + 1)
public class RequestLimits extends OncePerRequestFilter {
  @org.springframework.beans.factory.annotation.Value("${fieldkit.requests-per-minute:300}")
  private int limit;

  private record Window(long minute, int count) {}

  private final ConcurrentHashMap<String, Window> windows = new ConcurrentHashMap<>();

  @Override
  protected void doFilterInternal(
      HttpServletRequest req, HttpServletResponse res, FilterChain chain)
      throws ServletException, IOException {
    if (!req.getRequestURI().startsWith("/api/")) {
      chain.doFilter(req, res);
      return;
    }
    long minute = System.currentTimeMillis() / 60000;
    if (windows.size() > 10000) windows.entrySet().removeIf(e -> e.getValue().minute() != minute);
    if (windows.size() > 10000) {
      reject(res, 503, "BUSY", "Please try again shortly.");
      return;
    }
    Window window =
        windows.compute(
            req.getRemoteAddr(),
            (k, w) ->
                w == null || w.minute() != minute
                    ? new Window(minute, 1)
                    : new Window(minute, w.count() + 1));
    if (window.count() > limit) {
      res.setHeader("Retry-After", "60");
      reject(res, 429, "RATE_LIMITED", "Too many requests. Try again in a minute.");
      return;
    }
    if (req.getMethod().equals("POST")
        || req.getMethod().equals("PUT")
        || req.getMethod().equals("PATCH")
        || req.getMethod().equals("DELETE")) {
      byte[] body = req.getInputStream().readNBytes(32769);
      if (body.length > 32768) {
        reject(res, 413, "PAYLOAD_TOO_LARGE", "Keep the request below 32 KB.");
        return;
      }
      HttpServletRequest wrapped =
          new HttpServletRequestWrapper(req) {
            @Override
            public ServletInputStream getInputStream() {
              ByteArrayInputStream input = new ByteArrayInputStream(body);
              return new ServletInputStream() {
                public int read() {
                  return input.read();
                }

                public boolean isFinished() {
                  return input.available() == 0;
                }

                public boolean isReady() {
                  return true;
                }

                public void setReadListener(ReadListener listener) {
                  throw new UnsupportedOperationException();
                }
              };
            }

            @Override
            public BufferedReader getReader() {
              return new BufferedReader(
                  new InputStreamReader(getInputStream(), java.nio.charset.StandardCharsets.UTF_8));
            }
          };
      chain.doFilter(wrapped, res);
      return;
    }
    chain.doFilter(req, res);
  }

  private void reject(HttpServletResponse res, int status, String code, String message)
      throws IOException {
    res.setStatus(status);
    res.setContentType("application/json");
    res.setHeader("X-Content-Type-Options", "nosniff");
    res.getWriter()
        .write("{\"error\":{\"code\":\"" + code + "\",\"message\":\"" + message + "\"}}");
  }
}
