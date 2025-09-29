package com.alocode.util;

import org.apache.poi.ss.usermodel.*;
import org.apache.poi.ss.util.CellRangeAddress;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import com.alocode.service.ReporteService;
import com.alocode.model.Pedido;
import com.alocode.model.DetallePedido;
import java.text.SimpleDateFormat;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.Date;

public class ExcelExporter {
    private static final String FONT_NAME = "Arial";
    private static final short HEADER_FONT_SIZE = 11;
    private static final short BODY_FONT_SIZE = 10;
    private static final short TITLE_FONT_SIZE = 14;
    
    public static void exportToExcel(Object reporte, String nombreArchivo, HttpServletResponse response) {
        String fechaReporte = "";
        try (Workbook workbook = new XSSFWorkbook()) {
            // Estilos predefinidos
            CellStyle headerStyle = createHeaderStyle(workbook);
            CellStyle titleStyle = createTitleStyle(workbook);
            CellStyle dateStyle = createDateStyle(workbook);
            CellStyle centeredStyle = createCenteredDataStyle(workbook);
            CellStyle centeredCurrencyStyle = createCenteredCurrencyStyle(workbook);
            CellStyle separatorStyle = createSeparatorStyle(workbook);
            
            if (reporte instanceof ReporteService.ReporteDiario diario) {
                Sheet sheet = workbook.createSheet("Reporte Diario");
                // ...existing code...
                // Preparar nombre de archivo con fecha (DIA-MES-AÑO)
                if (diario.getFecha() != null) {
                    fechaReporte = new SimpleDateFormat("dd-MM-yyyy").format(diario.getFecha());
                }
                // Configurar anchos de columnas (en unidades de 1/256 de ancho de carácter)
                sheet.setColumnWidth(0, 16*256);  // Columna A - N° Pedido
                sheet.setColumnWidth(1, 20*256);  // Columna B - Mesa
                sheet.setColumnWidth(2, 20*256);  // Columna C - Usuario
                sheet.setColumnWidth(3, 15*256);  // Columna D - Venta
                sheet.setColumnWidth(4, 15*256);  // Columna E - Recargo
                sheet.setColumnWidth(5, 15*256);  // Columna F - Total
                sheet.setColumnWidth(6, 25*256);  // Columna G - Fecha Pagado
                sheet.setColumnWidth(7, 20*256);  // Columna H
                
                int rowIdx = 0;
                
                // Título del reporte
                Row titleRow = sheet.createRow(rowIdx++);
                titleRow.setHeightInPoints(25);
                Cell titleCell = titleRow.createCell(0);
                titleCell.setCellValue("REPORTE DIARIO");
                titleCell.setCellStyle(titleStyle);
                sheet.addMergedRegion(new CellRangeAddress(0, 0, 0, 7));
                
                // Información general
                rowIdx = addGeneralInfo(sheet, rowIdx, 
                    new String[] {"Fecha", "Monto Apertura", "Monto Cierre", "Total Ventas", "Total Recargos", "Total General"},
                    new Object[] {
                        diario.getFecha(),
                        diario.getMontoApertura(),
                        diario.getMontoCierre(),
                        diario.getTotalVentas(),
                        diario.getTotalRecargos(),
                        diario.getTotalNeto()
                    },
                    headerStyle, centeredStyle, centeredCurrencyStyle, dateStyle);
                
                // Espacio antes de la tabla de pedidos
                rowIdx = addSeparatorRow(sheet, rowIdx, 7, separatorStyle);
                
                // Cabecera de pedidos
                String[] pedidosHeaders = {
                    "N° Pedido", "Mesa", "Usuario", "Venta", "Recargo", "Total", "Fecha Pagado"
                };
                rowIdx = createTableHeader(sheet, rowIdx, pedidosHeaders, headerStyle);
                
                // Datos de pedidos
                // boolean firstPedido = true;
                for (Pedido p : diario.getPedidos()) {
                    Row row = sheet.createRow(rowIdx++);
                    row.setHeightInPoints(20);
                    int colIdx = 0;
                    addCell(row, colIdx++, p.getNumeroPedido(), centeredStyle);
                    addCell(row, colIdx++, p.getMesa() != null ? String.valueOf(p.getMesa().getNumero()) : "", centeredStyle);
                    addCell(row, colIdx++, p.getUsuario() != null ? p.getUsuario().getNombre() : "", centeredStyle);
                    // Calcular venta sin recargo
                    double ventaSinRecargo = p.getTotal() - p.getRecargo();
                    addCell(row, colIdx++, ventaSinRecargo, centeredCurrencyStyle);
                    addCell(row, colIdx++, p.getRecargo(), centeredCurrencyStyle);
                    addCell(row, colIdx++, p.getTotal(), centeredCurrencyStyle);
                    addCell(row, colIdx++, p.getFechaPagado() != null ? p.getFechaPagado() : null, dateStyle);

                    // Detalles del pedido
                    if (p.getDetalles() != null && !p.getDetalles().isEmpty()) {
                        // Título de detalles
                        Row detTitleRow = sheet.createRow(rowIdx++);
                        detTitleRow.setHeightInPoints(20);
                        Cell detTitleCell = detTitleRow.createCell(1);
                        detTitleCell.setCellValue("DETALLES DEL PEDIDO");
                        detTitleCell.setCellStyle(headerStyle);
                        sheet.addMergedRegion(new CellRangeAddress(detTitleRow.getRowNum(), detTitleRow.getRowNum(), 1, 4));

                        // Cabecera de detalles
                        String[] detallesHeaders = {"Producto", "Cantidad", "Precio Unitario", "Subtotal"};
                        rowIdx = createTableHeader(sheet, rowIdx, detallesHeaders, headerStyle, 1);

                        // Datos de detalles
                        for (DetallePedido d : p.getDetalles()) {
                            Row detRow = sheet.createRow(rowIdx++);
                            detRow.setHeightInPoints(18);
                            colIdx = 1;
                            addCell(detRow, colIdx++, d.getProducto() != null ? d.getProducto().getNombre() : "", centeredStyle);
                            addCell(detRow, colIdx++, d.getCantidad(), centeredStyle);
                            addCell(detRow, colIdx++, d.getPrecioUnitario(), centeredCurrencyStyle);
                            addCell(detRow, colIdx++, d.getSubtotal(), centeredCurrencyStyle);
                        }
                    }

                    // Espacio en blanco después de los detalles del pedido (o después del pedido si no hay detalles)
                    rowIdx = addSeparatorRow(sheet, rowIdx, 7, separatorStyle);
                }
                
            } else if (reporte instanceof ReporteService.ReporteSemanal semanal) {
                Sheet sheet = workbook.createSheet("Reporte Semanal");
                // Configurar anchos de columnas
                sheet.setColumnWidth(0, 16*256);  // ID Pedido
                sheet.setColumnWidth(1, 20*256);  // Mesa
                sheet.setColumnWidth(2, 20*256);  // Usuario
                sheet.setColumnWidth(3, 15*256);  // Venta
                sheet.setColumnWidth(4, 15*256);  // Recargo
                sheet.setColumnWidth(5, 15*256);  // Total
                sheet.setColumnWidth(6, 25*256);  // Fecha Pagado

                int rowIdx = 0;
                // Título del reporte
                Row titleRow = sheet.createRow(rowIdx++);
                titleRow.setHeightInPoints(25);
                Cell titleCell = titleRow.createCell(0);
                titleCell.setCellValue("REPORTE SEMANAL");
                titleCell.setCellStyle(titleStyle);
                sheet.addMergedRegion(new CellRangeAddress(0, 0, 0, 6));

                // Información general (igual que diario)
                rowIdx = addGeneralInfo(sheet, rowIdx,
                    new String[] {"Desde", "Hasta", "Total Semanal"},
                    new Object[] {semanal.getInicio(), semanal.getFin(), semanal.getTotalSemanal()},
                    headerStyle, centeredStyle, centeredCurrencyStyle, dateStyle);

                // Espacio antes de la tabla de resumen por día
                rowIdx = addSeparatorRow(sheet, rowIdx, 7, separatorStyle);

                // Cabecera resumen por día
                String[] headers = {"Fecha", "Monto Apertura", "Monto Cierre", "Total General", "Total Pedidos", "Estado Caja"};
                rowIdx = createTableHeader(sheet, rowIdx, headers, headerStyle);

                // Filas por cada caja (día)
                for (com.alocode.model.Caja caja : semanal.getCajas()) {
                    Row row = sheet.createRow(rowIdx++);
                    int colIdx = 0;
                    addCell(row, colIdx++, caja.getFecha(), dateStyle);
                    addCell(row, colIdx++, caja.getMontoApertura(), centeredCurrencyStyle);
                    addCell(row, colIdx++, caja.getMontoCierre(), centeredCurrencyStyle);
                    // Calcular total ventas del día (el total ya incluye recargos)
                    double totalVentasDia = semanal.getPedidos().stream()
                        .filter(p -> p.getCaja() != null && p.getCaja().getId().equals(caja.getId()))
                        .mapToDouble(p -> p.getTotal())
                        .sum();
                    addCell(row, colIdx++, totalVentasDia, centeredCurrencyStyle);
                    // Total pedidos del día
                    long totalPedidosDia = semanal.getPedidos().stream()
                        .filter(p -> p.getCaja() != null && p.getCaja().getId().equals(caja.getId()))
                        .count();
                    addCell(row, colIdx++, totalPedidosDia, centeredStyle);
                    // Estado caja
                    String estado = (caja.getMontoCierre() == null) ? "Abierta" : "Cerrada";
                    addCell(row, colIdx++, estado, centeredStyle);
                }

                // Espacio antes de la tabla de pedidos
                rowIdx = addSeparatorRow(sheet, rowIdx, 7, separatorStyle);

                // Cabecera de pedidos
                String[] pedidosHeaders = {"N° Pedido", "Mesa", "Usuario", "Venta", "Recargo", "Total", "Fecha Pagado"};
                rowIdx = createTableHeader(sheet, rowIdx, pedidosHeaders, headerStyle);
                for (Pedido p : semanal.getPedidos()) {
                    // No agregar separador entre pedidos en el reporte semanal
                    Row row = sheet.createRow(rowIdx++);
                    row.setHeightInPoints(20);
                    int colIdx = 0;
                    addCell(row, colIdx++, p.getNumeroPedido(), centeredStyle);
                    addCell(row, colIdx++, p.getMesa() != null ? String.valueOf(p.getMesa().getNumero()) : "", centeredStyle);
                    addCell(row, colIdx++, p.getUsuario() != null ? p.getUsuario().getNombre() : "", centeredStyle);
                    // Calcular venta sin recargo
                    double ventaSinRecargo = p.getTotal() - p.getRecargo();
                    addCell(row, colIdx++, ventaSinRecargo, centeredCurrencyStyle);
                    addCell(row, colIdx++, p.getRecargo(), centeredCurrencyStyle);
                    addCell(row, colIdx++, p.getTotal(), centeredCurrencyStyle);
                    addCell(row, colIdx++, p.getFechaPagado() != null ? p.getFechaPagado() : null, dateStyle);
                }

                // Nombre de archivo con rango de fechas
                SimpleDateFormat sdf = new SimpleDateFormat("dd-MM-yyyy");
                String desde = semanal.getInicio() != null ? sdf.format(semanal.getInicio()) : "";
                String hasta = semanal.getFin() != null ? sdf.format(semanal.getFin()) : "";
                // Evitar duplicar el rango de fechas en el nombre del archivo
                String sufijo = "_" + desde + "_a_" + hasta;
                if (!desde.isEmpty() && !hasta.isEmpty() && (nombreArchivo == null || !nombreArchivo.endsWith(sufijo))) {
                    nombreArchivo = nombreArchivo + sufijo;
                }
            } else if (reporte instanceof ReporteService.ReporteMensual mensual) {
                Sheet sheet = workbook.createSheet("Reporte Mensual");
                // Anchos de columnas
                sheet.setColumnWidth(0, 16*256); // ID Pedido
                sheet.setColumnWidth(1, 25*256); // Fecha Pagado
                sheet.setColumnWidth(2, 15*256); // Venta
                sheet.setColumnWidth(3, 15*256); // Recargo
                sheet.setColumnWidth(4, 15*256); // Total
                sheet.setColumnWidth(5, 20*256); // Usuario

                int rowIdx = 0;
                // Título
                Row titleRow = sheet.createRow(rowIdx++);
                titleRow.setHeightInPoints(25);
                Cell titleCell = titleRow.createCell(0);
                titleCell.setCellValue("REPORTE MENSUAL");
                titleCell.setCellStyle(titleStyle);
                sheet.addMergedRegion(new CellRangeAddress(0, 0, 0, 5));

                // Info general
                rowIdx = addGeneralInfo(sheet, rowIdx,
                    new String[] {"Desde", "Hasta", "Total Mensual"},
                    new Object[] {mensual.getInicio(), mensual.getFin(), mensual.getTotalMensual()},
                    headerStyle, centeredStyle, centeredCurrencyStyle, dateStyle);

                // Espacio antes de la tabla de resumen semanal
                rowIdx = addSeparatorRow(sheet, rowIdx, 6, separatorStyle);

                // Cabecera resumen semanal
                String[] headers = {"Semana", "Periodo", "Total General", "N° Pedidos"};
                rowIdx = createTableHeader(sheet, rowIdx, headers, headerStyle);

                // Filas por cada semana
                int semanaNum = 1;
                SimpleDateFormat sdf = new SimpleDateFormat("dd/MM");
                for (ReporteService.SemanaResumen semana : mensual.getSemanas()) {
                    Row row = sheet.createRow(rowIdx++);
                    int colIdx = 0;
                    addCell(row, colIdx++, "Semana " + semanaNum++, centeredStyle);
                    String periodo = sdf.format(semana.getInicio()) + " - " + sdf.format(semana.getFin());
                    addCell(row, colIdx++, periodo, centeredStyle);
                    addCell(row, colIdx++, semana.getTotalSemana(), centeredCurrencyStyle);
                    addCell(row, colIdx++, semana.getCantidadPedidos(), centeredStyle);
                }

                // Espacio antes de la tabla de pedidos
                rowIdx = addSeparatorRow(sheet, rowIdx, 6, separatorStyle);

                // Cabecera de pedidos
                String[] pedidosHeaders = {"N° Pedido", "Fecha Pagado", "Venta", "Recargo", "Total", "Usuario"};
                rowIdx = createTableHeader(sheet, rowIdx, pedidosHeaders, headerStyle);
                for (Pedido p : mensual.getPedidos()) {
                    Row row = sheet.createRow(rowIdx++);
                    int colIdx = 0;
                    addCell(row, colIdx++, p.getNumeroPedido(), centeredStyle);
                    addCell(row, colIdx++, p.getFechaPagado(), dateStyle);
                    // Calcular venta sin recargo
                    double ventaSinRecargo = p.getTotal() - p.getRecargo();
                    addCell(row, colIdx++, ventaSinRecargo, centeredCurrencyStyle);
                    addCell(row, colIdx++, p.getRecargo(), centeredCurrencyStyle);
                    addCell(row, colIdx++, p.getTotal(), centeredCurrencyStyle);
                    addCell(row, colIdx++, p.getUsuario() != null ? p.getUsuario().getNombre() : "", centeredStyle);
                }

                // Nombre de archivo con rango de fechas
                SimpleDateFormat sdfFile = new SimpleDateFormat("dd-MM-yyyy");
                String desde = mensual.getInicio() != null ? sdfFile.format(mensual.getInicio()) : "";
                String hasta = mensual.getFin() != null ? sdfFile.format(mensual.getFin()) : "";
                String sufijo = "_" + desde + "_a_" + hasta;
                if (!desde.isEmpty() && !hasta.isEmpty() && (nombreArchivo == null || !nombreArchivo.endsWith(sufijo))) {
                    nombreArchivo = nombreArchivo + sufijo;
                }
            }
            
            // Escribir el archivo
            String nombreFinal;
            if (reporte instanceof ReporteService.ReporteDiario && !fechaReporte.isEmpty()) {
                nombreFinal = nombreArchivo + "_" + fechaReporte + ".xlsx";
            } else if (reporte instanceof ReporteService.ReporteSemanal || reporte instanceof ReporteService.ReporteMensual) {
                // Para el reporte semanal y mensual, ya tienen el rango de fechas en nombreArchivo
                nombreFinal = nombreArchivo + ".xlsx";
            } else {
                // Para otros reportes, mantener la fecha y hora
                String fechaHora = new SimpleDateFormat("yyyy-MM-dd_HH-mm-ss").format(new Date());
                nombreFinal = nombreArchivo + "_" + fechaHora + ".xlsx";
            }
            response.setContentType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");
            response.setHeader("Content-Disposition", "attachment; filename=" + nombreFinal);
            workbook.write(response.getOutputStream());
            response.getOutputStream().flush();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
    
    // Métodos auxiliares para estilos
    
    private static CellStyle createHeaderStyle(Workbook workbook) {
        CellStyle style = workbook.createCellStyle();
        Font font = workbook.createFont();
        font.setFontName(FONT_NAME);
        font.setFontHeightInPoints(HEADER_FONT_SIZE);
        font.setBold(true);
        style.setFont(font);
        style.setAlignment(HorizontalAlignment.CENTER);
        style.setVerticalAlignment(VerticalAlignment.CENTER);
        style.setFillForegroundColor(IndexedColors.GREY_25_PERCENT.getIndex());
        style.setFillPattern(FillPatternType.SOLID_FOREGROUND);
        style.setBorderBottom(BorderStyle.THIN);
        style.setBorderTop(BorderStyle.THIN);
        style.setBorderLeft(BorderStyle.THIN);
        style.setBorderRight(BorderStyle.THIN);
        return style;
    }
    
    private static CellStyle createTitleStyle(Workbook workbook) {
        CellStyle style = workbook.createCellStyle();
        Font font = workbook.createFont();
        font.setFontName(FONT_NAME);
        font.setFontHeightInPoints(TITLE_FONT_SIZE);
        font.setBold(true);
        style.setFont(font);
        style.setAlignment(HorizontalAlignment.CENTER);
        style.setVerticalAlignment(VerticalAlignment.CENTER);
        return style;
    }
    
    private static CellStyle createDataStyle(Workbook workbook) {
        CellStyle style = workbook.createCellStyle();
        Font font = workbook.createFont();
        font.setFontName(FONT_NAME);
        font.setFontHeightInPoints(BODY_FONT_SIZE);
        style.setFont(font);
        style.setBorderBottom(BorderStyle.THIN);
        style.setBorderTop(BorderStyle.THIN);
        style.setBorderLeft(BorderStyle.THIN);
        style.setBorderRight(BorderStyle.THIN);
        style.setVerticalAlignment(VerticalAlignment.CENTER);
        return style;
    }
    
    private static CellStyle createCenteredDataStyle(Workbook workbook) {
        CellStyle style = createDataStyle(workbook);
        style.setAlignment(HorizontalAlignment.CENTER);
        return style;
    }
    
    private static CellStyle createCurrencyStyle(Workbook workbook) {
    CellStyle style = createDataStyle(workbook);
    style.setDataFormat(workbook.createDataFormat().getFormat("\"S/\"#,##0.00"));
    style.setAlignment(HorizontalAlignment.RIGHT);
    return style;
    }
    
    private static CellStyle createCenteredCurrencyStyle(Workbook workbook) {
        CellStyle style = createCurrencyStyle(workbook);
        style.setAlignment(HorizontalAlignment.CENTER);
        return style;
    }
    
    private static CellStyle createDateStyle(Workbook workbook) {
        CellStyle style = createCenteredDataStyle(workbook);
        style.setDataFormat(workbook.createDataFormat().getFormat("yyyy-MM-dd HH:mm"));
        return style;
    }
    
    private static CellStyle createSeparatorStyle(Workbook workbook) {
        CellStyle style = workbook.createCellStyle();
        style.setFillForegroundColor(IndexedColors.GREY_40_PERCENT.getIndex());
        style.setFillPattern(FillPatternType.SOLID_FOREGROUND);
        return style;
    }
    
    // Métodos auxiliares para manipulación de celdas
    
    private static int addGeneralInfo(Sheet sheet, int rowIdx, String[] labels, Object[] values, 
            CellStyle labelStyle, CellStyle valueStyle, CellStyle currencyStyle, CellStyle dateStyle) {
        for (int i = 0; i < labels.length; i++) {
            Row row = sheet.createRow(rowIdx++);
            row.setHeightInPoints(20);
            
            Cell labelCell = row.createCell(0);
            labelCell.setCellValue(labels[i]);
            labelCell.setCellStyle(labelStyle);
            
            Cell valueCell = row.createCell(1);
            if (values[i] instanceof Number) {
                valueCell.setCellValue(((Number) values[i]).doubleValue());
                valueCell.setCellStyle(currencyStyle);
            } else if (values[i] instanceof Date) {
                valueCell.setCellValue((Date) values[i]);
                valueCell.setCellStyle(dateStyle);
            } else {
                valueCell.setCellValue(values[i].toString());
                valueCell.setCellStyle(valueStyle);
            }
            
            // Combinar celdas para el valor
            sheet.addMergedRegion(new CellRangeAddress(row.getRowNum(), row.getRowNum(), 1, 6));
        }
        return rowIdx;
    }
    
    private static int createTableHeader(Sheet sheet, int rowIdx, String[] headers, CellStyle style) {
        return createTableHeader(sheet, rowIdx, headers, style, 0);
    }
    
    private static int createTableHeader(Sheet sheet, int rowIdx, String[] headers, CellStyle style, int startCol) {
        Row headerRow = sheet.createRow(rowIdx++);
        headerRow.setHeightInPoints(20);
        for (int i = 0; i < headers.length; i++) {
            Cell cell = headerRow.createCell(startCol + i);
            cell.setCellValue(headers[i]);
            cell.setCellStyle(style);
        }
        return rowIdx;
    }
    
    private static int addSeparatorRow(Sheet sheet, int rowIdx, int colsToMerge, CellStyle style) {
        Row separatorRow = sheet.createRow(rowIdx++);
        separatorRow.setHeightInPoints(6); // Altura pequeña para el separador
        
        Cell separatorCell = separatorRow.createCell(0);
        separatorCell.setCellValue("");
        separatorCell.setCellStyle(style);
        sheet.addMergedRegion(new CellRangeAddress(separatorRow.getRowNum(), separatorRow.getRowNum(), 0, colsToMerge - 1));
        
        return rowIdx;
    }
    
    private static void addCell(Row row, int colIdx, Object value, CellStyle style) {
        Cell cell = row.createCell(colIdx);
        if (value == null) {
            cell.setCellValue("");
        } else if (value instanceof Number) {
            cell.setCellValue(((Number) value).doubleValue());
        } else if (value instanceof Date) {
            cell.setCellValue((Date) value);
        } else {
            cell.setCellValue(value.toString());
        }
        cell.setCellStyle(style);
    }
}