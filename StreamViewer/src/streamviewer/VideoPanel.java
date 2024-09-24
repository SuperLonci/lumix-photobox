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
    private long lastImageDisplayTime = 0;
    private Dimension lastWindowSize;
    private BufferedImage smileyImage;
    private boolean showingSmiley = false;

    public VideoPanel() {
        setLayout(null); // Use null layout for absolute positioning

        // Load the smiley image
        try {
            smileyImage = ImageIO.read(new File("./src/streamviewer/partySmiley.png"));
        } catch (IOException e) {
            e.printStackTrace();
            System.err.println("Failed to load partySmiley.png. Make sure it exists in the correct directory.");
        }

        // Create the photo button
        photoButton = new RoundButton("Take Photo");
        photoButton.setBackground(new Color(255, 69, 58)); // A nicer red color
        photoButton.setForeground(Color.WHITE);
        photoButton.setFont(new Font("Arial", Font.BOLD, 30));
        photoButton.addActionListener(e -> {
            startCountdown();
            SwingUtilities.invokeLater(this::requestFocusInWindow); // Request focus after button press
        });
        add(photoButton);

        // Create the countdown label
        countdownLabel = new JLabel("") {
            @Override
            protected void paintComponent(Graphics g) {
                super.paintComponent(g);
                Graphics2D g2d = (Graphics2D) g.create();
                g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

                if (showingSmiley && smileyImage != null) {
                    // Draw the smiley image
                    int imageWidth = getWidth() / 2;
                    int imageHeight = getHeight() / 2;
                    int x = (getWidth() - imageWidth) / 2;
                    int y = (getHeight() - imageHeight) / 2;
                    g2d.drawImage(smileyImage, x, y, imageWidth, imageHeight, null);
                } else {
                    // Draw the countdown number
                    String text = getText();
                    Font font = new Font("Arial", Font.BOLD, 300);
                    g2d.setFont(font);
                    FontMetrics fm = g2d.getFontMetrics();
                    int textWidth = fm.stringWidth(text);
                    int textHeight = fm.getAscent();

                    int x = (getWidth() - textWidth) / 2;
                    int y = (getHeight() + textHeight) / 2;

                    // Draw outline
                    g2d.setColor(Color.BLACK);
                    g2d.setStroke(new BasicStroke(8));
                    g2d.drawString(text, x, y);

                    // Draw text
//                    g2d.setColor(Color.WHITE);
//                    g2d.drawString(text, x, y);
                }

                g2d.dispose();
            }
        };
        countdownLabel.setHorizontalAlignment(SwingConstants.CENTER);
        countdownLabel.setVerticalAlignment(SwingConstants.CENTER);
        countdownLabel.setForeground(Color.WHITE);
        add(countdownLabel);

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
        long currentTime = System.currentTimeMillis();
//        System.out.println("New image received. Time since last image: " + (currentTime - lastImageDisplayTime) + "ms");
        lastImageDisplayTime = currentTime;
        SwingUtilities.invokeLater(this::repaint);
    }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        if (this.bufferedImage != null) {
            Graphics2D g2d = (Graphics2D) g.create();
            int panelWidth = getWidth();
            int panelHeight = getHeight();
            int imageWidth = bufferedImage.getWidth();
            int imageHeight = bufferedImage.getHeight();

            double scale = Math.min((double) panelWidth / imageWidth, (double) panelHeight / imageHeight);
            int scaledWidth = (int) (imageWidth * scale);
            int scaledHeight = (int) (imageHeight * scale);
            int x = (panelWidth - scaledWidth) / 2;
            int y = (panelHeight - scaledHeight) / 2;

            g2d.drawImage(bufferedImage, x, y, scaledWidth, scaledHeight, null);
            g2d.dispose();
//            System.out.println("Image painted. Size: " + scaledWidth + "x" + scaledHeight);
        } else {
            System.out.println("No image to paint");
        }
    }

    private void toggleFullscreen() {
        Window window = SwingUtilities.getWindowAncestor(this);
        if (window instanceof JFrame) {
            JFrame frame = (JFrame) window;
            GraphicsEnvironment ge = GraphicsEnvironment.getLocalGraphicsEnvironment();
            GraphicsDevice gd = ge.getDefaultScreenDevice();

            if (frame.isUndecorated()) {
                exitFullscreen(frame, gd);
            } else {
                lastWindowSize = frame.getSize();
                enterFullscreen(frame, gd);
            }

            // Ensure focus is set back to the panel
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

    private void startCountdown() {
        if (countdownTimer != null && countdownTimer.isRunning()) {
            return; // Countdown is already in progress
        }

        countdownSeconds = 3;
        showingSmiley = false;
        updateCountdownLabel();

        countdownTimer = new Timer(1000, new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                countdownSeconds--;
                if (countdownSeconds > 0) {
                    updateCountdownLabel();
                } else if (countdownSeconds == 0) {
                    showSmileyImage();
                } else {
                    ((Timer)e.getSource()).stop();
                    takePhoto();
                    SwingUtilities.invokeLater(VideoPanel.this::requestFocusInWindow);
                }
            }
        });

        countdownTimer.start();
    }

    private void updateCountdownLabel() {
        showingSmiley = false;
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

    // Custom RoundButton class
    private static class RoundButton extends JButton {
        public RoundButton(String label) {
            super(label);
            setContentAreaFilled(false);
            setFocusPainted(false);
        }

        @Override
        protected void paintComponent(Graphics g) {
            Graphics2D g2 = (Graphics2D) g;
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

            // Create gradient paint for the button
            GradientPaint gradientPaint = new GradientPaint(
                    0, 0, getBackground().brighter(),
                    0, getHeight(), getBackground().darker()
            );
            g2.setPaint(gradientPaint);
            g2.fillOval(0, 0, getWidth(), getHeight());

            // Add inner shadow
            g2.setColor(new Color(0, 0, 0, 50));
            g2.fillOval(2, 2, getWidth() - 4, getHeight() - 4);

            g2.setColor(getForeground());
            g2.setFont(getFont());
            FontMetrics fm = g.getFontMetrics();
            int stringWidth = fm.stringWidth(getText());
            int stringHeight = fm.getAscent();
            g2.drawString(getText(), (getWidth() - stringWidth) / 2, (getHeight() + stringHeight) / 2 - 4);
            super.paintComponent(g);
        }

        @Override
        protected void paintBorder(Graphics g) {
            Graphics2D g2 = (Graphics2D) g;
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g2.setColor(getForeground());
            g2.drawOval(0, 0, getWidth() - 1, getHeight() - 1);
        }

        @Override
        public boolean contains(int x, int y) {
            int radius = getWidth() / 2;
            return (x - radius) * (x - radius) + (y - radius) * (y - radius) <= radius * radius;
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