/*
 * SourceImagePanel.java
 *
 * Created on 14. Januar 2007, 18:37
 *
 * To change this template, choose Tools | Template Manager
 * and open the template in the editor.
 */

package boardsaver;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Image;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.image.BufferedImage;
import javax.swing.JPanel;

/**
 *
 * @author Administrator
 */
public class SourceImagePanel extends JPanel {
    
    private class clickListener implements MouseListener {
        
        /**
         * Beendet das Warten auf den Mausklick und verwertet die Koordinaten.
         * Diese Methode ist nicht f�r den Anwender bestimmt.
         */
        synchronized public void mouseClicked(MouseEvent e){

            // Too early?
        BufferedImage image = main.getOriginalImage();
        if (image==null) return;

        int shouldWidth = getWidth();
        int shouldHeight = getHeight();
        if (((double)(image.getWidth())/getWidth()) > ((double)image.getHeight()/getHeight())) {
            shouldHeight = (int)((double)getWidth()/image.getWidth()*image.getHeight());
        } else {
            shouldWidth = (int)((double)getHeight()/image.getHeight()*image.getWidth());
        }

                imageSettings.setPointX(editPoint,e.getX()/(double)shouldWidth);
                imageSettings.setPointY(editPoint,e.getY()/(double)shouldHeight);
                editPoint = (editPoint+1) % 4;
                if (editPoint==0) cycleComplete();
                repaint();
        }
        
        /**
         * leere Implementierung (nur aus formalen Gr�nden vorhanden)
         */
        public void mouseEntered(MouseEvent e){
        }
        /**
         * leere Implementierung (nur aus formalen Gr�nden vorhanden)
         */
        public void mouseExited(MouseEvent e){
        }
        /**
         * leere Implementierung (nur aus formalen Gr�nden vorhanden)
         */
        public void mousePressed(MouseEvent e){
        }
        /**
         * leere Implementierung (nur aus formalen Gr�nden vorhanden)
         */
        public void mouseReleased(MouseEvent e){
        }
    }
    
    private MainFrame main;
    private TransformationSettings imageSettings;
    private BufferedImage scaledImage;
    int editPoint;
    
    /** Creates a new instance of SourceImagePanel */
    public SourceImagePanel(MainFrame main) {
        this.main = main;
        addMouseListener(new clickListener());

    }
    
    public void setImageSettings(TransformationSettings imageSettings) {
        this.imageSettings = imageSettings;
        repaint();
        editPoint = 0;
    }
    
    private void drawCross(Graphics g, double xd, double yd) {
        int x = (int)xd;
        int y = (int)yd;
        g.drawLine(x-5,y-5,x+5,y+5);
        g.drawLine(x-5,y+5,x+5,y-5);
    }

    public void paint(Graphics g1) {

        Graphics2D g = (Graphics2D)g1;
        g.clearRect(0,0,getWidth(),getHeight());

        // Too early?
        BufferedImage image = main.getOriginalImage();
        if (image==null) return;

        int shouldWidth = getWidth();
        int shouldHeight = getHeight();
        if (((double)(image.getWidth())/getWidth()) > ((double)image.getHeight()/getHeight())) {
            shouldHeight = (int)((double)getWidth()/image.getWidth()*image.getHeight());
        } else {
            shouldWidth = (int)((double)getHeight()/image.getHeight()*image.getWidth());
        }
        
        // Only draw if imageSettings!=null
            if (imageSettings!=null) {
                // Lade Grafik

                if ((scaledImage==null) || (scaledImage.getHeight()!=shouldHeight) || (scaledImage.getWidth()!=shouldWidth)) {
                    
                    scaledImage = Tools.createResizedCopy(main.getOriginalImage(), shouldWidth, shouldHeight);
                }
                g.drawImage(scaledImage,0,0,null);

                // Draw lines
                g.setColor(Color.BLUE);
                for (int i=0;i<4;i++) {
                    double oldX = imageSettings.getPointX((i+3)%4);
                    double oldY = imageSettings.getPointY((i+3)%4);
                    double newX = imageSettings.getPointX((i)%4);
                    double newY = imageSettings.getPointY((i)%4);
                    //System.out.println("oldX: "+oldX);
                    if ((!Double.isNaN(oldX)) && (!Double.isNaN(newX))) {
                        //System.out.println("Printing "+i+"\n");
                        g.drawLine((int)(shouldWidth*oldX), (int)(shouldHeight*oldY), (int)(shouldWidth*newX), (int)(shouldHeight*newY));
                    }
                }
                
                // Draw 4 Points
                for (int i=0;i<4;i++) {
                    if (imageSettings.getPointX(i)!=Double.NaN) {
                        double pointX = imageSettings.getPointX(i);
                        double pointY = imageSettings.getPointY(i);
                        
                        g.setColor(i==0?Color.RED:Color.GREEN);
                        g.setStroke(new BasicStroke(2));
                        drawCross(g,pointX*shouldWidth, pointY*shouldHeight);
                    }
                }
                
                
            }
        
    }
    
    // Wird aufgerufen, wenn alle 4 Punkte fertig markiert sind.
    public void cycleComplete() {
        main.notifyAboutAllPointsSelected();
    }
    
}
