package com.alocode.util;

import org.springframework.stereotype.Component;

/**
 * Utilidad para formatear valores de stock en las vistas según la configuración del cliente.
 * Si permitirProductosDerivados = false -> muestra enteros (sin decimales)
 * Si permitirProductosDerivados = true -> muestra con decimales (2 decimales)
 */
@Component("formatoStock")
public class FormatoStockUtil {

    /**
     * Formatea un valor Double de stock según si se permiten productos derivados o no.
     * 
     * @param stock el valor del stock
     * @param permitirDerivados si se permiten productos derivados (configuración del cliente)
     * @return String formateado con o sin decimales
     */
    public String formatear(Double stock, Boolean permitirDerivados) {
        if (stock == null) {
            return "0";
        }
        
        // Si no se permiten productos derivados, mostrar como entero
        if (permitirDerivados == null || !permitirDerivados) {
            return String.format("%d", stock.intValue());
        }
        
        // Si se permiten productos derivados, mostrar con 2 decimales
        return String.format("%.2f", stock).replace(",", ".");
    }
    
    /**
     * Versión sobrecargada que acepta valores primitivos double
     */
    public String formatear(double stock, Boolean permitirDerivados) {
        return formatear(Double.valueOf(stock), permitirDerivados);
    }
    
    /**
     * Formatea una operación aritmética (ej: stock - stockOcupado)
     */
    public String formatearDiferencia(Double valor1, Double valor2, Boolean permitirDerivados) {
        if (valor1 == null) valor1 = 0.0;
        if (valor2 == null) valor2 = 0.0;
        
        Double resultado = valor1 - valor2;
        return formatear(resultado, permitirDerivados);
    }
}
