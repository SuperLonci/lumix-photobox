package streamviewer;

import javax.swing.*;
import java.awt.*;
import java.io.IOException;
import com.fazecast.jSerialComm.*;

public class LedController {
    private SerialPort serialPort;
    private int currentMode = 4;
    private int currentBrightness = 255;

    public LedController(Options options) {
        String comPort = options.getComPort();
        if (comPort == null || comPort.equals("COM")) {
            System.out.println("No COM port specified. LED control disabled.");
        } else {
            connectToSerial(comPort);
        }
    }


    private void connectToSerial(String comPort) {
        serialPort = SerialPort.getCommPort(comPort);
        serialPort.setBaudRate(9600);
        serialPort.setNumDataBits(8);
        serialPort.setNumStopBits(1);
        serialPort.setParity(SerialPort.NO_PARITY);

        if (serialPort.openPort()) {
            System.out.println("Successfully connected to " + comPort);
        } else {
            System.err.println("Failed to connect to " + comPort);
        }
    }

    public void sendCommand(String command) {
        if (serialPort != null && serialPort.isOpen()) {
            try {
                serialPort.getOutputStream().write((command + "\n").getBytes());
                serialPort.getOutputStream().flush();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    public void cycleMode() {
        currentMode = (currentMode % 4) + 1;
        sendCommand("mode " + currentMode + ";");
    }

    public int getCurrentMode() {
        return currentMode;
    }

    public void showBrightnessDialog(JFrame parentFrame) {
        JDialog dialog = new JDialog(parentFrame, "Set LED Brightness", true);
        JSlider slider = new JSlider(JSlider.HORIZONTAL, 0, 255, currentBrightness);
        slider.setMajorTickSpacing(51);
        slider.setPaintTicks(true);
        slider.setPaintLabels(true);

        JButton applyButton = new JButton("Apply");
        applyButton.addActionListener(e -> {
            currentBrightness = slider.getValue();
            sendCommand("brightness " + currentBrightness + ";");
            dialog.dispose();
        });

        dialog.setLayout(new BorderLayout());
        dialog.add(slider, BorderLayout.CENTER);
        dialog.add(applyButton, BorderLayout.SOUTH);
        dialog.pack();
        dialog.setLocationRelativeTo(parentFrame);
        dialog.setVisible(true);
    }

    public void close() {
        if (serialPort != null && serialPort.isOpen()) {
            serialPort.closePort();
        }
    }
}