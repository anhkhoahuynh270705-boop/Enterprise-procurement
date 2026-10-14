package com.example.shopping.auth.service.impl;

import com.example.shopping.common.enums.ErrorEnum;
import com.example.shopping.auth.service.AuthorizationTransactions;
import com.example.shopping.auth.model.AuthorizationTransaction;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.time.Clock;
import java.util.Base64;
import java.util.HashMap;
import java.util.Map;
import org.springframework.stereotype.Service;

import lombok.AccessLevel;
import lombok.experimental.FieldDefaults;

@Service
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class AuthorizationTransactionsImpl implements AuthorizationTransactions {
    SecureRandom random = new SecureRandom();
    Clock clock;
    Map<String, AuthorizationTransaction> pending = new HashMap<>();

    public AuthorizationTransactionsImpl() {
        this(Clock.systemUTC());
    }

    AuthorizationTransactionsImpl(Clock clock) { 
        this.clock = clock; 
    }

    public synchronized AuthorizationTransaction create() {
        pending.values().removeIf(tx -> !tx.expiresAt().isAfter(clock.instant()));
        if (pending.size() >= 10000) 
            throw ErrorEnum.AUTH_PENDING_LIMIT_REACHED.ex();
        var tx = new AuthorizationTransaction(secret(), secret(), secret(), secret(), clock.instant().plusSeconds(600));
        pending.put(tx.state(), tx);
        return tx;
    }
    
    public synchronized AuthorizationTransaction consume(String state, String browserToken) {
        var tx = state == null ? null : pending.get(state);
        if (tx == null || browserToken == null || !MessageDigest.isEqual(
                tx.browserToken().getBytes(StandardCharsets.UTF_8), 
                browserToken.getBytes(StandardCharsets.UTF_8)))
            throw ErrorEnum.INVALID_LOGIN_TRANSACTION.ex();
        pending.remove(state);
        if (!tx.expiresAt().isAfter(clock.instant())) 
            throw ErrorEnum.EXPIRED_LOGIN_TRANSACTION.ex();
        return tx;
    }

    private String secret() {
        byte[] bytes = new byte[32]; 
        random.nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }
}
