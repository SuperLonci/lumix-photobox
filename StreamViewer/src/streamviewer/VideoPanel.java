package streamviewer;

import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.io.InputStream;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import javax.imageio.ImageIO;

public class VideoPanel extends JPanel implements CameraStateUpdateListener {
    private ExecutorService executorService;

    private int currentBackgroundMode = 0;
    private final BackgroundEffect[] backgroundEffects;
    private Color backgroundColor = Color.WHITE;

    private final Options options;
    private boolean isFullScreenTransition = false;
    private boolean isFullScreen = false;

    private BufferedImage currentImage;
    private final JButton photoButton;
    private Timer countdownTimer;
    private int countdownSeconds = 0;
    private Dimension lastWindowSize;
    private BufferedImage smileyImage;
    private boolean showingSmiley = false;
    private float countdownAlpha = 1.0f;
    private Timer smileyTimer;

    private final PhotoTaker photoTaker;

    private final JPanel infoPanel;
    private final JLabel batteryLabel;
    private final JLabel sdCardLabel;
    private final JLabel errorLabel;
    private final JLabel remainingImagesLabel;

    private final String cameraStateMonitorMode;
    private boolean showInfoPanel = false;

    private LedController ledController;

    private final Runnable cameraModeSwitch;

    public VideoPanel(Options options, Runnable cameraModeSwitch) {
        this.executorService = Executors.newSingleThreadExecutor();

        this.cameraModeSwitch = cameraModeSwitch;
        this.cameraStateMonitorMode = options.getCameraStateMonitorMode();

        this.options = options;

        setLayout(null); // Use null layout for absolute positioning
        this.executorService = Executors.newSingleThreadExecutor();

        // Initialize background effects
        backgroundEffects = new BackgroundEffect[]{
                new BackgroundEffect.NoEffect(),
                new BackgroundEffect.ColorFadeEffect(),
                new BackgroundEffect.ExtendedBackgroundEffect()
        };

        // Initialize photoButton
        photoButton = new RedRoundButton("Take Photo");
        photoButton.setBackground(new Color(255, 69, 58)); // A nicer red color
        photoButton.setForeground(Color.WHITE);
        photoButton.setFont(new Font("Arial", Font.BOLD, 30));
        photoButton.addActionListener(_ -> {
            startCountdown();
            SwingUtilities.invokeLater(this::requestFocusInWindow);
        });
        add(photoButton);

        photoTaker = new PhotoTaker(options);

        // Initialize info panel
        infoPanel = new JPanel();
        infoPanel.setLayout(new BoxLayout(infoPanel, BoxLayout.Y_AXIS));
        infoPanel.setBackground(new Color(0, 0, 0, 150));
        batteryLabel = new JLabel();
        sdCardLabel = new JLabel();
        errorLabel = new JLabel();
        remainingImagesLabel = new JLabel();  // Initialize the new label
        batteryLabel.setForeground(Color.WHITE);
        sdCardLabel.setForeground(Color.WHITE);
        errorLabel.setForeground(Color.RED);
        remainingImagesLabel.setForeground(Color.WHITE);  // Set color for the new label
        infoPanel.add(batteryLabel);
        infoPanel.add(sdCardLabel);
        infoPanel.add(remainingImagesLabel);  // Add the new label to the info panel
        infoPanel.add(errorLabel);
        infoPanel.setVisible(false);
        add(infoPanel);

        // Initialize LedController
        initializeLedController();

        // Set up key listener for Enter, Esc, F, and B keys
        setFocusable(true);
        addKeyListener(new KeyAdapter() {
            @Override
            public void keyPressed(KeyEvent e) {
                if (e.getKeyCode() == KeyEvent.VK_ENTER) {
                    startCountdown();
                } else if (e.getKeyCode() == KeyEvent.VK_F) {
                    toggleFullscreen();
                } else if (e.getKeyCode() == KeyEvent.VK_ESCAPE) {
                    exitFullscreen();
                } else if (e.getKeyCode() == KeyEvent.VK_B) {
                    cycleBackgroundMode();
                } else if (e.getKeyCode() == KeyEvent.VK_C) {
                    changeBackgroundColor();
                } else if (e.getKeyCode() == KeyEvent.VK_I && !cameraStateMonitorMode.equals("none")) {
                    toggleInfoPanel();
                } else if (e.getKeyCode() == KeyEvent.VK_L) {
                    ledController.cycleMode();
                } else if (e.getKeyCode() == KeyEvent.VK_H) {
                    Window window = SwingUtilities.getWindowAncestor(VideoPanel.this);
                    if (window instanceof JFrame) {
                        ledController.showBrightnessDialog((JFrame) window);
                    }
                } else if (e.getKeyCode() == KeyEvent.VK_K) {
                    cameraModeSwitch.run();
                }
            }
        });

        // Add component listener to handle resizing
        addComponentListener(new ComponentAdapter() {
            @Override
            public void componentResized(ComponentEvent e) {
                updateComponentPositions();
                for (BackgroundEffect effect : backgroundEffects) {
                    effect.updateDimensions(getWidth(), getHeight());
                }
            }
        });

        // Load the smiley image from the resources folder
        try (InputStream stream = getClass().getResourceAsStream("/partySmiley.png")) {
            if (stream == null) {
                System.err.println("Failed to load partySmiley.png. Make sure it exists in the resources folder.");
            } else {
                smileyImage = ImageIO.read(stream);
            }
        } catch (IOException e) {
            e.printStackTrace();
            System.err.println("Failed to load partySmiley.png.");
        }

        updateComponentPositions();
    }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        Graphics2D g2d = (Graphics2D) g;

        backgroundEffects[currentBackgroundMode].render(g2d);

        if (currentImage != null) {
            drawFittedImage(g2d, currentImage, true); // Draw video stream with 16:9 aspect ratio
        }

        if (showingSmiley && smileyImage != null) {
            drawFittedImage(g2d, smileyImage, false); // Draw smiley without forcing 16:9 aspect ratio
        }

        if (countdownSeconds > 0 && countdownSeconds <= 3) {
            drawCountdown(g2d);
        }
    }

    private void initializeLedController() {
        ledController = new LedController(options);
    }

    private void drawFittedImage(Graphics2D g2d, BufferedImage image, boolean force16by9) {
        int panelWidth = getWidth();
        int panelHeight = getHeight();
        int imageWidth = image.getWidth();
        int imageHeight = image.getHeight();

        double targetAspectRatio = force16by9 ? 16.0 / 9.0 : (double) imageWidth / imageHeight;
        double panelAspectRatio = (double) panelWidth / panelHeight;

        int scaledWidth, scaledHeight;
        if (panelAspectRatio > targetAspectRatio) {
            // Panel is wider, height is the limiting factor
            scaledHeight = panelHeight;
            scaledWidth = (int) (scaledHeight * targetAspectRatio);
        } else {
            // Panel is taller, width is the limiting factor
            scaledWidth = panelWidth;
            scaledHeight = (int) (scaledWidth / targetAspectRatio);
        }

        int x = (panelWidth - scaledWidth) / 2;
        int y = (panelHeight - scaledHeight) / 2;

        g2d.drawImage(image, x, y, scaledWidth, scaledHeight, null);
    }

    private void cycleBackgroundMode() {
        currentBackgroundMode = (currentBackgroundMode + 1) % backgroundEffects.length;
        repaint();
    }

    private void drawCountdown(Graphics2D g2d) {
        String text = String.valueOf(countdownSeconds);

        // Calculate the font size to be 90% of the screen height
        int fontSize = (int) (getHeight() * 0.9);
        Font font = new Font("Arial", Font.BOLD, fontSize);
        g2d.setFont(font);

        FontMetrics fm = g2d.getFontMetrics();
        int textWidth = fm.stringWidth(text);
        int textHeight = fm.getHeight();

        // Center the text both horizontally and vertically
        int x = (getWidth() - textWidth) / 2;
        int y = (getHeight() - textHeight) / 2 + fm.getAscent();

        // Create a gradual color change effect
        Color startColor = new Color(255, 69, 58); // Red
        Color endColor = new Color(255, 215, 0);   // Gold
        float fraction = (3 - countdownSeconds) / 2f;
        Color currentColor = interpolateColor(startColor, endColor, fraction);

        // Apply fade-out effect
        Composite originalComposite = g2d.getComposite();
        g2d.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, countdownAlpha));

        // Draw the text with a subtle shadow effect
        g2d.setColor(new Color(0, 0, 0, 100));
        g2d.drawString(text, x + 5, y + 5);

        g2d.setColor(currentColor);
        g2d.drawString(text, x, y);

        g2d.setComposite(originalComposite);
    }

    private Color interpolateColor(Color c1, Color c2, float fraction) {
        int red = (int) (c1.getRed() + fraction * (c2.getRed() - c1.getRed()));
        int green = (int) (c1.getGreen() + fraction * (c2.getGreen() - c1.getGreen()));
        int blue = (int) (c1.getBlue() + fraction * (c2.getBlue() - c1.getBlue()));
        return new Color(red, green, blue);
    }

    void updateComponentPositions() {
        int width = getWidth();
        int height = getHeight();

        // Position the photo button at the bottom right
        int buttonSize = 200;
        photoButton.setBounds(width - buttonSize - 10, height - buttonSize - 10, buttonSize, buttonSize);

        // Position the info panel at the bottom left
        int infoPanelWidth = 250;
        int infoPanelHeight = 80;
        infoPanel.setBounds(10, height - infoPanelHeight - 10, infoPanelWidth, infoPanelHeight);

        // Ensure the panel is repainted after updating positions
        revalidate();
        repaint();
    }

    public void displayNewImage(BufferedImage image) {
        this.currentImage = image;
        SwingUtilities.invokeLater(this::repaint);
    }

    private void changeBackgroundColor() {
        Color newColor = JColorChooser.showDialog(this, "Choose Background Color", backgroundColor);
        if (newColor != null) {
            setBackgroundColor(newColor);
        }
    }

    public void setBackgroundColor(Color color) {
        this.backgroundColor = color;
        ((BackgroundEffect.NoEffect) backgroundEffects[0]).setBackgroundColor(color);
        repaint();
    }

    private void startCountdown() {
        if (countdownTimer != null && countdownTimer.isRunning()) {
            return;
        }
        countdownSeconds = 3;
        showingSmiley = false;
        countdownAlpha = 1.0f;

        // Start with mode 2 for the countdown
        ledController.sendCommand("mode 2" + ";");

        countdownTimer = new Timer(16, new ActionListener() {
            private final long startTime = System.currentTimeMillis();
            private int lastSecond = 3;
            private boolean ledCommandSent = false;

            @Override
            public void actionPerformed(ActionEvent e) {
                long currentTime = System.currentTimeMillis();
                float elapsedSeconds = (currentTime - startTime) / 1000f;
                countdownSeconds = 3 - (int) elapsedSeconds;

                if (countdownSeconds != lastSecond) {
                    countdownAlpha = 1.0f;
                    lastSecond = countdownSeconds;
                } else {
                    float fractionOfSecond = elapsedSeconds % 1;
                    countdownAlpha = Math.max(0, 1 - fractionOfSecond);
                }

                // Switch to mode 1 approximately 0.5 seconds before photo is taken
                if (!ledCommandSent && elapsedSeconds >= 2.5) {
                    ledController.sendCommand("mode 1" + ";");
                    ledCommandSent = true;
                }

                if (countdownSeconds > 0) {
                    repaint();
                } else {
                    ((Timer) e.getSource()).stop();
                    showSmileyImage();
                }
            }
        });
        countdownTimer.start();
    }

    private void showSmileyImage() {
        showingSmiley = true;
        repaint();
        takePhoto();

        if (smileyTimer != null && smileyTimer.isRunning()) {
            smileyTimer.stop();
        }
        smileyTimer = new Timer(1000, e -> {
            showingSmiley = false;
            ((Timer) e.getSource()).stop();
            SwingUtilities.invokeLater(this::requestFocusInWindow);
            repaint();
        });
        smileyTimer.setRepeats(false);
        smileyTimer.start();
    }

    private void takePhoto() {
        repaint();
        photoButton.setEnabled(false); // Disable the button while taking a photo
        executorService.submit(() -> {
            photoTaker.takePhoto().thenAccept(result -> SwingUtilities.invokeLater(() -> {
                photoButton.setEnabled(true); // Re-enable the button
                if (result.isSuccess()) {
                    System.out.println("Photo taken successfully!");
                } else {
                    showErrorDialog(result.getErrorMessage());
                }
                // Return to the previous LED mode after taking the photo
                ledController.sendCommand("mode " + ledController.getCurrentMode() + ";");
            }));
        });
    }

    private void showErrorDialog(String errorMessage) {
        SwingUtilities.invokeLater(() -> {
            JOptionPane pane = new JOptionPane(errorMessage, JOptionPane.ERROR_MESSAGE);
            JDialog dialog = pane.createDialog(this, "Error Taking Photo");

            Timer timer = new Timer(5000, (_) -> dialog.dispose());
            timer.setRepeats(false);
            timer.start();

            dialog.setVisible(true);
        });
    }

    @Override
    public void onCameraStateUpdate(String batteryStatus, String sdCardStatus, String remainingImages, String errorMessage, boolean lowBattery, boolean noSdCard) {
        if (cameraStateMonitorMode.equals("none")) return;

        batteryLabel.setText("Battery: " + batteryStatus);
        sdCardLabel.setText("SD Card: " + sdCardStatus);
        remainingImagesLabel.setText("Remaining Images: " + remainingImages);

        // Check battery status
        if ("0/3".equals(batteryStatus) || lowBattery) {
            batteryLabel.setForeground(Color.RED);
        } else {
            batteryLabel.setForeground(Color.WHITE);
        }

        if (noSdCard) {
            sdCardLabel.setForeground(Color.RED);
            sdCardLabel.setText("SD Card: Not Found");
        } else {
            sdCardLabel.setForeground(Color.WHITE);
        }

        // Check if remaining images are below 50
        boolean lowRemainingImages = false;
        try {
            int remainingImagesCount = Integer.parseInt(remainingImages);
            if (remainingImagesCount < 50) {
                lowRemainingImages = true;
                remainingImagesLabel.setForeground(Color.RED);
            } else {
                remainingImagesLabel.setForeground(Color.WHITE);
            }
        } catch (NumberFormatException e) {
            // If parsing fails, keep the text white
            remainingImagesLabel.setForeground(Color.WHITE);
        }

        errorLabel.setText(errorMessage != null ? "Error: " + errorMessage : "");
        errorLabel.setVisible(errorMessage != null && !errorMessage.isEmpty());

        boolean shouldShowPanel = "0/3".equals(batteryStatus) || lowBattery || noSdCard || lowRemainingImages || (errorMessage != null && !errorMessage.isEmpty());
        showInfoPanel = showInfoPanel || shouldShowPanel;
        updateInfoPanelVisibility();
    }

    private void toggleInfoPanel() {
        showInfoPanel = !showInfoPanel;
        updateInfoPanelVisibility();
    }

    private void updateInfoPanelVisibility() {
        infoPanel.setVisible(showInfoPanel);
        repaint();
    }


    // Fullscreen
    private void toggleFullscreen() {
        Window window = SwingUtilities.getWindowAncestor(this);
        if (window instanceof JFrame frame) {
            GraphicsDevice gd = GraphicsEnvironment.getLocalGraphicsEnvironment().getDefaultScreenDevice();
            isFullScreenTransition = true;
            try {
                if (isFullScreen) {
                    exitFullscreen(frame, gd);
                } else {
                    lastWindowSize = frame.getSize();
                    enterFullscreen(frame, gd);
                }
                isFullScreen = !isFullScreen;
            } finally {
                isFullScreenTransition = false;
            }
            SwingUtilities.invokeLater(() -> {
                requestFocusInWindow();
                updateComponentPositions();
            });
        }
    }

    private void exitFullscreen() {
        Window window = SwingUtilities.getWindowAncestor(this);
        if (window instanceof JFrame frame) {
            GraphicsEnvironment ge = GraphicsEnvironment.getLocalGraphicsEnvironment();
            GraphicsDevice gd = ge.getDefaultScreenDevice();

            if (isFullScreen) {
                isFullScreenTransition = true;
                try {
                    exitFullscreen(frame, gd);
                    isFullScreen = false;
                } finally {
                    isFullScreenTransition = false;
                }
                SwingUtilities.invokeLater(() -> {
                    requestFocusInWindow();
                    updateComponentPositions();
                });
            }
        }
    }


    private void enterFullscreen(JFrame frame, GraphicsDevice gd) {
        frame.dispose();
        frame.setUndecorated(true);
        gd.setFullScreenWindow(frame);
    }

    private void exitFullscreen(JFrame frame, GraphicsDevice gd) {
        gd.setFullScreenWindow(null);
        frame.dispose();
        frame.setUndecorated(false);
        frame.setVisible(true);
        if (lastWindowSize != null) {
            frame.setSize(lastWindowSize);
        } else {
            frame.pack();
        }
    }

    @Override
    public void removeNotify() {
        super.removeNotify();
        if (!isFullScreenTransition) {
            if (ledController != null) {
                ledController.close();
                ledController = null;
            }
            if (executorService != null && !executorService.isShutdown()) {
                executorService.shutdownNow();
            }
        }
    }
}