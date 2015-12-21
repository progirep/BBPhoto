/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package boardsaver;

/**
 *
 * @author ehlers
 */
public class TransformationSettings {
    double[] pointsX;
    double[] pointsY;


    public TransformationSettings() {
        pointsX = new double[4];
        pointsY = new double[4];
        for (int i=0;i<4;i++) {
            pointsX[i] = Double.NaN;
            pointsY[i] = Double.NaN;
        }
    }



    double getPointX(int which) {
        return pointsX[which];
    }

    double getPointY(int which) {
        return pointsY[which];
    }

    void setPointX(int which, double value) {
        pointsX[which] = value;
        //System.out.println("SetPointX "+which+", "+value);

    }

    void setPointY(int which, double value) {
        pointsY[which] = value;
    }
}
