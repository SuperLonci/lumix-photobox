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
    private int countdownSeconds = 3;
    private Dimension lastWindowSize;
    private BufferedImage smileyImage;
    private boolean showingSmiley = false;

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

        // Create the countdown label
        countdownLabel = createCountdownLabel();
        add(countdownLabel);
    }

    private JLabel createCountdownLabel() {
        return new JLabel("") {
            @Override
            protected void paintComponent(Graphics g) {
                super.paintComponent(g);
                if (showingSmiley && smileyImage != null) {
                    drawFittedImage(g, smileyImage);
                } else {
                    drawCountdown(g);
                }
            }
        };
    }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        if (bufferedImage != null) {
            drawFittedImage(g, bufferedImage);
        }
    }

    private void drawFittedImage(Graphics g, BufferedImage image) {
        int panelWidth = getWidth();
        int panelHeight = getHeight();
        int imageWidth = image.getWidth();
        int imageHeight = image.getHeight();
        double scale = Math.min((double) panelWidth / imageWidth, (double) panelHeight / imageHeight);
        int scaledWidth = (int) (imageWidth * scale);
        int scaledHeight = (int) (imageHeight * scale);
        int x = (panelWidth - scaledWidth) / 2;
        int y = (panelHeight - scaledHeight) / 2;
        g.drawImage(image, x, y, scaledWidth, scaledHeight, null);
    }

    private void drawCountdown(Graphics g) {
        String text = countdownLabel.getText();
        Font font = new Font("Arial", Font.BOLD, 300);
        g.setFont(font);
        FontMetrics fm = g.getFontMetrics();
        int textWidth = fm.stringWidth(text);
        int textHeight = fm.getAscent();
        int x = (getWidth() - textWidth) / 2;
        int y = (getHeight() + textHeight) / 2;

        g.setColor(Color.WHITE);
        g.drawString(text, x, y);
    }

    void updateComponentPositions() {
        int width = getWidth();
        int height = getHeight();

        // Position the photo button at the bottom right
        int buttonSize = 200;
        photoButton.setBounds(width - buttonSize - 10, height - buttonSize - 10, buttonSize, buttonSize);

        // Position the countdown label in the center
        countdownLabel.setBounds(0, 0, width, height);

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
            return; // Countdown is already in progress
        }
        countdownSeconds = 3;
        showingSmiley = false;
        updateCountdownLabel();
        countdownTimer = new Timer(1000, e -> {
            countdownSeconds--;
            if (countdownSeconds > 0) {
                updateCountdownLabel();
            } else if (countdownSeconds == 0) {
                showSmileyImage();
            } else {
                ((Timer) e.getSource()).stop();
                takePhoto();
                SwingUtilities.invokeLater(this::requestFocusInWindow);
            }
        });
        countdownTimer.start();
    }

    private void updateCountdownLabel() {
        countdownLabel.setText(String.valueOf(countdownSeconds));
        countdownLabel.repaint();
    }

    private void showSmileyImage() {
        showingSmiley = true;
        countdownLabel.setText("");
        countdownLabel.repaint();
    }

    private void takePhoto() {
        showingSmiley = false;
        countdownLabel.setText("");
        countdownLabel.repaint();
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
        JFrame frame = new JFrame("Video Panel");
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setSize(800, 600);
        frame.add(new VideoPanel());
        frame.setVisible(true);
    }
}