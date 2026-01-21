package br.com.falbot.seplag.backend.seguranca;

import br.com.falbot.seplag.backend.seguranca.JwtAuthFilter.UsuarioAutenticado;
import jakarta.servlet.FilterChain;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.filter.OncePerRequestFilter;

import java.time.Instant;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class RateLimitFilter extends OncePerRequestFilter {

    private static final int LIMITE = 10;
    private static final long JANELA_MS = 60_000;

    private static class Estado {
        volatile long janelaInicio;
        volatile int contador;
    }

    private final Map<String, Estado> estados = new ConcurrentHashMap<>();

    private boolean isWhitelisted(String path) {
    return path.startsWith("/swagger-ui")
        || path.equals("/swagger-ui.html")
        || path.startsWith("/api-docs")
        || path.startsWith("/v3/api-docs")
        || path.startsWith("/actuator")
        || path.startsWith("/ws");
    }

    @Override
    protected void doFilterInternal(HttpServletRequest req, HttpServletResponse res, FilterChain chain) throws java.io.IOException, jakarta.servlet.ServletException {

        String path = req.getRequestURI();
        if (isWhitelisted(path)) {
            chain.doFilter(req, res);
            return;
        }

        String chave = chaveDoCliente(req);

        long agora = Instant.now().toEpochMilli();
        Estado st = estados.computeIfAbsent(chave, k -> {
            Estado e = new Estado();
            e.janelaInicio = agora;
            e.contador = 0;
            return e;
        });

        synchronized (st) {
            if (agora - st.janelaInicio >= JANELA_MS) {
                st.janelaInicio = agora;
                st.contador = 0;
            }
            st.contador++;
            if (st.contador > LIMITE) {
                res.setStatus(429);
                return;
            }
        }

        chain.doFilter(req, res);
    }

    private String chaveDoCliente(HttpServletRequest req) {
        var auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth != null && auth.getPrincipal() instanceof UsuarioAutenticado u) {
            return "U:" + u.id();
        }
        return "IP:" + req.getRemoteAddr();
    }
}
