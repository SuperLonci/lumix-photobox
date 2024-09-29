package streamviewer;

import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.UnknownHostException;
import java.net.SocketTimeoutException;
import java.util.concurrent.CompletableFuture;
import javax.swing.SwingUtilities;

public class PhotoTaker {
    private final String cameraIp;

    public PhotoTaker(Options options) {
        this.cameraIp = options.getCameraIp();
    }

    public CompletableFuture<PhotoResult> takePhoto() {
        return CompletableFuture.supplyAsync(() -> {
            String controlUrl = "http://" + cameraIp + "/cam.cgi?mode=camcmd&value=capture";
            try {
                URL url = new URL(controlUrl);
                HttpURLConnection connection = (HttpURLConnection) url.openConnection();
                connection.setRequestMethod("GET");
                connection.setConnectTimeout(5000); // 5 seconds timeout
                connection.setReadTimeout(5000); // 5 seconds timeout

                int responseCode = connection.getResponseCode();
                connection.disconnect();

                if (responseCode == HttpURLConnection.HTTP_OK) {
                    return new PhotoResult(true, null);
                } else {
                    return new PhotoResult(false, "Failed to take photo. Response Code: " + responseCode);
                }
            } catch (UnknownHostException e) {
                return new PhotoResult(false, "No device found at IP " + cameraIp + ". Please check the IP address and ensure the device is connected.");
            } catch (SocketTimeoutException e) {
                return new PhotoResult(false, "Connection timed out. Please check if the device is responsive.");
            } catch (IOException e) {
                return new PhotoResult(false, "Error taking photo: " + e.getMessage());
            }
        });
    }

    public static class PhotoResult {
        private final boolean success;
        private final String errorMessage;

        public PhotoResult(boolean success, String errorMessage) {
            this.success = success;
            this.errorMessage = errorMessage;
        }

        public boolean isSuccess() {
            return success;
        }

        public String getErrorMessage() {
            return errorMessage;
        }
    }
}