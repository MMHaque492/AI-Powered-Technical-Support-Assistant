package com.mizan.support;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.*;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.core.userdetails.*;
import org.springframework.security.provisioning.InMemoryUserDetailsManager;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.util.matcher.AntPathRequestMatcher;
@Configuration
class SecurityConfig {
    @Bean UserDetailsService users(@Value("${app.username}") String user, @Value("${app.password}") String password) {
        return new InMemoryUserDetailsManager(User.withUsername(user)
            .password(new BCryptPasswordEncoder().encode(password)).roles("SUPPORT").build());
    }
    @Bean BCryptPasswordEncoder encoder() { return new BCryptPasswordEncoder(); }
    @Bean SecurityFilterChain security(HttpSecurity http) throws Exception {
        return http.authorizeHttpRequests(a -> a.requestMatchers("/error").permitAll().anyRequest().authenticated())
            .formLogin(f -> f.defaultSuccessUrl("/", true).permitAll())
            .httpBasic(b -> {})
            .exceptionHandling(e -> e.defaultAuthenticationEntryPointFor((req,res,ex) -> res.sendError(401),
                new AntPathRequestMatcher("/api/**")))
            .headers(h -> h.contentSecurityPolicy(c -> c.policyDirectives(
                "default-src 'self'; script-src 'self'; style-src 'self'; img-src 'self' data:; frame-ancestors 'none'; base-uri 'self'; form-action 'self'")))
            .build();
    }
}
