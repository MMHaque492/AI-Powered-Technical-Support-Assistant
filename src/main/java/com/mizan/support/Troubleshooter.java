package com.mizan.support;
import org.springframework.stereotype.Component;
import java.util.Locale;
@Component
class Troubleshooter {
    String category(String text) {
        String t=text.toLowerCase(Locale.ROOT);
        if (t.matches("(?s).*(mysql|database|jdbc|sql|postgres).*$")) return "DATABASE";
        if (t.matches("(?s).*(network|dns|connection|wifi|timeout).*$")) return "NETWORK";
        if (t.matches("(?s).*(401|403|login|password|permission|unauthorized).*$")) return "AUTHENTICATION";
        if (t.matches("(?s).*(memory|slow|cpu|performance|latency).*$")) return "PERFORMANCE";
        return "APPLICATION";
    }
    String suggest(String category) {
        String steps=switch(category) {
            case "DATABASE" -> "1. Confirm the database service is running and the host, port and database name are correct.\n2. Test connectivity from the application host.\n3. Verify the database user has the required permissions; never paste credentials into logs.\n4. Check JDBC driver compatibility and the first underlying SQL error.\n5. Review connection pool usage before changing pool limits.";
            case "NETWORK" -> "1. Check whether the issue affects one device or all clients.\n2. Confirm the hostname resolves to the expected IP address.\n3. Check the service port, proxy and firewall rules.\n4. Compare a request from the service host with one from the client.\n5. Record timestamps and sanitized error messages for escalation.";
            case "AUTHENTICATION" -> "1. Check the response code: 401 usually means missing or invalid authentication; 403 usually means insufficient permission or a CSRF failure.\n2. Check session or token expiry and the system clock.\n3. Verify the user's assigned roles.\n4. For browser writes, confirm the CSRF token and session cookie are sent.\n5. Use the supported password recovery flow; do not disable security controls.";
            case "PERFORMANCE" -> "1. Measure response time and identify the slow operation.\n2. Inspect CPU, memory and database connection usage.\n3. Look for slow queries and repeated remote calls.\n4. Compare with a recent known-good deployment.\n5. Change one setting at a time and remeasure.";
            default -> "1. Capture the exact error and smallest reproducible example.\n2. Check application logs near the failure timestamp.\n3. Verify runtime versions and required configuration.\n4. Compare recent code or configuration changes.\n5. Reproduce in a test environment before applying a fix.";
        };
        return "Offline checklist — not an AI diagnosis\n\n"+steps+"\n\nVerify the result by repeating the original action. If it still fails, escalate with sanitized logs and reproduction steps. Back up data before any destructive change.";
    }
}
