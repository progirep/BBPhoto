/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package boardsaver;

import java.awt.AlphaComposite;
import java.awt.Graphics2D;
import java.awt.Image;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.Locale;
import javax.imageio.IIOImage;
import javax.imageio.ImageIO;
import javax.imageio.ImageWriteParam;
import javax.imageio.ImageWriter;
import javax.imageio.plugins.jpeg.JPEGImageWriteParam;
import javax.imageio.stream.ImageOutputStream;

/**
 * Some tools.
 * @author ehlers
 */
public class Tools {
 public static BufferedImage createResizedCopy(BufferedImage originalImage,
            int scaledWidth, int scaledHeight) {
        int imageType = originalImage.getType();
        BufferedImage scaledBI = new BufferedImage(scaledWidth, scaledHeight, imageType);
        Graphics2D g = scaledBI.createGraphics();
        g.drawImage(originalImage, 0, 0, scaledWidth, scaledHeight, null);
        g.dispose();
        return scaledBI;
    }

   static double euklid(double x, double y) {
        return Math.sqrt(x*x+y*y);
    }

 public static BufferedImage transformImage(BufferedImage src, int maxwidth, int maxheight, TransformationSettings settings, boolean enlargeImage, boolean useColor) {

        try {
            
            double X0 = settings.getPointX(0)*src.getWidth();
            double X1 = settings.getPointX(1)*src.getWidth();
            double X2 = settings.getPointX(2)*src.getWidth();
            double X3 = settings.getPointX(3)*src.getWidth();
            double Y0 = settings.getPointY(0)*src.getHeight();
            double Y1 = settings.getPointY(1)*src.getHeight();
            double Y2 = settings.getPointY(2)*src.getHeight();
            double Y3 = settings.getPointY(3)*src.getHeight();


            // Calculating width
            double imgwidth = (euklid(X1-X0,Y1-Y0)
            +  euklid(X2-X3,Y2-Y3))/2.0;
            double imgheight = (euklid(X2-X1,Y2-Y1)
            +  euklid(X3-X0,Y3-Y0))/2.0;

            if (Double.isNaN(imgwidth) || Double.isNaN(imgheight)) return null;

            int width = enlargeImage?maxwidth:(int)(Math.min(maxwidth,imgwidth));
            int height = enlargeImage?maxheight:(int)(Math.min(maxheight,imgheight));
            double factorX = (double)(imgwidth) / width;
            double factorY = (double)(imgheight) / height;
            if (factorX>factorY) {
                height = (int)(imgheight/factorX);
                factorY = (double)imgheight/height;
            } else {
                width = (int)(imgwidth/factorY);
                factorX = (double)imgwidth/width;
            }

            if ((width==0) || (height==0)) return null;

            // Create buffered Copy of Image
            // -> Copy, if possible & Color types match, otherwise draw into new BufferedImage of right type
            BufferedImage bSrc;
        /*if ((src instanceof BufferedImage)) {
            bSrc = (BufferedImage)src;
            //System.out.println("No recoding");
        } else */ {
                //System.out.print("Recoding!: ");
                //System.out.println("Original type: "+((BufferedImage)src).getType());
                if (useColor)
                    bSrc = new BufferedImage(src.getWidth(null),src.getHeight(null),BufferedImage.TYPE_3BYTE_BGR);
                else
                    bSrc = new BufferedImage(src.getWidth(null),src.getHeight(null),BufferedImage.TYPE_BYTE_GRAY);
                Graphics2D g = bSrc.createGraphics();
                g.drawImage(src,0,0,null);
                g.dispose();
        }

            // Create Image
            BufferedImage img;
            if (useColor)
                img = new BufferedImage(width,height,BufferedImage.TYPE_3BYTE_BGR);
            else
                img = new BufferedImage(width,height,BufferedImage.TYPE_BYTE_GRAY);

            // Draw onto Image
            double sx,sy;
            double px,py,mx,my;

            if (!useColor) {

                for (int y=0;y<height;y++) {

                    // Calculate relative Position
                    py = (double)y/height;
                    my = 1-py;

                    for (int x=0;x<width;x++) {

                        // Calculate relative position
                        px = (double)x/width;
                        mx = 1-px;

                        // calculate source position
                        sx = my*(px*X1+mx*X0) + py*(px*X2+mx*X3);
                        sy = my*(px*Y1+mx*Y0) + py*(px*Y2+mx*Y3);

                        // Calculate relevant pixels
                        int sx1 = (int)(Math.floor(sx));
                        int sy1 = (int)(Math.floor(sy));
                        int sx2 = (int)(Math.ceil(sx));
                        int sy2 = (int)(Math.ceil(sy));
                        int pixel_11_b = bSrc.getRGB(sx1,sy1) & 255;
                        int pixel_12_b = bSrc.getRGB(sx1,sy2) & 255;
                        int pixel_21_b = bSrc.getRGB(sx1,sy1) & 255;
                        int pixel_22_b = bSrc.getRGB(sx1,sy2) & 255;
                        double cpx = sx-sx1;
                        double cpy = sy-sy1;
                        double cmx = 1-cpx;
                        double cmy = 1-cpy;

                        int pixelgesamt_b = (int)(cmy*(pixel_11_b*cmx + pixel_21_b*cpx)+ cpy*(pixel_12_b*cmx + pixel_22_b*cpx));
                        img.setRGB(x,y,pixelgesamt_b*256+pixelgesamt_b*65536+pixelgesamt_b);
                        //int testing =  img.getRGB(x,y) & 255;
                        //System.out.println("pixel_gesamt: "+pixelgesamt_b+" - Geholt: "+testing);
                    }
                }
            } else {
                // Color
                for (int y=0;y<height;y++) {

                    // Calculate relative Position
                    py = (double)y/height;
                    my = 1-py;

                    for (int x=0;x<width;x++) {

                        // Calculate relative position
                        px = (double)x/width;
                        mx = 1-px;

                        // calculate source position
                        sx = my*(px*X1+mx*X0) + py*(px*X2+mx*X3);
                        sy = my*(px*Y1+mx*Y0) + py*(px*Y2+mx*Y3);

                        // Calculate relevant pixels
                        int sx1 = (int)(Math.floor(sx));
                        int sy1 = (int)(Math.floor(sy));
                        int sx2 = (int)(Math.ceil(sx));
                        int sy2 = (int)(Math.ceil(sy));
                        int pixel_11_b = bSrc.getRGB(sx1,sy1) & 255;
                        int pixel_12_b = bSrc.getRGB(sx1,sy2) & 255;
                        int pixel_21_b = bSrc.getRGB(sx1,sy1) & 255;
                        int pixel_22_b = bSrc.getRGB(sx1,sy2) & 255;
                        int pixel_11_g = (bSrc.getRGB(sx1,sy1) >> 8) & 255;
                        int pixel_12_g = (bSrc.getRGB(sx1,sy2) >> 8) & 255;
                        int pixel_21_g = (bSrc.getRGB(sx1,sy1) >> 8) & 255;
                        int pixel_22_g = (bSrc.getRGB(sx1,sy2) >> 8) & 255;
                        int pixel_11_r = (bSrc.getRGB(sx1,sy1) >> 16) & 255;
                        int pixel_12_r = (bSrc.getRGB(sx1,sy2) >> 16) & 255;
                        int pixel_21_r = (bSrc.getRGB(sx1,sy1) >> 16) & 255;
                        int pixel_22_r = (bSrc.getRGB(sx1,sy2) >> 16) & 255;
                        double cpx = sx-sx1;
                        double cpy = sy-sy1;
                        double cmx = 1-cpx;
                        double cmy = 1-cpy;

                        int pixelgesamt_b = (int)(cmy*(pixel_11_b*cmx + pixel_21_b*cpx)+ cpy*(pixel_12_b*cmx + pixel_22_b*cpx));
                        int pixelgesamt_g = (int)(cmy*(pixel_11_g*cmx + pixel_21_g*cpx)+ cpy*(pixel_12_g*cmx + pixel_22_g*cpx));
                        int pixelgesamt_r = (int)(cmy*(pixel_11_r*cmx + pixel_21_r*cpx)+ cpy*(pixel_12_r*cmx + pixel_22_r*cpx));
                        img.setRGB(x,y,pixelgesamt_g*256+pixelgesamt_r*65536+pixelgesamt_b);
                        //int testing =  img.getRGB(x,y) & 255;
                        //System.out.println("pixel_gesamt: "+pixelgesamt_b+" - Geholt: "+testing);
                    }
                }
            }

            // Done!
            return img;
        } catch (ArrayIndexOutOfBoundsException e) {
            // Invalid coordinates for trapezoid transformation
            throw new RuntimeException("Out of image bounds.");
        }

    }


  /**
     * Kopiert von: Java ist auch eine Insel!!!!!!!!!!!
     */
    public static void writeJPGImage(BufferedImage img, ByteArrayOutputStream out,
            float quality) throws IOException {
        ImageWriter writer = ImageIO.getImageWritersByFormatName("jpg").next();
        ImageOutputStream ios = ImageIO.createImageOutputStream(out);
        writer.setOutput(ios);
        ImageWriteParam iwparam = new JPEGImageWriteParam(Locale.getDefault());
        iwparam.setCompressionMode(ImageWriteParam.MODE_EXPLICIT);
        iwparam.setCompressionQuality(quality);
        writer.write(null, new IIOImage(img, null, null), iwparam);
        ios.flush();
        writer.dispose();
        ios.close();
    }

    /**
     * Takes a buffered image and creates a copy for which the lowest 4 bits of
     * all color bytes are truncated to 0.
     * @param img The source image. It is not changed during this operation.
     * @return The filtered image.
     */
    public static BufferedImage reduceColors(BufferedImage img) {
        int imageType = img.getType();
        BufferedImage newImage = new BufferedImage(img.getWidth(), img.getHeight(), imageType);

        int map[] = new int[256];
        for (int i=0;i<=255;i++) {
            map[i] = Math.min(255,Math.round(i*16/255f)*16);
        }

        for (int x=0;x<img.getWidth();x++) {
            for (int y=0;y<img.getHeight();y++) {
                int value = img.getRGB(x,y);
                
                newImage.setRGB(x,y,(value&0xFF000000)  |
                        map[(value&0xFF)] |
                        (map[((value >> 8)&0xFF)] << 8) |
                        (map[((value >> 16)&0xFF)] << 16) |
                        (map[((value >> 24)&0xFF)] << 24));
            }
        }
        return newImage;
    }
  
}
