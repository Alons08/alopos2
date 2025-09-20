package com.alocode.service;

import lombok.RequiredArgsConstructor;

import java.util.Optional;
import org.springframework.stereotype.Service;

import com.alocode.model.Caja;
import com.alocode.model.Pedido;
import com.alocode.model.enums.EstadoPedido;
import com.alocode.repository.CajaRepository;
import com.alocode.repository.PedidoRepository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Date;
import java.time.ZoneId;

@Service
@RequiredArgsConstructor
public class ReporteService {
    private final PedidoRepository pedidoRepository;
    private final CajaRepository cajaRepository;

    public ReporteDiario generarReporteDiario(LocalDateTime fecha) {
        Long clienteId = com.alocode.util.TenantContext.getCurrentTenant();
        LocalDateTime fin = fecha.plusDays(1);
        List<Pedido> pedidos = pedidoRepository.findByClienteIdAndEstadoAndFechaPagadoBetweenOrderByIdAsc(
            clienteId,
            EstadoPedido.PAGADO,
            fecha,
            fin);
        double totalVentas = pedidos.stream()
            .mapToDouble(p -> p.getTotal() - p.getRecargo())
            .sum();
        double totalRecargos = pedidos.stream()
            .mapToDouble(Pedido::getRecargo)
            .sum();
        double totalNeto = totalVentas + totalRecargos;
        Optional<Caja> caja = cajaRepository.findByFecha(clienteId, java.sql.Timestamp.valueOf(fecha)).stream().findFirst();
        return new ReporteDiario(
            java.sql.Timestamp.valueOf(fecha),
            caja.map(Caja::getMontoApertura).orElse(0.0),
            caja.map(Caja::getMontoCierre).orElse(0.0),
            totalVentas,
            totalRecargos,
            totalNeto,
            pedidos);
    }

    public ReporteSemanal generarReporteSemanal(LocalDateTime inicio, LocalDateTime fin) {
        Long clienteId = com.alocode.util.TenantContext.getCurrentTenant();
        List<Caja> cajas = cajaRepository.findByClienteIdAndFechaBetween(
            clienteId,
            new java.util.Date(java.sql.Timestamp.valueOf(inicio).getTime()),
            new java.util.Date(java.sql.Timestamp.valueOf(fin).getTime()));
        List<Pedido> pedidos = pedidoRepository.findByClienteIdAndEstadoAndFechaPagadoBetweenOrderByIdAsc(
            clienteId,
            EstadoPedido.PAGADO,
            inicio,
            fin);
        double totalSemanal = pedidos.stream()
            .mapToDouble(Pedido::getTotal)
            .sum();
        return new ReporteSemanal(
            java.sql.Timestamp.valueOf(inicio),
            java.sql.Timestamp.valueOf(fin),
            totalSemanal,
            cajas,
            pedidos);
    }

    public ReporteMensual generarReporteMensual(LocalDateTime inicio, LocalDateTime fin) {
    Long clienteId = com.alocode.util.TenantContext.getCurrentTenant();
    List<Pedido> pedidos = pedidoRepository.findByClienteIdAndEstadoAndFechaPagadoBetweenOrderByIdAsc(
        clienteId,
        EstadoPedido.PAGADO,
        inicio,
        fin);
    double totalMensual = pedidos.stream()
        .mapToDouble(Pedido::getTotal)
        .sum();
    List<ReporteService.SemanaResumen> semanas = new java.util.ArrayList<>();
    LocalDateTime semanaInicio = inicio;
    while (semanaInicio.isBefore(fin)) {
        final LocalDateTime semanaInicioFinal = semanaInicio;
        LocalDateTime semanaFin = semanaInicio.plusDays(6);
        if (semanaFin.isAfter(fin))
        semanaFin = fin;
        final LocalDateTime semanaFinFinal = semanaFin;
        List<Pedido> pedidosSemana = pedidos.stream()
            .filter(p -> {
            Date fechaPedidoDate = p.getFechaPagado();
            if (fechaPedidoDate == null)
                return false;
            LocalDateTime fechaPedido = fechaPedidoDate.toInstant().atZone(ZoneId.systemDefault())
                .toLocalDateTime();
            return (fechaPedido.isEqual(semanaInicioFinal) || fechaPedido.isAfter(semanaInicioFinal))
                && fechaPedido.isBefore(semanaFinFinal.plusDays(1));
            })
            .collect(java.util.stream.Collectors.toList());
        double totalSemana = pedidosSemana.stream().mapToDouble(Pedido::getTotal).sum();
        int cantidadPedidos = pedidosSemana.size();
        if (totalSemana > 0 || cantidadPedidos > 0) {
        semanas.add(new ReporteService.SemanaResumen(
            java.sql.Timestamp.valueOf(semanaInicioFinal),
            java.sql.Timestamp.valueOf(semanaFinFinal),
            totalSemana,
            cantidadPedidos,
            pedidosSemana));
        }
        semanaInicio = semanaFinFinal.plusDays(1);
    }
    return new ReporteMensual(
        java.sql.Timestamp.valueOf(inicio),
        java.sql.Timestamp.valueOf(fin),
        totalMensual,
        pedidos,
        semanas);
    }

    // Clases internas para los reportes

    // Clase para resumen semanal dentro del reporte mensual
    public static class SemanaResumen {
        private java.util.Date inicio;
        private java.util.Date fin;
        private double totalSemana;
        private int cantidadPedidos;
        private List<Pedido> pedidos;

        public SemanaResumen(java.util.Date inicio, java.util.Date fin, double totalSemana, int cantidadPedidos,
                List<Pedido> pedidos) {
            this.inicio = inicio;
            this.fin = fin;
            this.totalSemana = totalSemana;
            this.cantidadPedidos = cantidadPedidos;
            this.pedidos = pedidos;
        }

        public java.util.Date getInicio() {
            return inicio;
        }

        public java.util.Date getFin() {
            return fin;
        }

        public double getTotalSemana() {
            return totalSemana;
        }

        public int getCantidadPedidos() {
            return cantidadPedidos;
        }

        public List<Pedido> getPedidos() {
            return pedidos;
        }
    }

    public static class ReporteDiario {
        private java.util.Date fecha;
        private double montoApertura;
        private double montoCierre;
        private double totalVentas;
        private double totalRecargos;
        private double totalNeto;
        private List<Pedido> pedidos;

        public ReporteDiario(java.util.Date fecha, double montoApertura, double montoCierre, double totalVentas,
                double totalRecargos, double totalNeto, List<Pedido> pedidos) {
            this.fecha = fecha;
            this.montoApertura = montoApertura;
            this.montoCierre = montoCierre;
            this.totalVentas = totalVentas;
            this.totalRecargos = totalRecargos;
            this.totalNeto = totalNeto;
            this.pedidos = pedidos;
        }

        public java.util.Date getFecha() {
            return fecha;
        }

        public void setFecha(java.util.Date fecha) {
            this.fecha = fecha;
        }

        public double getMontoApertura() {
            return montoApertura;
        }

        public void setMontoApertura(double montoApertura) {
            this.montoApertura = montoApertura;
        }

        public double getMontoCierre() {
            return montoCierre;
        }

        public void setMontoCierre(double montoCierre) {
            this.montoCierre = montoCierre;
        }

        public double getTotalVentas() {
            return totalVentas;
        }

        public void setTotalVentas(double totalVentas) {
            this.totalVentas = totalVentas;
        }

        public double getTotalRecargos() {
            return totalRecargos;
        }

        public void setTotalRecargos(double totalRecargos) {
            this.totalRecargos = totalRecargos;
        }

        public double getTotalNeto() {
            return totalNeto;
        }

        public void setTotalNeto(double totalNeto) {
            this.totalNeto = totalNeto;
        }

        public List<Pedido> getPedidos() {
            return pedidos;
        }

        public void setPedidos(List<Pedido> pedidos) {
            this.pedidos = pedidos;
        }
    }

    public static class ReporteSemanal {
        private java.util.Date inicio;
        private java.util.Date fin;
        private double totalSemanal;
        private List<Caja> cajas;
        private List<Pedido> pedidos;

        public ReporteSemanal(java.util.Date inicio, java.util.Date fin, double totalSemanal, List<Caja> cajas,
                List<Pedido> pedidos) {
            this.inicio = inicio;
            this.fin = fin;
            this.totalSemanal = totalSemanal;
            this.cajas = cajas;
            this.pedidos = pedidos;
        }

        public java.util.Date getInicio() {
            return inicio;
        }

        public void setInicio(java.util.Date inicio) {
            this.inicio = inicio;
        }

        public java.util.Date getFin() {
            return fin;
        }

        public void setFin(java.util.Date fin) {
            this.fin = fin;
        }

        public double getTotalSemanal() {
            return totalSemanal;
        }

        public void setTotalSemanal(double totalSemanal) {
            this.totalSemanal = totalSemanal;
        }

        public List<Caja> getCajas() {
            return cajas;
        }

        public void setCajas(List<Caja> cajas) {
            this.cajas = cajas;
        }

        public List<Pedido> getPedidos() {
            return pedidos;
        }

        public void setPedidos(List<Pedido> pedidos) {
            this.pedidos = pedidos;
        }
    }

    public static class ReporteMensual {
        private java.util.Date inicio;
        private java.util.Date fin;
        private double totalMensual;
        private List<Pedido> pedidos;
        private List<SemanaResumen> semanas;

        public ReporteMensual(java.util.Date inicio, java.util.Date fin, double totalMensual, List<Pedido> pedidos,
                List<SemanaResumen> semanas) {
            this.inicio = inicio;
            this.fin = fin;
            this.totalMensual = totalMensual;
            this.pedidos = pedidos;
            this.semanas = semanas;
        }

        public java.util.Date getInicio() {
            return inicio;
        }

        public void setInicio(java.util.Date inicio) {
            this.inicio = inicio;
        }

        public java.util.Date getFin() {
            return fin;
        }

        public void setFin(java.util.Date fin) {
            this.fin = fin;
        }

        public double getTotalMensual() {
            return totalMensual;
        }

        public void setTotalMensual(double totalMensual) {
            this.totalMensual = totalMensual;
        }

        public List<Pedido> getPedidos() {
            return pedidos;
        }

        public void setPedidos(List<Pedido> pedidos) {
            this.pedidos = pedidos;
        }

        public List<SemanaResumen> getSemanas() {
            return semanas;
        }

        public void setSemanas(List<SemanaResumen> semanas) {
            this.semanas = semanas;
        }
    }
}