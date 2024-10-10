package streamviewer;

import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import javax.swing.SwingUtilities;

public class MockCameraStateMonitor extends CameraStateMonitor {
    private final ScheduledExecutorService scheduler;
    private final CameraStateUpdateListener updateListener;
    private boolean isRunning = false;

    public MockCameraStateMonitor(Options options, CameraStateUpdateListener updateListener) {
        super(options, updateListener);
        this.updateListener = updateListener;
        this.scheduler = Executors.newScheduledThreadPool(1);
    }

    @Override
    public void start() {
        if (!isRunning) {
            isRunning = true;
            scheduler.scheduleAtFixedRate(this::updateMockState, 0, 10, TimeUnit.SECONDS);
        }
    }

    @Override
    public void stop() {
        if (isRunning) {
            isRunning = false;
            scheduler.shutdown();
        }
    }

    private void updateMockState() {
        String batteryStatus = "0/3";
        String sdCardStatus = "write_enable";
        String remainingImages = "12";
        String errorMessage = "This is a mock";
        boolean lowBattery = false;
        boolean noSdCard = false;

        SwingUtilities.invokeLater(() -> {
            updateListener.onCameraStateUpdate(batteryStatus, sdCardStatus, remainingImages, errorMessage, lowBattery, noSdCard);
        });
    }
}