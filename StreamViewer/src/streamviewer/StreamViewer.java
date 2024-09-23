package streamviewer;

import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.SocketException;
import java.net.UnknownHostException;
import java.util.Arrays;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.function.Consumer;

import javax.imageio.ImageIO;

/**
 * Reads the camera video stream.
 *
 * The camera sends a continuous stream of UDP packets to whoever called its "startstream" method
 */
public class StreamViewer implements StreamViewerInterface {

// debugigng
    private long packetCount = 0;
    private long lastLogTime = System.currentTimeMillis();

    private Consumer<BufferedImage> imageConsumer;

    /**
     * The local UDP socket for receiving the video stream.
     */
    private final DatagramSocket localUdpSocket;

    /**
     * The UDP port to listen on.
     */
    private final int localUdpPort;

    /**
     * IP address of the local network interface communicating with the camera.
     */
    private final InetAddress cameraIp;

    private ExecutorService imageExecutor = Executors.newCachedThreadPool();

    @Override
    public void setImageConsumer(Consumer<BufferedImage> imageConsumer) {
        this.imageConsumer = imageConsumer;
    }

    /**
     * Create the Lumix videostream reader connected to the default UDP port 49199.
     *
     * @param imageConsumer the consumer to receive the BufferedImages received from the camera
     * @param cameraIp IPv4 address of the camera.
     * @param cameraNetmaskBitSize Size of the camera network's subnet.
     * @throws UnknownHostException If the camera IP address cannot be parsed.
     * @throws SocketException On network communication errors.
     */
    public StreamViewer(Consumer<BufferedImage> imageConsumer, String cameraIp, int cameraNetmaskBitSize)
            throws UnknownHostException, SocketException {
        this(imageConsumer, cameraIp, cameraNetmaskBitSize, 49199);
    }

    /**
     * Create the Lumix videostream reader.
     *
     * @param imageConsumer the consumer to receive the BufferedImages received from the camera
     * @param cameraIp IPv4 address of the camera.
     * @param cameraNetmaskBitSize Size of the camera network's subnet.
     * @param udpPort The UDP port to listen on.
     * @throws UnknownHostException If the camera IP address cannot be parsed.
     * @throws SocketException On network communication errors.
     */
    public StreamViewer(Consumer<BufferedImage> imageConsumer, String cameraIp, int cameraNetmaskBitSize, int udpPort)
            throws UnknownHostException, SocketException {
        this.imageConsumer = imageConsumer;
        this.cameraIp = NetUtil.findLocalIpInSubnet(cameraIp, cameraNetmaskBitSize);

        this.localUdpPort = udpPort;
        this.localUdpSocket = new DatagramSocket(this.localUdpPort);

        System.out.println("UDP Socket on " + this.cameraIp.getHostAddress() + ":" + this.localUdpPort
                + " created");
    }

    private BufferedImage retrieveImage(DatagramPacket receivedPacket) {
        final byte[] videoData = getImageData(receivedPacket);

        BufferedImage img = null;
        try {
            img = ImageIO.read(new ByteArrayInputStream(videoData));
            if (img == null) {
                System.out.println("ImageIO.read returned null. Data length: " + videoData.length);
            }
        } catch (IOException e) {
            System.err.println("Error while reading image data: " + e.getMessage());
            e.printStackTrace();
        }

        return img;
    }

    /**
     * The camera sends one JPEG image in each UDP packet.
     *
     * @param receivedPacket a received camera image packet
     * @return the jpeg image data
     */
    private byte[] getImageData(DatagramPacket receivedPacket) {
        final byte[] udpData = receivedPacket.getData();
        // The camera adds some kind of header to each packet, which we need to ignore
        int videoDataStart = getImageDataStart(receivedPacket, udpData);
        return Arrays.copyOfRange(udpData, videoDataStart, receivedPacket.getLength());
    }

    private int getImageDataStart(DatagramPacket receivedPacket, byte[] udpData) {
        int videoDataStart = 130;

        // The image data starts somewhere after the first 130 bytes, but at last in 320 bytes
        for (int k = 130; k < 320 && k < (receivedPacket.getLength() - 1); k++) {
            // The bytes FF and D8 signify the start of the jpeg data, see https://en.wikipedia.org/wiki/JPEG_File_Interchange_Format
            if ((udpData[k] == (byte) 0xFF) && (udpData[(k + 1)] == (byte) 0xD8)) {
                videoDataStart = k;
            }
        }

        return videoDataStart;
    }

    @Override
    public void run() {
        byte[] udpPacketBuffer = new byte[35000];

        while (!Thread.interrupted()) {
            try {
                final DatagramPacket receivedPacket = new DatagramPacket(udpPacketBuffer, udpPacketBuffer.length,
                        cameraIp, localUdpPort);

                localUdpSocket.receive(receivedPacket);

                packetCount++;
                long currentTime = System.currentTimeMillis();
                if (currentTime - lastLogTime > 5000) { // Log every 5 seconds
                    System.out.println("Received " + packetCount + " packets in the last 5 seconds");
                    packetCount = 0;
                    lastLogTime = currentTime;
                }

                imageExecutor.submit(() -> {
                    BufferedImage newImage = retrieveImage(receivedPacket);
                    if (newImage != null) {
                        imageConsumer.accept(newImage);
                    } else {
                        System.out.println("Failed to retrieve image from packet");
                    }
                });

            } catch (IOException e) {
                System.out.println("Error with client request : " + e.getMessage());
            }
        }

        imageExecutor.shutdown();
        localUdpSocket.close();
    }

}
