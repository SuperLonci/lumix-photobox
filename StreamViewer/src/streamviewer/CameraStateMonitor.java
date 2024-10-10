package streamviewer;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import javax.swing.SwingUtilities;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;

public class CameraStateMonitor {
    private final String cameraIp;
    private final ScheduledExecutorService scheduler;
    private final CameraStateUpdateListener updateListener;

    private String batteryStatus = "Unknown";
    private String sdCardStatus = "Unknown";
    private String remainingImages = "Unknown";
    private String errorMessage = null;

    private boolean isRunning = false;

    public CameraStateMonitor(Options options, CameraStateUpdateListener updateListener) {
        this.cameraIp = options.getCameraIp();
        this.updateListener = updateListener;
        this.scheduler = Executors.newScheduledThreadPool(1);
    }

    public void start() {
        if (!isRunning) {
            isRunning = true;
            scheduler.scheduleAtFixedRate(this::checkCameraState, 0, 10, TimeUnit.SECONDS);
        }
    }

    public void stop() {
        if (isRunning) {
            isRunning = false;
            scheduler.shutdown();
        }
    }

    private void checkCameraState() {
        try {
            String response = sendRequest();
            parseResponse(response);
            errorMessage = null;
        } catch (Exception e) {
            errorMessage = "Error connecting to camera: " + e.getMessage();
        }
        SwingUtilities.invokeLater(() -> {
            updateListener.onCameraStateUpdate(batteryStatus, sdCardStatus, remainingImages, errorMessage, isLowBattery(), isNoSdCard());
        });
    }

    private String sendRequest() throws Exception {
        URL url = new URL("http://" + cameraIp + "/cam.cgi?mode=getstate");
        HttpURLConnection connection = (HttpURLConnection) url.openConnection();
        connection.setRequestMethod("GET");
        connection.setConnectTimeout(5000);
        connection.setReadTimeout(5000);

        try (BufferedReader in = new BufferedReader(new InputStreamReader(connection.getInputStream()))) {
            StringBuilder response = new StringBuilder();
            String inputLine;
            while ((inputLine = in.readLine()) != null) {
                response.append(inputLine);
            }
            return response.toString();
        }
    }

    private void parseResponse(String response) throws Exception {
        DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
        DocumentBuilder builder = factory.newDocumentBuilder();
        Document doc = builder.parse(new ByteArrayInputStream(response.getBytes(StandardCharsets.UTF_8)));

        Element stateElement = (Element) doc.getElementsByTagName("state").item(0);

        NodeList battElements = stateElement.getElementsByTagName("batt");
        if (battElements.getLength() > 0) {
            batteryStatus = battElements.item(0).getTextContent();
        }

        NodeList sdCardElements = stateElement.getElementsByTagName("sdcardstatus");
        if (sdCardElements.getLength() > 0) {
            sdCardStatus = sdCardElements.item(0).getTextContent();
        }

        NodeList remainCapacityElements = stateElement.getElementsByTagName("remaincapacity");
        if (remainCapacityElements.getLength() > 0) {
            remainingImages = remainCapacityElements.item(0).getTextContent();
        }

        errorMessage = null;
    }

    public boolean isLowBattery() {
        return "0/3".equals(batteryStatus);
    }

    public boolean isNoSdCard() {
        return !"write_enable".equals(sdCardStatus);
    }
}