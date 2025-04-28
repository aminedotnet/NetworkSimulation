package controllers;

import models.*;
import java.io.*;
import java.util.*;
import java.util.stream.Collectors;

public class NetworkController {
    private final List<Device> devices;
    private final List<Connection> connections;
    private static final int VERSION = 1;
    private static final int MAX_RECONNECTION_ATTEMPTS = 3;

    public NetworkController() {
        this.devices = Collections.synchronizedList(new ArrayList<>());
        this.connections = Collections.synchronizedList(new ArrayList<>());
    }

    // Device Management
    public void addDevice(Device device) throws IllegalArgumentException {
        Objects.requireNonNull(device, "Device cannot be null");
        
        synchronized(devices) {
            if (devices.stream().anyMatch(d -> d.getName().equalsIgnoreCase(device.getName()))) {
                throw new IllegalArgumentException(
                    String.format("Device name '%s' already exists", device.getName()));
            }
            devices.add(device);
        }
    }

    public void removeDevice(Device device) {
        Objects.requireNonNull(device, "Device cannot be null");
        
        synchronized(devices) {
            if (devices.remove(device)) {
                synchronized(connections) {
                    // Disconnect all connections for this device
                    new ArrayList<>(connections).stream()
                        .filter(conn -> conn.involvesDevice(device))
                        .forEach(conn -> disconnectDevices(conn.getDevice1(), conn.getDevice2()));
                }
            }
        }
    }

    public synchronized void connectDevices(Device device1, Device device2) throws NetworkException {
        Objects.requireNonNull(device1, "First device cannot be null");
        Objects.requireNonNull(device2, "Second device cannot be null");
        
        // Validate basic connection rules
        if (device1.equals(device2)) {
            throw new NetworkException("Cannot connect a device to itself");
        }
        
        if (isConnected(device1, device2)) {
            throw new NetworkException("These devices are already connected");
        }
        
        // Let the devices themselves validate if they can connect
        try {
            device1.connectTo(device2);
            device2.connectTo(device1);
            connections.add(new Connection(device1, device2));
        } catch (IllegalStateException e) {
            // Rollback if either connection fails
            device1.disconnectFrom(device2);
            device2.disconnectFrom(device1);
            throw new NetworkException(e.getMessage());
        }
    }

    public void disconnectDevices(Device device1, Device device2) {
        Objects.requireNonNull(device1, "First device cannot be null");
        Objects.requireNonNull(device2, "Second device cannot be null");
        
        synchronized(connections) {
            Connection connection = findConnection(device1, device2);
            if (connection != null) {
                device1.disconnectFrom(device2);
                device2.disconnectFrom(device1);
                connections.remove(connection);
            }
        }
    }

    private void validateConnection(Device device1, Device device2) throws NetworkException {
        if (device1.equals(device2)) {
            throw new NetworkException("Cannot connect a device to itself");
        }
        
        if (isConnected(device1, device2)) {
            throw new NetworkException("These devices are already connected");
        }
        
        if (!device1.canConnectTo(device2) || !device2.canConnectTo(device1)) {
            throw new NetworkException("Connection not allowed between these devices");
        }
    }

    // File Operations
    public void saveToFile(String path) throws IOException {
        synchronized(devices) {
            synchronized(connections) {
                try (ObjectOutputStream oos = new ObjectOutputStream(new FileOutputStream(path))) {
                    oos.writeInt(VERSION);
                    oos.writeObject(new ArrayList<>(devices));
                    oos.writeObject(new ArrayList<>(connections));
                }
            }
        }
    }

    @SuppressWarnings("unchecked")
    public void loadFromFile(String path) throws IOException, ClassNotFoundException, NetworkException {
        synchronized(devices) {
            synchronized(connections) {
                try (ObjectInputStream ois = new ObjectInputStream(new FileInputStream(path))) {
                    int version = ois.readInt();
                    if (version != VERSION) {
                        throw new IOException("Unsupported file version: " + version);
                    }
                    
                    List<Device> loadedDevices = (List<Device>) ois.readObject();
                    List<Connection> loadedConnections = (List<Connection>) ois.readObject();
                    
                    // Validate loaded data
                    validateLoadedData(loadedDevices, loadedConnections);
                    
                    devices.clear();
                    connections.clear();
                    
                    devices.addAll(loadedDevices);
                    connections.addAll(loadedConnections);
                }
            }
        }
    }

    private void validateLoadedData(List<Device> devices, List<Connection> connections) throws NetworkException {
        // Check for duplicate device names
        Set<String> names = new HashSet<>();
        for (Device device : devices) {
            if (!names.add(device.getName())) {
                throw new NetworkException("Duplicate device name found: " + device.getName());
            }
        }
        
        // Validate connections
        for (Connection conn : connections) {
            if (!devices.contains(conn.getDevice1()) || !devices.contains(conn.getDevice2())) {
                throw new NetworkException("Connection references missing device");
            }
        }
    }

    // Helper Methods
    public boolean isConnected(Device d1, Device d2) {
        synchronized(connections) {
            return connections.stream()
                .anyMatch(conn -> conn.involvesDevice(d1) && conn.involvesDevice(d2));
        }
    }

    private Connection findConnection(Device d1, Device d2) {
        synchronized(connections) {
            return connections.stream()
                .filter(conn -> conn.involvesDevice(d1) && conn.involvesDevice(d2))
                .findFirst()
                .orElse(null);
        }
    }

    public Device getDeviceByName(String name) {
        synchronized(devices) {
            return devices.stream()
                .filter(d -> d.getName().equalsIgnoreCase(name))
                .findFirst()
                .orElse(null);
        }
    }

    public synchronized boolean isDeviceNameUnique(String name, Device excludeDevice) {
        return devices.stream()
            .filter(d -> !d.equals(excludeDevice))
            .noneMatch(d -> d.getName().equalsIgnoreCase(name));
    }

    public synchronized void updateDevice(Device device, String newName, String newIp) {
        device.setName(newName);
        device.setIpAddress(newIp);
    }
    
    // Getters with defensive copies
    public List<Device> getDevices() {
        synchronized(devices) {
            return new ArrayList<>(devices);
        }
    }

    public List<Connection> getConnections() {
        synchronized(connections) {
            return new ArrayList<>(connections);
        }
    }

    
    public Serializable getNetworkData() {
        Map<String, Object> data = new HashMap<>();
        synchronized(devices) {
            data.put("devices", new ArrayList<>(devices));
        }
        synchronized(connections) {
            data.put("connections", new ArrayList<>(connections));
        }
        return (Serializable) data;
    }

    public void loadNetworkData(Object data) {
        @SuppressWarnings("unchecked")
        Map<String, Object> networkData = (Map<String, Object>) data;
        
        // Clear current network
        synchronized(devices) {
            devices.clear();
            @SuppressWarnings("unchecked")
            List<Device> loadedDevices = (List<Device>) networkData.get("devices");
            if (loadedDevices != null) {
                devices.addAll(loadedDevices);
            }
        }
        
        synchronized(connections) {
            connections.clear();
            @SuppressWarnings("unchecked")
            List<Connection> loadedConnections = (List<Connection>) networkData.get("connections");
            if (loadedConnections != null) {
                connections.addAll(loadedConnections);
            }
        }
    }

    // Custom exception for network operations
    public static class NetworkException extends Exception {
        public NetworkException(String message) {
            super(message);
        }
        
        public NetworkException(String message, Throwable cause) {
            super(message, cause);
        }
    }
}