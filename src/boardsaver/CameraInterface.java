/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package boardsaver;

import java.awt.image.BufferedImage;
import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.Date;
import javax.imageio.ImageIO;
import javax.swing.JOptionPane;

/**
 * Camera interface.
 *
 * Does not use camera if argument is provided.
 *
 * @author ehlers
 */
public class CameraInterface {

    boolean useCamera;
    boolean onlyCapture;
    String filenameOnCameraOfReceivedImage;
    String pictureNumberReceived;
    BufferedImage image;
    
    CameraInterface(String where) throws RuntimeException, IOException {

        if ((where==null) || (where.length()==0)) {
            useCamera = true;
            onlyCapture = false;
            getLastImageFromCamera();
        } else {
            if (where.toLowerCase().equals("--capture")) {
                useCamera = true;
                onlyCapture = true;
                captureNewImage();
            } else {
                useCamera = false;
                onlyCapture = false;
                image = ImageIO.read(new File(where));
            }
        }
    }

    public void getLastImageFromCamera() throws IOException {
        String[] filenames = getLastImageFilename();

            if (filenames==null) {
                throw new RuntimeException("Cannot get filename of last photo on camera using gphoto2. Possible causes:<UL><LI>gphoto2 is not installed</LI><LI>Your camera is mounted elsewhere</LI><LI>There is no photo on the camera</LI><LI>Your camera is not connected.</LI></UL>Check the output of \"gphoto2 -L\" on the terminal.");
            }

            pictureNumberReceived = filenames[1];
            filenameOnCameraOfReceivedImage = filenames[0];
            getImageFromCamera(filenameOnCameraOfReceivedImage, pictureNumberReceived);
    }

    public BufferedImage getImage() { return image; }

    public void captureNewImage() throws IOException {
        if (!useCamera) throw new RuntimeException("CameraInterface: \"captureNewImage\" invalid for non-camera mode.");

        // Save old data
        String oldNum = pictureNumberReceived;

        // Capture new image
        String cmd = "gphoto2 --capture-image";
        System.out.println("Command: "+cmd);
        Process p = Runtime.getRuntime().exec(cmd);
        BufferedReader in = new BufferedReader(new InputStreamReader(p.getInputStream()));
        try {
            p.waitFor();
        } catch (InterruptedException e) {
            System.out.println("Warning: InterruptedException caught.");
        }
        String s = in.readLine();
        while (s!=null) {
          System.out.println(s);
          s = in.readLine();
        }

        if (p.exitValue()>0) throw new IOException("Failed to grab new image from the camera.");


        // Get last image
        getLastImageFromCamera();
        deletePictureFromCamera(false);

        // Restore data
        pictureNumberReceived = oldNum;
    }



    public boolean canPerformCapturingAgain() {
        //return useCamera;
        return onlyCapture;
    }

    public void deletePictureFromCamera(boolean askBeforehand) {
        if (!useCamera) return;

        // Don't delete the picture if this is the captured picture.
        if (onlyCapture && askBeforehand) return;

        if (pictureNumberReceived!=null) {

                try {
                    Object[] strings = new String[] {"Delete","Don't delete"};
                    
                    boolean isOk;
                    if (askBeforehand) {
                        int wahl = JOptionPane.showOptionDialog(null,"Do you want to delete the old picture from the camera?","Delete old picture",JOptionPane.YES_NO_OPTION,JOptionPane.QUESTION_MESSAGE,null,strings,strings[1]);
                        isOk= (wahl == JOptionPane.YES_OPTION);
                    } else {
                        isOk = true;
                    }

                    if (isOk) {
                        String cmd = "gphoto2 -R -d "+pictureNumberReceived;
                        System.out.println("Command: "+cmd);
                        Process p = Runtime.getRuntime().exec(cmd);
                        BufferedReader in = new BufferedReader(new InputStreamReader(p.getInputStream()));
                        p.waitFor();
                        String s = in.readLine();
                        while (s!=null) {
                            System.out.println(s);
                            s = in.readLine();
                        }
                    }
                } catch (Exception e) {
                    System.out.println("Exception!!!!!");
                    e.printStackTrace();
                }
            }


    }
    
    private static String[] getLastImageFilename() throws IOException {

        Process p = Runtime.getRuntime().exec("gphoto2 -L");
        BufferedReader in = new BufferedReader(new InputStreamReader(p.getInputStream()));

       try {
          p.waitFor();
       } catch (InterruptedException e) {
          // Ignore
       } 

        String s = in.readLine();
        String[] lastfile = null;
        while (s!=null) {
            System.out.println(s);
            if (s.startsWith("#")) {
                int tabindex = s.indexOf(" ");
                String number = s.substring(1,tabindex).trim();
                if (tabindex!=-1) {
                    s = s.substring(tabindex).trim();
                    tabindex = s.indexOf(" ");
                    if (tabindex!=-1) {
                        s = s.substring(0,tabindex).trim();
                        if (s.toUpperCase().endsWith(".JPG"))
                            lastfile = new String[]{s,number};
                    }
                }
            }
            s = in.readLine();
        }

        return lastfile;
    }

    private void getImageFromCamera(String cameraFile, String nofImage) throws RuntimeException, IOException {

        // Make a safe directory
        File tempdir = File.createTempFile("photo", ""+(new Date()).getTime());

        if(!(tempdir.delete())) throw new RuntimeException("Something went wrong with deleting a temporary file.");
        if(!(tempdir.mkdir())) throw new RuntimeException("Something went wrong with creating a temporary directory.");

        System.out.println("Using tempdir: "+tempdir);
        System.out.println("Running Command: "+"gphoto2 --force-overwrite -p "+nofImage);

        Process p = Runtime.getRuntime().exec("gphoto2 --force-overwrite -p "+nofImage,null,tempdir);
        BufferedReader in = new BufferedReader(new InputStreamReader(p.getInputStream()));
        
       try {
          p.waitFor();
       } catch (InterruptedException e) {
          // Ignore
       } 

        String s = in.readLine();
        while (s!=null) {
            System.out.println(s);
            s = in.readLine();
        }

        String filename = tempdir.getAbsolutePath()+File.separatorChar+cameraFile;
        System.out.println("Reading filename: "+filename);
        image = ImageIO.read(new File(filename));
        if(!((new File(filename)).delete())) throw new RuntimeException("Could not delete received image");

        if(!(tempdir.delete())) throw new RuntimeException("Something went wrong with deleting a temporary directory after reading an image.");
    }
}
