/*
 * SourceImagePanel.java
 *
 * Created on 14. Januar 2007, 18:37
 *
 * To change this template, choose Tools | Template Manager
 * and open the template in the editor.
 */

package boardsaver;

import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.event.ComponentEvent;
import java.awt.event.ComponentListener;
import java.awt.image.BufferedImage;
import javax.swing.JPanel;
import javax.swing.SwingUtilities;

/**
 *
 * @author Administrator
 */
public class TransformedImagePanel extends JPanel {
    
    MainFrame main;
    BufferedImage lastOne;

    boolean needsUpdate;
    boolean isDone;
    int lastSizeX = -1;
    int lastSizeY = -1;
    
    /** Creates a new instance of SourceImagePanel */
    public TransformedImagePanel(MainFrame _main) {
        this.main = _main;
        isDone = false;
        needsUpdate = false;

        addComponentListener(new ComponentListener() {
            public void componentResized(ComponentEvent e) {
                if ((getWidth()!=lastSizeX) || (getHeight()!=lastSizeY)) {
                    needsUpdate = true;
                    lastSizeX = getWidth();
                    lastSizeY = getHeight();
                }
            }
            public void componentHidden(ComponentEvent e) {}
            public void componentMoved(ComponentEvent e) {}
            public void componentShown(ComponentEvent e) {}
        });

        // Start worker thread
        (new Thread() {
            public void run() {
                while (!isDone) {
                    while (!needsUpdate) {
                        try {
                            Thread.sleep(100);
                        } catch (Exception e) {
                            // don't care.
                        }
                    }
                    needsUpdate = false;

                    try {
                        SwingUtilities.invokeAndWait(new Runnable() {
                                public void run() {
                                    main.setPreviewingThreadWorking(true);
                                    Thread.yield();
                                }
                        });
                    } catch (Exception e) {
                        e.printStackTrace();
                    }

                    lastOne = main.getTransformedImage(getWidth(), getHeight());

                    try {
                    SwingUtilities.invokeAndWait(new Runnable() {
                            public void run() {
                                repaint();
                                if (!needsUpdate) {
                                    SwingUtilities.invokeLater(new Runnable() {
                            public void run() {
                                main.setPreviewingThreadWorking(false);
                                Thread.yield();
                            }
                    });
                               }         }
                            
                    }); } catch (Exception e) {
                        // Ignore, not so important
                    }
                }
            }
        }).start();
    }

    public void notifyNewOne() {
        needsUpdate = true;
    }

    public void notifyDone() {
        isDone = true;
    }

    
    
    public void paint(Graphics g1) {
        
        Graphics2D g = (Graphics2D)g1;
        g.clearRect(0,0,getWidth(),getHeight());
        
        if (lastOne!=null)
            g.drawImage(lastOne,0,0,null);
        
        
    }
    
}

