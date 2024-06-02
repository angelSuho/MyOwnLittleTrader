package com.trader.coin.crypto.infrastructure.jwt;

import com.auth0.jwt.JWT;
import com.auth0.jwt.algorithms.Algorithm;
import com.trader.coin.common.infrastructure.config.APIProperties;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.UUID;

@Component
@RequiredArgsConstructor
public class JwtTokenUtil {

    public String getAuthenticationToken(APIProperties api) {
        String accessKey;
        String secretKey;
        if (api.getCrypto_market().equals("upbit")) {
            accessKey = api.getUpbit().getAccessKey();
            secretKey = api.getUpbit().getSecretKey();
        } else {
            accessKey = api.getBinance().getAccessKey();
            secretKey = api.getBinance().getSecretKey();
        }

        Algorithm algorithm = Algorithm.HMAC256(secretKey);
        String jwtToken = JWT.create()
                .withClaim("access_key", accessKey)
                .withClaim("nonce", UUID.randomUUID().toString())
                .sign(algorithm);
        return "Bearer " + jwtToken;
    }

    public String getAuthenticationToken(APIProperties apiProperties, String queryHash) {
        String accessKey;
        String secretKey;
        if (apiProperties.getCrypto_market().equals("upbit")) {
            accessKey = apiProperties.getUpbit().getAccessKey();
            secretKey = apiProperties.getUpbit().getSecretKey();
        } else {
            accessKey = apiProperties.getBinance().getAccessKey();
            secretKey = apiProperties.getBinance().getSecretKey();
        }

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
