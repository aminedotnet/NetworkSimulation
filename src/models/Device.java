// Device.java (Abstract base class)
package models;

import java.io.Serializable;

public abstract class Device implements Serializable {
    protected String name;
    protected String ipAddress;

    public Device(String name, String ipAddress) {
        this.name = name;
        this.ipAddress = ipAddress;
    }

    // Getters and setters
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public String getIpAddress() { return ipAddress; }
    public void setIpAddress(String ipAddress) { this.ipAddress = ipAddress; }

    // Abstract methods
    public abstract boolean canConnectTo(Device other);
    public abstract void connectTo(Device other) throws IllegalStateException;
    public abstract void disconnectFrom(Device other);
    public abstract boolean isConnectedTo(Device other);
}