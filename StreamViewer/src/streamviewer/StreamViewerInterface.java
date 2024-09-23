package streamviewer;

import java.util.function.Consumer;
import java.awt.image.BufferedImage;

public interface StreamViewerInterface extends Runnable {
    void setImageConsumer(Consumer<BufferedImage> imageConsumer);
}
