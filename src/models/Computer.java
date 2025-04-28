// Computer.java
package models;

import java.io.Serializable;

public class Computer extends Device implements Serializable{
    private Device connectedDevice;

    public Computer(String name, String ipAddress) {
        super(name, ipAddress);
    }

    @Override
    public boolean canConnectTo(Device other) {
        // Can connect to one switch or one computer
        return connectedDevice == null;
    }

    @Override
    public void connectTo(Device other) throws IllegalStateException {
        if (!canConnectTo(other)) {
            throw new IllegalStateException("Computer already connected to another device");
        }
        connectedDevice = other;
    }

    @Override
    public void disconnectFrom(Device other) {
        if (connectedDevice != null && connectedDevice.equals(other)) {
            connectedDevice = null;
        }
    }

    @Override
    public boolean isConnectedTo(Device other) {
        return connectedDevice != null && connectedDevice.equals(other);
    }
    
 // In your Device class (or base class)
    @Override
    public String toString() {
        return getName(); // Or any other string representation you prefer
    }

    public Device getConnectedDevice() {
        return connectedDevice;
    }
}