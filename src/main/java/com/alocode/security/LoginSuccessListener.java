package com.alocode.security;

import com.alocode.repository.UsuarioRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationListener;
import org.springframework.security.authentication.event.AuthenticationSuccessEvent;
import org.springframework.stereotype.Component;

import java.util.Date;

// Listener que se ejecuta cuando un usuario inicia sesión correctamente.
// Reinicia los intentos fallidos y actualiza la fecha del último intento.
// Se usa para desbloquear cuentas tras un login exitoso.
@Component
public class LoginSuccessListener implements ApplicationListener<AuthenticationSuccessEvent> {

    @Autowired
    private UsuarioRepository usuarioRepository;

    @Override
    public void onApplicationEvent(AuthenticationSuccessEvent event) {
        String username = event.getAuthentication().getName();
        usuarioRepository.getUserByUsername(username).ifPresent(usuario -> {
            usuario.setIntentosFallidos(0);
            usuario.setFechaUltimoIntento(new Date());
            usuarioRepository.save(usuario);
        });
    }
    
}
