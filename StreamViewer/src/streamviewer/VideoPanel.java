package streamviewer;

import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.awt.image.BufferedImage;
import java.awt.image.DataBufferInt;
import java.io.File;
import java.io.IOException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import javax.imageio.ImageIO;

public class VideoPanel extends JPanel {

    private boolean blurredBackgroundEnabled = true;
    private int frameCount = 0;
    private static final int UPDATE_INTERVAL = 5; // Update background every X frames
    private int blurIntensity = 10;
    private Color backgroundColor = Color.WHITE;

    private BufferedImage bufferedImage = null;
    private BufferedImage blurredBackground = null;
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

    private PhotoTaker photoTaker;
    private final Options options;
    private ExecutorService executorService;


    public VideoPanel(Options options) {
        setLayout(null); // Use null layout for absolute positioning
        this.options = options;
        this.executorService = Executors.newSingleThreadExecutor();

        // Initialize photoButton
        photoButton = new RedRoundButton("Take Photo");
        photoButton.setBackground(new Color(255, 69, 58)); // A nicer red color
        photoButton.setForeground(Color.WHITE);
        photoButton.setFont(new Font("Arial", Font.BOLD, 30));
        photoButton.addActionListener(e -> {
            startCountdown();
            SwingUtilities.invokeLater(this::requestFocusInWindow);
        });
        add(photoButton);

        photoTaker = new PhotoTaker(options);

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
                    toggleBlurredBackground();
                } else if (e.getKeyCode() == KeyEvent.VK_C) {
                    changeBackgroundColor();
                }
            }
        });

        // Add component listener to handle resizing
        addComponentListener(new ComponentAdapter() {
            @Override
            public void componentResized(ComponentEvent e) {
                updateComponentPositions();
                updateBlurredBackground(true);
            }
        });

        // Load the smiley image
        try {
            smileyImage = ImageIO.read(new File("./src/streamviewer/partySmiley.png"));
        } catch (IOException e) {
            e.printStackTrace();
            System.err.println("Failed to load partySmiley.png. Make sure it exists in the correct directory.");
        }

        updateComponentPositions();
    }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        Graphics2D g2d = (Graphics2D) g;

        g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

        if (blurredBackgroundEnabled && blurredBackground != null) {
            g2d.drawImage(blurredBackground, 0, 0, getWidth(), getHeight(), null);
        } else {
            // Use the selected background color
            g2d.setColor(backgroundColor);
            g2d.fillRect(0, 0, getWidth(), getHeight());
        }

        if (bufferedImage != null) {
            drawFittedImage(g2d, bufferedImage);
        }

        if (showingSmiley && smileyImage != null) {
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

    private void updateBlurredBackground(boolean force) {
        if (bufferedImage == null) return;

        frameCount++;
        if (!force && frameCount % UPDATE_INTERVAL != 0) return;

        int panelWidth = getWidth();
        int panelHeight = getHeight();

        // Downscale the image for faster processing
        int scaledWidth = panelWidth / 4;
        int scaledHeight = panelHeight / 4;

        BufferedImage scaledImage = new BufferedImage(scaledWidth, scaledHeight, BufferedImage.TYPE_INT_RGB);
        Graphics2D g2d = scaledImage.createGraphics();
        g2d.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
        g2d.drawImage(bufferedImage, 0, 0, scaledWidth, scaledHeight, null);
        g2d.dispose();

        // Apply blur effect using the current blurIntensity
        BufferedImage blurred = new BufferedImage(scaledWidth, scaledHeight, BufferedImage.TYPE_INT_RGB);
        stackBlur(((DataBufferInt) scaledImage.getRaster().getDataBuffer()).getData(),
                ((DataBufferInt) blurred.getRaster().getDataBuffer()).getData(),
                scaledWidth, scaledHeight, blurIntensity);

        // Scale the blurred image back up to the panel size
        blurredBackground = new BufferedImage(panelWidth, panelHeight, BufferedImage.TYPE_INT_RGB);
        g2d = blurredBackground.createGraphics();
        g2d.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
        g2d.drawImage(blurred, 0, 0, panelWidth, panelHeight, null);
        g2d.dispose();

        repaint();
    }

    public void setBlurIntensity(int intensity) {
        this.blurIntensity = Math.max(1, Math.min(intensity, 100));
        updateBlurredBackground(true);
    }

    public void setBackgroundColor(Color color) {
        this.backgroundColor = color;
        repaint();
    }

    private void changeBackgroundColor() {
        Color newColor = JColorChooser.showDialog(this, "Choose Background Color", backgroundColor);
        if (newColor != null) {
            setBackgroundColor(newColor);
        }
    }

    public int getBlurIntensity() {
        return this.blurIntensity;
    }

    public void displayNewImage(BufferedImage image) {
        this.bufferedImage = image;
        updateBlurredBackground(false);
        SwingUtilities.invokeLater(this::repaint);
    }

    private void toggleBlurredBackground() {
        blurredBackgroundEnabled = !blurredBackgroundEnabled;
        repaint();
    }

    // Stack Blur Algorithm by Mario Klingemann <mario@quasimondo.com>
    private void stackBlur(int[] src, int[] dst, int w, int h, int radius) {
        int[] r = new int[w * h];
        int[] g = new int[w * h];
        int[] b = new int[w * h];

        for (int i = 0; i < src.length; i++) {
            r[i] = (src[i] >> 16) & 0xff;
            g[i] = (src[i] >> 8) & 0xff;
            b[i] = src[i] & 0xff;
        }

        int x, y, xp, yp, i;
        int sp;
        int rsum, gsum, bsum, num;

        for (y = 0; y < h; y++) {
            rsum = gsum = bsum = 0;
            for (i = -radius; i <= radius; i++) {
                yp = Math.max(0, Math.min(y + i, h - 1));
                sp = yp * w;
                rsum += r[sp];
                gsum += g[sp];
                bsum += b[sp];
            }
            for (x = 0; x < w; x++) {
                r[y * w + x] = rsum / (radius * 2 + 1);
                g[y * w + x] = gsum / (radius * 2 + 1);
                b[y * w + x] = bsum / (radius * 2 + 1);

                if (y == 0) continue;

                xp = Math.max(0, Math.min(x + radius + 1, w - 1));
                yp = Math.max(0, Math.min(y - radius, h - 1));

                rsum += r[yp * w + xp] - r[yp * w + Math.max(0, x - radius)];
                gsum += g[yp * w + xp] - g[yp * w + Math.max(0, x - radius)];
                bsum += b[yp * w + xp] - b[yp * w + Math.max(0, x - radius)];
            }
        }

        for (x = 0; x < w; x++) {
            rsum = gsum = bsum = 0;
            for (i = -radius; i <= radius; i++) {
                xp = Math.max(0, Math.min(x + i, w - 1));
                rsum += r[xp];
                gsum += g[xp];
                bsum += b[xp];
            }
            for (y = 0; y < h; y++) {
                dst[y * w + x] = (0xff << 24) | (clamp(rsum / (radius * 2 + 1)) << 16) | (clamp(gsum / (radius * 2 + 1)) << 8) | clamp(bsum / (radius * 2 + 1));

                if (x == 0) continue;

                xp = Math.max(0, Math.min(x + radius + 1, w - 1));
                yp = Math.max(0, Math.min(y - radius, h - 1));

                rsum += r[yp * w + xp] - r[Math.max(0, y - radius) * w + x];
                gsum += g[yp * w + xp] - g[Math.max(0, y - radius) * w + x];
                bsum += b[yp * w + xp] - b[Math.max(0, y - radius) * w + x];
            }
        }
    }

    private int clamp(int v) {
        return v > 255 ? 255 : (v < 0 ? 0 : v);
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
//        System.out.println("showSmileyImage called");
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
        photoButton.setEnabled(false); // Disable the button while taking a photo
        executorService.submit(() -> {
            photoTaker.takePhoto().thenAccept(result -> {
                SwingUtilities.invokeLater(() -> {
                    photoButton.setEnabled(true); // Re-enable the button
                    if (result.isSuccess()) {
                        System.out.println("Photo taken successfully!");
                    } else {
                        showErrorDialog(result.getErrorMessage());
                    }
                });
            });
        });
    }

    private void showErrorDialog(String errorMessage) {
        SwingUtilities.invokeLater(() -> {
            JOptionPane.showMessageDialog(this,
                    errorMessage,
                    "Error Taking Photo",
                    JOptionPane.ERROR_MESSAGE);
        });
    }

    public void shutdown() {
        executorService.shutdown();
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
            Options options = Options.read();
            JFrame frame = new JFrame("Video Panel");
            frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
            frame.setSize(800, 600);
            VideoPanel videoPanel = new VideoPanel(options);
            frame.add(videoPanel);
            frame.setVisible(true);
        });
    }
}