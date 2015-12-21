/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package boardsaver;

import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import javax.swing.JPanel;

/**
 *
 * @author ehlers
 */
public class ResizePreviewPanel extends JPanel {

    BufferedImage image;

    public void setImage(BufferedImage _image) {
        image = _image;
        repaint();
    }

    public void paint(Graphics g1) {

        Graphics2D g = (Graphics2D)g1;
        g.clearRect(0,0,getWidth(),getHeight());

        if (image!=null)
            g.drawImage(image,0,0,null);


    }
}
