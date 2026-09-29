package com.example.shopping.supplier.document.validation;

import com.example.shopping.supplier.document.model.ValidatedSupplierDocument;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.zip.*;
import org.apache.poi.poifs.filesystem.POIFSFileSystem;
import org.springframework.http.HttpStatus;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

public final class SupplierDocumentValidator {
    public static final int MAX_BYTES = 10 * 1024 * 1024;
    private SupplierDocumentValidator() {}

    public static ValidatedSupplierDocument validate(MultipartFile file) throws IOException {
        if (file == null || file.isEmpty()) 
            throw invalid("Vui lòng chọn file có nội dung.");
        if (file.getSize() > MAX_BYTES) 
            throw new ResponseStatusException(HttpStatus.PAYLOAD_TOO_LARGE, "File tối đa 10 MB.");
        String name = Objects.toString(file.getOriginalFilename(), "").replace('\\', '/');
        name = name.substring(name.lastIndexOf('/') + 1).replaceAll("[\\p{Cntrl}]", "").strip();
        if (name.isBlank() || name.length() > 200) 
            throw invalid("Tên file phải có từ 1 đến 200 ký tự.");
        String extension = name.substring(name.lastIndexOf('.') + 1).toLowerCase(Locale.ROOT);
        byte[] bytes;
        try (var stream = file.getInputStream()) { bytes = stream.readNBytes(MAX_BYTES + 1); }
        if (bytes.length > MAX_BYTES) 
            throw new ResponseStatusException(HttpStatus.PAYLOAD_TOO_LARGE, "File tối đa 10 MB.");
        String mime = switch (extension) {
            case "pdf" -> starts(bytes, "%PDF-") ? "application/pdf" : null;
            case "png" -> starts(bytes, new byte[]{(byte)137,80,78,71,13,10,26,10}) ? "image/png" : null;
            case "jpg", "jpeg" -> starts(bytes, new byte[]{(byte)255,(byte)216,(byte)255}) ? "image/jpeg" : null;
            case "gif" -> starts(bytes, "GIF87a") || starts(bytes, "GIF89a") ? "image/gif" : null;
            case "bmp" -> starts(bytes, "BM") ? "image/bmp" : null;
            case "webp" -> bytes.length >= 12 && starts(bytes, "RIFF") && new String(bytes, 8, 4, StandardCharsets.US_ASCII).equals("WEBP") ? "image/webp" : null;
            case "docx" -> officeZip(bytes, "word/document.xml") ? "application/vnd.openxmlformats-officedocument.wordprocessingml.document" : null;
            case "xlsx" -> officeZip(bytes, "xl/workbook.xml") ? "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet" : null;
            case "doc" -> legacyOffice(bytes, "WordDocument") ? "application/msword" : null;
            case "xls" -> legacyOffice(bytes, "Workbook") || legacyOffice(bytes, "Book") ? "application/vnd.ms-excel" : null;
            default -> null;
        };
        if (mime == null) 
            throw invalid("File không hợp lệ. Chỉ nhận PDF, Word, Excel, PNG, JPG, GIF, BMP hoặc WebP đúng định dạng.");
        return new ValidatedSupplierDocument(name, mime, bytes);
    }
    private static boolean starts(byte[] bytes, String prefix) { 
        return starts(bytes, prefix.getBytes(StandardCharsets.US_ASCII)); }
    private static boolean starts(byte[] bytes, byte[] prefix) {
        return bytes.length >= prefix.length && Arrays.equals(bytes, 0, prefix.length, prefix, 0, prefix.length);
    }
    private static boolean legacyOffice(byte[] bytes, String entry) {
        if (!starts(bytes, new byte[]{(byte)0xD0,(byte)0xCF,0x11,(byte)0xE0,(byte)0xA1,(byte)0xB1,0x1A,(byte)0xE1})) 
            return false;
        try (var fs = new POIFSFileSystem(
            new ByteArrayInputStream(bytes))) { 
            return fs.getRoot().hasEntry(entry); 
        }
        catch (IOException | RuntimeException e) { 
            return false; 
        }
    }
    private static boolean officeZip(byte[] bytes, String required) {
        boolean main = false, types = false; long expanded = 0; int entries = 0;
        try (var zip = new ZipInputStream(new ByteArrayInputStream(bytes))) {
            ZipEntry entry; byte[] buffer = new byte[8192];
            while ((entry = zip.getNextEntry()) != null) {
                if (++entries > 2000 || entry.getName().toLowerCase(Locale.ROOT).endsWith("vbaproject.bin")) 
                    return false;
                main |= entry.getName().equals(required); types |= entry.getName().equals("[Content_Types].xml");
                int count;
                while ((count = zip.read(buffer)) != -1) { expanded += count; if (expanded > 50L * 1024 * 1024) 
                    return false; 
                }
            }
            return main && types;
        } catch (IOException e) { 
            return false; 
        }
    }
    private static ResponseStatusException invalid(String message) { 
        return new ResponseStatusException(HttpStatus.BAD_REQUEST, message); 
    }
}
