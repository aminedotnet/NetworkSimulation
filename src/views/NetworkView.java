package views;

import models.*;
import controllers.*;

import javax.imageio.ImageIO;
import javax.swing.*;
import javax.swing.filechooser.FileNameExtensionFilter;

import java.awt.*;
import java.awt.event.*;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.HashMap;
import java.util.Map;
import java.util.List;
import java.util.stream.Collectors;

public class NetworkView extends JFrame {
	private Image pcImage;
	private Image switchImage;
	private JTextArea deviceInfoArea;
    private final NetworkController controller;
    private JPanel canvas;  // Removed final modifier
    private Device selectedDevice = null;
    private final Map<Device, Point> devicePositions = new HashMap<>();
    private Point dragStartPoint;
    private JLabel statusBar;  // Removed final modifier

    public NetworkView(NetworkController controller) {
        this.controller = controller;
        
        try {
            // Try loading from resources first
            pcImage = ImageIO.read(getClass().getResource("/images/pc.png"));
            switchImage = ImageIO.read(getClass().getResource("/images/switch.png"));
            
            // If not found in resources, try file system
            if (pcImage == null) pcImage = ImageIO.read(new File("images/pc.png"));
            if (switchImage == null) switchImage = ImageIO.read(new File("images/switch.png"));
            
            // Scale images if loaded successfully
            if (pcImage != null) pcImage = pcImage.getScaledInstance(50, 50, Image.SCALE_SMOOTH);
            if (switchImage != null) switchImage = switchImage.getScaledInstance(50, 50, Image.SCALE_SMOOTH);
        } catch (Exception e) {
            System.err.println("Warning: Could not load device images: " + e.getMessage());
            pcImage = null;
            switchImage = null;
        }
        
        this.canvas = new JPanel() {
            @Override
            protected void paintComponent(Graphics g) {
                super.paintComponent(g);
                drawNetwork(g);
            }
        };
        this.statusBar = new JLabel(" Ready");
        initializeUI();
    }

    private void initializeUI() {
        setTitle("Network Simulator");
        setSize(1000, 700);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLayout(new BorderLayout());
        // Create device info panel
        JPanel infoPanel = new JPanel(new BorderLayout());
        infoPanel.setBorder(BorderFactory.createTitledBorder("Device Details"));
        deviceInfoArea = new JTextArea(10, 20);
        deviceInfoArea.setEditable(false);
        deviceInfoArea.setFont(new Font("Monospaced", Font.PLAIN, 12));
        infoPanel.add(new JScrollPane(deviceInfoArea), BorderLayout.CENTER);
        // Add to frame (let's put it on the EAST side)
        add(infoPanel, BorderLayout.EAST);
        // Toolbar
        JToolBar toolBar = new JToolBar();
        JButton addComputerBtn = new JButton("Add Computer");
        JButton addSwitchBtn = new JButton("Add Switch");
        JButton saveBtn = new JButton("Save");
        JButton loadBtn = new JButton("Load");

        toolBar.add(addComputerBtn);
        toolBar.add(addSwitchBtn);
        toolBar.add(saveBtn);
        toolBar.add(loadBtn);
        add(toolBar, BorderLayout.NORTH);

        // Canvas setup
        canvas.setBackground(Color.WHITE);
        canvas.setFocusable(true);

        // Mouse listeners
        canvas.addMouseListener(new MouseAdapter() {
            @Override
            public void mousePressed(MouseEvent e) {
                if (SwingUtilities.isRightMouseButton(e)) {
                    selectDeviceAtPoint(e.getPoint());
                } else {
                    selectDeviceAtPoint(e.getPoint());
                    dragStartPoint = e.getPoint();
                }
            }
        });

        canvas.addMouseMotionListener(new MouseAdapter() {
            @Override
            public void mouseDragged(MouseEvent e) {
                if (selectedDevice != null && !SwingUtilities.isRightMouseButton(e)) {
                    Point newPos = e.getPoint();
                    int dx = newPos.x - dragStartPoint.x;
                    int dy = newPos.y - dragStartPoint.y;
                    Point currentPos = devicePositions.get(selectedDevice);
                    devicePositions.put(selectedDevice, new Point(currentPos.x + dx, currentPos.y + dy));
                    dragStartPoint = newPos;
                    canvas.repaint();
                }
            }
        });

        // Initialize context menu
        initContextMenu();

        add(new JScrollPane(canvas), BorderLayout.CENTER);
        add(statusBar, BorderLayout.SOUTH);

        // Button actions
        addComputerBtn.addActionListener(e -> addDevice("Computer"));
        addSwitchBtn.addActionListener(e -> addDevice("Switch"));
        saveBtn.addActionListener(e -> saveNetwork());
        loadBtn.addActionListener(e -> loadNetwork());
    }

 // Update the drawNetwork method
    private void drawNetwork(Graphics g) {
        Graphics2D g2d = (Graphics2D) g;
        g2d.setStroke(new BasicStroke(2));
        
        // Draw connections
        for (Connection conn : controller.getConnections()) {
            Point p1 = devicePositions.get(conn.getDevice1());
            Point p2 = devicePositions.get(conn.getDevice2());
            if (p1 != null && p2 != null) {
                g2d.setColor(Color.BLUE);
                g2d.drawLine(p1.x + 25, p1.y + 25, p2.x + 25, p2.y + 25);
            }
        }
        
        // Draw devices
        for (Device device : controller.getDevices()) {
            Point pos = devicePositions.get(device);
            if (pos == null) continue;
            
            // Highlight selected device
            if (device == selectedDevice) {
                // Create a semi-transparent gray color (RGB: 200,200,200 with 50% transparency)
                g2d.setColor(new Color(200, 200, 200, 128));
                g2d.fillOval(pos.x - 5, pos.y - 5, 60, 60);
                
                // Optional: Add a subtle border
//                g2d.setColor(new Color(150, 150, 150));
//                g2d.drawOval(pos.x - 5, pos.y - 5, 60, 60);
            }
            
            if (device instanceof Switch) {
                if (switchImage != null) {
                    g2d.drawImage(switchImage, pos.x, pos.y, this);
                } else {
                    // Fallback to rectangle
                    g2d.setColor(Color.RED);
                    g2d.fillRect(pos.x, pos.y, 50, 50);
                    g2d.setColor(Color.WHITE);
                    g2d.drawString("SW", pos.x + 15, pos.y + 25);
                }
                
                // Draw device info
                Switch sw = (Switch) device;
                g2d.setColor(Color.BLACK);
                g2d.drawString(sw.getName(), pos.x, pos.y + 70);
                g2d.drawString("Conn: " + sw.getConnectedDevices().size(), pos.x, pos.y + 85);
            } else {
                if (pcImage != null) {
                    g2d.drawImage(pcImage, pos.x, pos.y, this);
                } else {
                    // Fallback to rectangle
                    g2d.setColor(Color.BLUE);
                    g2d.fillRect(pos.x, pos.y, 50, 50);
                    g2d.setColor(Color.WHITE);
                    g2d.drawString("PC", pos.x + 15, pos.y + 25);
                }
                
                // Draw device info
                g2d.setColor(Color.BLACK);
                g2d.drawString(device.getName(), pos.x, pos.y + 70);
            }
        }
    }

    private void initContextMenu() {
        JPopupMenu contextMenu = new JPopupMenu();
        
        JMenuItem connectItem = new JMenuItem("Connect to...");
        JMenuItem disconnectItem = new JMenuItem("Disconnect");
        JMenuItem updateItem = new JMenuItem("Update");
        JMenuItem deleteItem = new JMenuItem("Delete");
        
        connectItem.addActionListener(e -> showConnectionDialog());
        disconnectItem.addActionListener(e -> disconnectSelected());
        updateItem.addActionListener(e -> editSelectedDevice()); // Add this line
        deleteItem.addActionListener(e -> deleteSelectedDevice());
        
        contextMenu.add(connectItem);
        contextMenu.add(disconnectItem);
        contextMenu.addSeparator();
        contextMenu.add(updateItem); // Add this line
        contextMenu.add(deleteItem);
        
        canvas.setComponentPopupMenu(contextMenu);
    }

    private void selectDeviceAtPoint(Point point) {
        Device previouslySelected = selectedDevice;
        selectedDevice = null;
        
        for (Device device : controller.getDevices()) {
            Point pos = devicePositions.get(device);
            if (pos == null) continue;

            Rectangle bounds = new Rectangle(pos.x, pos.y, 50, 50);
            if (bounds.contains(point)) {
                selectedDevice = device;
                break;
            }
        }
        
        if (selectedDevice != previouslySelected) {
            updateDeviceInfoDisplay();
            canvas.repaint();
            statusBar.setText(" " + (selectedDevice != null ? 
                "Selected: " + selectedDevice.getName() : "No device selected"));
        }
    }


    private void updateDeviceInfoDisplay() {
        if (selectedDevice == null) {
            deviceInfoArea.setText("");
            return;
        }
        
        StringBuilder info = new StringBuilder();
        info.append("Type: ").append(selectedDevice.getClass().getSimpleName()).append("\n");
        info.append("Name: ").append(selectedDevice.getName()).append("\n");
        info.append("IP: ").append(selectedDevice.getIpAddress()).append("\n");
        
        if (selectedDevice instanceof Switch) {
            Switch sw = (Switch) selectedDevice;
            info.append("Connections: ").append(sw.getConnectedDevices().size()).append("\n");
            info.append("Available ports: ").append(sw.getAvailablePorts()).append("\n");
        } else if (selectedDevice instanceof Computer) {
            Computer pc = (Computer) selectedDevice;
            info.append("Connected to: ")
                .append(pc.getConnectedDevice() != null ? 
                       pc.getConnectedDevice().getName() : "None")
                .append("\n");
        }
        
        deviceInfoArea.setText(info.toString());
    }
    
    private void addDevice(String type) {
        String defaultName = type + " " + (controller.getDevices().size() + 1);
        String name = JOptionPane.showInputDialog(this, 
            "Enter " + type + " name:", 
            defaultName);
        
        if (name == null || name.trim().isEmpty()) return;

        String ip = JOptionPane.showInputDialog(this, 
            "Enter IP address:", 
            "192.168.1." + (controller.getDevices().size() + 1));
        
        if (ip == null || ip.trim().isEmpty()) return;

        if (!ip.matches("^\\d{1,3}\\.\\d{1,3}\\.\\d{1,3}\\.\\d{1,3}$")) {
            JOptionPane.showMessageDialog(this, 
                "Please enter a valid IP address (e.g., 192.168.1.1)",
                "Invalid Input", 
                JOptionPane.ERROR_MESSAGE);
            return;
        }

        Device device = type.equals("Computer") ? 
            new Computer(name, ip) : new Switch(name, ip);

        try {
            controller.addDevice(device);
            devicePositions.put(device, new Point(
                (int)(Math.random() * (canvas.getWidth() - 100)),
                (int)(Math.random() * (canvas.getHeight() - 100))
            ));
            canvas.repaint();
            statusBar.setText(" Added " + type + ": " + name);
        } catch (Exception e) {
            JOptionPane.showMessageDialog(this, 
                e.getMessage(),
                "Add Device Error", 
                JOptionPane.ERROR_MESSAGE);
        }
    }

    private void deleteSelectedDevice() {
        if (selectedDevice == null) {
            JOptionPane.showMessageDialog(this, 
                "Please select a device first",
                "No Selection", 
                JOptionPane.WARNING_MESSAGE);
            return;
        }

        int confirm = JOptionPane.showConfirmDialog(
            this,
            "Delete " + selectedDevice.getName() + "?",
            "Confirm Delete",
            JOptionPane.YES_NO_OPTION);
            
        if (confirm == JOptionPane.YES_OPTION) {
            String deviceName = selectedDevice.getName();
            controller.removeDevice(selectedDevice);
            devicePositions.remove(selectedDevice);
            selectedDevice = null;
            canvas.repaint();
            statusBar.setText(" Deleted device: " + deviceName);
            updateDeviceInfoDisplay();
        }
    }
    
    private void showConnectionDialog() {
        if (selectedDevice == null) {
            JOptionPane.showMessageDialog(this, 
                "Please select a device first", 
                "No Device Selected", 
                JOptionPane.WARNING_MESSAGE);
            return;
        }
        
        List<Device> availableDevices = controller.getDevices().stream()
            .filter(d -> !d.equals(selectedDevice))
            .filter(d -> !controller.isConnected(selectedDevice, d))
            .filter(d -> {
                try {
                    return selectedDevice.canConnectTo(d) && d.canConnectTo(selectedDevice);
                } catch (Exception e) {
                    return false;
                }
            })
            .sorted((d1, d2) -> {
                // Sort switches first, then computers
                if (d1 instanceof Switch && d2 instanceof Computer) return -1;
                if (d1 instanceof Computer && d2 instanceof Switch) return 1;
                return d1.getName().compareTo(d2.getName());
            })
            .collect(Collectors.toList());
        
        if (availableDevices.isEmpty()) {
            JOptionPane.showMessageDialog(this, 
                "No valid devices to connect to", 
                "No Available Devices", 
                JOptionPane.INFORMATION_MESSAGE);
            return;
        }
        
        Device target = (Device) JOptionPane.showInputDialog(
            this,
            "Connect " + selectedDevice.getName() + " to:",
            "Connect Devices",
            JOptionPane.QUESTION_MESSAGE,
            null,
            availableDevices.toArray(),
            availableDevices.get(0));
        
        if (target != null) {
            try {
                controller.connectDevices(selectedDevice, target);
                canvas.repaint();
                updateDeviceInfoDisplay();
                statusBar.setText("Connected " + selectedDevice.getName() + " to " + target.getName());
            } catch (Exception ex) {
                JOptionPane.showMessageDialog(this, 
                    ex.getMessage(),
                    "Connection Error", 
                    JOptionPane.ERROR_MESSAGE);
            }
        }
    }

    private void disconnectSelected() {
        if (selectedDevice == null) {
            JOptionPane.showMessageDialog(this, 
                "Please select a device first", 
                "No Device Selected", 
                JOptionPane.WARNING_MESSAGE);
            return;
        }
        
        if (selectedDevice instanceof Switch) {
            disconnectSwitch((Switch) selectedDevice);
        } else {
            disconnectComputer((Computer) selectedDevice);
        }
        updateDeviceInfoDisplay();
    }

    private void disconnectSwitch(Switch switchDevice) {
        List<Device> connectedDevices = switchDevice.getConnectedDevices();
        
        if (connectedDevices.isEmpty()) {
            JOptionPane.showMessageDialog(this, 
                "Switch is not connected to any devices", 
                "No Connections", 
                JOptionPane.INFORMATION_MESSAGE);
            return;
        }
        
        Device target = (Device) JOptionPane.showInputDialog(
            this,
            "Disconnect " + switchDevice.getName() + " from:",
            "Disconnect Devices",
            JOptionPane.QUESTION_MESSAGE,
            null,
            connectedDevices.toArray(),
            connectedDevices.get(0));
        
        if (target != null) {
            int confirm = JOptionPane.showConfirmDialog(
                this,
                "Disconnect " + switchDevice.getName() + " from " + target.getName() + "?",
                "Confirm Disconnection",
                JOptionPane.YES_NO_OPTION);
                
            if (confirm == JOptionPane.YES_OPTION) {
                controller.disconnectDevices(switchDevice, target);
                canvas.repaint();
                statusBar.setText("Disconnected " + switchDevice.getName() + " from " + target.getName());
            }
        }
    }

    private void disconnectComputer(Computer computer) {
        if (computer.getConnectedDevice() == null) {
            JOptionPane.showMessageDialog(this, 
                "Computer is not connected to any device", 
                "Not Connected", 
                JOptionPane.INFORMATION_MESSAGE);
            return;
        }
        
        int confirm = JOptionPane.showConfirmDialog(
            this,
            "Disconnect " + computer.getName() + " from " + computer.getConnectedDevice().getName() + "?",
            "Confirm Disconnection",
            JOptionPane.YES_NO_OPTION);
            
        if (confirm == JOptionPane.YES_OPTION) {
            controller.disconnectDevices(computer, computer.getConnectedDevice());
            canvas.repaint();
            statusBar.setText("Disconnected " + computer.getName());
        }
    }

    private void saveNetwork() {
        JFileChooser fileChooser = new JFileChooser();
        fileChooser.setDialogTitle("Save Network Project");
        fileChooser.setFileFilter(new FileNameExtensionFilter("Network Files (*.net)", "net"));
        fileChooser.setSelectedFile(new File("network_project.net"));
        
        int userSelection = fileChooser.showSaveDialog(this);
        
        if (userSelection == JFileChooser.APPROVE_OPTION) {
            File fileToSave = fileChooser.getSelectedFile();
            if (!fileToSave.getName().toLowerCase().endsWith(".net")) {
                fileToSave = new File(fileToSave.getAbsolutePath() + ".net");
            }
            
            try {
                // Create a map to hold both network data and positions
                Map<String, Object> saveData = new HashMap<>();
                saveData.put("network", controller.getNetworkData());
                saveData.put("positions", devicePositions);
                
                // Use the controller's save method but pass our combined data
                try (ObjectOutputStream oos = new ObjectOutputStream(
                        new FileOutputStream(fileToSave))) {
                    oos.writeObject(saveData);
                }
                
                statusBar.setText("Project saved successfully: " + fileToSave.getName());
            } catch (Exception ex) {
                JOptionPane.showMessageDialog(this, 
                    "Failed to save project: " + ex.getMessage(),
                    "Save Error", 
                    JOptionPane.ERROR_MESSAGE);
            }
        }
    }

    private void loadNetwork() {
        JFileChooser fileChooser = new JFileChooser();
        fileChooser.setDialogTitle("Load Network Project");
        fileChooser.setFileFilter(new FileNameExtensionFilter("Network Files (*.net)", "net"));
        
        int userSelection = fileChooser.showOpenDialog(this);
        
        if (userSelection == JFileChooser.APPROVE_OPTION) {
            File fileToLoad = fileChooser.getSelectedFile();
            
            try (ObjectInputStream ois = new ObjectInputStream(
                    new FileInputStream(fileToLoad))) {
                
                @SuppressWarnings("unchecked")
                Map<String, Object> loadedData = (Map<String, Object>) ois.readObject();
                
                // Load network data through controller
                controller.loadNetworkData(loadedData.get("network"));
                
                // Load device positions
                @SuppressWarnings("unchecked")
                Map<Device, Point> loadedPositions = (Map<Device, Point>) loadedData.get("positions");
                devicePositions.clear();
                devicePositions.putAll(loadedPositions);
                
                selectedDevice = null;
                updateDeviceInfoDisplay();
                canvas.repaint();
                statusBar.setText("Project loaded: " + fileToLoad.getName());
            } catch (Exception ex) {
                JOptionPane.showMessageDialog(this, 
                    "Failed to load project: " + ex.getMessage(),
                    "Load Error", 
                    JOptionPane.ERROR_MESSAGE);
            }
        }
    }
    
    private void editSelectedDevice() {
        if (selectedDevice == null) {
            JOptionPane.showMessageDialog(this, 
                "Please right-click on a device first to select it",
                "No Selection", 
                JOptionPane.WARNING_MESSAGE);
            return;
        }

        // Store current position before making changes
        Point currentPosition = devicePositions.get(selectedDevice);
        
        // Create edit panel
        JPanel panel = new JPanel(new GridLayout(2, 2, 5, 5));
        panel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
        
        JTextField nameField = new JTextField(selectedDevice.getName());
        JTextField ipField = new JTextField(selectedDevice.getIpAddress());
        
        panel.add(new JLabel("Name:"));
        panel.add(nameField);
        panel.add(new JLabel("IP Address:"));
        panel.add(ipField);

        // Show dialog
        int result = JOptionPane.showConfirmDialog(
            this,
            panel,
            "Edit " + selectedDevice.getClass().getSimpleName(),
            JOptionPane.OK_CANCEL_OPTION,
            JOptionPane.PLAIN_MESSAGE);

        if (result == JOptionPane.OK_OPTION) {
            String newName = nameField.getText().trim();
            String newIp = ipField.getText().trim();

            // Validate input
            if (newName.isEmpty()) {
                showError("Device name cannot be empty", "Invalid Name");
                return;
            }

            if (!newIp.matches("^\\d{1,3}\\.\\d{1,3}\\.\\d{1,3}\\.\\d{1,3}$")) {
                showError("Please enter a valid IP address (e.g., 192.168.1.1)", "Invalid IP");
                return;
            }

            // Check for duplicate names (excluding current device)
            if (controller.getDevices().stream()
                .filter(d -> !d.equals(selectedDevice))
                .anyMatch(d -> d.getName().equalsIgnoreCase(newName))) {
                showError("Device name '" + newName + "' already exists", "Duplicate Name");
                return;
            }

            // Remove old device from positions map before updating
            devicePositions.remove(selectedDevice);
            
            // Update device properties
            selectedDevice.setName(newName);
            selectedDevice.setIpAddress(newIp);
            
            // Put back in positions map with same position
            devicePositions.put(selectedDevice, currentPosition);
            
            canvas.repaint();
            statusBar.setText("Updated device: " + newName);
            updateDeviceInfoDisplay();
        }
    }

    // Helper method for error messages
    
    private void showWarning(String message, String title) {
        JOptionPane.showMessageDialog(this, 
            message, 
            title, 
            JOptionPane.WARNING_MESSAGE);
    }

    private void showInfo(String message, String title) {
        JOptionPane.showMessageDialog(this, 
            message, 
            title, 
            JOptionPane.INFORMATION_MESSAGE);
    }

    private void showError(String message, String title) {
        JOptionPane.showMessageDialog(this, 
            message, 
            title, 
            JOptionPane.ERROR_MESSAGE);
    }

    private void initializeDevicePositions() {
        devicePositions.clear();
        int width = canvas.getWidth();
        int height = canvas.getHeight();
        
        int cols = (int) Math.ceil(Math.sqrt(controller.getDevices().size()));
        int rows = (int) Math.ceil((double)controller.getDevices().size() / cols);
        
        int cellWidth = width / (cols + 1);
        int cellHeight = height / (rows + 1);
        
        for (int i = 0; i < controller.getDevices().size(); i++) {
            Device device = controller.getDevices().get(i);
            int col = i % cols;
            int row = i / cols;
            devicePositions.put(device, 
                new Point(
                    cellWidth * (col + 1) - 30, 
                    cellHeight * (row + 1) - 20
                )
            );
        }
    }
}