/*
 * HistogramFilter.java
 *
 * Created on 15. Januar 2007, 19:22
 *
 * To change this template, choose Tools | Template Manager
 * and open the template in the editor.
 */

package boardsaver;

import java.awt.image.BufferedImage;

/**
 *
 * @author Administrator
 */
public class GradientReductionFilter  {
    
    public static final String SETTING_OVERSHOOTBLACK = "G_BLACK";
    public static final String SETTING_OVERSHOOTWHITE = "G_WHITE";
    public static final int MAX_PIXELS = 3000000;
    
    private GradientReductionFilterSettings settings;
    
    /** Creates a new instance of HistogramFilter */
    public GradientReductionFilter() {
    }
    
    public void setSettings( GradientReductionFilterSettings settings) {
        this.settings = settings;
    }

    /**
     * This function performs the filter operation.
     * @param image The image to be filtered. Note that it might be altered.
     * @return The new image. Might actually be the same image as "image", but does not have to be!
     */
    public BufferedImage filterImage(BufferedImage image) {

        if (image==null) return null;

        // Resize image if necessary (>3MP)
        int nofPixels = image.getHeight()*image.getWidth();
        if (nofPixels>MAX_PIXELS) {
            int width = (int)((double)image.getWidth()*MAX_PIXELS/nofPixels);
            int height = (int)((double)image.getHeight()*MAX_PIXELS/nofPixels);
            image = Tools.createResizedCopy(image,width,height);
        }
        
        // Step 1: Calculate the average of the x-c to x+c pixels in one Line
        // Also calculate the maximum darkness
        
        // Line buffers
        // Adressing: Y first, then X!
        int[][] rLine = new int[image.getHeight()][image.getWidth()];
        int[][] gLine = new int[image.getHeight()][image.getWidth()];
        int[][] bLine = new int[image.getHeight()][image.getWidth()];
        
        // What is the pixel Range (the value "c"?)
        final int pixelRange = Math.max(1,Math.min(image.getWidth()/10,image.getHeight()/10));
        int maxDarkness = 255;
        
        // Create RGB-Version of raw image
        int[][] rawR = new int[image.getHeight()][image.getWidth()];
        int[][] rawG = new int[image.getHeight()][image.getWidth()];
        int[][] rawB = new int[image.getHeight()][image.getWidth()];
        {
            int[] rawImage = new int[image.getHeight()*image.getWidth()];
            image.getRGB(0,0,image.getWidth(),image.getHeight(),rawImage,0,image.getWidth());
            for (int y=0;y<image.getHeight();y++) {
                int[] currentRLine = rawR[y];
                int[] currentGLine = rawG[y];
                int[] currentBLine = rawB[y];
                int yOffset = y*image.getWidth();
                for (int x=0;x<image.getWidth();x++) {
                    final int pixel = rawImage[yOffset+x];
                    int r = pixel & 255;
                    int g = (pixel >> 8) & 255;
                    int b = (pixel >> 16) & 255;
                    maxDarkness = Math.min((r+g+b)/3,maxDarkness);
                    currentRLine[x] = r;
                    currentGLine[x] = g;
                    currentBLine[x] = b;
                }
            }
        }
        
        for (int y=0;y<image.getHeight();y++) {
            
            // Calculate starting values
            int sumR = 0;
            int sumG = 0;
            int sumB = 0;
            int[] currentRLine = rLine[y];
            int[] currentGLine = gLine[y];
            int[] currentBLine = bLine[y];
            int[] currentRRaw = rawR[y];
            int[] currentGRaw = rawG[y];
            int[] currentBRaw = rawB[y];
            
            int yOffset = y*image.getWidth();
            for (int i=0;i<pixelRange;i++) {
                sumR += currentRRaw[i];
                sumG += currentGRaw[i];
                sumB += currentBRaw[i];
            }
            
            // Process the first pixelRange pixels
            for (int i=0;i<=pixelRange;i++) {
                sumR += currentRRaw[i+pixelRange];
                sumG += currentGRaw[i+pixelRange];
                sumB += currentBRaw[i+pixelRange];
                currentRLine[i] = sumR / (pixelRange+i+1);
                currentGLine[i] = sumG / (pixelRange+i+1);
                currentBLine[i] = sumB / (pixelRange+i+1);
            }
            
            // Process the middle pixels
            for (int i=pixelRange+1;i<image.getWidth()-pixelRange;i++) {
                sumR += currentRRaw[i+pixelRange] - currentRRaw[i-pixelRange-1];
                sumG += currentGRaw[i+pixelRange] - currentGRaw[i-pixelRange-1];
                sumB += currentBRaw[i+pixelRange] - currentBRaw[i-pixelRange-1];
                currentRLine[i] = sumR / (2*pixelRange+1);
                currentGLine[i] = sumG / (2*pixelRange+1);
                currentBLine[i] = sumB / (2*pixelRange+1);
            }
            
            // Process the very right Pixels
            for (int i=image.getWidth()-pixelRange;i<image.getWidth();i++) {
                sumR -= currentRRaw[i-pixelRange-1];
                sumG -= currentGRaw[i-pixelRange-1];
                sumB -= currentBRaw[i-pixelRange-1];
                currentRLine[i] = sumR / (pixelRange+image.getWidth()-i);
                currentGLine[i] = sumG / (pixelRange+image.getWidth()-i);
                currentBLine[i] = sumB / (pixelRange+image.getWidth()-i);
                
            }
            
        }
        
        // Calculate ground color (the other direction)
        // Line buffers
        // Adressing: Y first, then X!
        int[][] rNoiseLine = new int[image.getHeight()][image.getWidth()];
        int[][] gNoiseLine = new int[image.getHeight()][image.getWidth()];
        int[][] bNoiseLine = new int[image.getHeight()][image.getWidth()];
        for (int y=0;y<image.getHeight();y++) {
            
            // Calculate starting values
            int[] currentRNoiseLine = rNoiseLine[y];
            int[] currentGNoiseLine = gNoiseLine[y];
            int[] currentBNoiseLine = bNoiseLine[y];
            
            int yOffset = y*image.getWidth();
            
            // For the first lines:
            if (y<pixelRange) {
                for (int x=0;x<image.getWidth();x++) {
                    int sumR = 0;
                    int sumG = 0;
                    int sumB = 0;
                    for (int i=0;i<=y+pixelRange;i++) {
                        sumR += rLine[i][x];
                        sumG += gLine[i][x];
                        sumB += bLine[i][x];
                    }
                    currentRNoiseLine[x] = sumR / (y+pixelRange+1);
                    currentGNoiseLine[x] = sumG / (y+pixelRange+1);
                    currentBNoiseLine[x] = sumB / (y+pixelRange+1);
                }
            } else if (y>=image.getHeight()-pixelRange) {
                for (int x=0;x<image.getWidth();x++) {
                    int sumR = 0;
                    int sumG = 0;
                    int sumB = 0;
                    for (int i=y-pixelRange;i<image.getHeight();i++) {
                        sumR += rLine[i][x];
                        sumG += gLine[i][x];
                        sumB += bLine[i][x];
                    }
                    int lines = image.getHeight()-y;
                    currentRNoiseLine[x] = sumR / (image.getHeight()-y+pixelRange);
                    currentGNoiseLine[x] = sumG / (image.getHeight()-y+pixelRange);
                    currentBNoiseLine[x] = sumB / (image.getHeight()-y+pixelRange);
                }
            } else {
                for (int x=0;x<image.getWidth();x++) {
                    int sumR = 0;
                    int sumG = 0;
                    int sumB = 0;
                    for (int i=y-pixelRange;i<=y+pixelRange;i++) {
                        sumR += rLine[i][x];
                        sumG += gLine[i][x];
                        sumB += bLine[i][x];
                    }
                    int lines = image.getHeight()-y;
                    currentRNoiseLine[x] = sumR / (1+2*pixelRange);
                    currentGNoiseLine[x] = sumG / (1+2*pixelRange);
                    currentBNoiseLine[x] = sumB / (1+2*pixelRange);
                }
                
            }
        }
        
        // Berechne Wei�overshoot in %
        // double overshootWhite = 255*(1.0+settings.getSetting(SETTING_OVERSHOOTWHITE)*2);
        
        
        // Debug! Set RGB values
        double maxBrightnessMultiplier = 255.0/(255 - maxDarkness);
        double pb = settings.getOverShootWhite();
        double pbpd = (1-pb)*settings.getOverShootBlack();
        double nenner = (pb+pbpd);
        for (int y=0;y<image.getHeight();y++) {
            for (int x=0;x<image.getWidth();x++) {
                //double r = Math.max(0,Math.min(255,(rawR[y][x] - rNoiseLine[y][x])/(maxBrightness - rNoiseLine[y][x])*overshootWhite));
                //double g = Math.max(0,Math.min(255,(rawG[y][x] - gNoiseLine[y][x])/(maxBrightness - gNoiseLine[y][x])*overshootWhite));
                //double b = Math.max(0,Math.min(255,(rawB[y][x] - bNoiseLine[y][x])/(maxBrightness - bNoiseLine[y][x])*overshootWhite));

                double rNA = (255-rNoiseLine[y][x]*maxBrightnessMultiplier);
                double rR = rawR[y][x]*maxBrightnessMultiplier;
                double r = Math.max(0,Math.min(255,
                        (255 * (rR - pbpd*(255-rNA)))
                        /
                        (255 - nenner * (255-rNA) - rNA)
                        ));
                double gNA = (255-gNoiseLine[y][x]*maxBrightnessMultiplier);
                double gR = rawG[y][x]*maxBrightnessMultiplier;
                double g = Math.max(0,Math.min(255,
                        (255 * (gR - pbpd*(255-gNA)))
                        /
                        (255 - nenner * (255-gNA) - gNA)
                        ));
                double bNA = (255-bNoiseLine[y][x]*maxBrightnessMultiplier);
                double bR = rawB[y][x]*maxBrightnessMultiplier;
                double b = Math.max(0,Math.min(255,
                        (255 * (bR - pbpd*(255-bNA)))
                        /
                        (255 - nenner * (255-bNA) - bNA)
                        ));
                
                image.setRGB(x,y,((int)r)+((int)g)*256+((int)b)*65536);
                
                
            }
        }
        
        return image;
        
    }
}