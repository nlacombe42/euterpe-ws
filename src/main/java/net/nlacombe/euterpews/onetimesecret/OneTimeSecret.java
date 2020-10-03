package net.nlacombe.euterpews.onetimesecret;

import java.time.Instant;
import java.util.UUID;

public class OneTimeSecret<T> {

    private final UUID secretId;
    private final T secret;
    private final Instant expiry;

    public OneTimeSecret(UUID secretId, T secret, Instant expiry) {
        this.secretId = secretId;
        this.secret = secret;
        this.expiry = expiry;
    }

    public UUID getSecretId() {
        return secretId;
    }

    public T getSecret() {
        return secret;
    }

    public Instant getExpiry() {
        return expiry;
    }
}
