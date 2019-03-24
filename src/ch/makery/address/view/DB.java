package ch.makery.address.view;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.Date;
public class DB {

	static String dbConn = "jdbc:postgresql://comp421.cs.mcgill.ca:5432/cs421";
	static String user =  "cs421g20";
	static String  pass = "b00sterjuice1";
	
	//Main connection method 
	public static Connection connect() throws Exception {
		
		Connection conn = null;
        try {
            conn = DriverManager.getConnection(dbConn, user, pass);
            System.out.println("Connected to the PostgreSQL server successfully.");
        } 
        catch (SQLException e) {
			System.out.println("Could not connect");
			throw new Exception("Could not connect");
        }
 
        return conn;
    }
	
	public static void disconnect(Connection c) throws Exception {
		
		try {
			c.close();
			System.out.println("Connection closed");
		}
		catch (Exception e) {
			System.out.println("Connection wasn't closed");
			throw new Exception("Connection wasn't closed");
		}
		
	}
	
	public static ArrayList<String> availCars(Connection c, Timestamp start, Timestamp end) throws Exception {

		ArrayList<String> res = new ArrayList<String>();
		Statement smt = null;
		ResultSet rs = null;
		try {
			smt = c.createStatement();

			rs = smt.executeQuery("SELECT * FROM vehicle WHERE ('" + start
					+ "' BETWEEN avail_start AND avail_end) AND ('" + end + "' BETWEEN avail_start AND avail_end);");
			while (rs.next()) {

				String license = rs.getString("license_plate");
				String price = rs.getString("price_per_hr");
				String brand = rs.getString("brand");
				String model = rs.getString("model");

				String total = license + ", " + price + ", " + brand + ", " + model;

				res.add(total);

			}

			return res;
		}

		catch (Exception e) {
			throw new Exception("Problem with available cars");

		} finally {
			rs.close();
			smt.close();
		}

	}

	/*
	 * Get the starttime, endtime and liscense plate from the user
	 * 
	 * get the price per hr from vehicle matching license plate 
	 * generate the personal keycode from the currently existing vehicle codes +1
	 * get card num from card details matching u_id 
	 * 
	 * check that the entered vehicle is available 
	 * 
	 */
	
	public static String makeRes(Connection c, String license_plate, int u_id, Timestamp start_dateentered,
	Timestamp end_dateentered) {	
	
//		Connection c = app.connect();
		double totalPrice = 0;
		String cardNum = "";
		int newCode = 0;
		String returnString = "";
		
		try { 
			
			Statement smt = c.createStatement();
			
			ResultSet cardSet = smt.executeQuery("select card_num from carddetails where u_id = " + u_id + ";" );
			
			while (cardSet.next()) {
				
				cardNum  = cardSet.getString("card_num");	
			}
			cardSet.close();
			
			ResultSet vSet = smt.executeQuery("select price_per_hr from vehicle where license_plate = '" + license_plate + "';" );
			
			while (vSet.next()) {
				
				double hrPrice  = vSet.getDouble("price_per_hr");			
		    	int numHours = (int) ((end_dateentered.getTime() - start_dateentered.getTime()) / (1000 * 60 * 60));
		    	totalPrice = numHours * hrPrice;
				
			}	
			vSet.close();
			
			ResultSet codeSet = smt.executeQuery("SELECT MAX(key_code) FROM reservation");
			
			while (codeSet.next()) {
				
				newCode = codeSet.getInt("max") + 1;
				
			}
			
			codeSet.close();
			
			
			
			smt.executeUpdate("INSERT INTO reservation (start_datetime, end_datetime, license_plate, u_id, total_price, key_code, card_num) values ('"		
					+ start_dateentered + "', '" +end_dateentered + "', " + license_plate
					+ ", " + u_id + ", " + totalPrice + ", " + newCode + ", " + cardNum 
					+ ");");
			
			returnString = "Total price: " + totalPrice + "\nYour unique keycode: " + newCode;
			
			
//			c.close();
			
		}
				
		catch (Exception e) {
	    	returnString = "Your reservation was not made successfully";
		}
	  
		return returnString;
		
		
	}
	
	//Cancel a reservatin
	
	public static String cancelRes(Connection c, int u_id, int code) throws Exception {
		
		String returnString = "";
//		Connection c = app.connect();
		
		try { 
			
			Statement smt = c.createStatement();
			
			smt.executeUpdate("Delete from reservation where u_id = " + u_id + 
				" and key_code = " + code + ";");
			
			
			smt.close();
//			c.close();
			returnString = "Your reservation was successfully canceled";
			return returnString;
		}
				
		catch (Exception e) {
			throw new Exception("Sorry, Your reservation was not successfully canceled!");
		}
		
		
		
		

	}
	
	
	public static ArrayList<String> getHistory(Connection c, int u_id) throws Exception {

//		Connection c = app.connect();
		
		ArrayList<String> result = new ArrayList<String>();
		Statement smt = null;
		ResultSet hist = null;
		try { 
			
			smt = c.createStatement();
			
			hist = smt.executeQuery(
					"select license_plate, start_datetime, real_return_datetime, total_price from reservation where u_id = "
							+ u_id + ";");
				
			while (hist.next()) {
				
					String license = hist.getString("license_plate");
					String sTime = hist.getString("start_datetime");
					String rTime = hist.getString("real_return_datetime");
					
					if (rTime == null) {
						rTime = "Not returned";
					}
					
					String price = hist.getString("total_price");
				
				String total = String.format("%s, %s, %s, %s", license, sTime, rTime, price);

					result.add(total);
				
			}
			
			hist.close();
			smt.close();
			return result;
			
		}
				
		catch (Exception e) {
			throw new Exception(e.getClass().getName() + ": " + e.getMessage());
		} finally {
			hist.close();
			smt.close();
		}
		
		
	}

	
	public static String returnVehicle(Connection c, int code, int rating, String comment) throws Exception {
		
		//The same connection will be used through the method call
//		Connection c = app.connect();
		
		boolean isSurge = false;
		String licensePlate = "";
		Timestamp starttime;
		Timestamp endtime;
		
		int renterId = 0;
		int ownerId = 0;
		
		Date date = new Date();
		Timestamp realReturn = new Timestamp(date.getTime());
		double tripPrice = 0;
		double hrPrice = 0;
		int numLateHours = 0;
		boolean isLate = false;

		Statement smt = null;
		ResultSet checkRs = null;
		ResultSet priceSet = null;
		
		try { 
			
			smt = c.createStatement();
			
			//Run a query on that statement -- to my knowledge there is one query per statement
			checkRs = smt.executeQuery("SELECT * FROM reservation WHERE key_code = " +
			code);
			
			
			//Loop to go through the results of the query 
			while (checkRs.next()) {
				
				String returnDate = "";
				
				//Get the return date
				returnDate  = checkRs.getString("real_return_datetime");
				
				//Get the license plate number 
				licensePlate = checkRs.getString("license_plate");
				renterId = checkRs.getInt("u_id");
				
				//check if Surge pricing is on
				int surgeCheck = checkRs.getInt("surged");
				if (surgeCheck == 1) {
					isSurge = true;
				}
				
				starttime = checkRs.getTimestamp("start_datetime");
				endtime = checkRs.getTimestamp("end_datetime");
				tripPrice = checkRs.getDouble("total_price");
				
				if (realReturn.before(starttime)) {
					return "Cannot return the vehicle before you start the reservation. \nGo to cancel reservation instead.";
				}

				if (endtime.before((Timestamp) realReturn)) {
			    	isLate = true;
			    	numLateHours = (int) ((((Timestamp) realReturn).getTime() - endtime.getTime()) / (1000 * 60 * 60));
		        }
								
				if (returnDate != null) {
					return "this car has already been returned!";
				}
				
			}

			checkRs.close();

	        smt.executeUpdate("UPDATE reservation SET real_return_datetime = '" + realReturn + 
	        		"' where key_code = " + code + ";");		
	        
		    System.out.println("Your return time has been updated.");
			priceSet = smt.executeQuery("SELECT * from vehicle where license_plate = '" + licensePlate + "';");
		        
		    while (priceSet.next()) {
		    	hrPrice = priceSet.getDouble("price_per_hr");	
		    	ownerId = priceSet.getInt("u_id");
		    }
		    
			priceSet.close();

		    if (isLate) {
		    	double lateHrlyPrice = hrPrice * 1.2;
		    	tripPrice += numLateHours * lateHrlyPrice;

		    } 
		    
		    if (isSurge) {
		    	tripPrice = tripPrice * 1.2;
		    }

			smt.executeUpdate("Update reservation set total_price = " + tripPrice + ";");


		    smt.executeUpdate("insert into rates values (" + renterId + ", "+ ownerId + ", '" + realReturn 
		    		+ "', " + rating + ", '" + comment + "');");

			System.out.println("Operation done successfully");
			return "Operation done successfully";
		}
		catch (Exception e) {
			throw new Exception(e.getClass().getName() + ": " + e.getMessage());
		} finally {
			smt.close();
		}
	  

		
		
	}
	
	public static String[] getCar(Connection c, String license) throws Exception {
		Statement smt = null;
		ResultSet hist = null;
		String[] car = new String[2];
		try {
			smt = c.createStatement();
			hist = smt.executeQuery("select * from vehicle where license_plate = '" + license + "';");
			hist.next();

			String licensePlate = hist.getString("license_plate");
			String price = hist.getString("price_per_hr");
			String brand = hist.getString("brand");
			String model = hist.getString("model");
			String code = hist.getString("personal_key_code");

			car[0] = licensePlate + ", " + brand + ", " + model + ", " + code;
			car[1] = price;
			return car;
		} catch (Exception e) {
			e.printStackTrace();
			throw new Exception();
		} finally {
			smt.close();
			hist.close();
		}
	}

	public static ArrayList<String> viewVehicle(Connection c, int u_id) throws Exception {
		
		//display all vehicles
		
//		Connection c = app.connect();
		
		ArrayList<String> result = new ArrayList<String>();
		Statement smt = null;
		ResultSet hist = null;
		try { 
			
			smt = c.createStatement();
			
			hist = smt.executeQuery(
					"select license_plate, price_per_hr, brand, model, personal_key_code from vehicle where u_id = "
							+ u_id + ";");
			
			while (hist.next()) {
			
					String license = hist.getString("license_plate");
					String price = hist.getString("price_per_hr");
					String brand = hist.getString("brand");
					String model = hist.getString("model");
					String code = hist.getString("personal_key_code");
					
					String total = license + ", " + price + ", " +  brand + ", " + model + ", " + code;


					result.add(total);
					
					
			}
				
			
			

			for (int i=0; i<result.size(); i++) {
				System.out.println(result.get(i));
			}
			
			return result;
			
		}
				
		catch (Exception e) {
			throw new Exception(e.getClass().getName() + ": " + e.getMessage());

		}
		finally {
			hist.close();
			smt.close();
		}
		
	}
	
	//given a license plate and a keycode, change the hourly rate
	
	public static String changeRate(Connection c, String licensePlate, int u_id, double newRate) throws Exception {
		
//		Connection c = app.connect();
		String returnString = "The rate for vehicle with license plate : " + licensePlate +" was successfully changed to :$" + newRate;
		Statement smt = null;
		try { 
			
			smt = c.createStatement();
			
			smt.executeUpdate("UPDATE vehicle SET price_per_hr = " + newRate + " WHERE u_id = "
					+ u_id + " AND license_plate = '" + licensePlate + "';");

			return "Rate successfully changed to " + newRate;
		}
				
		catch (Exception e) {
			throw new Exception(e.getClass().getName()+": "+ e.getMessage());
		} finally {
			smt.close();
		}
		
		
		
	}
	
	public static String addCar(Connection c, Timestamp start, Timestamp end, String licensePlate, int u_id, int i_id,
			double price, 
			String brand, String model,
			int year, int claim_id, Date ins_ex_date) throws Exception {
			
		String returnString = "Your car has been successfully added!";
		int newCode = 0;
	
		Statement smt = null;
		ResultSet cSet = null;
		try { 
			
			smt = c.createStatement();
			
			cSet = smt.executeQuery("SELECT MAX(personal_key_code) FROM vehicle;");
			
			while (cSet.next()) {
				
				newCode = (cSet.getInt("max")) + 1;
				
			}
						
			smt.executeUpdate("INSERT INTO vehicle(avail_start, avail_end, license_plate, u_id, i_id, price_per_hr, brand, model, year, personal_key_code, claim_id, insurance_exp_date) VALUES ('" +
			start + "', '" + end + "', '" + licensePlate + "', " + u_id+", " + i_id + ", " + price + ", '" + brand + "', '" + model + "', " + year + ", " + String.valueOf(newCode) + ", " + 
					claim_id + ", '" + ins_ex_date + "');");

			return returnString;
		}
				
		catch (Exception e) {
			throw new Exception(e.getClass().getName() + ": " + e.getMessage());
		} finally {
			smt.close();
			cSet.close();
		}

	}
	
	public static ArrayList<Tuple<String, Integer>> generatePieChart(Connection c) throws Exception {
		ArrayList<Tuple<String, Integer>> list = new ArrayList<>();
		Statement smt = null;
		ResultSet rs = null;
		try {
			smt = c.createStatement();
			rs = smt.executeQuery("SELECT brand, COUNT(*) AS count FROM vehicle GROUP BY brand");
			while (rs.next()) {
				String brand = rs.getString("brand");
				int count = rs.getInt("count");
				list.add(new Tuple<String, Integer>(brand, count));
			}

			return list;
		} catch (Exception e) {
			e.printStackTrace();
			throw new Exception("Pie chart data error");
		} finally {
			smt.close();
			rs.close();
		}
		
	}

	public static ArrayList<Tuple<Number, Number>> generateLineChart(Connection c) throws Exception {
		ArrayList<Tuple<Number, Number>> list = new ArrayList<>();
		Statement smt = null;
		ResultSet rs = null;
		try {
			 smt = c.createStatement();
			 rs = smt.executeQuery(
					"SELECT DATE_PART('year',C.birthdate) As Year, COUNT(*) AS number_of_strikes FROM client C, strike S WHERE C.u_id = S.u_id AND DATE_PART('year',C.birthdate) >= '1990' GROUP BY DATE_PART('year',C.birthdate);");
	    	while(rs.next()) {
				double birthdate = rs.getDouble("year");
				int numOfStrikes = rs.getInt("number_of_strikes");
				list.add(new Tuple<Number, Number>((int) birthdate, numOfStrikes));
			}
			
			return list;
		} catch (Exception e) {
			e.printStackTrace();
			throw new Exception("Line graph data error");
		} finally {
			smt.close();
			rs.close();
		}
		
	}

}
