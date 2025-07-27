package com.encuestas.incide.services;

import net.sf.jasperreports.engine.*;
import net.sf.jasperreports.engine.export.JRPdfExporter;
import net.sf.jasperreports.export.SimpleExporterInput;
import net.sf.jasperreports.export.SimpleOutputStreamExporterOutput;
import net.sf.jasperreports.export.SimplePdfExporterConfiguration;
import org.commonmark.node.Node;
import org.commonmark.parser.Parser;
import org.commonmark.renderer.html.HtmlRenderer;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.springframework.stereotype.Component;


import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;
@Component
public class JasperReportGeneratorV7 {

    /**
     * Genera un PDF de JasperReports a partir de un String Markdown.
     *
     * @param markdownText El texto enriquecido en formato Markdown.
     * @return Un array de bytes que representa el PDF generado.
     * @throws JRException Si ocurre un error durante la generación del reporte.
     */
    public byte[] generatePdfFromMarkdown(String markdownText, String stage) throws JRException, IOException {
        // 1. Convertir Markdown a HTML
        String htmlContent = convertMarkdownToHtml(markdownText);

        // 2. Cargar la plantilla JRXML
        InputStream jrxmlStream = getClass().getResourceAsStream("/templates/reporte_pdf.jrxml");
        if (jrxmlStream == null) {
            throw new JRException("No se encontró la plantilla JRXML: reporte_pdf.jrxml");
        }

        // 3. Compilar la plantilla
        JasperReport jasperReport = JasperCompileManager.compileReport(jrxmlStream);

        // 4. Crear mapa de parámetros (el nombre debe coincidir con el definido en el JRXML)
        Map<String, Object> parameters = new HashMap<>();
        InputStream logoStream = getClass().getResourceAsStream("/templates/ybs-bg.png");
        BufferedImage logoImage = ImageIO.read(logoStream);
        parameters.put("logo", logoImage);
        parameters.put("stage", stage);
        System.out.println(stage);
        String htmlParaJasper = htmlContent
                .replaceAll("(?i)<strong>", "<b>")
                .replaceAll("(?i)</strong>", "</b>");
        parameters.put("richText", addMarginToParagraphs(htmlParaJasper));  // 👈 clave: usar "richText"
        System.out.println("Contenido HTML generado:\n" + addMarginToParagraphs(htmlParaJasper));
        // 5. Generar el informe con JREmptyDataSource
        JasperPrint jasperPrint = JasperFillManager.fillReport(jasperReport, parameters, new JREmptyDataSource());

        // 6. Exportar a PDF
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        JRPdfExporter exporter = new JRPdfExporter();

        exporter.setExporterInput(new SimpleExporterInput(jasperPrint));
        exporter.setExporterOutput(new SimpleOutputStreamExporterOutput(outputStream));

        SimplePdfExporterConfiguration configuration = new SimplePdfExporterConfiguration();
        exporter.setConfiguration(configuration);

        exporter.exportReport();

        return outputStream.toByteArray();
    }

    /**
     * Convierte Markdown a HTML.
     */
    private String convertMarkdownToHtml(String markdown) {
        Parser parser = Parser.builder().build();
        Node document = parser.parse(markdown);
        HtmlRenderer renderer = HtmlRenderer.builder().build();

        String html = renderer.render(document);
        html = html.replace("<p>", "<br><p>");
        html = html.replace("</p>", "</p><br>");
       html = html.replace("</li>", "</li><br>");
        html = html.replace("</ol>", "</ol><br><br>");


        return html;
    }

    public String addMarginToParagraphs(String html) {
        Document doc = Jsoup.parse(html);
        Elements paragraphs = doc.select("p");
        for (Element p : paragraphs) {
            p.attr("style", "margin-bottom:10px;");
        }
        return doc.body().html(); // devuelve el HTML con el estilo agregado
    }

}
