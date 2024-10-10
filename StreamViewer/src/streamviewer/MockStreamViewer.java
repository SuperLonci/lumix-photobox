package streamviewer;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.io.InputStream;
import java.util.function.Consumer;

public class MockStreamViewer implements StreamViewerInterface {

    private Consumer<BufferedImage> imageConsumer;
    private BufferedImage mockImage;

    public MockStreamViewer(String imagePath) throws IOException {
        try (InputStream inputStream = getClass().getResourceAsStream(imagePath)) {
            if (inputStream == null) {
                throw new IOException("Unable to find resource: " + imagePath);
            }
            mockImage = ImageIO.read(inputStream);
        } catch (IOException e) {
            System.err.println("Failed to load mock image: " + e.getMessage());
            throw e; // Rethrow the exception to be handled by the caller
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