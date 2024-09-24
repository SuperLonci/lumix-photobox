package streamviewer;

import javax.swing.*;
import java.awt.*;

// Custom RoundButton class
public class RedRoundButton extends JButton {
    public RedRoundButton(String label) {
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