package br.com.falbot.seplag.backend.seguranca;

import jakarta.servlet.FilterChain;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.web.filter.OncePerRequestFilter;

import java.util.Set;

public class OriginBlockFilter extends OncePerRequestFilter {

    private final Set<String> permitidas;

    public OriginBlockFilter(Set<String> permitidas) {
        this.permitidas = permitidas;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest req, HttpServletResponse res, FilterChain chain) throws java.io.IOException, jakarta.servlet.ServletException {
        String origin = req.getHeader("Origin");

        //Curl/Postman não enviam Origin -> deixa passar
        if (origin != null && !permitidas.contains(origin)) {
            res.setStatus(403);
            return;
        }

        chain.doFilter(req, res);
    }
}
