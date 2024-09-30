package streamviewer;

import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import javax.swing.*;

public class LumixStreamViewer {

    private static Thread streamViewerThread;
    private static JFrame window;
    private static boolean isFullscreen = false;
    private static VideoPanel videoPanel;

    public static void main(String[] args) {
        System.load(System.getProperty("user.dir") + "/lib/opencv_java4100.dll");  // For Windows

        Options options = Options.read();
        if (options == null) {
            return;  // User cancelled the input dialog
        }

        String cameraIp = options.getCameraIp();
        int cameraNetMaskBitSize = options.getCameraNetMaskBitSize();
        String viewerType = options.getViewerType();
        int webcamIndex = options.getWebcamIndex();

        videoPanel = new VideoPanel(options);

        StreamViewerInterface streamViewer = null;
        boolean viewerCreated = false;

        while (!viewerCreated) {
            try {
                switch (viewerType) {
                    case "mock":
                        streamViewer = new MockStreamViewer("./mockImage.png");
                        break;
                    case "real":
                        streamViewer = new StreamViewer(videoPanel::displayNewImage, cameraIp, cameraNetMaskBitSize);
                        break;
                    case "webcam":
                        streamViewer = new WebcamStreamViewer(webcamIndex);
                        break;
                    default:
                        throw new IllegalArgumentException("Invalid viewer type selected.");
                }
                viewerCreated = true;
            } catch (Exception e) {
                String errorMessage = "Error creating " + viewerType + " viewer: " + e.getMessage();
                if (viewerType.equals("webcam")) {
                    errorMessage += "\n\nPossible reasons for this error:\n" +
                            "1. No webcam is connected to your system.\n" +
                            "2. The selected webcam index is incorrect.\n" +
                            "3. The webcam is being used by another application.\n" +
                            "4. There are issues with the webcam drivers.\n\n" +
                            "Please ensure a webcam is connected and try again with a different index (starting from 0).";
                }
                errorMessage += "\n\nWould you like to try again with different settings?";

                int response = JOptionPane.showConfirmDialog(null, errorMessage, "Error", JOptionPane.YES_NO_OPTION);
                if (response != JOptionPane.YES_OPTION) {
                    System.exit(1);
                }
                options = Options.read();
                if (options == null) {
                    System.exit(0);
                }
                viewerType = options.getViewerType();
                webcamIndex = options.getWebcamIndex();
            }
        }

        streamViewer.setImageConsumer(videoPanel::displayNewImage);
        streamViewerThread = new Thread(streamViewer);
        streamViewerThread.start();

        window = new JFrame("Lumix Photobox");
        window.add(videoPanel);
        window.setSize(800, 600);  // Set a default size
        window.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        window.setVisible(true);

        window.addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosing(WindowEvent ev) {
                try {
                    streamViewerThread.interrupt();
                    streamViewerThread.join();
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }

                System.exit(0);
            }
        });
    }
}