/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package boardsaver;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import javax.swing.JOptionPane;

/**
 *
 * @author ehlers
 */
class AskDeleteThread extends Thread {

        MainFrame main;
        CameraInterface cam;

        public AskDeleteThread(MainFrame main, CameraInterface cam) {
            this.main = main;
            this.cam = cam;
        }

        public void run()  {
            setPriority(Thread.MIN_PRIORITY);
            while (main.isVisible()) Thread.yield();

            cam.deletePictureFromCamera(true);
            
            System.exit(0);
        }

    }
