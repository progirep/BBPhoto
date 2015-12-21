/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package boardsaver;

/**
 *
 * @author ehlers
 */
public class Main {

    /**
     * @param args the command line arguments
     */
    public static void main(String[] args) {
       if (args.length>0) {
            MainFrame mainWindow = new MainFrame(args[0]);
            mainWindow.setVisible(true);
       } else {
           MainFrame mainWindow = new MainFrame(null);
            mainWindow.setVisible(true);
       }
    }

}
