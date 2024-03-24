package com.trader.coin.crypto.infrastructure.jwt;

import com.auth0.jwt.JWT;
import com.auth0.jwt.algorithms.Algorithm;
import com.trader.coin.common.infrastructure.config.ConfigProperties;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.UUID;

@Component
@RequiredArgsConstructor
public class JwtTokenUtil {

    public String getAuthenticationToken(ConfigProperties configProperties) {
        String accessKey = configProperties.getAccessKey();
        String secretKey = configProperties.getSecretKey();
        String serverUrl = configProperties.getServerUrl();

        Algorithm algorithm = Algorithm.HMAC256(secretKey);
        String jwtToken = JWT.create()
                .withClaim("access_key", accessKey)
                .withClaim("nonce", UUID.randomUUID().toString())
                .sign(algorithm);
        return "Bearer " + jwtToken;
    }

    public String getAuthenticationToken(ConfigProperties configProperties, String queryHash) {
        String accessKey = configProperties.getAccessKey();
        String secretKey = configProperties.getSecretKey();

        Algorithm algorithm = Algorithm.HMAC256(secretKey);
        String jwtToken = JWT.create()
                .withClaim("access_key", accessKey)
                .withClaim("nonce", UUID.randomUUID().toString())
                .withClaim("query_hash", queryHash)
                .withClaim("query_hash_alg", "SHA512")
                .sign(algorithm);
        return "Bearer " + jwtToken;
    }
}
