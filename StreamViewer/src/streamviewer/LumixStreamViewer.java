package streamviewer;

import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.awt.event.KeyEvent;
import java.awt.event.KeyAdapter;
import javax.swing.JFrame;
import java.awt.GraphicsEnvironment;
import java.awt.GraphicsDevice;

public class LumixStreamViewer {

    private static Thread streamViewerThread;
    private static JFrame window;
    private static boolean isFullscreen = false;
    private static VideoPanel videoPanel;

    public static void main(String[] args) {
        Options options = Options.read(args);
        String cameraIp = options.getCameraIp();
        int cameraNetMaskBitSize = options.getCameraNetMaskBitSize();

        videoPanel = new VideoPanel();

        StreamViewerInterface streamViewer;

        // Select the stream viewer based on a command-line argument or system property
        String viewerType = System.getProperty("viewer.type", "mock");

        switch (viewerType) {
            case "mock":
                streamViewer = new MockStreamViewer("./mockImage.png");
                break;
            case "real":
            default:
                try {
                    streamViewer = new StreamViewer(videoPanel::displayNewImage, cameraIp, cameraNetMaskBitSize);
                } catch (Exception e) {
                    System.out.println("Error creating StreamViewer: " + e.getMessage());
                    System.exit(1);
                    return;
                }
                break;
        }

        streamViewer.setImageConsumer(videoPanel::displayNewImage);
        streamViewerThread = new Thread(streamViewer);
        streamViewerThread.start();

        window = new JFrame("Lumix Live Stream viewer on " + cameraIp + ":49199");
        window.add(videoPanel);
        window.setSize(800, 600);  // Set a default size
        window.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        window.setVisible(true);
        videoPanel.requestFocusInWindow();

        // Add key listener for fullscreen toggle
        window.addKeyListener(new KeyAdapter() {
            @Override
            public void keyPressed(KeyEvent e) {
                if (e.getKeyCode() == KeyEvent.VK_F11) {
                    toggleFullscreen();
                }
            }
        });

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

    private static void toggleFullscreen() {
        GraphicsEnvironment ge = GraphicsEnvironment.getLocalGraphicsEnvironment();
        GraphicsDevice gd = ge.getDefaultScreenDevice();

        if (!isFullscreen) {
            window.dispose();
            window.setUndecorated(true);
            gd.setFullScreenWindow(window);
        } else {
            gd.setFullScreenWindow(null);
            window.dispose();
            window.setUndecorated(false);
            window.setVisible(true);
        }

        isFullscreen = !isFullscreen;
        videoPanel.updateComponentPositions(); // Update component positions after toggling fullscreen
    }
}