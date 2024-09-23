package streamviewer;

import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.awt.image.BufferedImage;

public class VideoPanel extends JPanel {

    private BufferedImage bufferedImage = null;
    private JButton photoButton;
    private JLabel countdownLabel;
    private Timer countdownTimer;
    private int countdownSeconds = 3;
    private long lastImageDisplayTime = 0;

    public VideoPanel() {
        setLayout(null); // Use null layout for absolute positioning

        // Create the photo button
        photoButton = new RoundButton("Take Photo");
        photoButton.setBackground(new Color(255, 69, 58)); // A nicer red color
        photoButton.setForeground(Color.WHITE);
        photoButton.setFont(new Font("Arial", Font.BOLD, 30));
        photoButton.addActionListener(e -> startCountdown());
        add(photoButton);

        // Create the countdown label
        countdownLabel = new JLabel("") {
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2d = (Graphics2D) g.create();
                g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

                String text = getText();
                FontMetrics fm = g2d.getFontMetrics(getFont());
                int textWidth = fm.stringWidth(text);
                int textHeight = fm.getAscent();

                // Draw shadow
                g2d.setColor(Color.BLACK);
                g2d.drawString(text, (getWidth() - textWidth) / 2 + 5, (getHeight() + textHeight) / 2 + 5);

                // Draw text
                g2d.setColor(getForeground());
                g2d.drawString(text, (getWidth() - textWidth) / 2, (getHeight() + textHeight) / 2);

                g2d.dispose();
            }
        };
        countdownLabel.setHorizontalAlignment(SwingConstants.CENTER);
        countdownLabel.setFont(new Font("Arial", Font.BOLD, 150)); // Increased font size
        countdownLabel.setForeground(Color.WHITE);
        add(countdownLabel);

        // Set up key listener for Enter key
        setFocusable(true);
        addKeyListener(new KeyAdapter() {
            @Override
            public void keyPressed(KeyEvent e) {
                if (e.getKeyCode() == KeyEvent.VK_ENTER) {
                    startCountdown();
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
    }

    public void displayNewImage(BufferedImage image) {
        this.bufferedImage = image;
        long currentTime = System.currentTimeMillis();
        System.out.println("New image received. Time since last image: " + (currentTime - lastImageDisplayTime) + "ms");
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
            System.out.println("Image painted. Size: " + scaledWidth + "x" + scaledHeight);
        } else {
            System.out.println("No image to paint");
        }
    }

    private void startCountdown() {
        if (countdownTimer != null && countdownTimer.isRunning()) {
            return; // Countdown is already in progress
        }

        countdownSeconds = 3;
        updateCountdownLabel();

        countdownTimer = new Timer(1000, new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                countdownSeconds--;
                if (countdownSeconds > 0) {
                    updateCountdownLabel();
                } else {
                    ((Timer)e.getSource()).stop();
                    countdownLabel.setText("");
                    takePhoto();
                }
            }
        });

        countdownTimer.start();
    }

    private void updateCountdownLabel() {
        countdownLabel.setText(String.valueOf(countdownSeconds));
    }

    private void takePhoto() {
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