package dk.trustworks.essentials.spring.examples.taskmanagement.ui;

import dk.trustworks.essentials.reactive.command.CommandBus;
import dk.trustworks.essentials.spring.examples.taskmanagement.commands.CreateUser;
import dk.trustworks.essentials.spring.examples.taskmanagement.commands.RegisterUser;
import dk.trustworks.essentials.spring.examples.taskmanagement.domain.UserId;
import dk.trustworks.essentials.spring.examples.taskmanagement.readmodel.UserReadModels;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Controller;
import org.springframework.validation.BindingResult;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.time.Instant;

@Controller
@Validated
public class RegistrationController {
    private static final Logger log = LoggerFactory.getLogger(RegistrationController.class);
    private final CommandBus commandBus;
    private final UserReadModels userReadModels;
    private final PasswordEncoder passwordEncoder;

    public RegistrationController(CommandBus commandBus,
                                  UserReadModels userReadModels,
                                  PasswordEncoder passwordEncoder) {
        this.commandBus = commandBus;
        this.userReadModels = userReadModels;
        this.passwordEncoder = passwordEncoder;
    }

    @GetMapping("/register")
    public String registerPage(@ModelAttribute("registration") RegistrationForm ignored) {
        return "register";
    }

    @PostMapping("/register")
    public String register(@Valid @ModelAttribute("registration") RegistrationForm form,
                           BindingResult bindingResult,
                           RedirectAttributes redirectAttributes) {
        if (bindingResult.hasErrors()) {
            redirectAttributes.addAttribute("error", "invalid");
            return "redirect:/register";
        }

        var normalizedUserId = normalize(form.getUserId());
        var normalizedDisplayName = normalize(form.getDisplayName());
        if (normalizedUserId == null || normalizedDisplayName == null || form.getEmail() == null || form.getPassword() == null) {
            redirectAttributes.addAttribute("error", "invalid");
            return "redirect:/register";
        }

        if (userReadModels.exists(normalizedUserId)) {
            redirectAttributes.addAttribute("error", "exists");
            return "redirect:/register";
        }

        var now = Instant.now();
        var userId = UserId.of(normalizedUserId);
        log.info("Registration requested for userId='{}'", normalizedUserId);
        commandBus.send(new CreateUser(userId, normalizedDisplayName, now));
        if (!waitFor(() -> userReadModels.exists(normalizedUserId), 3000)) {
            log.warn("CreateUser projection not available in time for userId='{}'", normalizedUserId);
            redirectAttributes.addAttribute("error", "processing");
            return "redirect:/register";
        }

        commandBus.send(new RegisterUser(userId,
                                         form.getEmail(),
                                         passwordEncoder.encode(form.getPassword()),
                                         now.plusSeconds(1)));
        if (!waitFor(() -> userReadModels.findAuthByUserId(normalizedUserId)
                                         .filter(view -> view.registered() && view.passwordHash() != null && !view.passwordHash().isBlank())
                                         .isPresent(),
                     3000)) {
            log.warn("RegisterUser projection not available in time for userId='{}'", normalizedUserId);
            redirectAttributes.addAttribute("error", "processing");
            return "redirect:/register";
        }

        redirectAttributes.addAttribute("registered", "1");
        redirectAttributes.addAttribute("username", normalizedUserId);
        log.info("Registration completed for userId='{}'", normalizedUserId);
        return "redirect:/login";
    }

    private static String normalize(String value) {
        if (value == null) {
            return null;
        }
        var normalized = value.trim();
        return normalized.isEmpty() ? null : normalized;
    }

    private static boolean waitFor(java.util.function.BooleanSupplier condition, long timeoutMs) {
        var deadline = System.currentTimeMillis() + timeoutMs;
        while (System.currentTimeMillis() < deadline) {
            if (condition.getAsBoolean()) {
                return true;
            }
            try {
                Thread.sleep(40);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                return false;
            }
        }
        return condition.getAsBoolean();
    }

    public static class RegistrationForm {
        @NotBlank
        @Size(min = 3, max = 80)
        private String userId;
        @NotBlank
        @Size(min = 2, max = 120)
        private String displayName;
        @NotBlank
        @Email
        private String email;
        @NotBlank
        @Size(min = 8, max = 128)
        private String password;

        public String getUserId() {
            return userId;
        }

        public void setUserId(String userId) {
            this.userId = userId;
        }

        public String getDisplayName() {
            return displayName;
        }

        public void setDisplayName(String displayName) {
            this.displayName = displayName;
        }

        public String getEmail() {
            return email;
        }

        public void setEmail(String email) {
            this.email = email;
        }

        public String getPassword() {
            return password;
        }

        public void setPassword(String password) {
            this.password = password;
        }
    }
}
