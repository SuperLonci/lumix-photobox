package streamviewer;

import org.opencv.core.Core;
import org.opencv.core.Mat;
import org.opencv.imgproc.Imgproc;
import org.opencv.videoio.VideoCapture;
import org.opencv.videoio.Videoio;

import java.awt.image.BufferedImage;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Consumer;

public class WebcamStreamViewer implements StreamViewerInterface {

    private VideoCapture capture;
    private final AtomicBoolean running;
    private Consumer<BufferedImage> imageConsumer;
    private final int deviceIndex;

    public WebcamStreamViewer(int deviceIndex) {
        this.deviceIndex = deviceIndex;
        this.running = new AtomicBoolean(false);
    }

    @Override
    public void setImageConsumer(Consumer<BufferedImage> imageConsumer) {
        this.imageConsumer = imageConsumer;
    }

    @Override
    public void run() {
        running.set(true);
        Mat frame = new Mat();

        try {
            this.capture = new VideoCapture(deviceIndex);
            if (!capture.isOpened()) {
                throw new RuntimeException("Failed to open camera with index " + deviceIndex + ". Ensure a webcam is connected and the index is correct.");
            }

            // Set the resolution to 1280x720 (16:9 aspect ratio)
            capture.set(Videoio.CAP_PROP_FRAME_WIDTH, 1280);
            capture.set(Videoio.CAP_PROP_FRAME_HEIGHT, 720);

            while (running.get() && capture.isOpened()) {
                if (capture.read(frame)) {
                    BufferedImage image = matToBufferedImage(frame);
                    if (imageConsumer != null) {
                        imageConsumer.accept(image);
                    }
                } else {
                    throw new RuntimeException("Failed to read frame from camera. The webcam may have been disconnected.");
                }

                try {
                    Thread.sleep(33); // Approximately 30 fps
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    break;
                }
            }
        } catch (Exception e) {
            throw new RuntimeException("WebcamStreamViewer error: " + e.getMessage(), e);
        } finally {
            if (capture != null && capture.isOpened()) {
                capture.release();
            }
        }
    }

    public void stop() {
        running.set(false);
    }

    private BufferedImage matToBufferedImage(Mat mat) {
        Mat convertedMat = new Mat();
        Imgproc.cvtColor(mat, convertedMat, Imgproc.COLOR_BGR2RGB);
        int width = convertedMat.width();
        int height = convertedMat.height();
        int channels = convertedMat.channels();
        byte[] sourcePixels = new byte[width * height * channels];
        convertedMat.get(0, 0, sourcePixels);

        BufferedImage image = new BufferedImage(width, height, BufferedImage.TYPE_3BYTE_BGR);
        final byte[] targetPixels = ((java.awt.image.DataBufferByte) image.getRaster().getDataBuffer()).getData();

        for (int i = 0; i < sourcePixels.length; i += 3) {
            targetPixels[i] = sourcePixels[i + 2];     // Blue channel
            targetPixels[i + 1] = sourcePixels[i + 1]; // Green channel
            targetPixels[i + 2] = sourcePixels[i];     // Red channel
        }

        return image;
    }
}