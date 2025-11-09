package com.example.ehe_server.service.user;

import com.example.ehe_server.entity.User;
import com.example.ehe_server.repository.AdminRepository;
import com.example.ehe_server.service.audit.UserContextService;
import com.example.ehe_server.service.intf.auth.CookieServiceInterface;
import com.example.ehe_server.service.intf.auth.JwtRefreshTokenServiceInterface;
import com.example.ehe_server.service.intf.auth.JwtTokenGeneratorInterface;
import com.example.ehe_server.service.intf.user.JwtTokenRenewalServiceInterface;
import com.example.ehe_server.service.intf.log.LoggingServiceInterface;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.crypto.bcrypt.BCrypt;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class JwtTokenRenewalService implements JwtTokenRenewalServiceInterface {

    private final AdminRepository adminRepository;
    private final JwtTokenGeneratorInterface jwtTokenGenerator;
    private final CookieServiceInterface cookieService;
    private final LoggingServiceInterface loggingService;
    private final UserContextService userContextService;
    private final JwtRefreshTokenServiceInterface jwtRefreshTokenService;

    @Value("${jwt.refresh.expiration.time}")
    private long jwtRefreshExpirationTime;

    @Value("${jwt.refresh.max.expiration.time}")
    private long jwtRefreshTokenMaxExpireTime;

    public JwtTokenRenewalService(
            AdminRepository adminRepository,
            JwtTokenGeneratorInterface jwtTokenGenerator,
            CookieServiceInterface cookieService,
            LoggingServiceInterface loggingService,
            UserContextService userContextService,
            JwtRefreshTokenServiceInterface jwtRefreshTokenService) {
        this.adminRepository = adminRepository;
        this.jwtTokenGenerator = jwtTokenGenerator;
        this.cookieService = cookieService;
        this.loggingService = loggingService;
        this.userContextService = userContextService;
        this.jwtRefreshTokenService = jwtRefreshTokenService;
    }

    @Override
    @Transactional(timeout = 10)
    public void renewToken(Long userId, HttpServletRequest request, HttpServletResponse response) {
        long startTime = System.currentTimeMillis();
        System.out.println("\n=== TOKEN RENEWAL START === Thread: " + Thread.currentThread().getName() + " Time: " + startTime);

        try {
            // Step 1: Get current user
            System.out.println("[STEP 1] Getting current user from context...");
            long step1Start = System.currentTimeMillis();
            User user = userContextService.getCurrentHumanUser();
            long step1End = System.currentTimeMillis();
            System.out.println("[STEP 1] COMPLETED in " + (step1End - step1Start) + "ms - User ID: " + (user != null ? user.getUserId() : "NULL"));

            // Step 2: Check if admin
            System.out.println("[STEP 2] Checking if user is admin...");
            long step2Start = System.currentTimeMillis();
            boolean isAdmin = adminRepository.existsByAdminId(user.getUserId());
            long step2End = System.currentTimeMillis();
            System.out.println("[STEP 2] COMPLETED in " + (step2End - step2Start) + "ms - Is Admin: " + isAdmin);

            // Step 3: Determine role
            System.out.println("[STEP 3] Determining user role...");
            String role = "USER";
            if (isAdmin) {
                role = "ADMIN";
            }
            System.out.println("[STEP 3] COMPLETED - Role: " + role);

            // Step 4: Update audit context
            System.out.println("[STEP 4] Updating audit context...");
            long step4Start = System.currentTimeMillis();
            userContextService.setUser(String.valueOf(user.getUserId()), role);
            long step4End = System.currentTimeMillis();
            System.out.println("[STEP 4] COMPLETED in " + (step4End - step4Start) + "ms");

            // Step 5: Remove old refresh token
            System.out.println("[STEP 5] Removing old refresh token...");
            long step5Start = System.currentTimeMillis();
            if (request.getCookies() != null) {
                for (Cookie cookie : request.getCookies()) {
                    if ("jwt_refresh_token".equals(cookie.getName())) {
                        String refreshToken = cookie.getValue();
                        System.out.println("[STEP 5] Found refresh token cookie, removing from DB...");
                        jwtRefreshTokenService.removeRefreshTokenByToken(refreshToken);
                        System.out.println("[STEP 5] Refresh token removed from DB");
                        break;
                    }
                }
            } else {
                System.out.println("[STEP 5] No cookies found in request");
            }
            long step5End = System.currentTimeMillis();
            System.out.println("[STEP 5] COMPLETED in " + (step5End - step5Start) + "ms");

            // Step 6: Generate new tokens
            System.out.println("[STEP 6] Generating new access token...");
            long step6Start = System.currentTimeMillis();
            String jwtAccessToken = jwtTokenGenerator.generateAccessToken(Long.valueOf(user.getUserId()), role);
            long step6Mid = System.currentTimeMillis();
            System.out.println("[STEP 6a] Access token generated in " + (step6Mid - step6Start) + "ms");

            System.out.println("[STEP 6] Generating new refresh token...");
            String jwtRefreshToken = jwtTokenGenerator.generateRefreshToken(Long.valueOf(user.getUserId()), role);
            long step6End = System.currentTimeMillis();
            System.out.println("[STEP 6b] Refresh token generated in " + (step6End - step6Mid) + "ms");
            System.out.println("[STEP 6] COMPLETED - Total token generation: " + (step6End - step6Start) + "ms");

            // Step 7: Add cookies to response
            System.out.println("[STEP 7] Adding JWT cookies to response...");
            long step7Start = System.currentTimeMillis();
            cookieService.addJwtAccessCookie(jwtAccessToken, response);
            cookieService.addJwtRefreshCookie(jwtRefreshToken, response);
            long step7End = System.currentTimeMillis();
            System.out.println("[STEP 7] COMPLETED in " + (step7End - step7Start) + "ms");

            // Step 8: Hash the refresh token
            System.out.println("[STEP 8] Hashing refresh token with BCrypt...");
            long step8Start = System.currentTimeMillis();
            String refreshTokenHash = BCrypt.hashpw(jwtRefreshToken, BCrypt.gensalt());
            long step8End = System.currentTimeMillis();
            System.out.println("[STEP 8] COMPLETED in " + (step8End - step8Start) + "ms");

            // Step 9: Save refresh token to database
            System.out.println("[STEP 9] Saving refresh token to database...");
            long step9Start = System.currentTimeMillis();
            jwtRefreshTokenService.saveRefreshToken(
                    user,
                    refreshTokenHash,
                    jwtRefreshExpirationTime,
                    jwtRefreshTokenMaxExpireTime
            );
            long step9End = System.currentTimeMillis();
            System.out.println("[STEP 9] COMPLETED in " + (step9End - step9Start) + "ms");

            // Step 10: Log success
            System.out.println("[STEP 10] Logging success...");
            long step10Start = System.currentTimeMillis();
            loggingService.logAction("JWT token renewed successfully");
            long step10End = System.currentTimeMillis();
            System.out.println("[STEP 10] COMPLETED in " + (step10End - step10Start) + "ms");

            long endTime = System.currentTimeMillis();
            long totalTime = endTime - startTime;
            System.out.println("=== TOKEN RENEWAL COMPLETED === Total time: " + totalTime + "ms\n");

        } catch (Exception e) {
            long endTime = System.currentTimeMillis();
            long totalTime = endTime - startTime;
            System.err.println("=== TOKEN RENEWAL FAILED === Total time before failure: " + totalTime + "ms");
            System.err.println("Error: " + e.getMessage());
            e.printStackTrace();
            throw e;
        }
    }
}