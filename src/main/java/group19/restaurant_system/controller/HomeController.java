package group19.restaurant_system.controller;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class HomeController {

    @GetMapping("/")
    public String home() {
        return """
                <!doctype html>
                <html lang="zh-Hant">
                <head>
                    <meta charset="UTF-8">
                    <meta http-equiv="refresh" content="0; url=/index.html">
                    <title>Redirecting...</title>
                </head>
                <body>
                    <p>Redirecting to <a href="/index.html">frontend dashboard</a>...</p>
                </body>
                </html>
                """;
    }
}
