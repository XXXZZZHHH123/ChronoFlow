package nus.edu.u.framework.security.factory;

import static nus.edu.u.common.constant.SecurityConstants.REFRESH_TOKEN_COOKIE_NAME;
import static nus.edu.u.common.constant.SecurityConstants.REFRESH_TOKEN_COOKIE_PATH;

/**
 * @author Lu Shuwen
 * @date 2025-09-06
 */
public class LongLifeRefreshTokenCookie extends AbstractCookieFactory {

    private static final String COOKIE_NAME = REFRESH_TOKEN_COOKIE_NAME;

    private static final String PATH = REFRESH_TOKEN_COOKIE_PATH;

    public LongLifeRefreshTokenCookie(boolean httpOnly, boolean secure, int maxAge) {
        super(httpOnly, secure, PATH, maxAge);
    }

    @Override
    protected String getCookieName() {
        return REFRESH_TOKEN_COOKIE_NAME;
    }
}
