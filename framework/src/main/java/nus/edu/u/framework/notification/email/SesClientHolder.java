package nus.edu.u.framework.notification.email;

import software.amazon.awssdk.services.sesv2.SesV2Client;

public final class SesClientHolder {
    private static volatile SesV2Client INSTANCE;

    private SesClientHolder() {}

    public static void init(SesV2Client client) { INSTANCE = client; }

    public static SesV2Client get() {
        if (INSTANCE == null) {
            throw new IllegalStateException("SesV2Client not initialized yet");
        }
        return INSTANCE;
    }
}
