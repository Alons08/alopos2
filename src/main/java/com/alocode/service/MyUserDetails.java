package com.alocode.service;


import lombok.AllArgsConstructor;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import com.alocode.model.Rol;
import com.alocode.model.Usuario;

import java.util.*;

@AllArgsConstructor
public class MyUserDetails implements UserDetails {

    private Usuario usuario;

    // Agrega este método getter
    public Usuario getUsuario() {
        return usuario;
    }

    //M3todo que nos obtiene los roles de un usuario
    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        Set<Rol> roles = usuario.getRoles();
        List<SimpleGrantedAuthority> authorities = new ArrayList<>();

        for(Rol rol: roles){
            authorities.add(new SimpleGrantedAuthority(rol.getNombre()));
        }
        return authorities;
    }

    @Override
    public String getPassword() {
        return usuario.getPassword();
    }

    @Override
    public String getUsername() {
        return usuario.getUsername();
    }

    // Asegúrate de implementar los demás métodos requeridos por UserDetails
    @Override
    public boolean isAccountNonExpired() {
        return true;
    }

    @Override
    public boolean isAccountNonLocked() {
        return true;
    }

    @Override
    public boolean isCredentialsNonExpired() {
        return true;
    }

    @Override
    public boolean isEnabled() {
        return usuario.getActivo() != null && usuario.getActivo();
    }
}

