package shop.serve.ShopNServe.service;

import org.springframework.stereotype.Service;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Base64;

@Service
public class JwtService {
    private static final String SECRET = "CHANGE_ME_TO_A_LONG_RANDOM_SECRET_32CHARS_MIN";
    private static final String HMAC_ALG = "HmacSHA256";

    private static final Base64.Encoder B64_URL_ENC = Base64.getUrlEncoder().withoutPadding();
    private static final Base64.Decoder B64_URL_DEC = Base64.getUrlDecoder();

    private static final long TTL_SECONDS = 60 * 60;

    public String generate(String username) {
        long iat = Instant.now().getEpochSecond();
        long exp = iat + TTL_SECONDS;

        String headerJson = "{\"alg\":\"HS256\",\"typ\":\"JWT\"}";
        String payloadJson = "{\"sub\":\"" + escape(username) + "\",\"iat\":" + iat + ",\"exp\":" + exp + "}";

        String header = B64_URL_ENC.encodeToString(headerJson.getBytes(StandardCharsets.UTF_8));
        String payload = B64_URL_ENC.encodeToString(payloadJson.getBytes(StandardCharsets.UTF_8));

        String signingInput = header + "." + payload;
        String sig = hmacSha256Base64Url(signingInput);

        return signingInput + "." + sig;
    }

    public boolean validate(String authHeaderOrToken) {
        String token = extractToken(authHeaderOrToken);
        //System.out.println("[JWT] validate() input=" + authHeaderOrToken);
        //System.out.println("[JWT] extracted token=" + token);

        if (token == null) return false;

        String[] parts = token.split("\\.");
        //System.out.println("[JWT] parts=" + parts.length);
        if (parts.length != 3) return false;

        String signingInput = parts[0] + "." + parts[1];
        String expectedSig = hmacSha256Base64Url(signingInput);

        //System.out.println("[JWT] expectedSig=" + expectedSig);
        //System.out.println("[JWT] actualSig  =" + parts[2]);

        if (!safeEquals(expectedSig, parts[2])) {
            System.out.println("[JWT] signature mismatch");
            return false;
        }

        String payloadJson = new String(B64_URL_DEC.decode(parts[1]), StandardCharsets.UTF_8);
        long exp = readLongClaim(payloadJson, "exp");
        long now = Instant.now().getEpochSecond();

        System.out.println("[JWT] payload=" + payloadJson);
        System.out.println("[JWT] exp=" + exp + " now=" + now);

        boolean ok = exp > now;
        System.out.println("[JWT] exp check=" + ok);
        return ok;
    }
    public String subject(String authHeaderOrToken) {
        String token = extractToken(authHeaderOrToken);
        if (token == null) return null;
        String[] parts = token.split("\\.");
        if (parts.length != 3) return null;

        String payloadJson = new String(B64_URL_DEC.decode(parts[1]), StandardCharsets.UTF_8);
        return readStringClaim(payloadJson, "sub");
    }

    private static String extractToken(String authHeaderOrToken) {
        if (authHeaderOrToken == null) return null;
        String s = authHeaderOrToken.trim();
        if (s.isEmpty()) return null;

        if (s.regionMatches(true, 0, "Bearer ", 0, 7)) {
            s = s.substring(7).trim();
        }
        return s.isEmpty() ? null : s;
    }

    private static String hmacSha256Base64Url(String data) {
        try {
            Mac mac = Mac.getInstance(HMAC_ALG);
            mac.init(new SecretKeySpec(SECRET.getBytes(StandardCharsets.UTF_8), HMAC_ALG));
            byte[] sig = mac.doFinal(data.getBytes(StandardCharsets.UTF_8));
            return B64_URL_ENC.encodeToString(sig);
        } catch (Exception e) {
            throw new IllegalStateException("JWT signing failed", e);
        }
    }

    private static boolean safeEquals(String a, String b) {
        if (a == null || b == null) return false;
        if (a.length() != b.length()) return false;
        int r = 0;
        for (int i = 0; i < a.length(); i++) r |= a.charAt(i) ^ b.charAt(i);
        return r == 0;
    }

    private static long readLongClaim(String json, String key) {
        String needle = "\"" + key + "\":";
        int i = json.indexOf(needle);
        if (i < 0) return 0;
        i += needle.length();

        int j = i;
        while (j < json.length() && Character.isWhitespace(json.charAt(j))) j++;

        int start = j;
        while (j < json.length() && Character.isDigit(json.charAt(j))) j++;

        if (start == j) return 0;
        return Long.parseLong(json.substring(start, j));
    }

    private static String readStringClaim(String json, String key) {
        String needle = "\"" + key + "\":\"";
        int i = json.indexOf(needle);
        if (i < 0) return null;
        i += needle.length();
        int j = json.indexOf("\"", i);
        if (j < 0) return null;
        return unescape(json.substring(i, j));
    }

    private static String escape(String s) {
        return s.replace("\\", "\\\\").replace("\"", "\\\"");
    }

    private static String unescape(String s) {
        return s.replace("\\\"", "\"").replace("\\\\", "\\");
    }
}