import java.net.HttpURLConnection;
import java.net.URI;

/** Small dependency-free container readiness probe. */
public final class HealthCheck {
  public static void main(String[] args) {
    try {
      var connection = (HttpURLConnection)
          URI.create("http://127.0.0.1:8080/api/health").toURL().openConnection();
      connection.setConnectTimeout(3000);
      connection.setReadTimeout(3000);
      int status = connection.getResponseCode();
      connection.disconnect();
      if (status != 200) System.exit(1);
    } catch (Exception e) {
      System.exit(1);
    }
  }
}
