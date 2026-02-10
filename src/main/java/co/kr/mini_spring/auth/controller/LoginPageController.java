package co.kr.mini_spring.auth.controller;

import org.springframework.security.oauth2.client.registration.ClientRegistration;
import org.springframework.security.oauth2.client.registration.ClientRegistrationRepository;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

import java.util.ArrayList;
import java.util.List;

@Controller
public class LoginPageController {

    private static final String AUTHORIZATION_BASE_URI = "/oauth2/authorization";

    private final ClientRegistrationRepository clientRegistrationRepository;

    public LoginPageController(ClientRegistrationRepository clientRegistrationRepository) {
        this.clientRegistrationRepository = clientRegistrationRepository;
    }

    @GetMapping("/login")
    public String loginPage(Model model) {
        model.addAttribute("providers", getProviders());
        return "login";
    }

    @GetMapping("/login/error")
    public String loginError() {
        return "login-error";
    }

    @SuppressWarnings("unchecked")
    private List<LoginProvider> getProviders() {
        if (!(clientRegistrationRepository instanceof Iterable)) {
            return List.of();
        }

        List<LoginProvider> providers = new ArrayList<>();
        for (ClientRegistration registration : (Iterable<ClientRegistration>) clientRegistrationRepository) {
            providers.add(new LoginProvider(
                    registration.getRegistrationId(),
                    registration.getClientName(),
                    AUTHORIZATION_BASE_URI + "/" + registration.getRegistrationId()
            ));
        }
        return providers;
    }

    public record LoginProvider(String id, String name, String authorizationUrl) {}
}
