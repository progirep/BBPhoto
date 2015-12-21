/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

/*
 * MainFrame.java
 *
 * Created on 26.06.2009, 15:05:05
 */
package boardsaver;

import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.util.Vector;
import javax.imageio.ImageIO;
import javax.swing.JEditorPane;
import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JOptionPane;
import javax.swing.SwingUtilities;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
import javax.swing.event.TableModelEvent;
import javax.swing.event.TableModelListener;
import javax.swing.table.TableModel;

/**
 *
 * @author ehlers
 */
public class MainFrame extends javax.swing.JFrame {

    CameraInterface cameraInterface;
    String pathToImageFileIfGiven;
    TransformationSettings transformationSettings;
    GradientReductionFilterSettings gradientFilterReductionSettings;
    SizeChooserTableModel tableModel;
    boolean askForDeletion;
    String lastFileName;

    // Some custom Panels
    SourceImagePanel sourceImagePanel;
    TransformedImagePanel filterPreviewPanel;
    ResizePreviewPanel resizingPreviewPanel;
    BufferedImage maximumQualityFilteredImage;

    /**
     * Private class for showing Exceptions to the user in a invoke-later fashion
     */
    class ExceptionToEditPanePainter implements Runnable {

        JEditorPane whichPane;
        String message;

        public void run() {
            whichPane.setText(message);
        }

        public ExceptionToEditPanePainter(JEditorPane _which, String _message) {
            whichPane = _which;
            message = _message;
            try {
                SwingUtilities.invokeAndWait(this);
            } catch (InterruptedException e) {
                e.printStackTrace();
            } catch (InvocationTargetException e) {
                e.printStackTrace();
            }

        }
    }

    /*
     * Private class for the export table model
     *
     * Starts empty and is activated then.
     */
    private class SizeChooserTableModel implements TableModel {

        Vector<TableModelListener> listeners;
        int sizeX[];
        int sizeY[];
        byte[][][] data;
        boolean updateThreadRunning;

        public SizeChooserTableModel() {
            listeners = new Vector<TableModelListener>();
            sizeX = new int[0];
            updateThreadRunning = false;
        }

        public void activate() {
            int[] maxima = {1280, 1024, 768, 640, 400};

            Vector<Integer> preX = new Vector<Integer>();
            Vector<Integer> preY = new Vector<Integer>();

            preX.add(maximumQualityFilteredImage.getWidth());
            preY.add(maximumQualityFilteredImage.getHeight());

            for (int i = 0; i < maxima.length; i++) {
                double ratio = Math.max(maximumQualityFilteredImage.getWidth() / (double) maxima[i], maximumQualityFilteredImage.getHeight() / (double) maxima[i]);
                if (ratio > 1) {
                    preX.add((int) (maximumQualityFilteredImage.getWidth() / ratio));
                    preY.add((int) (maximumQualityFilteredImage.getHeight() / ratio));
                }
            }

            sizeX = new int[preX.size()];
            sizeY = new int[preX.size()];
            for (int i = 0; i < preX.size(); i++) {
                sizeX[i] = preX.get(i);
                sizeY[i] = preY.get(i);
            }

            if (data==null) {
                data = new byte[sizeX.length][5][];

                // Only notify every one if we are not overwriting an old table.
                for (TableModelListener l : listeners) {
                    l.tableChanged(new TableModelEvent(this));
                }
            } else {
                data = new byte[sizeX.length][5][];
            }

            // Start worker thread
            updateThreadRunning = true;
            (new Thread() {

                public void run() {
                    // Compress
                    for (int size = 0; size < sizeX.length; size++) {
                        for (int scenario = 0; scenario < 5; scenario++) {


                            // Make scaled image;
                            int imageType = gradientFilterReductionSettings.isUseColor() ? BufferedImage.TYPE_INT_RGB : BufferedImage.TYPE_BYTE_GRAY;
                            int xsize = sizeX[size];
                            int ysize = sizeY[size];

                            BufferedImage scaledImage = new BufferedImage(xsize, ysize, imageType);
                            Graphics2D g = scaledImage.createGraphics();
                            g.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BICUBIC);
                            g.setRenderingHint(RenderingHints.KEY_COLOR_RENDERING, RenderingHints.VALUE_COLOR_RENDER_QUALITY);
                            g.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
                            g.drawImage(maximumQualityFilteredImage, 0, 0, xsize, ysize, null);
                            g.dispose();

                            // JPEG emulation?
                            if (scenario <= 1) {

                                // No!
                                try {
                                    ByteArrayOutputStream fos = new ByteArrayOutputStream();
                                    ImageIO.write(scenario==1?Tools.reduceColors(scaledImage):scaledImage, "PNG", fos);
                                    data[size][scenario] = fos.toByteArray();
                                } catch (IOException e) {
                                    JOptionPane.showMessageDialog(null, "IO-Exception in PNG memory en/de-coding shouldn't happen!");
                                }

                            } else {
                                try {
                                    // Encode as a JPEG
                                    ByteArrayOutputStream fos = new ByteArrayOutputStream();

                                    float quality =
                                            (scenario == 2) ? 0.3f : (scenario == 3) ? 0.6f : 0.9f;

                                    Tools.writeJPGImage(scaledImage, fos, quality);

                                    // Read again
                                    data[size][scenario] = fos.toByteArray();

                                } catch (IOException e) {
                                    JOptionPane.showMessageDialog(null, "IO-Exception in JPEG memory en/de-coding shouldn't happen!");
                                }
                            }

                            final TableModelEvent evt = new TableModelEvent(tableModel, size, size, scenario + 1);

                            // Update table
                            SwingUtilities.invokeLater(new Runnable() {

                                public void run() {
                                    for (TableModelListener l : listeners) {
                                        l.tableChanged(evt);
                                    }
                                }
                            });

                            // Check if we need to update the view
                            int selectedsize = jTable1.getSelectedRow();
                            int selectedMode = jTable1.getSelectedColumn() - 1;

                            if ((scenario==selectedMode) && (selectedsize==size)) {
                                SwingUtilities.invokeLater(new Runnable() {

                                public void run() {
                                   eventSelectionChanged();
                                }
                                });
                            }

                            updateThreadRunning = false;

                        }
                    }
                }
            }).start();

        }

        public int getRowCount() {
            return sizeX.length;
        }

        public int getColumnCount() {
            return 6;
        }

        public void eventSelectionChanged() {

            int size = jTable1.getSelectedRow();
            int mode = jTable1.getSelectedColumn() - 1;

            if ((mode >= 0) && (size > -1)) {
                try {
                    if (data[size][mode] != null) {
                        resizingPreviewPanel.setImage(ImageIO.read(new ByteArrayInputStream(data[size][mode])));
                        jButtonSaveAs.setEnabled(true);
                    } else {
                        jButtonSaveAs.setEnabled(false);
                    }
                } catch (IOException e) {
                    JOptionPane.showMessageDialog(null, "IO-Exception in internal decoding of the images.");
                }
            } else {
                jButtonSaveAs.setEnabled(false);
            }
        }

        public String getColumnName(int columnIndex) {
            switch (columnIndex) {
                case 0:
                    return "Size";
                case 1:
                    return "PNG";
                case 2:
                    return "PNG (4bit/color)";
                case 3:
                    return "JPG 30%";
                case 4:
                    return "JPG 60%";
                case 5:
                    return "JPG 90%";
            }
            return null;
        }

        public Class<?> getColumnClass(int columnIndex) {
            return String.class;
        }

        public boolean isCellEditable(int rowIndex, int columnIndex) {
            return false;
        }

        public Object getValueAt(int rowIndex, int columnIndex) {
            switch (columnIndex) {
                case 0:
                    return "" + sizeX[rowIndex] + "x" + sizeY[rowIndex];
                default:
                    byte[] d = data[rowIndex][columnIndex - 1];
                    if (d == null) {
                        return "-";
                    }
                    return "" + (d.length / 1024) + " kB";
            }
        }

        public void setValueAt(Object aValue, int rowIndex, int columnIndex) {
            throw new RuntimeException("Not supported");
        }

        public void addTableModelListener(TableModelListener l) {
            listeners.add(l);
        }

        public void removeTableModelListener(TableModelListener l) {
            listeners.remove(l);
        }
    }

    public byte[] getSelectedPictureSizeData() {
        int mode = jTable1.getSelectedColumn() - 1;
        return tableModel.data[jTable1.getSelectedRow()][mode];
    }

    public BufferedImage getOriginalImage() {
        if (cameraInterface == null) {
            return null;
        }
        return cameraInterface.getImage();
    }

    public void notifyAboutAllPointsSelected() {
        jButtonPictureSelectionDone.setEnabled(true);
    }

    public void setPreviewingThreadWorking(boolean isWorking) {
        if (jLabelWorking != null) {
            jLabelWorking.setText(isWorking ? "Updating preview..." : "Preview is up-to-date.");
        }
    }

    /** Creates new form MainFrame */
    public MainFrame(String _pathToImageFileIfGiven) {
        askForDeletion = false;
        this.pathToImageFileIfGiven = _pathToImageFileIfGiven;
        transformationSettings = new TransformationSettings();
        gradientFilterReductionSettings = new GradientReductionFilterSettings(this);
        sourceImagePanel = new SourceImagePanel(this);
        sourceImagePanel.setImageSettings(transformationSettings);
        filterPreviewPanel = new TransformedImagePanel(this);
        resizingPreviewPanel = new ResizePreviewPanel();
        tableModel = new SizeChooserTableModel();

        initComponents();

        // Add table selection change listener
        jTable1.getSelectionModel().addListSelectionListener(new ListSelectionListener() {

            public void valueChanged(ListSelectionEvent e) {
                tableModel.eventSelectionChanged();
            }
        });

        jTable1.getColumnModel().getSelectionModel().addListSelectionListener(new ListSelectionListener() {

            public void valueChanged(ListSelectionEvent e) {
                tableModel.eventSelectionChanged();
            }
        });



        // Load some defaults
        {
            double min = Math.sqrt(gradientFilterReductionSettings.getOverShootWhite());

            min = jSliderWhite.getMaximum() * min;

            jSliderWhite.setValue((int) (Math.round(min)));
            jSliderBlack.setValue((int) (Math.round(jSliderBlack.getMaximum() * gradientFilterReductionSettings.getOverShootBlack())));
            jCheckBoxColor.setSelected(gradientFilterReductionSettings.isUseColor());
        }

        jPictureEditPane.setVisible(false);
        jFilterSettingsPane.setVisible(false);
        jPanelSizeChoosing.setVisible(false);

        // Start new thread to get image
        (new Thread() {

            public void run() {
                try {
                    cameraInterface = new CameraInterface(pathToImageFileIfGiven);

                    try {
                        SwingUtilities.invokeAndWait(new Runnable() {

                            public void run() {
                                jPictureEditPane.setVisible(true);
                                jPanelReceive.setVisible(false);
                            }
                        });
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    } catch (InvocationTargetException e) {
                        e.printStackTrace();
                    }

                } catch (IOException e) {
                    new ExceptionToEditPanePainter(jInitMessagePane, "<P><B>Failed to get first image from the camera.</B></P><P>IOException: " + e.toString() + "</P>");
                } catch (RuntimeException e) {
                    new ExceptionToEditPanePainter(jInitMessagePane, "<P><B>Failed to get first image from the camera.</B></P><P>RuntimeException: " + e.toString() + "</P>");

                }
            }
        }).start();


    }

    /** This method is called from within the constructor to
     * initialize the form.
     * WARNING: Do NOT modify this code. The content of this method is
     * always regenerated by the Form Editor.
     */
    @SuppressWarnings("unchecked")
    // <editor-fold defaultstate="collapsed" desc="Generated Code">//GEN-BEGIN:initComponents
    private void initComponents() {
        java.awt.GridBagConstraints gridBagConstraints;

        jPanelReceive = new javax.swing.JPanel();
        jScrollPane1 = new javax.swing.JScrollPane();
        jInitMessagePane = new javax.swing.JEditorPane();
        jPictureEditPane = new javax.swing.JPanel();
        jPanel1 = new javax.swing.JPanel();
        jPanel2 = sourceImagePanel;
        jPanel3 = new javax.swing.JPanel();
        jButtonPictureSelectionDone = new javax.swing.JButton();
        jEditorPane1 = new javax.swing.JEditorPane();
        jFilterSettingsPane = new javax.swing.JPanel();
        jPanel4 = new javax.swing.JPanel();
        jPanel5 = filterPreviewPanel;
        jPanel6 = new javax.swing.JPanel();
        jLabel1 = new javax.swing.JLabel();
        jLabel3 = new javax.swing.JLabel();
        jLabel2 = new javax.swing.JLabel();
        jSliderWhite = new javax.swing.JSlider();
        jCheckBoxColor = new javax.swing.JCheckBox();
        jSliderBlack = new javax.swing.JSlider();
        jButtonGotoExport = new javax.swing.JButton();
        jLabelWorking = new javax.swing.JLabel();
        jPanelSizeChoosing = new javax.swing.JPanel();
        jSplitPane1 = new javax.swing.JSplitPane();
        jPanel10 = new javax.swing.JPanel();
        jScrollPane3 = new javax.swing.JScrollPane();
        jTable1 = new javax.swing.JTable();
        jButtonSaveAs = new javax.swing.JButton();
        jButtonRepeatSaving = new javax.swing.JButton();
        jPanel7 = new javax.swing.JPanel();
        jScrollPane2 = new javax.swing.JScrollPane();
        jPanel9 = resizingPreviewPanel;

        setDefaultCloseOperation(javax.swing.WindowConstants.EXIT_ON_CLOSE);
        setTitle("BoardSaver v.1.1 - (C) 2009 by Rüdiger Ehlers");
        setMinimumSize(new java.awt.Dimension(1000, 650));
        addWindowListener(new java.awt.event.WindowAdapter() {
            public void windowClosed(java.awt.event.WindowEvent evt) {
                formWindowClosed(evt);
            }
        });
        getContentPane().setLayout(new java.awt.GridBagLayout());

        jPanelReceive.setMinimumSize(new java.awt.Dimension(450, 200));
        jPanelReceive.setLayout(new java.awt.GridBagLayout());

        jInitMessagePane.setContentType("text/html");
        jInitMessagePane.setEditable(false);
        jInitMessagePane.setText("<html>\n  <head>\n\n  </head>\n  <body>\n    <p style=\"margin-top: 0\">\n       Please wait while a picture from the camera is being received...\n    </p>\n  </body>\n</html>\n");
        jScrollPane1.setViewportView(jInitMessagePane);

        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.fill = java.awt.GridBagConstraints.BOTH;
        gridBagConstraints.weightx = 1.0;
        gridBagConstraints.weighty = 1.0;
        jPanelReceive.add(jScrollPane1, gridBagConstraints);

        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.fill = java.awt.GridBagConstraints.BOTH;
        gridBagConstraints.weightx = 1.0;
        gridBagConstraints.weighty = 1.0;
        getContentPane().add(jPanelReceive, gridBagConstraints);

        jPictureEditPane.setLayout(new java.awt.GridBagLayout());

        jPanel1.setBorder(javax.swing.BorderFactory.createTitledBorder("Picture"));
        jPanel1.setLayout(new java.awt.GridBagLayout());

        javax.swing.GroupLayout jPanel2Layout = new javax.swing.GroupLayout(jPanel2);
        jPanel2.setLayout(jPanel2Layout);
        jPanel2Layout.setHorizontalGroup(
            jPanel2Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGap(0, 160, Short.MAX_VALUE)
        );
        jPanel2Layout.setVerticalGroup(
            jPanel2Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGap(0, 446, Short.MAX_VALUE)
        );

        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.fill = java.awt.GridBagConstraints.BOTH;
        gridBagConstraints.weightx = 1.0;
        gridBagConstraints.weighty = 1.0;
        jPanel1.add(jPanel2, gridBagConstraints);

        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.fill = java.awt.GridBagConstraints.BOTH;
        gridBagConstraints.weightx = 1.0;
        gridBagConstraints.weighty = 1.0;
        jPictureEditPane.add(jPanel1, gridBagConstraints);

        jPanel3.setLayout(new java.awt.GridBagLayout());

        jButtonPictureSelectionDone.setText("Proceed");
        jButtonPictureSelectionDone.setEnabled(false);
        jButtonPictureSelectionDone.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jButtonPictureSelectionDoneActionPerformed(evt);
            }
        });
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 1;
        gridBagConstraints.gridy = 0;
        gridBagConstraints.fill = java.awt.GridBagConstraints.BOTH;
        gridBagConstraints.insets = new java.awt.Insets(2, 2, 2, 2);
        jPanel3.add(jButtonPictureSelectionDone, gridBagConstraints);

        jEditorPane1.setBackground(new java.awt.Color(213, 213, 213));
        jEditorPane1.setEditable(false);
        jEditorPane1.setText("Please mark the four corners of the region your are interested in and click onto the \"Proceed\" button.");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 0;
        gridBagConstraints.fill = java.awt.GridBagConstraints.BOTH;
        gridBagConstraints.weightx = 1.0;
        gridBagConstraints.insets = new java.awt.Insets(2, 2, 2, 2);
        jPanel3.add(jEditorPane1, gridBagConstraints);

        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 1;
        gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
        gridBagConstraints.weightx = 1.0;
        jPictureEditPane.add(jPanel3, gridBagConstraints);

        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.fill = java.awt.GridBagConstraints.BOTH;
        gridBagConstraints.weightx = 1.0;
        gridBagConstraints.weighty = 1.0;
        getContentPane().add(jPictureEditPane, gridBagConstraints);

        jFilterSettingsPane.setLayout(new java.awt.GridBagLayout());

        jPanel4.setBorder(javax.swing.BorderFactory.createTitledBorder("Preview"));
        jPanel4.setLayout(new java.awt.GridBagLayout());

        javax.swing.GroupLayout jPanel5Layout = new javax.swing.GroupLayout(jPanel5);
        jPanel5.setLayout(jPanel5Layout);
        jPanel5Layout.setHorizontalGroup(
            jPanel5Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGap(0, 227, Short.MAX_VALUE)
        );
        jPanel5Layout.setVerticalGroup(
            jPanel5Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGap(0, 420, Short.MAX_VALUE)
        );

        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.fill = java.awt.GridBagConstraints.BOTH;
        gridBagConstraints.weightx = 1.0;
        gridBagConstraints.weighty = 1.0;
        jPanel4.add(jPanel5, gridBagConstraints);

        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridwidth = 2;
        gridBagConstraints.fill = java.awt.GridBagConstraints.BOTH;
        gridBagConstraints.weightx = 1.0;
        gridBagConstraints.weighty = 1.0;
        jFilterSettingsPane.add(jPanel4, gridBagConstraints);

        jPanel6.setBorder(javax.swing.BorderFactory.createTitledBorder("Filter Settings"));
        jPanel6.setLayout(new java.awt.GridBagLayout());

        jLabel1.setHorizontalAlignment(javax.swing.SwingConstants.RIGHT);
        jLabel1.setText("Cleanness of the white board:");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 1;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.EAST;
        gridBagConstraints.insets = new java.awt.Insets(2, 2, 2, 2);
        jPanel6.add(jLabel1, gridBagConstraints);

        jLabel3.setHorizontalAlignment(javax.swing.SwingConstants.RIGHT);
        jLabel3.setText("Color:");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.anchor = java.awt.GridBagConstraints.EAST;
        gridBagConstraints.insets = new java.awt.Insets(2, 2, 2, 2);
        jPanel6.add(jLabel3, gridBagConstraints);

        jLabel2.setHorizontalAlignment(javax.swing.SwingConstants.RIGHT);
        jLabel2.setText("Drawing amplification:");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 2;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.EAST;
        gridBagConstraints.insets = new java.awt.Insets(2, 2, 2, 2);
        jPanel6.add(jLabel2, gridBagConstraints);

        jSliderWhite.setMajorTickSpacing(100);
        jSliderWhite.setMaximum(1000);
        jSliderWhite.setPaintTicks(true);
        jSliderWhite.addChangeListener(new javax.swing.event.ChangeListener() {
            public void stateChanged(javax.swing.event.ChangeEvent evt) {
                jSliderWhiteStateChanged(evt);
            }
        });
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 1;
        gridBagConstraints.gridy = 1;
        gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        gridBagConstraints.weightx = 1.0;
        jPanel6.add(jSliderWhite, gridBagConstraints);

        jCheckBoxColor.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jCheckBoxColorActionPerformed(evt);
            }
        });
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        jPanel6.add(jCheckBoxColor, gridBagConstraints);

        jSliderBlack.setMajorTickSpacing(100);
        jSliderBlack.setMaximum(1000);
        jSliderBlack.setPaintTicks(true);
        jSliderBlack.addChangeListener(new javax.swing.event.ChangeListener() {
            public void stateChanged(javax.swing.event.ChangeEvent evt) {
                jSliderBlackStateChanged(evt);
            }
        });
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 1;
        gridBagConstraints.gridy = 2;
        gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        gridBagConstraints.weightx = 1.0;
        jPanel6.add(jSliderBlack, gridBagConstraints);

        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 1;
        gridBagConstraints.gridwidth = 2;
        gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
        gridBagConstraints.weightx = 1.0;
        jFilterSettingsPane.add(jPanel6, gridBagConstraints);

        jButtonGotoExport.setText("Export...");
        jButtonGotoExport.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jButtonGotoExportActionPerformed(evt);
            }
        });
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 1;
        gridBagConstraints.gridy = 2;
        jFilterSettingsPane.add(jButtonGotoExport, gridBagConstraints);

        jLabelWorking.setText("Waiting...");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 2;
        gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
        gridBagConstraints.weightx = 1.0;
        jFilterSettingsPane.add(jLabelWorking, gridBagConstraints);

        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.fill = java.awt.GridBagConstraints.BOTH;
        gridBagConstraints.weightx = 1.0;
        gridBagConstraints.weighty = 1.0;
        getContentPane().add(jFilterSettingsPane, gridBagConstraints);

        jPanelSizeChoosing.setLayout(new java.awt.GridBagLayout());

        jSplitPane1.setDividerLocation(150);
        jSplitPane1.setOrientation(javax.swing.JSplitPane.VERTICAL_SPLIT);
        jSplitPane1.setResizeWeight(0.3);
        jSplitPane1.setOpaque(true);

        jPanel10.setBorder(javax.swing.BorderFactory.createTitledBorder("Resolution/Quality"));
        jPanel10.setLayout(new java.awt.GridBagLayout());

        jTable1.setFont(new java.awt.Font("DejaVu Sans", 1, 16));
        jTable1.setModel(tableModel);
        jTable1.setCellSelectionEnabled(true);
        jTable1.setRowHeight(24);
        jTable1.setSelectionMode(javax.swing.ListSelectionModel.SINGLE_SELECTION);
        jScrollPane3.setViewportView(jTable1);

        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridheight = 3;
        gridBagConstraints.fill = java.awt.GridBagConstraints.BOTH;
        gridBagConstraints.weightx = 1.0;
        gridBagConstraints.weighty = 1.0;
        jPanel10.add(jScrollPane3, gridBagConstraints);

        jButtonSaveAs.setText("Save As...");
        jButtonSaveAs.setEnabled(false);
        jButtonSaveAs.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jButtonSaveAsActionPerformed(evt);
            }
        });
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 1;
        gridBagConstraints.gridy = 0;
        gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.NORTH;
        jPanel10.add(jButtonSaveAs, gridBagConstraints);

        jButtonRepeatSaving.setText("Auto-Repeat");
        jButtonRepeatSaving.setEnabled(false);
        jButtonRepeatSaving.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jButtonRepeatSavingActionPerformed(evt);
            }
        });
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 1;
        gridBagConstraints.gridy = 1;
        gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.NORTH;
        jPanel10.add(jButtonRepeatSaving, gridBagConstraints);

        jSplitPane1.setRightComponent(jPanel10);

        jPanel7.setBorder(javax.swing.BorderFactory.createTitledBorder("Preview"));
        jPanel7.setLayout(new java.awt.GridBagLayout());

        javax.swing.GroupLayout jPanel9Layout = new javax.swing.GroupLayout(jPanel9);
        jPanel9.setLayout(jPanel9Layout);
        jPanel9Layout.setHorizontalGroup(
            jPanel9Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGap(0, 512, Short.MAX_VALUE)
        );
        jPanel9Layout.setVerticalGroup(
            jPanel9Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGap(0, 233, Short.MAX_VALUE)
        );

        jScrollPane2.setViewportView(jPanel9);

        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.fill = java.awt.GridBagConstraints.BOTH;
        gridBagConstraints.weightx = 1.0;
        gridBagConstraints.weighty = 1.0;
        jPanel7.add(jScrollPane2, gridBagConstraints);

        jSplitPane1.setLeftComponent(jPanel7);

        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 0;
        gridBagConstraints.fill = java.awt.GridBagConstraints.BOTH;
        gridBagConstraints.weightx = 1.0;
        gridBagConstraints.weighty = 1.0;
        jPanelSizeChoosing.add(jSplitPane1, gridBagConstraints);

        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.fill = java.awt.GridBagConstraints.BOTH;
        gridBagConstraints.weightx = 1.0;
        gridBagConstraints.weighty = 1.0;
        getContentPane().add(jPanelSizeChoosing, gridBagConstraints);

        java.awt.Dimension screenSize = java.awt.Toolkit.getDefaultToolkit().getScreenSize();
        setBounds((screenSize.width-1000)/2, (screenSize.height-650)/2, 1000, 650);
    }// </editor-fold>//GEN-END:initComponents

    private void jButtonPictureSelectionDoneActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jButtonPictureSelectionDoneActionPerformed
        // User has pressed the "Proceed" button in the picture selection
        jFilterSettingsPane.setVisible(true);
        jPictureEditPane.setVisible(false);

        notifyFilterSettingsChange();
    }//GEN-LAST:event_jButtonPictureSelectionDoneActionPerformed

    private void formWindowClosed(java.awt.event.WindowEvent evt) {//GEN-FIRST:event_formWindowClosed
        filterPreviewPanel.notifyDone();
    }//GEN-LAST:event_formWindowClosed

    private void jSliderWhiteStateChanged(javax.swing.event.ChangeEvent evt) {//GEN-FIRST:event_jSliderWhiteStateChanged
        double value = (double) (jSliderWhite.getValue()) / jSliderWhite.getMaximum();
        // Quadratic correction for easier setting
        value = value * value;
        gradientFilterReductionSettings.setOverShootWhite(value);
}//GEN-LAST:event_jSliderWhiteStateChanged

    private void jSliderBlackStateChanged(javax.swing.event.ChangeEvent evt) {//GEN-FIRST:event_jSliderBlackStateChanged
        gradientFilterReductionSettings.setOverShootBlack((double) (jSliderBlack.getValue()) / jSliderBlack.getMaximum());
}//GEN-LAST:event_jSliderBlackStateChanged

    private void jCheckBoxColorActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jCheckBoxColorActionPerformed
        // Color check box
        gradientFilterReductionSettings.setUseColor(jCheckBoxColor.isSelected());
}//GEN-LAST:event_jCheckBoxColorActionPerformed

    private void jButtonGotoExportActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jButtonGotoExportActionPerformed
        jFilterSettingsPane.setVisible(false);

        // Recycle the initMessagePane for stating that work is in progress.
        jInitMessagePane.setText("<P>Computing high-quality version of the optimized image.</P><P>This may take some time.</P>");
        jPanelReceive.setVisible(true);

        (new Thread() {

            public void run() {

                // Doing some work....
                BufferedImage transformed = Tools.transformImage(getOriginalImage(), 2048, 2048, transformationSettings, false, gradientFilterReductionSettings.isUseColor());
                GradientReductionFilter filter = new GradientReductionFilter();
                filter.setSettings(gradientFilterReductionSettings);
                maximumQualityFilteredImage = filter.filterImage(transformed);
                maximumQualityFilteredImage = CropWhiteBorder.crop(maximumQualityFilteredImage);

                Thread.yield();

                // Switching views
                SwingUtilities.invokeLater(new Runnable() {

                    public void run() {
                        jSplitPane1.setDividerLocation(getHeight() - 250);
                        tableModel.activate();
                        jPanelSizeChoosing.setVisible(true);
                        jPanelReceive.setVisible(false);
                        jButtonRepeatSaving.setVisible(cameraInterface.canPerformCapturingAgain());
                    }
                });
            }
        }).start();

    }//GEN-LAST:event_jButtonGotoExportActionPerformed

    private void jButtonSaveAsActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jButtonSaveAsActionPerformed
        // Saving something
        //Create a file chooser
        final JFileChooser fc = new JFileChooser();
        fc.setDialogTitle("Same images as...");

        //In response to a button click:
        int returnVal = fc.showSaveDialog(this);

        if (returnVal == JFileChooser.APPROVE_OPTION) {
            String dest = fc.getSelectedFile().getAbsolutePath();

            try {
                lastFileName = saveFile(dest,true);
                // Only proceed if not aborted.
                if (lastFileName!=null) {
                    if (!askForDeletion) {
                        askForDeletion = true;
                        (new AskDeleteThread(this, cameraInterface)).start();
                        setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
                    }
                }

                jButtonRepeatSaving.setEnabled(true);

            } catch (java.io.IOException e) {
                JOptionPane.showMessageDialog(null, "IO-Exception: Cannot write the image file!");
            }

        }
    }//GEN-LAST:event_jButtonSaveAsActionPerformed
    /**
     * A method for perfoming the file saving operation
     * @param dest The filename
     * @return The filename, fixed (i.e. with suffix added if necessary)
     * @throws java.io.IOException
     */
    private String saveFile(String dest, boolean askWhenOverwriting) throws IOException {
        int mode = jTable1.getSelectedColumn() - 1;

        if ((dest.toLowerCase().endsWith(".png")) && (mode <= 1)) {
            // File extension already present
        } else if ((dest.toLowerCase().endsWith(".jpg")) && (mode > 1)) {
            // Again, file extension is already present
        } else {
            dest = dest + ((mode <= 1) ? ".png" : ".jpg");
        }

        if (askWhenOverwriting) {
            if ((new File(dest)).exists()) {
                int result = JOptionPane.showConfirmDialog(this, "Do you really want to overwrite the following file?\n\n" + dest,
                        "Overwriting file", JOptionPane.YES_NO_CANCEL_OPTION, JOptionPane.QUESTION_MESSAGE);
                switch (result) {
                    case JOptionPane.NO_OPTION:
                        return null;
                    case JOptionPane.CANCEL_OPTION:
                        return null;
                }
            }
        }

        FileOutputStream fo = new FileOutputStream(dest);
        //int size = jTable1.getSelectedRow();


        if (mode >= 0) {
            if (getSelectedPictureSizeData() != null) {
                fo.write(getSelectedPictureSizeData());
            } else {
                System.out.println("Saving file which is not ready yet!");
            }
        }

        fo.close();

        return dest;
    }

    private void jButtonRepeatSavingActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jButtonRepeatSavingActionPerformed
        // Pressed "Repeated saving button"
        jPanelSizeChoosing.setVisible(false);

        // Recycle the initMessagePane for stating that work is in progress.
        jInitMessagePane.setText("<P>Repeating the process with the same settings for a new picture</P>");
        jPanelReceive.setVisible(true);

        (new Thread() {

            public void run() {

                try {
                    cameraInterface.captureNewImage();
                } catch (IOException e) {
                    jInitMessagePane.setText("<P>Error: Cannot capture a new photo due to IOException: " + e.getMessage()+"<BR>"+" </P>");
                    return;
                }

                // Doing some work....
                BufferedImage transformed = Tools.transformImage(getOriginalImage(), 2048, 2048, transformationSettings, false, gradientFilterReductionSettings.isUseColor());
                GradientReductionFilter filter = new GradientReductionFilter();
                filter.setSettings(gradientFilterReductionSettings);
                maximumQualityFilteredImage = filter.filterImage(transformed);


                // Update table
                // Might still be running!
                while (tableModel.updateThreadRunning) {
                    try {
                        sleep(50);
                    } catch (Exception e) {
                        // Who cares?
                    }
                }

                try {
                    SwingUtilities.invokeAndWait(new Runnable() {

                        public void run() {
                            tableModel.activate();
                        }
                    });
                } catch (InterruptedException e) {
                    throw new RuntimeException(e);
                } catch (InvocationTargetException e) {
                    throw new RuntimeException(e);
                }

                // Perform busy waiting
                while (getSelectedPictureSizeData()==null) {
                   try {
                        sleep(50);
                    } catch (Exception e) {
                        // Who cares?
                    }
                }

                // Save and overwrite without asking
                try {
                    saveFile(lastFileName,false);
                } catch (IOException e) {
                    jInitMessagePane.setText("<P>Error: Cannot capture a new photo due to IOException: " + e.getStackTrace() + " </P>");
                    return;
                }
                
                // Switch views
                SwingUtilities.invokeLater(new Runnable() {

                    public void run() {
                        jPanelSizeChoosing.setVisible(true);
                        jPanelReceive.setVisible(false);
                        jButtonRepeatSaving.setVisible(cameraInterface.canPerformCapturingAgain());

                        // Repaint Preview
                        tableModel.eventSelectionChanged();
                    }
                });
            }
        }).start();
    }//GEN-LAST:event_jButtonRepeatSavingActionPerformed

    public void notifyFilterSettingsChange() {
        filterPreviewPanel.notifyNewOne();
    }

    public BufferedImage getTransformedImage(int maxWidth, int maxHeight) {
        if (getOriginalImage() == null) {
            return null;
        }
        BufferedImage transformed = Tools.transformImage(getOriginalImage(), maxWidth, maxHeight, transformationSettings, false, gradientFilterReductionSettings.isUseColor());

        GradientReductionFilter filter = new GradientReductionFilter();
        filter.setSettings(gradientFilterReductionSettings);
        filter.filterImage(transformed);
        return transformed;
    }

    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.JButton jButtonGotoExport;
    private javax.swing.JButton jButtonPictureSelectionDone;
    private javax.swing.JButton jButtonRepeatSaving;
    private javax.swing.JButton jButtonSaveAs;
    private javax.swing.JCheckBox jCheckBoxColor;
    private javax.swing.JEditorPane jEditorPane1;
    private javax.swing.JPanel jFilterSettingsPane;
    private javax.swing.JEditorPane jInitMessagePane;
    private javax.swing.JLabel jLabel1;
    private javax.swing.JLabel jLabel2;
    private javax.swing.JLabel jLabel3;
    private javax.swing.JLabel jLabelWorking;
    private javax.swing.JPanel jPanel1;
    private javax.swing.JPanel jPanel10;
    private javax.swing.JPanel jPanel2;
    private javax.swing.JPanel jPanel3;
    private javax.swing.JPanel jPanel4;
    private javax.swing.JPanel jPanel5;
    private javax.swing.JPanel jPanel6;
    private javax.swing.JPanel jPanel7;
    private javax.swing.JPanel jPanel9;
    private javax.swing.JPanel jPanelReceive;
    private javax.swing.JPanel jPanelSizeChoosing;
    private javax.swing.JPanel jPictureEditPane;
    private javax.swing.JScrollPane jScrollPane1;
    private javax.swing.JScrollPane jScrollPane2;
    private javax.swing.JScrollPane jScrollPane3;
    private javax.swing.JSlider jSliderBlack;
    private javax.swing.JSlider jSliderWhite;
    private javax.swing.JSplitPane jSplitPane1;
    private javax.swing.JTable jTable1;
    // End of variables declaration//GEN-END:variables
}
