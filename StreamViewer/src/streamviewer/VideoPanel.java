package streamviewer;

import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import javax.imageio.ImageIO;

public class VideoPanel extends JPanel {

    private BufferedImage bufferedImage = null;
    private JButton photoButton;
    private JLabel countdownLabel;
    private Timer countdownTimer;
    private int countdownSeconds = 0;
    private Dimension lastWindowSize;
    private BufferedImage smileyImage;
    private boolean showingSmiley = false;
    private float countdownAlpha = 1.0f;
    private float countdownScale = 1.0f;
    private Timer smileyTimer;

    public VideoPanel() {
        setLayout(null); // Use null layout for absolute positioning

        // Set up key listener for Enter, Esc and F key
        setFocusable(true);
        addKeyListener(new KeyAdapter() {
            @Override
            public void keyPressed(KeyEvent e) {
                if (e.getKeyCode() == KeyEvent.VK_ENTER) {
                    startCountdown();
                } else if (e.getKeyCode() == KeyEvent.VK_F) {
                    System.out.println("F key pressed"); // Debug print
                    toggleFullscreen();
                } else if (e.getKeyCode() == KeyEvent.VK_ESCAPE) {
                    System.out.println("ESC key pressed"); // Debug print
                    exitFullscreen();
                }
            }
        });

        // Add component listener to handle resizing
        addComponentListener(new ComponentAdapter() {
            @Override
            public void componentResized(ComponentEvent e) {
                updateComponentPositions();
            }
        });

        // Load the smiley image
        try {
            smileyImage = ImageIO.read(new File("./src/streamviewer/partySmiley.png"));
        } catch (IOException e) {
            e.printStackTrace();
            System.err.println("Failed to load partySmiley.png. Make sure it exists in the correct directory.");
        }

        // Create the photo button
        photoButton = new RedRoundButton("Take Photo");
        photoButton.setBackground(new Color(255, 69, 58)); // A nicer red color
        photoButton.setForeground(Color.WHITE);
        photoButton.setFont(new Font("Arial", Font.BOLD, 30));
        photoButton.addActionListener(e -> {
            startCountdown();
            SwingUtilities.invokeLater(this::requestFocusInWindow); // Request focus after button press
        });
        add(photoButton);

        updateComponentPositions();
    }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        Graphics2D g2d = (Graphics2D) g;

        // Enable antialiasing for smoother rendering
        g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

        if (bufferedImage != null) {
            drawFittedImage(g2d, bufferedImage);
        }

        if (showingSmiley && smileyImage != null) {
//            System.out.println("Attempting to draw smiley image");
            drawFittedImage(g2d, smileyImage);
        }

        if (countdownSeconds > 0 && countdownSeconds <= 3) {
            drawCountdown(g2d);
        }
    }

    private void drawFittedImage(Graphics2D g2d, BufferedImage image) {
        int panelWidth = getWidth();
        int panelHeight = getHeight();
        int imageWidth = image.getWidth();
        int imageHeight = image.getHeight();
        double scale = Math.min((double) panelWidth / imageWidth, (double) panelHeight / imageHeight);
        int scaledWidth = (int) (imageWidth * scale);
        int scaledHeight = (int) (imageHeight * scale);
        int x = (panelWidth - scaledWidth) / 2;
        int y = (panelHeight - scaledHeight) / 2;
        g2d.drawImage(image, x, y, scaledWidth, scaledHeight, null);
//        System.out.println("Image drawn at: " + x + "," + y + " with dimensions: " + scaledWidth + "x" + scaledHeight);
    }

    private void drawCountdown(Graphics2D g2d) {
        String text = String.valueOf(countdownSeconds);

        // Calculate the font size to be 90% of the screen height
        int fontSize = (int)(getHeight() * 0.9);
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

        // Ensure the panel is repainted after updating positions
        revalidate();
        repaint();
    }

    public void displayNewImage(BufferedImage image) {
        this.bufferedImage = image;
        SwingUtilities.invokeLater(this::repaint);
    }

    private void startCountdown() {
        if (countdownTimer != null && countdownTimer.isRunning()) {
            return;
        }
        countdownSeconds = 3;
        showingSmiley = false;
        countdownAlpha = 1.0f;
        countdownScale = 1.0f;

        countdownTimer = new Timer(16, new ActionListener() {
            private long startTime = System.currentTimeMillis();
            private int lastSecond = 3;

            @Override
            public void actionPerformed(ActionEvent e) {
                long currentTime = System.currentTimeMillis();
                float elapsedSeconds = (currentTime - startTime) / 1000f;
                countdownSeconds = 3 - (int) elapsedSeconds;

                if (countdownSeconds != lastSecond) {
                    countdownAlpha = 1.0f;
                    countdownScale = 1.0f;
                    lastSecond = countdownSeconds;
                } else {
                    float fractionOfSecond = elapsedSeconds % 1;
                    countdownAlpha = Math.max(0, 1 - fractionOfSecond);
                    countdownScale = 1 + (0.5f * fractionOfSecond);
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
        System.out.println("showSmileyImage called");
        showingSmiley = true;
        repaint();

        if (smileyTimer != null && smileyTimer.isRunning()) {
            smileyTimer.stop();
        }
        smileyTimer = new Timer(1000, e -> {
//            System.out.println("Smiley timer finished");
            showingSmiley = false;
            ((Timer) e.getSource()).stop();
            takePhoto();
            SwingUtilities.invokeLater(this::requestFocusInWindow);
        });
        smileyTimer.setRepeats(false);
        smileyTimer.start();
    }

    private void takePhoto() {
        repaint();
        // TODO: Implement photo-taking functionality
        System.out.println("Photo taken!");
    }

    // Fullscreen
    private void toggleFullscreen() {
        Window window = SwingUtilities.getWindowAncestor(this);
        if (window instanceof JFrame) {
            JFrame frame = (JFrame) window;
            GraphicsDevice gd = GraphicsEnvironment.getLocalGraphicsEnvironment().getDefaultScreenDevice();
            if (frame.isUndecorated()) {
                exitFullscreen(frame, gd);
            } else {
                lastWindowSize = frame.getSize();
                enterFullscreen(frame, gd);
            }
            SwingUtilities.invokeLater(this::requestFocusInWindow);
            updateComponentPositions();
        }
    }

    private void exitFullscreen() {
        System.out.println("exitFullscreen() called"); // Debug print
        Window window = SwingUtilities.getWindowAncestor(this);
        if (window instanceof JFrame) {
            JFrame frame = (JFrame) window;
            GraphicsEnvironment ge = GraphicsEnvironment.getLocalGraphicsEnvironment();
            GraphicsDevice gd = ge.getDefaultScreenDevice();

            if (frame.isUndecorated()) {
                exitFullscreen(frame, gd);
                requestFocusInWindow();
                updateComponentPositions();
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

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            JFrame frame = new JFrame("Video Panel");
            frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
            frame.setSize(800, 600);
            VideoPanel videoPanel = new VideoPanel();
            frame.add(videoPanel);
            frame.setVisible(true);
        });
    }
}