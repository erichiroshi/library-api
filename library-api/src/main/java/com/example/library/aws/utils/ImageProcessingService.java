package com.example.library.aws.utils;

import java.awt.Graphics2D;
import java.awt.Image;
import java.awt.Transparency;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;

import javax.imageio.ImageIO;

import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

@Service
public class ImageProcessingService {
    
    public MultipartFile compressImage(MultipartFile file, int maxWidth) throws IOException {
        BufferedImage original = ImageIO.read(file.getInputStream());
        
        if (original == null) {
            throw new IOException("Invalid or corrupted image file");
        }
        
        if (original.getWidth() <= maxWidth) {
            return file;  // // Retorna original sem processar
        }
        
        // Calcular nova altura mantendo aspect ratio
        int newHeight = (int) ((original.getHeight() * maxWidth) / (double) original.getWidth());
        
        int imageType = original.getTransparency() == Transparency.OPAQUE 
					        	    ? BufferedImage.TYPE_INT_RGB 
					        	    : BufferedImage.TYPE_INT_ARGB;  // ← Preserva transparência

    	BufferedImage resized = new BufferedImage(maxWidth, newHeight, imageType);
    	Graphics2D graphics = resized.createGraphics();
        graphics.drawImage(original.getScaledInstance(maxWidth, newHeight, Image.SCALE_SMOOTH), 0, 0, null);
        graphics.dispose();
        
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        ImageIO.write(resized, "jpg", baos);
        
        return new CustomMultipartFile(
            file.getName(),
            file.getOriginalFilename(),
            "image/jpeg",
            baos.toByteArray()
        );
    }
}