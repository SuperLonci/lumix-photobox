package streamviewer;

import javax.swing.*;
import java.awt.*;

public class Options {
    private final String cameraIp;
    private final int cameraNetMaskBitSize;
    private final String viewerType;
    private final int webcamIndex;

    private Options(String cameraIp, int cameraNetMaskBitSize, String viewerType, int webcamIndex) {
        this.cameraIp = cameraIp;
        this.cameraNetMaskBitSize = cameraNetMaskBitSize;
        this.viewerType = viewerType;
        this.webcamIndex = webcamIndex;
    }

    public static Options read() {
        JTextField ipField = new JTextField("192.168.54.1", 15);
        JTextField maskField = new JTextField("24", 5);
        String[] viewerTypes = {"mock", "real", "webcam"};
        JComboBox<String> viewerTypeCombo = new JComboBox<>(viewerTypes);
        JTextField webcamIndexField = new JTextField("0", 5);

        JPanel panel = new JPanel(new GridLayout(0, 2));
        panel.add(new JLabel("Camera IP address:"));
        panel.add(ipField);
        panel.add(new JLabel("Camera IP netmask size:"));
        panel.add(maskField);
        panel.add(new JLabel("Viewer Type:"));
        panel.add(viewerTypeCombo);
        panel.add(new JLabel("Webcam Index:"));
        panel.add(webcamIndexField);

        int result = JOptionPane.showConfirmDialog(null, panel,
                "Enter Camera Settings", JOptionPane.OK_CANCEL_OPTION);
        if (result == JOptionPane.OK_OPTION) {
            String cameraIp = ipField.getText();
            int cameraNetMaskBitSize;
            try {
                cameraNetMaskBitSize = Integer.parseInt(maskField.getText());
            } catch (NumberFormatException e) {
                cameraNetMaskBitSize = 24;
            }
            String viewerType = (String) viewerTypeCombo.getSelectedItem();
            int webcamIndex;
            try {
                webcamIndex = Integer.parseInt(webcamIndexField.getText());
            } catch (NumberFormatException e) {
                webcamIndex = 0;
            }
            return new Options(cameraIp, cameraNetMaskBitSize, viewerType, webcamIndex);
        } else {
            System.exit(0);
            return null;
        }
    }

    public String getCameraIp() {
        return cameraIp;
    }

    public int getCameraNetMaskBitSize() {
        return cameraNetMaskBitSize;
    }

    public String getViewerType() {
        return viewerType;
    }

    public int getWebcamIndex() {
        return webcamIndex;
    }
}