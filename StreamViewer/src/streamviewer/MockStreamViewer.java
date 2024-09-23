package streamviewer;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.util.function.Consumer;

public class MockStreamViewer implements StreamViewerInterface {

    private Consumer<BufferedImage> imageConsumer;
    private BufferedImage mockImage;

    public MockStreamViewer(String imagePath) {
        try {
            mockImage = ImageIO.read(getClass().getResourceAsStream(imagePath));
        } catch (IOException e) {
            System.err.println("Failed to load mock image: " + e.getMessage());
            e.printStackTrace();
        }
    }

    @Override
    public void setImageConsumer(Consumer<BufferedImage> imageConsumer) {
        this.imageConsumer = imageConsumer;
    }

    @Override
    public void run() {
        while (!Thread.interrupted()) {
            if (mockImage != null && imageConsumer != null) {
                imageConsumer.accept(mockImage);
            }
            try {
                Thread.sleep(33); // Simulate 30 FPS
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }
    }
}