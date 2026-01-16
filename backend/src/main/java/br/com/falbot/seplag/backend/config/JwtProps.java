package br.com.falbot.seplag.backend.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public class JwtProps {
    @Value("${app.jwt.secret}") public String secret;
    @Value("${app.jwt.issuer}") public String issuer;
    @Value("${app.jwt.access-minutes}") public int accessMinutes;
    @Value("${app.jwt.refresh-days}") public int refreshDays;
}
