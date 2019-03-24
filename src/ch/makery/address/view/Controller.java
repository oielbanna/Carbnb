package ch.makery.address.view;

import java.sql.Connection;
import java.sql.Timestamp;
import java.util.ArrayList;

import com.jfoenix.controls.JFXListView;
import com.jfoenix.controls.JFXSlider;
import com.jfoenix.controls.JFXTextArea;
import com.jfoenix.controls.JFXTextField;

import ch.makery.address.MainApp;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.geometry.Side;
import javafx.scene.chart.Axis;
import javafx.scene.chart.CategoryAxis;
import javafx.scene.chart.LineChart;
import javafx.scene.chart.NumberAxis;
import javafx.scene.chart.PieChart;
import javafx.scene.chart.XYChart;
import javafx.scene.control.DatePicker;
import javafx.scene.control.Label;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.stage.Stage;

public class Controller extends FXMLLoader {
	@FXML
	private DatePicker new_startTime;
	@FXML
	private DatePicker new_endTime;
	@FXML
	private JFXListView<String> new_availableList;
	@FXML
	private Label new_reservedMsg;


	@FXML
	private JFXTextField carReturn_code;
	@FXML
	private Label carReturn_displayMsg;
	@FXML
	private JFXSlider carReturn_rating;
	@FXML
	private JFXTextArea carReturn_comment;

	@FXML
	private JFXTextField cancel_code;
	@FXML
	private Label cancel_msg;

	@FXML
	private JFXListView<String> log_reservations;

	@FXML
	private JFXListView<String> mngCars_carsList;
	@FXML
	private Label mngCars_selectedCarDetails;
	@FXML
	private JFXTextField mngCars_selectedCarPrice;
	@FXML
	private Label mngCars_updateCarMsg;
	@FXML
	private JFXTextField newCar_lience;
	@FXML
	private JFXTextField newCar_price;
	@FXML
	private DatePicker newCar_availStart;
	@FXML
	private DatePicker newCar_availEnd;
	@FXML
	private JFXTextField newCar_brand;
	@FXML
	private JFXTextField newCar_model;
	@FXML
	private JFXTextField newCar_year;
	@FXML
	private JFXTextField newCar_insuranceClaim;
	@FXML
	private DatePicker newCar_insuranceExp;
	@FXML
	private Label newCar_msg;

	@FXML
	private PieChart piechart;
	@FXML
	private VBox linechartPane;


	// Reference to the main application.
	private MainApp mainApp;

	private Connection connection;
	/**
	 * The constructor. The constructor is called before the initialize() method.
	 */
	public Controller() {
	}

	/**
	 * Initializes the controller class. This method is automatically called after
	 * the fxml file has been loaded.
	 */
	@FXML
	private void initialize() throws Exception {
		connection = MainApp.c;
		populateLog();

		populateTables();

		displayMyCars();

	}

	@FXML
	public void populateTables() throws Exception {

		ObservableList<PieChart.Data> pieChartData = FXCollections.observableArrayList();
		ArrayList<Tuple<String, Integer>> list = DB.generatePieChart(connection);

		for (int i = 0; i < list.size(); i++) {
			Tuple<String, Integer> item = list.get(i);
			pieChartData.add(new PieChart.Data(item.first, item.second));
		}
		piechart.setTitle("Percentage of Vehicles per Brand");
		piechart.setLabelLineLength(15);
		piechart.setLegendSide(Side.LEFT);
		piechart.setData(pieChartData);

		Axis<String> xAxis = new CategoryAxis();
		NumberAxis yAxis = new NumberAxis();
		xAxis.setLabel("Birthyear");
		xAxis.setTickLabelFill(Color.WHITE);
		yAxis.setLabel("Number of Strikes");
		yAxis.setTickLabelFill(Color.WHITE);

		LineChart<String, Number> lineChart = new LineChart<String, Number>(xAxis, yAxis);
		lineChart.setTitle("Strikes vs Ages");
		XYChart.Series<String, Number> series = new XYChart.Series<String, Number>();
		ArrayList<Tuple<Number, Number>> listChart = DB.generateLineChart(connection);
		for (int i = 0; i < listChart.size(); i++) {
			Tuple<Number, Number> item = listChart.get(i);
			series.getData().add(new XYChart.Data<String, Number>(item.first + "", item.second, 1));
		}

		lineChart.getData().add(series);
		lineChart.getXAxis().setAutoRanging(true);
		linechartPane.getChildren().add(lineChart);

	}

	@FXML
	public void populateLog() throws Exception {

		ArrayList<String> list = DB.getHistory(connection, 4);
		ObservableList<String> items = FXCollections.observableArrayList("License plate, Start time, End time, price");
		for (int i = 0; i < list.size(); i++) {
			items.add(list.get(i));
		}
		log_reservations.setItems(items);

	}

	@FXML
	public void setMainApp(MainApp mainApp) {
		this.mainApp = mainApp;

	}

	@FXML
	public void findAvailableCars() throws Exception {

		Timestamp startTime = Timestamp.valueOf(new_startTime.getValue().atStartOfDay());
		Timestamp endTime = Timestamp.valueOf(new_endTime.getValue().atStartOfDay());
		System.out.println("start: " + startTime + " End: " + endTime);
		ArrayList<String> cars = DB.availCars(connection, startTime, endTime);

		JFXListView<String> list = new_availableList;
		ObservableList<String> items = FXCollections.observableArrayList();
		for (int i = 0; i < cars.size(); i++) {
			items.add(cars.get(i));
		}
		list.setItems(items);
	}

	@FXML
	public void reserve() {
		Timestamp startTime = Timestamp.valueOf(new_startTime.getValue().atStartOfDay());
		Timestamp endTime = Timestamp.valueOf(new_endTime.getValue().atStartOfDay());
		String selectedCar = new_availableList.getSelectionModel().getSelectedItem();

		System.out.println(selectedCar);
		String license = selectedCar.substring(0, selectedCar.indexOf(','));

		String s = DB.makeRes(connection, license, 4, startTime, endTime);
		new_reservedMsg.setText(s);
	}

	@FXML
	private void returnCar() throws Exception {
		String s = ""; // change this variable to display whatever you want on view.

		String code = carReturn_code.getText();
		double rating = carReturn_rating.getValue();
		String comment = carReturn_comment.getText();

		s = DB.returnVehicle(connection, Integer.parseInt(code), (int) rating, comment);
		carReturn_displayMsg.setText(s);
		
	}

	@FXML
	private void cancelReservation() throws Exception {
		String s = "";

		int code = Integer.parseInt(cancel_code.getText());
		s = DB.cancelRes(connection, 4, code);
		cancel_msg.setText(s);

	}

	@FXML
	void displayMyCars() throws Exception {
		JFXListView<String> list = mngCars_carsList;

		ArrayList<String> cars = DB.viewVehicle(connection, 4);
		// add your cars to this list
		// License, Model, Year, Price
		ObservableList<String> items = FXCollections.observableArrayList();
		for (int i = 0; i < cars.size(); i++) {
			items.add(cars.get(i));

		}

		list.setItems(items);
	}

	@FXML
	private void showSelectedCar() throws Exception {
		String car = mngCars_carsList.getSelectionModel().getSelectedItem();

		String license = car.substring(0, car.indexOf(","));
		String[] carStuff = new String[2];
		carStuff = DB.getCar(connection, license);

		mngCars_selectedCarDetails.setText(carStuff[0]);

		mngCars_selectedCarPrice.setText(carStuff[1]);

	}

	@FXML
	private void editCar() throws Exception {
		double newPrice = Double.parseDouble(mngCars_selectedCarPrice.getText());
		String car = mngCars_selectedCarDetails.getText();

		String license = car.substring(0, car.indexOf(","));
		String s = DB.changeRate(connection, license, 4, newPrice);
		mngCars_updateCarMsg.setText(s);

		// UPDATE LIST OF CARS
		displayMyCars();
	}

	@FXML
	private void addNewCar() throws Exception {
		String license = newCar_lience.getText();

		Double price = Double.valueOf(newCar_price.getText());

		Timestamp availStart = Timestamp.valueOf(newCar_availStart.getValue().atStartOfDay());

		Timestamp availEnd = Timestamp.valueOf(newCar_availEnd.getValue().atStartOfDay());

		String brand = newCar_brand.getText();
		String model = newCar_model.getText();
		int year = Integer.parseInt(newCar_year.getText());

		int insuranceClaim = Integer.parseInt(newCar_insuranceClaim.getText());
		Timestamp insuranceExp = Timestamp.valueOf(newCar_insuranceExp.getValue().atStartOfDay());

		// CALL METHOD TO ADD CAR HERE
		String s = DB.addCar(connection, availStart, availEnd, license, 4, 4, price, brand, model, year, insuranceClaim,
				insuranceExp);
		// UPDATE LIST OF CARS
		newCar_msg.setText(s);
		displayMyCars();

	}

	@FXML
	private void logout() {
		Stage stage = mainApp.primaryStage;
		stage.close();
	}



}