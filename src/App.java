import controllers.NetworkController;
import views.NetworkView;
import javax.swing.UIManager;
import java.awt.Font;


public class App {
	
    public static void main(String[] args) {
    	 Font nonSerifFont = new Font("Arial", Font.BOLD, 14);
    	    UIManager.put("Button.font", nonSerifFont);
    	    UIManager.put("Label.font", nonSerifFont);
    	    UIManager.put("TextField.font", nonSerifFont);
    	    UIManager.put("TextArea.font", nonSerifFont);
    	    UIManager.put("ComboBox.font", nonSerifFont);
    	    UIManager.put("List.font", nonSerifFont);
    	    UIManager.put("Menu.font", nonSerifFont);
    	    UIManager.put("MenuItem.font", nonSerifFont);
    	    UIManager.put("TabbedPane.font", nonSerifFont);
    	    UIManager.put("TitledBorder.font", nonSerifFont);
        NetworkController controller = new NetworkController();
        NetworkView view = new NetworkView(controller);
        view.setVisible(true);
    }
} 