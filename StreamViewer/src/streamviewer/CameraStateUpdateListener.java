package streamviewer;

public interface CameraStateUpdateListener {
    void onCameraStateUpdate(String batteryStatus, String sdCardStatus, String remainingImages, String errorMessage, boolean lowBattery, boolean noSdCard);
}