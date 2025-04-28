package models;

import java.io.Serializable;

/**
 * Represents a connection between two devices in the network
 */
public class Connection implements Serializable{
	private static final long serialVersionUID = 1L;
    private final Device device1;
    private final Device device2;

    public Connection(Device device1, Device device2) {
        if (device1 == null || device2 == null) {
            throw new IllegalArgumentException("Devices cannot be null");
        }
        this.device1 = device1;
        this.device2 = device2;
    }

    public Device getDevice1() {
        return device1;
    }

    public Device getDevice2() {
        return device2;
    }

    /**
     * Checks if this connection involves the given device
     */
    public boolean involvesDevice(Device device) {
        return device1.equals(device) || device2.equals(device);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Connection that = (Connection) o;
        return (device1.equals(that.device1) && device2.equals(that.device2)) ||
               (device1.equals(that.device2) && device2.equals(that.device1));
    }

    @Override
    public int hashCode() {
        return device1.hashCode() + device2.hashCode(); // Order doesn't matter
    }

    @Override
    public String toString() {
        return "Connection{" +
                device1.getName() + " (" + device1.getClass().getSimpleName() + ")" +
                " <-> " +
                device2.getName() + " (" + device2.getClass().getSimpleName() + ")" +
                '}';
    }
}