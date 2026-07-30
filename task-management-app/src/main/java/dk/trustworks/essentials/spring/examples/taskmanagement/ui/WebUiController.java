package dk.trustworks.essentials.spring.examples.taskmanagement.ui;

import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

@Controller
public class WebUiController {
    @GetMapping("/")
    public String index(Authentication authentication, Model model) {
        model.addAttribute("currentUser", authentication != null ? authentication.getName() : "");
        return "index";
    }

    @GetMapping("/login")
    public String login() {
        return "login";
    }

    @GetMapping("/tasks/{taskId}/view")
    public String taskView(@PathVariable String taskId, Authentication authentication, Model model) {
        model.addAttribute("taskId", taskId);
        model.addAttribute("currentUser", authentication != null ? authentication.getName() : "");
        return "task-view";
    }
}
