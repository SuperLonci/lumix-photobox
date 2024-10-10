package streamviewer;

public interface CameraStateUpdateListener {
    void onCameraStateUpdate(String batteryStatus, String sdCardStatus, String errorMessage, boolean lowBattery, boolean noSdCard);
}