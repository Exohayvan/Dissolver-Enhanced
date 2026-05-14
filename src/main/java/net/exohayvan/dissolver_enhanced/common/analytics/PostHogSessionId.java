package net.exohayvan.dissolver_enhanced.common.analytics;

import java.security.SecureRandom;
import java.util.UUID;

public final class PostHogSessionId {
    private static final SecureRandom RANDOM = new SecureRandom();

    private PostHogSessionId() {
    }

    public static String create() {
        long timestamp = System.currentTimeMillis() & 0x0000FFFFFFFFFFFFL;
        long randomA = RANDOM.nextLong() & 0xFFFL;
        long randomB = RANDOM.nextLong() & 0x3FFFFFFFFFFFFFFFL;

        long mostSignificantBits = (timestamp << 16) | 0x7000L | randomA;
        long leastSignificantBits = 0x8000000000000000L | randomB;

        return new UUID(mostSignificantBits, leastSignificantBits).toString();
    }
}
