package com.alocode.controller;

import org.springframework.security.access.annotation.Secured;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;

import com.alocode.service.ReporteService;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;

import com.alocode.util.ExcelExporter;

import java.util.Date;
import java.time.LocalDateTime;

@Controller
@RequestMapping("/reportes")
@RequiredArgsConstructor
public class ReporteController {
    private final ReporteService reporteService;
    
    @GetMapping("/diario")
    public String reporteDiario(@RequestParam(value = "fecha", required = false) String fecha, Model model) {
        LocalDate diaDate;
        if (fecha != null) {
            diaDate = LocalDate.parse(fecha);
        } else {
            Date hoy = new Date();
            diaDate = hoy.toInstant().atZone(ZoneId.systemDefault()).toLocalDate();
        }
        LocalDateTime diaLdt = diaDate.atStartOfDay();
        ReporteService.ReporteDiario reporte = reporteService.generarReporteDiario(diaLdt);
        model.addAttribute("reporte", reporte);
        // Fechas para navegación
        model.addAttribute("diaAnterior", diaDate.minusDays(1).toString());
        model.addAttribute("diaSiguiente", diaDate.plusDays(1).toString());
        return "reporte-diario";
    }
    
    @GetMapping("/semanal")
    public String reporteSemanal(@RequestParam(value = "inicio", required = false) String inicio,
                                 @RequestParam(value = "fin", required = false) String fin,
                                 Model model) {
        LocalDate inicioDate;
        LocalDate finDate;
        if (inicio != null && fin != null) {
            inicioDate = LocalDate.parse(inicio);
            finDate = LocalDate.parse(fin);
        } else {
            Date inicioSemana = obtenerInicioSemana();
            Date finSemana = new Date(inicioSemana.getTime() + 6 * 86400000); // +6 días
            inicioDate = inicioSemana.toInstant().atZone(ZoneId.systemDefault()).toLocalDate();
            finDate = finSemana.toInstant().atZone(ZoneId.systemDefault()).toLocalDate();
        }
        LocalDateTime inicioLdt = inicioDate.atStartOfDay();
        LocalDateTime finLdt = finDate.atStartOfDay();
        ReporteService.ReporteSemanal reporte = reporteService.generarReporteSemanal(inicioLdt, finLdt);
        model.addAttribute("reporte", reporte);
        // Fechas para navegación
        model.addAttribute("semanaAnteriorInicio", inicioDate.minusWeeks(1).toString());
        model.addAttribute("semanaAnteriorFin", finDate.minusWeeks(1).toString());
        model.addAttribute("semanaSiguienteInicio", inicioDate.plusWeeks(1).toString());
        model.addAttribute("semanaSiguienteFin", finDate.plusWeeks(1).toString());
        return "reporte-semanal";
    }
    
    @GetMapping("/mensual")
    @Secured("ADMIN")
    public String reporteMensual(@RequestParam(value = "inicio", required = false) String inicio,
                                 @RequestParam(value = "fin", required = false) String fin,
                                 Model model) {
        LocalDate inicioDate;
        LocalDate finDate;
        if (inicio != null && fin != null) {
            inicioDate = LocalDate.parse(inicio);
            finDate = LocalDate.parse(fin);
        } else {
            Date inicioMes = obtenerInicioMes();
            Date finMes = obtenerFinMes();
            inicioDate = inicioMes.toInstant().atZone(ZoneId.systemDefault()).toLocalDate();
            finDate = finMes.toInstant().atZone(ZoneId.systemDefault()).toLocalDate();
        }
        LocalDateTime inicioLdt = inicioDate.atStartOfDay();
        LocalDateTime finLdt = finDate.atStartOfDay();
        ReporteService.ReporteMensual reporte = reporteService.generarReporteMensual(inicioLdt, finLdt);
        model.addAttribute("reporte", reporte);
        // Fechas para navegación
        model.addAttribute("mesAnteriorInicio", inicioDate.minusMonths(1).withDayOfMonth(1).toString());
        model.addAttribute("mesAnteriorFin", inicioDate.minusMonths(1).withDayOfMonth(inicioDate.minusMonths(1).lengthOfMonth()).toString());
        model.addAttribute("mesSiguienteInicio", inicioDate.plusMonths(1).withDayOfMonth(1).toString());
        model.addAttribute("mesSiguienteFin", inicioDate.plusMonths(1).withDayOfMonth(inicioDate.plusMonths(1).lengthOfMonth()).toString());
        return "reporte-mensual";
    }
    
    @GetMapping("/exportar/diario")
    public void exportarReporteDiarioExcel(HttpServletResponse response,
                                           @RequestParam(value = "fecha", required = false) String fecha) {
        LocalDate diaDate;
        if (fecha != null) {
            diaDate = LocalDate.parse(fecha);
        } else {
            Date hoy = new Date();
            diaDate = hoy.toInstant().atZone(ZoneId.systemDefault()).toLocalDate();
        }
        LocalDateTime diaLdt = diaDate.atStartOfDay();
        ReporteService.ReporteDiario reporte = reporteService.generarReporteDiario(diaLdt);
        ExcelExporter.exportToExcel(reporte, "reporte-diario", response);
    }

    @GetMapping("/exportar/semanal")
    public void exportarReporteSemanalExcel(HttpServletResponse response,
                                           @RequestParam(value = "inicio", required = false) String inicio,
                                           @RequestParam(value = "fin", required = false) String fin) {
        LocalDate inicioDate;
        LocalDate finDate;
        if (inicio != null && fin != null) {
            inicioDate = LocalDate.parse(inicio);
            finDate = LocalDate.parse(fin);
        } else {
            Date inicioSemana = obtenerInicioSemana();
            Date finSemana = new Date(inicioSemana.getTime() + 6 * 86400000); // +6 días
            inicioDate = inicioSemana.toInstant().atZone(ZoneId.systemDefault()).toLocalDate();
            finDate = finSemana.toInstant().atZone(ZoneId.systemDefault()).toLocalDate();
        }
        LocalDateTime inicioLdt = inicioDate.atStartOfDay();
        LocalDateTime finLdt = finDate.atStartOfDay();
        ReporteService.ReporteSemanal reporte = reporteService.generarReporteSemanal(inicioLdt, finLdt);
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd-MM-yyyy");
        String nombreArchivo = "reporte-semanal_" + inicioDate.format(formatter) + "_a_" + finDate.format(formatter);
        ExcelExporter.exportToExcel(reporte, nombreArchivo, response);
    }

    @GetMapping("/exportar/mensual")
    public void exportarReporteMensualExcel(HttpServletResponse response,
                                           @RequestParam(value = "inicio", required = false) String inicio,
                                           @RequestParam(value = "fin", required = false) String fin) {
        LocalDate inicioDate;
        LocalDate finDate;
        if (inicio != null && fin != null) {
            inicioDate = LocalDate.parse(inicio);
            finDate = LocalDate.parse(fin);
        } else {
            Date inicioMes = obtenerInicioMes();
            Date finMes = obtenerFinMes();
            inicioDate = inicioMes.toInstant().atZone(ZoneId.systemDefault()).toLocalDate();
            finDate = finMes.toInstant().atZone(ZoneId.systemDefault()).toLocalDate();
        }
        LocalDateTime inicioLdt = inicioDate.atStartOfDay();
        LocalDateTime finLdt = finDate.atStartOfDay();
        ReporteService.ReporteMensual reporte = reporteService.generarReporteMensual(inicioLdt, finLdt);
        // Nombre: reporte-mensual_YYYY-MM-01_a_YYYY-MM-<fin>
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd-MM-yyyy");
        String nombreArchivo = "reporte-mensual_" + inicioDate.format(formatter) + "_a_" + finDate.format(formatter);
        ExcelExporter.exportToExcel(reporte, nombreArchivo, response);
    }
    
    // Métodos auxiliares para calcular fechas
    private Date obtenerInicioSemana() {
        // Devuelve el lunes de la semana actual a las 00:00:00
        LocalDate now = LocalDate.now();
        LocalDate monday = now.with(java.time.DayOfWeek.MONDAY);
        ZonedDateTime zdt = monday.atStartOfDay(ZoneId.systemDefault());
        return Date.from(zdt.toInstant());
    }
    
    private Date obtenerInicioMes() {
    // Primer día del mes actual a las 00:00:00
    LocalDate now = LocalDate.now();
    LocalDate firstDay = now.withDayOfMonth(1);
    ZonedDateTime zdt = firstDay.atStartOfDay(ZoneId.systemDefault());
    return Date.from(zdt.toInstant());
    }
    
    private Date obtenerFinMes() {
    // Último día del mes actual a las 23:59:59
    LocalDate now = LocalDate.now();
    LocalDate lastDay = now.withDayOfMonth(now.lengthOfMonth());
    ZonedDateTime zdt = lastDay.atTime(23, 59, 59).atZone(ZoneId.systemDefault());
    return Date.from(zdt.toInstant());
    }
}
