package streamviewer;

import javax.swing.*;
import java.awt.*;

public class Options {
    private final String cameraIp;
    private final int cameraNetMaskBitSize;
    private String viewerType;
    private final int webcamIndex;
    private final boolean mockCamera;
    private final String comPort;
    private final boolean useCameraStateMonitor;

    private Options(String cameraIp, int cameraNetMaskBitSize, String viewerType, int webcamIndex, boolean mockCamera, String comPort, boolean useCameraStateMonitor) {
        this.cameraIp = cameraIp;
        this.cameraNetMaskBitSize = cameraNetMaskBitSize;
        this.viewerType = viewerType;
        this.webcamIndex = webcamIndex;
        this.mockCamera = mockCamera;
        this.comPort = comPort;
        this.useCameraStateMonitor = useCameraStateMonitor;
    }

    public static Options read() {
        JTextField ipField = new JTextField("192.168.54.1", 15);
        JTextField maskField = new JTextField("24", 5);
        String[] viewerTypes = {"webcam", "real", "mock"};
        JComboBox<String> viewerTypeCombo = new JComboBox<>(viewerTypes);
        JTextField webcamIndexField = new JTextField("0", 5);
        JCheckBox mockCameraCheckbox = new JCheckBox("Mock Camera Communication");
        JTextField comPortField = new JTextField("COM",10);
        JCheckBox useCameraStateMonitorCheckbox = new JCheckBox("Use Camera State Monitor");

        JPanel panel = new JPanel(new GridLayout(0, 2));
        panel.add(new JLabel("Camera IP address:"));
        panel.add(ipField);
        panel.add(new JLabel("Camera IP netmask size:"));
        panel.add(maskField);
        panel.add(new JLabel("Viewer Type:"));
        panel.add(viewerTypeCombo);
        panel.add(new JLabel("Webcam Index:"));
        panel.add(webcamIndexField);
        panel.add(new JLabel("Mock Camera:"));
        panel.add(mockCameraCheckbox);
        panel.add(new JLabel("COM Port (optional):"));
        panel.add(comPortField);
        panel.add(new JLabel("Use Camera State Monitor:"));
        panel.add(useCameraStateMonitorCheckbox);

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
            boolean mockCamera = mockCameraCheckbox.isSelected();
            String comPort = comPortField.getText().trim();
            // If COM port is empty, set it to null
            if (comPort.isEmpty()) {
                comPort = null;
            }
            boolean useCameraStateMonitor = useCameraStateMonitorCheckbox.isSelected();
            return new Options(cameraIp, cameraNetMaskBitSize, viewerType, webcamIndex, mockCamera, comPort, useCameraStateMonitor);
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

    public void setViewerType(String viewerType) {
        this.viewerType = viewerType;
    }

    public int getWebcamIndex() {
        return webcamIndex;
    }

    public boolean isMockCamera() {
        return mockCamera;
    }

    public String getComPort() {
        return comPort;
    }

    public boolean isUseCameraStateMonitor() {
        return useCameraStateMonitor;
    }
}