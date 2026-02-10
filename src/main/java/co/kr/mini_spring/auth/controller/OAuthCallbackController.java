package co.kr.mini_spring.auth.controller;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class OAuthCallbackController {

    @GetMapping("/oauth/callback")
    public String oauthCallback() {
        return "oauth-callback";
    }
}
