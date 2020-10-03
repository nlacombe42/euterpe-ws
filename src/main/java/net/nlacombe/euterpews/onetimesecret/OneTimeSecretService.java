package net.nlacombe.euterpews.onetimesecret;

import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.Instant;
import java.util.HashMap;
import java.util.Map;
import java.util.Timer;
import java.util.TimerTask;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
public class OneTimeSecretService {

    private final Map<UUID, OneTimeSecret<?>> oneTimeSecretById;

    public OneTimeSecretService() {
        oneTimeSecretById = new HashMap<>();

        new Timer().schedule(new TimerTask() {
            @Override
            public void run() {
                var expiredSecrets = oneTimeSecretById.values().stream()
                    .filter(secret -> Instant.now().isAfter(secret.getExpiry()))
                    .collect(Collectors.toSet());

                expiredSecrets.forEach(expiredSecret -> oneTimeSecretById.remove(expiredSecret.getSecretId()));
            }
        }, 0, 1000);
    }

    public <T> OneTimeSecret<T> createOneTimeSecret(T secret) {
        var oneTimeSecret = new OneTimeSecret<T>(UUID.randomUUID(), secret, Instant.now().plus(Duration.ofMinutes(1)));

        oneTimeSecretById.put(oneTimeSecret.getSecretId(), oneTimeSecret);

        return oneTimeSecret;
    }

    public <T> T getAndRemoveOneTimeSecret(UUID secretId) {
        if (!oneTimeSecretById.containsKey(secretId))
            return null;

        var oneTimeSecret = oneTimeSecretById.get(secretId);

        oneTimeSecretById.remove(oneTimeSecret.getSecretId());

        return (T) oneTimeSecret.getSecret();
    }
}
