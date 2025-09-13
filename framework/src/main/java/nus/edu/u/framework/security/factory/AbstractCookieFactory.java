package nus.edu.u.framework.security.factory;

import jakarta.servlet.http.Cookie;

/**
 * @author Lu Shuwen
 * @date 2025-09-06
 */
public abstract class AbstractCookieFactory {

  protected final boolean httpOnly;

  protected final boolean secure;

  protected final String path;

  protected final int maxAge;

  protected AbstractCookieFactory(boolean httpOnly, boolean secure, String path, int maxAge) {
    this.httpOnly = httpOnly;
    this.secure = secure;
    this.path = path;
    this.maxAge = maxAge;
  }

  public Cookie createCookie(String value) {
    Cookie cookie = new Cookie(getCookieName(), value);
    cookie.setHttpOnly(httpOnly);
    cookie.setSecure(secure);
    cookie.setPath(path);
    cookie.setMaxAge(maxAge);
    return cookie;
  }

  protected abstract String getCookieName();
}
