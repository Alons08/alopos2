package com.alocode.security;

import com.alocode.repository.UsuarioRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationListener;
import org.springframework.security.authentication.event.AuthenticationFailureBadCredentialsEvent;
import org.springframework.stereotype.Component;

import java.util.Calendar;
import java.util.Date;

// Listener que se ejecuta cuando ocurre un fallo de login.
// Incrementa los intentos fallidos y puede bloquear la cuenta si supera el límite.
// Se usa para proteger contra ataques de fuerza bruta y bloquear cuentas tras varios intentos fallidos.
@Component
public class LoginFailureListener implements ApplicationListener<AuthenticationFailureBadCredentialsEvent> {

    @Autowired
    private UsuarioRepository usuarioRepository;

    @Override
    public void onApplicationEvent(AuthenticationFailureBadCredentialsEvent event) {
        String username = (String) event.getAuthentication().getPrincipal();
        usuarioRepository.getUserByUsername(username).ifPresent(usuario -> {
            Date ahora = new Date();
            Date ultimoIntento = usuario.getFechaUltimoIntento();
            boolean esNuevoDia = false;
            if (ultimoIntento == null) {
                esNuevoDia = true;
            } else {
                Calendar cal1 = Calendar.getInstance();
                cal1.setTime(ultimoIntento);
                Calendar cal2 = Calendar.getInstance();
                cal2.setTime(ahora);
                esNuevoDia = cal1.get(Calendar.YEAR) != cal2.get(Calendar.YEAR)
                        || cal1.get(Calendar.DAY_OF_YEAR) != cal2.get(Calendar.DAY_OF_YEAR);
            }
            if (esNuevoDia) {
                usuario.setIntentosFallidos(1);
            } else {
                usuario.setIntentosFallidos(usuario.getIntentosFallidos() + 1);
            }
            usuario.setFechaUltimoIntento(ahora);
            if (usuario.getIntentosFallidos() >= 4) {
                usuario.setActivo(false);
            }
            usuarioRepository.save(usuario);
        });
    }
    
}
