package ch.makery.address;

import java.io.IOException;
import java.sql.Connection;

import ch.makery.address.view.Controller;
import ch.makery.address.view.DB;
import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.scene.layout.BorderPane;
import javafx.stage.Stage;

public class MainApp extends Application {

	public Stage primaryStage;
	public BorderPane rootLayout;
	public static Connection c;


	/**
	 * Constructor
	 */
    public MainApp() {

    }


    @Override
	public void start(Stage primaryStage) throws Exception {
        this.primaryStage = primaryStage;
		this.primaryStage.setTitle("Car Airbnb");

        initRootLayout();

    }

    /**
     * Initializes the root layout.
     */
	public void initRootLayout() throws Exception {
		try {
			// Load root layout from fxml file.
			FXMLLoader loader = new FXMLLoader();
			loader.setLocation(MainApp.class.getResource("view/RootLayout.fxml"));
			rootLayout = (BorderPane) loader.load();

			Controller controller = loader.getController();
			controller.setMainApp(this);

			// Show the scene containing the root layout.
			Scene scene = new Scene(rootLayout);
			primaryStage.setScene(scene);
			primaryStage.show();
		} catch (IOException e) {
			e.printStackTrace();
			throw new Exception();
		}
	}



    public static void main(String[] args) {

		try {
			c = DB.connect();
			launch(args);

		} catch (Exception e) {
			e.printStackTrace();
			System.out.println(e.getMessage());
		} finally {
			try {
				DB.disconnect(c);
			} catch (Exception e) {
				System.out.println(e.getMessage());
			}
			System.out.println("App terminated properly.");
		}
    }
}