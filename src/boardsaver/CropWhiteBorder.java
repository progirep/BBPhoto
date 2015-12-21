/*
 * Automatically removes white border
 *
 * (C) 2012 by Ruediger Ehlers
 */

package boardsaver;

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;

/**
 *
 * @author ehlers
 */
public class CropWhiteBorder {

    public static BufferedImage crop(BufferedImage src) {

        // Analyse image
        int maxX = src.getWidth();
        int maxY = src.getHeight();
        int minX = 0;
        int minY = 0;
        int threshold = 256*3*9/10;
        Color _white = new Color(255, 255, 255, 0);
        int white = _white.getRGB();

        // Get left border
        boolean foundLeft = false;
        for (int i=0;(i<src.getWidth()) && (!foundLeft);i++) {
            for (int j=0;(j<src.getHeight()) && (!foundLeft);j++) {
                int pixel = src.getRGB(i,j);
                int red = (pixel >> 16) & 0xff;
                int green = (pixel >> 8) & 0xff;
                int blue = (pixel) & 0xff;
                if (red+green+blue <= threshold) { // Almost full white
                    minX = i;
                    foundLeft = true;
                }
            }
        }
        minX = Math.max(0,minX-5);

        // Get right border
        boolean foundRight = false;
        for (int i=src.getWidth()-1;(i>=0) && (!foundRight);i--) {
            for (int j=0;(j<src.getHeight()) && (!foundRight);j++) {
                int pixel = src.getRGB(i,j);
                int red = (pixel >> 16) & 0xff;
                int green = (pixel >> 8) & 0xff;
                int blue = (pixel) & 0xff;
                if (red+green+blue <= threshold) { // Almost full white
                    maxX = i;
                    foundRight = true;
                }
            }
        }
        maxX = Math.min(src.getWidth(),maxX+5);

        // Get bottom border
        boolean foundBottom = false;
        for (int i=src.getHeight()-1;(i>=0) && (!foundBottom);i--) {
            for (int j=0;(j<src.getWidth()) && (!foundBottom);j++) {
                int pixel = src.getRGB(j,i);
                int red = (pixel >> 16) & 0xff;
                int green = (pixel >> 8) & 0xff;
                int blue = (pixel) & 0xff;
                if (red+green+blue <= threshold) { // Almost full white
                    maxY = i;
                    foundBottom = true;
                }
            }
        }
        maxY = Math.min(src.getHeight(),maxY+5);

        // Get left border
        boolean foundTop = false;
        for (int i=0;(i<src.getHeight()) && (!foundTop);i++) {
            for (int j=0;(j<src.getWidth()) && (!foundTop);j++) {
                int pixel = src.getRGB(j,i);
                int red = (pixel >> 16) & 0xff;
                int green = (pixel >> 8) & 0xff;
                int blue = (pixel) & 0xff;
                if (red+green+blue <= threshold) { // Almost full white
                    minY = i;
                    foundTop = true;
                }
            }
        }
        minY = Math.max(0,minY-5);




        // Render
        BufferedImage result = new BufferedImage(maxX-minX,maxY-minY,src.getType());
        Graphics2D g = (Graphics2D)result.getGraphics();
        g.drawImage(src, -minX, -minY, null);
        g.dispose();
        return result;

    }

}
