package streamviewer;

import java.awt.*;
import java.awt.image.BufferedImage;

public abstract class BackgroundEffect {
    protected BufferedImage bufferedImage;
    protected int panelWidth;
    protected int panelHeight;

    public void updateDimensions(int width, int height) {
        this.panelWidth = width;
        this.panelHeight = height;
    }

    public void setImage(BufferedImage image) {
        this.bufferedImage = image;
    }

    public abstract void render(Graphics2D g2d);

    public static class NoEffect extends BackgroundEffect {
        private Color backgroundColor = Color.WHITE;

        public void setBackgroundColor(Color color) {
            this.backgroundColor = color;
        }

        @Override
        public void render(Graphics2D g2d) {
            g2d.setColor(backgroundColor);
            g2d.fillRect(0, 0, panelWidth, panelHeight);
        }
    }

    public static class ColorFadeEffect extends BackgroundEffect {
        @Override
        public void render(Graphics2D g2d) {
            if (bufferedImage == null) return;

            int imageWidth = bufferedImage.getWidth();
            int imageHeight = bufferedImage.getHeight();

            // Draw the original image in the center
            int x = (panelWidth - imageWidth) / 2;
            int y = (panelHeight - imageHeight) / 2;
            g2d.drawImage(bufferedImage, x, y, null);

            // Get edge colors
            int leftColor = bufferedImage.getRGB(0, imageHeight / 2);
            int rightColor = bufferedImage.getRGB(imageWidth - 1, imageHeight / 2);
            int topColor = bufferedImage.getRGB(imageWidth / 2, 0);
            int bottomColor = bufferedImage.getRGB(imageWidth / 2, imageHeight - 1);

            // Create gradients for sides
            createHorizontalGradient(g2d, 0, y, x, imageHeight, rightColor, leftColor);
            createHorizontalGradient(g2d, x + imageWidth, y, panelWidth - (x + imageWidth), imageHeight, leftColor, rightColor);

            // Create gradients for top and bottom
            createVerticalGradient(g2d, x, 0, imageWidth, y, bottomColor, topColor);
            createVerticalGradient(g2d, x, y + imageHeight, imageWidth, panelHeight - (y + imageHeight), topColor, bottomColor);

            // Fill corners
            g2d.setColor(new Color(leftColor));
            g2d.fillRect(0, 0, x, y);
            g2d.fillRect(0, y + imageHeight, x, panelHeight - (y + imageHeight));
            g2d.setColor(new Color(rightColor));
            g2d.fillRect(x + imageWidth, 0, panelWidth - (x + imageWidth), y);
            g2d.fillRect(x + imageWidth, y + imageHeight, panelWidth - (x + imageWidth), panelHeight - (y + imageHeight));
        }

        private void createHorizontalGradient(Graphics2D g2d, int x, int y, int width, int height, int startColor, int endColor) {
            GradientPaint gradient = new GradientPaint(x, y, new Color(startColor), x + width, y, new Color(endColor));
            g2d.setPaint(gradient);
            g2d.fillRect(x, y, width, height);
        }

        private void createVerticalGradient(Graphics2D g2d, int x, int y, int width, int height, int startColor, int endColor) {
            GradientPaint gradient = new GradientPaint(x, y, new Color(startColor), x, y + height, new Color(endColor));
            g2d.setPaint(gradient);
            g2d.fillRect(x, y, width, height);
        }
    }

    public static class ExtendedBackgroundEffect extends BackgroundEffect {
        private BufferedImage extendedBackground;

        @Override
        public void updateDimensions(int width, int height) {
            super.updateDimensions(width, height);
            extendedBackground = null; // Reset the extended background when dimensions change
        }

        @Override
        public void setImage(BufferedImage image) {
            super.setImage(image);
            extendedBackground = null; // Reset the extended background when the image changes
        }

        @Override
        public void render(Graphics2D g2d) {
            if (bufferedImage == null) return;

            updateExtendedBackground();

            // Draw the extended background
            g2d.drawImage(extendedBackground, 0, 0, panelWidth, panelHeight, null);

            // Draw the actual stream image
            int x = (panelWidth - bufferedImage.getWidth()) / 2;
            int y = (panelHeight - bufferedImage.getHeight()) / 2;
            g2d.drawImage(bufferedImage, x, y, null);
        }

        private void updateExtendedBackground() {
            if (bufferedImage == null) return;

            int imageWidth = bufferedImage.getWidth();
            int imageHeight = bufferedImage.getHeight();

            // Create a new buffered image for the extended background if needed
            if (extendedBackground == null || extendedBackground.getWidth() != panelWidth || extendedBackground.getHeight() != panelHeight) {
                extendedBackground = new BufferedImage(panelWidth, panelHeight, BufferedImage.TYPE_INT_RGB);
            }

            Graphics2D g2d = extendedBackground.createGraphics();

            // Extend left and right
            for (int y = 0; y < imageHeight; y++) {
                int leftColor = bufferedImage.getRGB(0, y);
                int rightColor = bufferedImage.getRGB(imageWidth - 1, y);

                for (int x = 0; x < panelWidth; x++) {
                    if (x < (panelWidth - imageWidth) / 2) {
                        g2d.setColor(new Color(leftColor));
                        g2d.drawLine(x, y, x, y);
                    } else if (x >= (panelWidth + imageWidth) / 2) {
                        g2d.setColor(new Color(rightColor));
                        g2d.drawLine(x, y, x, y);
                    }
                }
            }

            // Extend top and bottom
            for (int x = 0; x < panelWidth; x++) {
                Color topColor, bottomColor;

                if (x < (panelWidth - imageWidth) / 2) {
                    topColor = new Color(bufferedImage.getRGB(0, 0));
                    bottomColor = new Color(bufferedImage.getRGB(0, imageHeight - 1));
                } else if (x >= (panelWidth + imageWidth) / 2) {
                    topColor = new Color(bufferedImage.getRGB(imageWidth - 1, 0));
                    bottomColor = new Color(bufferedImage.getRGB(imageWidth - 1, imageHeight - 1));
                } else {
                    int imageX = x - (panelWidth - imageWidth) / 2;
                    topColor = new Color(bufferedImage.getRGB(imageX, 0));
                    bottomColor = new Color(bufferedImage.getRGB(imageX, imageHeight - 1));
                }

                for (int y = 0; y < panelHeight; y++) {
                    if (y < (panelHeight - imageHeight) / 2) {
                        g2d.setColor(topColor);
                        g2d.drawLine(x, y, x, y);
                    } else if (y >= (panelHeight + imageHeight) / 2) {
                        g2d.setColor(bottomColor);
                        g2d.drawLine(x, y, x, y);
                    }
                }
            }

            g2d.dispose();
        }
    }
}