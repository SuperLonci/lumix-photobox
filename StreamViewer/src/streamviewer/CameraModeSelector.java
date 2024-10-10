package streamviewer;

import javax.swing.*;
import java.awt.*;

public class CameraModeSelector {
    private final JFrame parentFrame;
    private final Options options;

    public CameraModeSelector(JFrame parentFrame, Options options) {
        this.parentFrame = parentFrame;
        this.options = options;
    }

    public String selectMode() {
        String[] modes = {"real", "mock", "webcam"};
        JComboBox<String> modeComboBox = new JComboBox<>(modes);
        modeComboBox.setSelectedItem(options.getViewerType());

        JPanel panel = new JPanel(new GridLayout(0, 1));
        panel.add(new JLabel("Select Camera Mode:"));
        panel.add(modeComboBox);

        int result = JOptionPane.showConfirmDialog(parentFrame, panel, "Camera Mode Selection",
                JOptionPane.OK_CANCEL_OPTION, JOptionPane.PLAIN_MESSAGE);

        if (result == JOptionPane.OK_OPTION) {
            return (String) modeComboBox.getSelectedItem();
        }
        return null;
    }
}