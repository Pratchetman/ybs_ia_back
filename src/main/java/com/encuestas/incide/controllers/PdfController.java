package com.encuestas.incide.controllers;

import com.encuestas.incide.services.JasperReportGeneratorV7;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletResponse;
import net.sf.jasperreports.engine.JRException;
import net.sourceforge.tess4j.Tesseract;
import net.sourceforge.tess4j.TesseractException;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.rendering.PDFRenderer;
import org.apache.poi.xssf.usermodel.XSSFSheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.*;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.file.Files;
import java.util.Map;

@RestController
@CrossOrigin(origins = {"http://localhost:3000", "https://ybs-ia.arrabalemprende.org"}) // Cambia al dominio de tu frontendy
@RequestMapping("/pdf")
public class PdfController {

    @Value("${openai.api-key}")
    private String openAiApiKey;

    @Value("${router.ai-key}")
    private String routerAiKey;
    private final String openAiEndpoint = "https://api.openai.com/v1/chat/completions";

    private final String openrRouterAiEndPoint = "https://openrouter.ai/api/v1/chat/completions";

    @Autowired
    private JasperReportGeneratorV7 jasperReportGeneratorV7;

    @GetMapping("/health")
    public String healthCheck() {
        return "OK_YBS";
    }
    @PostMapping(value = "/incide", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public void uploadPdf(
            @RequestParam("file") MultipartFile file,
            HttpServletResponse response
    ) throws IOException {

        // 1. Cargar PDF
        PDDocument document = PDDocument.load(file.getInputStream());
        PDFRenderer renderer = new PDFRenderer(document);

        // 2. Configurar OCR
        Tesseract tesseract = new Tesseract();
        tesseract.setDatapath("C:/Users/Empleo1/AppData/Local/Programs/Tesseract-OCR/"); // Cambia a la ruta correcta en tu sistema
        tesseract.setLanguage("spa");

        // 3. Crear Excel
        XSSFWorkbook workbook = new XSSFWorkbook();
        XSSFSheet sheet = workbook.createSheet("Encuestas");
        sheet.createRow(0).createCell(0).setCellValue("Puntuación");
        sheet.getRow(0).createCell(1).setCellValue("Satisfecho");

        for (int i = 0; i < document.getNumberOfPages(); i++) {
            BufferedImage image = renderer.renderImageWithDPI(i, 300);

            // Guardar imagen temporal
            File tempImg = File.createTempFile("page_" + i, ".jpg");
            ImageIO.write(image, "jpg", tempImg);

            // OCR
            String result;
            try {
                result = tesseract.doOCR(tempImg);
            } catch (TesseractException e) {
                result = "Error OCR";
            }

            // Extraer datos (simplificado)
            String puntuacion = extractValue(result, "Puntuación");
            boolean satisfecho = result.contains("X Satisfecho");

            // Escribir en Excel
            int rowIndex = i + 1;
            sheet.createRow(rowIndex).createCell(0).setCellValue(puntuacion);
            sheet.getRow(rowIndex).createCell(1).setCellValue(satisfecho ? "Sí" : "No");

            tempImg.delete(); // Eliminar archivo temporal
        }

        document.close();

        // 4. Devolver Excel como descarga
        response.setContentType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");
        response.setHeader("Content-Disposition", "attachment; filename=resultado.xlsx");

        OutputStream out = response.getOutputStream();
        workbook.write(out);
        out.flush();
        workbook.close();
    }

    @PostMapping("/plan")
    public String analizarPlanEmpresa(@RequestBody Map<String, Object> payload) throws IOException {
        // Extraer valores del JSON recibido
        String tipoNegocio = (String) payload.get("tipoNegocio");
        Object importeObj = payload.get("importe");

        // Convertir importe a String o número según convenga
        String importeStr = importeObj != null ? importeObj.toString() : "0";

        // Construir prompt con los valores recibidos
        String prompt = String.format("Puedes darme un ejemplo de plan de empresa en español para un negocio de %s con %s€ de presupuesto?", tipoNegocio, importeStr);
        System.out.println(prompt);
        // Aquí llamarías a ChatGPT con ese prompt
        return llamarAChatGPT(prompt);
    }

    @PostMapping("/ybs/fases")
    public String analizar(@RequestBody String prompt) throws IOException {

        System.out.println(prompt);
        // Aquí llamarías a ChatGPT con ese prompt
        return llamarAChatGPT(prompt);
    }

    @PostMapping("/ybs/fases/pdf")
    public ResponseEntity<byte[]> makePdf(@RequestBody Map<String, Object> payload) {
        String pdfRichText = (String) payload.get("pdfRichText");
        String stage = (String) payload.get("stage");
        try {
            byte[] pdfBytes = jasperReportGeneratorV7.generatePdfFromMarkdown(pdfRichText, stage);

            HttpHeaders headers = new HttpHeaders();
            // 1. Indicar que la respuesta es un PDF
            headers.setContentType(MediaType.APPLICATION_PDF);

            String fileName = "reporte_fase.pdf";
            // 2. Sugerir un nombre de archivo para la descarga
            // "attachment" fuerza la descarga, "inline" intentaría mostrarlo en el navegador
            headers.setContentDispositionFormData("inline", fileName);

            // 3. Devolver el PDF con un estado HTTP 200 OK
            return new ResponseEntity<>(pdfBytes, headers, org.springframework.http.HttpStatus.OK);

        } catch (JRException e) {
            // Manejo de errores: loggea la excepción y devuelve un error 500
            e.printStackTrace(); // En producción, usa un logger apropiado (ej. SLF4J con Logback)
            return new ResponseEntity<>(null, org.springframework.http.HttpStatus.INTERNAL_SERVER_ERROR);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    private String extraerTextoDePdf(MultipartFile file) throws IOException {
        PDDocument document = PDDocument.load(file.getInputStream());
        PDFRenderer renderer = new PDFRenderer(document);

        Tesseract tesseract = new Tesseract();
        tesseract.setDatapath("C:/Users/Empleo1/AppData/Local/Programs/Tesseract-OCR/");
        tesseract.setLanguage("spa");

        StringBuilder textoCompleto = new StringBuilder();

        for (int i = 0; i < document.getNumberOfPages(); i++) {
            BufferedImage image = renderer.renderImageWithDPI(i, 300);
            File tempImg = File.createTempFile("page_" + i, ".jpg");
            ImageIO.write(image, "jpg", tempImg);

            try {
                textoCompleto.append(tesseract.doOCR(tempImg)).append("\n");
            } catch (TesseractException e) {
                textoCompleto.append("[ERROR OCR página ").append(i).append("]\n");
            }

            Files.deleteIfExists(tempImg.toPath());
        }

        document.close();
        return textoCompleto.toString();
    }

    private String llamarAChatGPT(String prompt) throws IOException {
        HttpClient client = HttpClient.newHttpClient();
        ObjectMapper objectMapper = new ObjectMapper(); // Instancia de ObjectMapper
        String requestBody = """
        {
          "model": "google/gemma-3-27b-it:free",
          "max_tokens": 4000,
          "messages": [
            {
              "role": "user",
              "content": [
                {
                  "type": "text",
                  "text": "%s"
                }
              ]
            }
          ]
        }
    """.formatted(prompt.replace("\"", "\\\"")); // Escapar comillas dobles

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(openrRouterAiEndPoint))
                .header("Content-Type", "application/json")
                .header("Authorization", "Bearer " + routerAiKey)
                .POST(HttpRequest.BodyPublishers.ofString(requestBody))
                .build();

        try {
            HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
            // **Parsear el JSON y extraer el 'content'**
            JsonNode rootNode = objectMapper.readTree(response.body());

            String content = "No se pudo extraer la respuesta."; // Valor por defecto

            if (rootNode.has("choices") && rootNode.get("choices").isArray()) {
                JsonNode choices = rootNode.get("choices");
                if (choices.size() > 0) {
                    JsonNode firstChoice = choices.get(0);
                    if (firstChoice.has("message") && firstChoice.get("message").has("content")) {
                        content = firstChoice.get("message").get("content").asText();
                    }
                }
            }
            return response.body();
        }catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            return "Error al llamar a la IA (interrupción).";
        } catch (Exception e) { // Captura cualquier otra excepción de parsing JSON
            e.printStackTrace(); // Imprime la traza para depuración
            return "Error al parsear la respuesta de la IA: " + e.getMessage();
        }
    }

    private String extractValue(String texto, String campo) {
        for (String linea : texto.split("\n")) {
            if (linea.toLowerCase().contains(campo.toLowerCase())) {
                String[] partes = linea.split(":");
                if (partes.length > 1) return partes[1].trim();
            }
        }
        return "No detectado";
    }
}

