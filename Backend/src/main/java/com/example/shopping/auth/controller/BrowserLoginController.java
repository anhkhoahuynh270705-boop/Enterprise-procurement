package com.example.shopping.auth.controller;

import com.example.shopping.auth.service.BrowserLoginService;
import java.net.URI;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class BrowserLoginController {

    private final BrowserLoginService login;

    @Value("${auth.cookie.secure:true}") 
    private boolean secureCookie = true;
    @Value("${auth.oidc.frontend-callback-uri:http://localhost:4200/auth/callback}") 
    private String frontendCallback;
    @Value("${auth.oidc.frontend-login-uri:http://localhost:4200/login}") 
    private String frontendLogin;
    
    @GetMapping("/authorize")
    public ResponseEntity<Void> authorize() {
        var start = login.begin();
        return redirect(start.url()).header(HttpHeaders.SET_COOKIE,
            cookie("oidc_login", start.browserToken(), "/api/auth/callback", 600)).build();
    }
    @GetMapping("/callback")
    public ResponseEntity<Void> callback(@RequestParam(required = false) String state,
            @RequestParam(required = false) String code, @RequestParam(required = false) String error,
            @CookieValue(value = "oidc_login", required = false) String browserToken) {
        String clear = cookie("oidc_login", "", "/api/auth/callback", 0);
        try {
            var tokens = login.complete(state, browserToken, code, error);
            long lifetime = tokens.getRefreshExpiresIn() != null && tokens.getRefreshExpiresIn() > 0 ? tokens.getRefreshExpiresIn() : -1;
            return redirect(frontendCallback).header(HttpHeaders.SET_COOKIE, clear,
                cookie("refresh_token", tokens.getRefreshToken(), "/api/auth", lifetime)).build();
        } catch (RuntimeException ex) {
            return redirect(frontendLogin + "?error=authentication_failed")
                .header(HttpHeaders.SET_COOKIE, clear).build();
        }
    }

    private ResponseEntity.BodyBuilder redirect(String location) {
        return ResponseEntity.status(HttpStatus.FOUND).location(URI.create(location))
            .header(HttpHeaders.CACHE_CONTROL, "no-store").header("Referrer-Policy", "no-referrer");
    }
    
    private String cookie(String name, String value, String path, long age) {
        return ResponseCookie.from(name, value).httpOnly(true).secure(secureCookie).sameSite("Lax")
            .path(path).maxAge(age).build().toString();
    }
}