package com.alocode.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.ViewControllerRegistry;

@Configuration
public class WebMvcConfigurer implements org.springframework.web.servlet.config.annotation.WebMvcConfigurer{

    /* Aquí se ponen las rutas (VISTAS) que se muestran a los usuarios
       que quieren acceder a un permiso que su rol no tiene permitido
       MEDIO RARO ESE COMENTARIO*/



    /*M3todo para configurar rutas del navegador asociándolas directamente con vistas HTML,
        sin necesidad de implementar un controlador para estas páginas.*/
    @Override
    public void addViewControllers(ViewControllerRegistry registry) {
                                //(ruta en el navegador).setViewName(archivo html)
        /*registry.addViewController("/").setViewName("ranking"); //me ahorro el GET en HomeController*/
        registry.addViewController("/403").setViewName("403");
        /*   COMENTE LO QUE VIENE PORQUE YA HICE EL CONTROLADOR DE LOGIN PARA PERSONALIZAR LOS MENSAJES
        registry.addViewController("/login").setViewName("login"); //el POST spring security ya lo hace automaticamente y me ahorro el GET en LoginController
        */
    }

}


