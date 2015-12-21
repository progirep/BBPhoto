/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package boardsaver;

/**
 *
 * @author ehlers
 */
public class GradientReductionFilterSettings {
    private double overShootWhite;
    private double overShootBlack;
    private boolean useColor;
    private MainFrame main;

    GradientReductionFilterSettings(MainFrame _main) {
        main = _main;
        overShootWhite = 0.05;
        overShootBlack = 0.7;
        useColor = false;
    }

    public double getOverShootBlack() {
        return overShootBlack;
    }

    public void setOverShootBlack(double overShootBlack) {
        this.overShootBlack = overShootBlack;
        main.notifyFilterSettingsChange();
    }

    public double getOverShootWhite() {
        return overShootWhite;
    }

    public void setOverShootWhite(double overShootWhite) {
        this.overShootWhite = overShootWhite;
        main.notifyFilterSettingsChange();
    }

    public boolean isUseColor() {
        return useColor;
    }

    public void setUseColor(boolean useColor) {
        this.useColor = useColor;
        main.notifyFilterSettingsChange();
    }
}
