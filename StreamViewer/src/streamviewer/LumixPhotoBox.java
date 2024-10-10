package streamviewer;

import javax.swing.*;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.IOException;

public class LumixPhotoBox {
    private static JFrame window;
    private static VideoPanel videoPanel;
    private static StreamViewerInterface currentStreamViewer;
    private static Options options;
    private static Thread streamViewerThread;
    private static CameraStateMonitor cameraStateMonitor;

    public static void main(String[] args) {

        System.load(System.getProperty("user.dir") + "/lib/opencv_java4100.dll");  // For Windows

        SwingUtilities.invokeLater(() -> {
            options = Options.read();
            if (options == null) {
                return;  // User cancelled the input dialog
            }

            initializeUI();
            initializeStreamViewer();
            initializeCameraStateMonitor();
        });
    }

    private static void initializeUI() {
        window = new JFrame("Lumix Photobox");
        videoPanel = new VideoPanel(options, LumixPhotoBox::switchCameraMode);
        window.add(videoPanel);
        window.setSize(1600, 900);
        window.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        window.setVisible(true);

        window.addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosing(WindowEvent ev) {
                stopStreamViewer();
                stopCameraStateMonitor();
                System.exit(0);
            }
        });
    }

    private static void initializeCameraStateMonitor() {
        switch (options.getCameraStateMonitorMode()) {
            case "live":
                cameraStateMonitor = new CameraStateMonitor(options, videoPanel);
                break;
            case "mock":
                cameraStateMonitor = new MockCameraStateMonitor(options, videoPanel);
                break;
            case "none":
            default:
                cameraStateMonitor = null;
                return;
        }
        cameraStateMonitor.start();
    }

    private static void stopCameraStateMonitor() {
        if (cameraStateMonitor != null) {
            cameraStateMonitor.stop();
        }
    }

    private static boolean initializeStreamViewer() {
        stopStreamViewer(); // Stop any existing viewer

        try {
            currentStreamViewer = createStreamViewer(options.getViewerType());
            currentStreamViewer.setImageConsumer(videoPanel::displayNewImage);
            streamViewerThread = new Thread(currentStreamViewer);
            streamViewerThread.start();
            return true;
        } catch (Exception e) {
            handleStreamViewerError(e);
            return false;
        }
    }

    private static StreamViewerInterface createStreamViewer(String viewerType) throws Exception {
        switch (viewerType) {
            case "mock":
                return new MockStreamViewer("/mockImage.png");
            case "real":
                return new LumixStreamViewer(videoPanel::displayNewImage, options.getCameraIp(), options.getCameraNetMaskBitSize());
            case "webcam":
                return new WebcamStreamViewer(options.getWebcamIndex());
            default:
                throw new IllegalArgumentException("Invalid viewer type selected.");
        }
    }

    private static void stopStreamViewer() {
        if (currentStreamViewer != null) {
            if (currentStreamViewer instanceof WebcamStreamViewer) {
                ((WebcamStreamViewer) currentStreamViewer).stop();
            }
            if (streamViewerThread != null) {
                streamViewerThread.interrupt();
                try {
                    streamViewerThread.join(1000); // Wait for the thread to finish, but not more than 1 second
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
            currentStreamViewer = null;
            streamViewerThread = null;
        }
        // Clear the video panel
        SwingUtilities.invokeLater(() -> videoPanel.displayNewImage(null));
    }

    public static void switchCameraMode() {
        CameraModeSelector selector = new CameraModeSelector(window, options);
        String newMode = selector.selectMode();
        if (newMode != null && !newMode.equals(options.getViewerType())) {
            String previousMode = options.getViewerType();
            options.setViewerType(newMode);
            if (!initializeStreamViewer()) {
                // If initialization fails, revert to the previous mode
                options.setViewerType(previousMode);
                initializeStreamViewer();
            }
            // Update CameraStateMonitor if needed
            stopCameraStateMonitor();
            initializeCameraStateMonitor();
        }
    }

    private static void handleStreamViewerError(Exception e) {
        String errorMessage = "Error initializing stream viewer: " + e.getMessage();
        if (e instanceof IOException && options.getViewerType().equals("mock")) {
            errorMessage += "\nMake sure the mock image file exists in the resources folder.";
        }
        JOptionPane.showMessageDialog(window, errorMessage, "Error", JOptionPane.ERROR_MESSAGE);

        // Revert to the previous viewer type if available
        if (currentStreamViewer != null) {
            options.setViewerType(getPreviousViewerType());
            initializeStreamViewer();
        }
    }

    private static String getPreviousViewerType() {
        // This method should return a valid viewer type that was working before
        // For simplicity, we'll return "webcam" as a fallback, but you might want to implement
        // a more sophisticated method to remember the last working viewer type
        return "webcam";
    }
}