package ch.makery.address.view;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Timestamp;
import java.util.Date;
public class Old {

	String dbConn = "jdbc:postgresql://comp421.cs.mcgill.ca:5432/cs421";
	String user = "cs421g20";
	String pass = "b00sterjuice1";
	
	// Static representation of the app -- each method is called on this instance
	static Old app = new Old();

	//Main connection method 
	public Connection connect() {
		
		Connection conn = null;
        try {
            conn = DriverManager.getConnection(dbConn, user, pass);
            System.out.println("Connected to the PostgreSQL server successfully.");
        } 
        
        catch (SQLException e) {
            System.out.println("bad");
        }
 
        return conn;
    }
	
	/*User wants to return a vehicle --  param (u_id, keycode for car)
	 * check that the real_return time is null
	 * input the current time into real return time
	 * check for surge pricing 
	 * check if the real return time is past the end_date time 
	 * 		Every hour past the set endtime inc. by 20%
	 * 		check for surge price
	 *  
	*/
	
	public static String returnVehicle(int code, int rating, String comment) {
		
		//The same connection will be used through the method call
		Connection c = app.connect();
		
		
//		Scanner sc = new Scanner (System.in);
		System.out.println("Enter your vehicle's keycode: ");
		
		//Get the key code for the vehicle 
		//TO DO: Error handling for bad inputs
//		int code = sc.nextInt();
		
		
		boolean isSurge = false;
		String licensePlate = "";
		Timestamp starttime;
		Timestamp endtime;
		
		int renterId = 0;
		int ownerId = 0;
		
		Date date = new Date();
        Object realReturn = new Timestamp(date.getTime());  
		double tripPrice = 0;
		double hrPrice = 0;
		int numLateHours = 0;
		boolean isLate = false;
	
		
		
		
		
		try { 
			
			Statement smt = c.createStatement();			
			
			//Run a query on that statement -- to my knowledge there is one query per statement
			ResultSet checkRs = smt.executeQuery( "SELECT * FROM reservation WHERE key_code = " + 
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
				
				if (endtime.before((Timestamp) realReturn)) {
			    	isLate = true;
			    	numLateHours = (int) ((((Timestamp) realReturn).getTime() - endtime.getTime()) / (1000 * 60 * 60));
		        }
								
				if (returnDate != null) {
					System.out.println("this car has already been returned!");
					System.exit(0);
				}
				
			}

			
			checkRs.close();        
	        
	        smt.executeUpdate("UPDATE reservation SET real_return_datetime = '" + realReturn + 
	        		"' where key_code = " + code + ";");		
	        
	     
	       
		    System.out.println("Your return time has been updated.");
		       
		       /*TO DO: Take the license plate of the vehicle to find the milage rate for the given car. Mult this by 1.2 if (isSurge)
		  				subtract end from start, mult by the hourly rate
		        * (1) if the realreturn is within the res length, put the calc figure in the total cost col
		   
		       		(2) if the realreturn date is past the end date - subtract hours of real from end - mult cost by 1.2 and add to the base fare. 
		       		put this in the hourly rate 
		       */
		    
		    ResultSet priceSet = smt.executeQuery("SELECT * from vehicle where license_plate = '" + licensePlate + "';");
		        
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
		    
		    System.out.println("Your total cost is: $" + tripPrice);
		    
		    System.out.println("Please give a rating: ");
		    
//		    int rating = sc.nextInt();
//		    sc.nextLine();
		    
		    System.out.println("Please give a comment : ");
		    
//		    String comment = sc.next();
		    
//		    System.out.println("insert into rates values (" + renterId + ", "+ ownerId + ", " + realReturn 
//    		+ ", " + rating + ", " + comment + ");");
		    
		    smt.executeUpdate("insert into rates values (" + renterId + ", "+ ownerId + ", '" + realReturn 
		    		+ "', " + rating + ", '" + comment + "');");
		    
		    c.close();
		}
		catch (Exception e) {
	    	System.err.println( e.getClass().getName()+": "+ e.getMessage() );   
		}
	  
		
		//All sql commands must be in try/catch block -- catches sql errors. 
		System.out.println("Operation done successfully");
		return "Operation done successfully";

		
		
	}
	
}
