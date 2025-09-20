package com.alocode.service.impl;

import com.alocode.model.Usuario;
import com.alocode.repository.*;
import com.alocode.service.MyUserDetails;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;

import java.util.Optional;

public class UserDetailsServiceImpl implements UserDetailsService {

    @Autowired
    private UsuarioRepository repository;

    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException{
        // En Spring Security, el parámetro se llama username pero nosotros usamos email
        Optional<Usuario> usuarioOptional = repository.getUserByUsername(username);

        if(usuarioOptional.isEmpty()){
            throw new UsernameNotFoundException("Usuario no encontrado con username: " + username);
        }
        return new MyUserDetails(usuarioOptional.get());
    }

}
