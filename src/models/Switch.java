// Switch.java
package models;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

public class Switch extends Device implements Serializable{
    private final List<Device> connectedDevices;
    private static final int MAX_PORTS = 8;

    public Switch(String name, String ipAddress) {
        super(name, ipAddress);
        this.connectedDevices = new ArrayList<>();
    }

    @Override
    public boolean canConnectTo(Device other) {
        return connectedDevices.size() < MAX_PORTS;
    }

    @Override
    public void connectTo(Device other) throws IllegalStateException {
        if (!canConnectTo(other)) {
            throw new IllegalStateException("Switch has no available ports");
        }
        connectedDevices.add(other);
    }

    @Override
    public void disconnectFrom(Device other) {
        connectedDevices.remove(other);
    }

    
    @Override
    public String toString() {
        return getName(); // Or any other string representation you prefer
    }

    @Override
    public boolean isConnectedTo(Device other) {
        return connectedDevices.contains(other);
    }

    public List<Device> getConnectedDevices() {
        return new ArrayList<>(connectedDevices);
    }

    public int getAvailablePorts() {
        return MAX_PORTS - connectedDevices.size();
    }
}