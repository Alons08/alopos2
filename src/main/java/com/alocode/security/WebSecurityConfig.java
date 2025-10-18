package com.alocode.security;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.config.annotation.authentication.builders.AuthenticationManagerBuilder;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;

import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

import com.alocode.service.impl.UserDetailsServiceImpl;

@Configuration //esta anotación registra Beans en el contenedor de Spring Boot
@EnableWebSecurity //habilitar la seguridad en la app y poder usar la BD sin problemas
public class WebSecurityConfig {
    @Autowired
    private CustomAuthenticationSuccessHandler customAuthenticationSuccessHandler;

    //SE USA EN EL FILTER CHAIN
    @Autowired
    private CustomAuthenticationFailureHandler customAuthenticationFailureHandler;

    @Autowired
    private UsuarioActivoFilter usuarioActivoFilter;

    //Para tener al usuario en la fabrica de Spring
    @Bean
    public UserDetailsService userDetailsService(){
        return new UserDetailsServiceImpl();
    }

    //Para encriptar la contraseña del ususario
    @Bean
    public BCryptPasswordEncoder passwordEncoder(){
        return new BCryptPasswordEncoder();
    }

    /*Haremos una implementacion del AuthenticationProvider que se utiliza
        comunmente para autenticar usuarios en BD. Además, es el responsable
         de verificar las credenciales del usuario y autenticar el usuario*/
    @Bean
    public DaoAuthenticationProvider authenticationProvider(UserDetailsService userDetailsService) {
        DaoAuthenticationProvider authenticationProvider = new DaoAuthenticationProvider(userDetailsService);
        authenticationProvider.setPasswordEncoder(passwordEncoder());
        return authenticationProvider;
    }

    //Luego de lo de arriba tenemos que registrarlo en el manejador de autenticacion
    @Bean
    public AuthenticationManager authenticationManager(HttpSecurity httpSecurity, DaoAuthenticationProvider authenticationProvider) throws Exception {
        AuthenticationManagerBuilder authMB = httpSecurity.getSharedObject(AuthenticationManagerBuilder.class);
        authMB.authenticationProvider(authenticationProvider);
        return authMB.build();
    }

    //CREAMOS un FILTRO (define las reglas de autorizacion para las solicitudes HTTP)
    @Bean
    protected SecurityFilterChain filterChain(HttpSecurity httpSecurity) throws Exception {
        httpSecurity
                .addFilterBefore(usuarioActivoFilter, UsernamePasswordAuthenticationFilter.class)
                // Se definen las reglas de acceso a las rutas
                .authorizeHttpRequests(auth -> auth
                        //rutas públicas
                        .requestMatchers("/login", "/css/**", "/js/**", "/images/**").permitAll()

                        //rutas de la secretaria y administrador en común
                        .requestMatchers("/pedidos/**", "/reportes/diario").hasAnyAuthority("SECRETARIA", "ADMIN")

                        //rutas del administrador
                        .requestMatchers("/caja/**", "/productos/**", "/mesas/**", "/reportes/**", "/cliente/**").hasAuthority("ADMIN")
                        .requestMatchers("/admin/usuarios/**").hasAuthority("ADMIN")

                        //todas las demás rutas requieren autenticación
                        .anyRequest().authenticated()
                )
                // Configura el manejo del inicio de sesión en la aplicación.
        .formLogin(form -> form
            .loginPage("/login")
            .failureHandler(customAuthenticationFailureHandler)
            .successHandler(customAuthenticationSuccessHandler)
            .permitAll()
        )
                // Configura "Remember Me" para mantener la sesión activa incluso al cerrar el navegador
                .rememberMe(remember -> remember
                    .key("uniqueAndSecretKey") // Clave secreta para encriptar la cookie
                    .tokenValiditySeconds(7 * 24 * 60 * 60) // 7 días de validez (balance profesional)
                    .userDetailsService(userDetailsService())
                    .rememberMeParameter("remember-me") // Nombre del parámetro en el formulario
                    .rememberMeCookieName("alopos-remember-me") // Nombre de la cookie
                )
                // Limita a una sola sesión activa por usuario
                .sessionManagement(session -> session
                    .maximumSessions(1)
                    .maxSessionsPreventsLogin(false) // Si es true, bloquea el nuevo login; si es false, cierra la sesión anterior
                )
                // Configura el manejo del cierre de sesión en la aplicación.
                .logout(logout -> logout
                        .logoutUrl("/logout")  //define la URL para realizar el cierre de sesión
                        .logoutSuccessUrl("/login?logout")  //redirige a login con parámetro de logout
                        .invalidateHttpSession(true)  //invalida la sesión
                        .deleteCookies("JSESSIONID", "alopos-remember-me")  //elimina cookies de sesión y remember-me
                        .permitAll() //permite el acceso al cierre de sesión para todos.
                )
                // Configura el manejo de excepciones relacionadas con el acceso denegado
                .exceptionHandling(e -> e
                        .accessDeniedPage("/403") /*ENNN "WebMvcConfigurer" vinculo el endpoint a un HTML*/
                );

        return httpSecurity.build();
    }

}
