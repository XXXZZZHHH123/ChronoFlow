package nus.edu.u.framework.security.factory;

import static nus.edu.u.common.constant.SecurityConstants.REFRESH_TOKEN_COOKIE_NAME;
import static nus.edu.u.common.constant.SecurityConstants.REFRESH_TOKEN_COOKIE_PATH;

/**
 * @author Lu Shuwen
 * @date 2025-09-06
 */
public class ZeroLifeRefreshTokenCookie extends AbstractCookieFactory {

    public static final String TOKEN_NAME = REFRESH_TOKEN_COOKIE_NAME;

    private static final String PATH = REFRESH_TOKEN_COOKIE_PATH;

    public ZeroLifeRefreshTokenCookie(boolean httpOnly, boolean secure) {
        super(httpOnly, secure, PATH, 0);
    }

    @Override
    public String getCookieName() {
        return TOKEN_NAME;
    }
}
