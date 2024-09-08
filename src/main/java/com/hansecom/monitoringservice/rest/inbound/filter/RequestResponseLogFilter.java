package com.hansecom.monitoringservice.rest.inbound.filter;

import static net.logstash.logback.argument.StructuredArguments.value;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ReadListener;
import jakarta.servlet.ServletException;
import jakarta.servlet.ServletInputStream;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletRequestWrapper;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.UnsupportedEncodingException;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import lombok.extern.slf4j.Slf4j;
import net.minidev.json.parser.JSONParser;
import net.minidev.json.parser.ParseException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpHeaders;
import org.springframework.http.server.ServletServerHttpRequest;
import org.springframework.http.server.ServletServerHttpResponse;
import org.springframework.stereotype.Component;
import org.springframework.util.AntPathMatcher;
import org.springframework.util.StreamUtils;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;
import org.springframework.web.util.ContentCachingResponseWrapper;

/**
 * Filter responsible for logging the request and response, including the bodies.
 *
 * <p>It can be configured to not log some requests using the property {@code
 * logging.web.path.exclude}
 */
@Slf4j
@Component
@Order(value = Ordered.HIGHEST_PRECEDENCE + 100)
public class RequestResponseLogFilter extends OncePerRequestFilter {

  /** {@link AntPathMatcher} used to validate URLs. */
  private static final AntPathMatcher antPathMatcher = new AntPathMatcher();

  @Value("${logging.web.path.exclude:}")
  private Set<String> excludePaths;

  @Override
  protected void doFilterInternal(
      HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
      throws ServletException, IOException {

    // wrappers for request and response to be able to access the body
    CachedHttpServletRequest contentCachingRequestWrapper = new CachedHttpServletRequest(request);
    ContentCachingResponseWrapper contentCachingResponseWrapper =
        new ContentCachingResponseWrapper(response);

    String requestBody =
        getAsStringValue(
            contentCachingRequestWrapper.cachedPayload, request.getCharacterEncoding());
    logRequest(request, requestBody);

    // after log the request continue the filter chain
    filterChain.doFilter(contentCachingRequestWrapper, contentCachingResponseWrapper);

    // in the end log the response
    String responseBody =
        getAsStringValue(
            contentCachingResponseWrapper.getContentAsByteArray(), response.getCharacterEncoding());
    logResponse(request, response, responseBody);

    // needed for the client receive the body
    contentCachingResponseWrapper.copyBodyToResponse();
  }

  @Override
  protected boolean shouldNotFilter(HttpServletRequest request) {

    return excludePaths.parallelStream()
        .anyMatch(path -> antPathMatcher.match(path, request.getRequestURI()));
  }

  /**
   * Converts from byte array to string.
   *
   * @param contentAsByteArray the byte array content
   * @param characterEncoding content encoding
   * @return the corresponding content as string
   */
  private String getAsStringValue(byte[] contentAsByteArray, String characterEncoding) {

    try {
      return new String(
          contentAsByteArray, characterEncoding == null ? "UTF-8" : characterEncoding);
    } catch (UnsupportedEncodingException e) {
      log.error("Error parsing body to string", e);
      return "";
    }
  }

  /**
   * Creates a message that contains the identification of the request.
   *
   * @param prefix the prefix to use in the message
   * @param request the http request object
   * @return a {@link StringBuilder} containing the request identification
   */
  private StringBuilder requestBuilder(String prefix, HttpServletRequest request) {

    StringBuilder msg = new StringBuilder();
    msg.append(prefix);
    msg.append(request.getMethod()).append(' ');
    msg.append(request.getRequestURI());

    String queryString = request.getQueryString();
    if (queryString != null) {
      msg.append('?').append(queryString);
    }

    return msg;
  }

  /**
   * Logs the incoming request.
   *
   * @param request the http request object
   * @param payload the request payload
   */
  private void logRequest(HttpServletRequest request, String payload) {
    Map<String, Object> requestMap = new HashMap<>();

    String client = request.getRemoteAddr();
    if (StringUtils.hasLength(client)) {
      requestMap.put("client", client);
    }
    HttpSession session = request.getSession(false);
    if (session != null) {
      requestMap.put("session", session.getId());
    }
    String user = request.getRemoteUser();
    if (user != null) {
      requestMap.put("user", user);
    }

    HttpHeaders headers = new ServletServerHttpRequest(request).getHeaders();
    Map<String, Object> requestHeadersMap = new HashMap<>(headers);
    requestMap.put("headers", requestHeadersMap);

    if (payload != null) {
      logPayload(requestMap, payload, "Error parsing request payload");
    }

    StringBuilder msg = requestBuilder("HTTP Request: ", request);
    log.info(msg + " - {}", value("request", requestMap));
  }

  /**
   * Method that parses the payload to a json object.
   *
   * @param requestMap the request map
   * @param payload the payload
   * @param errorMsg the error message to use in case of error
   */
  private void logPayload(Map<String, Object> requestMap, String payload, String errorMsg) {
    try {
      JSONParser parser = new JSONParser(JSONParser.MODE_PERMISSIVE);
      requestMap.put("payload", parser.parse(payload));
    } catch (ParseException e) {
      log.error(errorMsg, e);
    }
  }

  /**
   * Logs the response.
   *
   * @param request the http request object
   * @param response the http response object
   * @param payload the response payload
   */
  private void logResponse(
      HttpServletRequest request, HttpServletResponse response, String payload) {

    Map<String, Object> responseMap = new HashMap<>();

    try (ServletServerHttpResponse servletServerHttpResponse =
        new ServletServerHttpResponse(response)) {
      Map<String, Object> responseHeadersMap =
          new HashMap<>(servletServerHttpResponse.getHeaders());
      responseMap.put("headers", responseHeadersMap);
    }

    responseMap.put("status", response.getStatus());

    if (payload != null) {
      logPayload(responseMap, payload, "Error parsing response payload");
    }

    StringBuilder msg = requestBuilder("HTTP Response: ", request);
    log.info(msg + " - {}", value("response", responseMap));
  }

  /*
   * Auxiliary classes
   */

  /** Caches the request body content. */
  private static class CachedHttpServletRequest extends HttpServletRequestWrapper {

    private final byte[] cachedPayload;

    /**
     * Constructor.
     *
     * @param request the http request object
     * @throws IOException if some read or write operation fails
     */
    public CachedHttpServletRequest(HttpServletRequest request) throws IOException {
      super(request);
      InputStream requestInputStream = request.getInputStream();
      this.cachedPayload = StreamUtils.copyToByteArray(requestInputStream);
    }

    @Override
    public ServletInputStream getInputStream() {
      return new CachedServletInputStream(this.cachedPayload);
    }

    @Override
    public BufferedReader getReader() {
      ByteArrayInputStream byteArrayInputStream = new ByteArrayInputStream(this.cachedPayload);
      return new BufferedReader(new InputStreamReader(byteArrayInputStream));
    }
  }

  /** Caches the request input stream. */
  @Slf4j
  private static class CachedServletInputStream extends ServletInputStream {

    private final InputStream cachedInputStream;

    /**
     * Constructor.
     *
     * @param cachedBody the request body in byte array
     */
    public CachedServletInputStream(byte[] cachedBody) {
      this.cachedInputStream = new ByteArrayInputStream(cachedBody);
    }

    @Override
    public boolean isFinished() {
      try {
        return cachedInputStream.available() == 0;
      } catch (IOException exp) {
        log.error(exp.getMessage());
      }
      return false;
    }

    @Override
    public boolean isReady() {
      return true;
    }

    @Override
    public void setReadListener(ReadListener readListener) {
      throw new UnsupportedOperationException();
    }

    @Override
    public int read() throws IOException {
      return cachedInputStream.read();
    }
  }
}
