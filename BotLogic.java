    public class BotLogic{
    	private Connection connection = null;  

//    	Prices
    	final Integer POINTS_ON_CORRECT_GUESS = 100;
    	final Integer HEROREQUEST_PRICE = 500;
    	final Integer ITEMBUNDLE_PRICE = 1000;
    	final Integer REPLAYANALYSIS_PRICE = 1000;
    	
//    	Guessingstatus related
    	boolean guessingroundActive;
    	int whatIsGuessed;
		boolean guessLock = true;
		int guessCount = 0;
		int guessroundID = 0;
    	
//		Reward related
		boolean herorequestActive;
		String requestedHero;
		int herorequestID = 0;
		
    	/**
        * Constructor that opens the DB-Connection
        */
    	public BotLogic(){
    		try {
				Class.forName( "org.sqlite.JDBC" );
				connection = DriverManager.getConnection( "jdbc:sqlite:./BotDB.db", "", "" );
				guessingroundActive = false;
				deleteAllGuesses();
			} catch (ClassNotFoundException | SQLException e) {
				e.printStackTrace();
			}
    	}
    	
    	 /**
         * Method that handles incoming messages
         * which method is handled is written in commands above the if()
         */
    	public void newMessage(User user, String message) {
    		// IMPORTANT: OPERATIONS HERE ARE BEFORE SPLITTING TO WORDS   		    		
    		// blocking spam links containing ".af/"
    		if(message.contains(".af/")){
    			sendMessage(user.getChannel(), "/timeout " + user.nick + " 1");
				return;
    		}
    		
    		String[] words = message.split(" ");
    		
    		// IMPORTANT: OPERATIONS HERE ARE after SPLITTING TO WORDS 
    		// !points
    		if(words[0].equalsIgnoreCase("!points") && (words.length == 1)){
    			pointsRequestedBy(user);
    			return;
    		}

    		// !points <nick>
    		if(words[0].equalsIgnoreCase("!points") && (words.length == 2)){
    			if(!user.isModerator() && !user.isAdmin()){
    				sendMessage(user.getChannel(), "Hey " + user.nick + ". Only moderators are allowed to ask for points of others.");
    				return;
    			}
    			pointsRequestedOf(words[1], user);
    			return;
    		}
    		
    		// !points <add|remove> <username> <number>
    		if(words[0].equalsIgnoreCase("!points") && (words.length == 4)){
    			if(!user.isModerator() && !user.isAdmin()){
    				sendMessage(user.getChannel(), "Hey " + user.nick + ". Only moderators are allowed to add or remove points from others.");
    				return;
    			}
    			handleAddOrRemovePoints(words, user);
    			return;
    		}
    		
    		// !mmr
    		if(words[0].equalsIgnoreCase("!mmr") && (words.length == 1)){
    			sendMessage(user.getChannel(), "Solo MMR = " + getSoloMMR() + ", Party MMR = " + getPartyMMR());
    			return;
    		}
    		
    		// !mmrpeak
    		if(words[0].equalsIgnoreCase("!mmrpeak") && (words.length == 1)){
    			sendMessage(user.getChannel(), "Solo MMR peak = " + getSoloMMRMax() + ", Party MMR peak = " + getPartyMMRMax());
    			return;
    		}
    		
    		// !mmr <solo|party|solomax|partymax> <newMMR>
    		if(words[0].equalsIgnoreCase("!mmr") && (words.length == 3)){
    			if(!user.isModerator() && !user.isAdmin()){
    				sendMessage(user.getChannel(), "Hey " + user.nick + ". Only moderators are allowed to add or remove points from others.");
    				return;
    			}
    			handleMMRUpdate(user, words);
				return;
    		}
    		
    		// !start
    		if(words[0].equalsIgnoreCase("!start") && (words.length == 1)){
    			if(!user.isModerator() && !user.isAdmin()){
    				sendMessage(user.getChannel(), "Hey " + user.nick + ", only moderators are allowed to start a new round of guessing.");
    				return;
    			}
    			startGuessing(user);
    			return;
    		}
    		
    		// !start <whattoguess>
    		if(words[0].equalsIgnoreCase("!start") && (words.length == 2)){
    			if(!user.isModerator() && !user.isAdmin()){
    				sendMessage(user.getChannel(), "Hey " + user.nick + ", only moderators are allowed to start a new round of guessing.");
    				return;
    			}
    			if(words[1].equalsIgnoreCase("death")){
    				whatIsGuessed = 1;
    				startSpecificGuessing(user);
    				return;
    			}
    			if(words[1].equalsIgnoreCase("kills")){
    				whatIsGuessed = 2;
    				startSpecificGuessing(user);
    				return;
    			}
    			if(words[1].equalsIgnoreCase("gpm")){
    				whatIsGuessed = 3;
    				startSpecificGuessing(user);
    				return;
    			}
    			if(words[1].equalsIgnoreCase("xpm")){
    				whatIsGuessed = 4;
    				startSpecificGuessing(user);
    				return;
    			}
    		}
    		
    		// !guess
    		if(words[0].equalsIgnoreCase("!guess") && (words.length == 2)){
				if(guessLock){
					sendMessage(user.getChannel(), "Hey " + user.nick + ", guessing is currently disabled. Please wait until the new round is started.");
					return;
				}
    			if(isThisANumber(words[1])){
					handleUserGuess(Integer.parseInt(words[1]), user);
					return;
				}else{
					sendMessage(user.getChannel(), "Hey " + user.nick + ", that's no number. Use !guess <number>.");
					return;
				}
    		}
    		
    		// !abort
    		if(words[0].equalsIgnoreCase("!abort") && (words.length == 1)){
    			if(!user.isModerator() && !user.isAdmin()){
    				sendMessage(user.getChannel(), "Hey " + user.nick + ", only moderators are allowed to abort guessing.");
    				return;
    			}
    			handleAbort(user);
    			return;
    		}	
    		
    		// !restart
    		if(words[0].equalsIgnoreCase("!restart") && (words.length == 1)){
    			if(!user.isModerator() && !user.isAdmin()){
    				sendMessage(user.getChannel(), "Hey " + user.nick + ", only moderators are allowed to restart guessing.");
    				return;
    			}
    			handleRestart(user);
    			return;
    		}
    		
    		// !result
    		if(words[0].equalsIgnoreCase("!result") && (words.length == 2)){
    			if(!user.isModerator() && !user.isAdmin()){
    				sendMessage(user.getChannel(), "Hey " + user.nick + ", only moderators are allowed to provide a result.");
    				return;
    			}
    			if(isThisANumber(words[1])){
					handleResultAndRewards(user, Integer.parseInt(words[1]));
					return;
				}else{
					sendMessage(user.getChannel(), "Hey " + user.nick + ", that's no number. Use !result <number>.");
					return;
				}
    		}	
    		
    		// !whatDidWeGuess
    		if(words[0].equalsIgnoreCase("!WhatDidWeGuess") && (words.length == 1)){
    			handleWhatDidWeGuess(user);
    			return;
    		}
    		
    		// !herorequest
    		if(words[0].equalsIgnoreCase("!herorequest") && ( (words.length == 2) || (words.length == 3) ) ){
    			handleHerorequest(words, user);
    			return;
    		}
    		
    		// !hrdone
    		if(words[0].equalsIgnoreCase("!hrdone") && words.length == 1){
    			handleHRDone();
    			return;
    		}
    		
    		// !replayanalysis
    		if(words[0].equalsIgnoreCase("!replayanalysis") && words.length == 1){
    			handleReplayanalsyse(user);
    			return;
    		}
    		
    		// !itembundle
    		if(words[0].equalsIgnoreCase("!itembundle") && words.length == 1){
    			handleItembundlerequest(user);
    			return;
    		}
    		
    	}
	
    	
    	
//----------------------------Command Handling & Helper Methods--------------------------------------------------------------------
    
    	//    	-----Guessing Related-----
    	public void handleWhatDidWeGuess(User user){
    		if(whatIsGuessed == 1){
    			sendMessage(user.getChannel(), "Death");
    			return;
    		}
    		if(whatIsGuessed == 2){
    			sendMessage(user.getChannel(), "Kills");
    			return;
    		}
    		if(whatIsGuessed == 3){
    			sendMessage(user.getChannel(), "GPM");
    			return;
    		}
    		if(whatIsGuessed == 4){
    			sendMessage(user.getChannel(), "XPM");
    			return;
    		}
    	}

    	public void handleResultAndRewards(User user, int result){
    		if(!guessingroundActive){
    			sendMessage(user.getChannel(), "Hey " + user.nick + ", there's no active round of guessing which could be ended.");
    			return;
    		}
    		if(!guessLock){
    			sendMessage(user.getChannel(), "Hey " + user.nick + ", guessing is still enabled. Options are !restart or !abort.");
    			return;
    		}
    		
    		ArrayList<String> winners = new ArrayList<String>();
    		String selectSQL = "SELECT * FROM GUESSES;";
			PreparedStatement preparedStatement;
			
			if(whatIsGuessed == 1 || whatIsGuessed == 2){
				try {
					preparedStatement = connection.prepareStatement(selectSQL);
					ResultSet resultSet = preparedStatement.executeQuery();
	    			while (resultSet.next()) {
	    				String username = resultSet.getString(1);
	    				int guess = resultSet.getInt(2);
	    				if(guess == result){
	    					addPointsToUser(username.toLowerCase(), POINTS_ON_CORRECT_GUESS);
	    					winners.add(username.toLowerCase());
	    				}
	    			}
				} catch (SQLException e) {
					System.out.println("We just failed at computing all guesses. Whe checked whether someone is correct?");
					e.printStackTrace();
				}
			}else{
				try {
					preparedStatement = connection.prepareStatement(selectSQL);
					ResultSet resultSet = preparedStatement.executeQuery();
	    			while (resultSet.next()) {
	    				String username = resultSet.getString(1);
	    				int guess = resultSet.getInt(2);
	    				if((guess <= (result + 25)) && (guess >= (result - 25)) ){
	    					addPointsToUser(username.toLowerCase(), POINTS_ON_CORRECT_GUESS);
	    					winners.add(username.toLowerCase());
	    				}
	    			}
				} catch (SQLException e) {
					System.out.println("We just failed at computing all guesses. Whe checked whether someone is correct?");
					e.printStackTrace();
				}
			}
			
			
			int winnerCount = winners.size();
			String winnerList = "";
			if(winnerCount > 0){
				winnerList = generateWinnerList(winners, winnerCount);
			}
			
			if(winnerCount == 0){
				sendMessage(user.getChannel(), "Noone guessed correctly. Good luck next time!");
			}else if(winnerCount == 1){
				sendMessage(user.getChannel(), "We had only one winner this time. Congratulations to: " + winnerList);
			}else{
				float winnerCountFloat = (float) winnerCount;
				float percentage = (winnerCountFloat * 100 / guessCount);
				sendMessage(user.getChannel(), "We had " + winnerCount + " winners this time! That means, that " + String.format("%.02f", percentage) + "% of all entries were correct. Congratulations to: " + winnerList);
			}
			guessLock = true;
    		deleteAllGuesses();
    		guessCount = 0;
    		guessingroundActive = false;
    		guessroundID++;
    		endBettingPhase();
    	}
    	
		private String generateWinnerList(ArrayList<String> winners, int winnerCount) {
			String winnersList;
			if(winnerCount == 1){
				winnersList = winners.get(0);
				return winnersList;
			}else{
				StringBuilder stringBuilder = new StringBuilder();
				stringBuilder.append(winners.get(0));

				for (int i = 1; i < winners.size(); i++) {
					stringBuilder.append(", ");
					stringBuilder.append(winners.get(i));
				}
				String finalString = stringBuilder.toString();
				return finalString;
			}
		}

    	public void handleRestart(User user){
    		if(!guessingroundActive){
    			sendMessage(user.getChannel(), "Hey " + user.nick + ", there's no active round of guessing which could be restarted.");
    			return;
    		}
    		handleAbort(user);
    		sendMessage(user.getChannel(), "Lets start this all over again. All guesses got deleted.");
    		startGuessing(user);
    	}
    	
    	public void handleAbort(User user){
    		if(!guessingroundActive){
    			sendMessage(user.getChannel(), "Hey " + user.nick + ", there's no active round of guessing which could be aborted.");
    			return;
    		}
    		guessLock = true;
    		deleteAllGuesses();
    		guessCount = 0;
    		guessingroundActive = false;
    		guessroundID++;
    		endBettingPhase();
    		sendMessage(user.getChannel(), "Okay, all guesses deleted, the active round got stopped.");
    	}
    	
    	public void handleUserGuess(int guessedValue, User user){
    		if(didThisUserAlreadyGuess(user.nick.toLowerCase())){
    			insertGuessIntoDB(user.nick.toLowerCase(), guessedValue);
    		}else{
	    		if( (whatIsGuessed == 3) && guessedValue <= 50){
	    			sendMessage(user.getChannel(), "Hey " + user.nick + ", i got that,  but are you sure you want to guess that low? We're guessing GPM currently!");
	    			insertGuessIntoDB(user.nick.toLowerCase(), guessedValue);
	    			guessCount = guessCount + 1;
	    			return;
	    		}
	    		if( (whatIsGuessed == 3) && guessedValue <= 50){
	    			sendMessage(user.getChannel(), "Hey " + user.nick + ", i got that,  but are you sure you want to guess that low? We're guessing GPM currently!");
	    			insertGuessIntoDB(user.nick.toLowerCase(), guessedValue);
	    			guessCount = guessCount + 1;
	    			return;
	    		}else{
	    			insertGuessIntoDB(user.nick.toLowerCase(), guessedValue);
	    			guessCount = guessCount + 1;
	    			return;
	    		}
    		}
		}
    	
    	public void insertGuessIntoDB(String username, int guess){
    		if(didThisUserAlreadyGuess(username.toLowerCase())){
    			String selectSQL = "UPDATE GUESSES SET VALUE = ? WHERE NAME = ?;";
    			PreparedStatement preparedStatement;
    			try {
    				preparedStatement = connection.prepareStatement(selectSQL);
    				preparedStatement.setInt(1, guess);
    				preparedStatement.setString(2, username.toLowerCase());
    				preparedStatement.executeUpdate();
    			} catch (SQLException e) {
    				System.out.println("We just failed at updating the guess of " + username + ".");
    				e.printStackTrace();
    			}
    		}else{
    			String selectSQL = "INSERT INTO GUESSES (NAME,VALUE) VALUES (?,?);";
    			PreparedStatement preparedStatement;
    			try {
    				preparedStatement = connection.prepareStatement(selectSQL);
    				preparedStatement.setString(1, username.toLowerCase());
    				preparedStatement.setInt(2, guess);
    				preparedStatement.executeUpdate();
    			} catch (SQLException e) {
    				System.out.println("We just failed at adding the guess of " + username + "to the Guess table?");
    				e.printStackTrace();
    			}
    		}
    	}
    	
    	public void deleteAllGuesses(){
    		String selectSQL = "Delete from GUESSES;";
			PreparedStatement preparedStatement;
			try {
				preparedStatement = connection.prepareStatement(selectSQL);
				preparedStatement.executeUpdate();
			} catch (SQLException e) {
				System.out.println("We just failed at deleting all guesses");
				e.printStackTrace();
			}
    	}
    	
    	public boolean didThisUserAlreadyGuess(String username){
    		username = username.toLowerCase();
    		String selectSQL = "SELECT COUNT(*) FROM GUESSES WHERE NAME = ?";
			PreparedStatement preparedStatement;
			try {
				preparedStatement = connection.prepareStatement(selectSQL);
				preparedStatement.setString(1, username.toLowerCase());
				ResultSet resultSet = preparedStatement.executeQuery();
    			if (resultSet.next()) {
    				if ( resultSet.getInt("COUNT(*)") == 1 ){
    					return true;
    				}
    				if ( resultSet.getInt("COUNT(*)") == 0 ){
    					return false;
    				}
    			}
			} catch (SQLException e) {
				System.out.println("We just failed at checking whether " + username + " already guessed.");
				e.printStackTrace();
			}
    		
			System.out.println("We had an error while checking whether " + username + " already guessed.");
			return false;
    	}
    	    	
    	public void startGuessing(User user){
        	if(guessingroundActive){
        		sendMessage(user.getChannel(), "Hey " + user.nick + ", there's already a round of guessing active. Please end this first with !result <number> or !abort it");
    			return;
       		}
        	sendMessage(user.getChannel(), "/subscribers");
        	sendMessage(user.getChannel(), "A new round of guessing has started. Viewers can now guess the outcome of the current game");
        	startBettingPhase();
        	whatIsGuessed = randInt(1, 4);
        	sendGuessPhaseStartMessage(whatIsGuessed, user);
        	sendMessage(user.getChannel(), "You can now use the command !guess <number> to make a guess. ");
        	writeLater(7, "/me Good luck, let's go!", user, guessroundID);
        	writeLater(8, "/subscribersoff", user, guessroundID);
        	writeLater(180, "/me 2 Minutes left to place your guess", user, guessroundID);
//        	180
        	writeLater(240, "/me 1 Minute left to place your guess", user, guessroundID);
//        	240
        	delayEndBettingPhase(301, guessroundID);
//        	301
        	sendGuessingLockedMessage(300, user, guessroundID);
//        	300
    	}
    	
    	public void startSpecificGuessing(User user){
        	if(guessingroundActive){
        		sendMessage(user.getChannel(), "Hey " + user.nick + ", there's already a round of guessing active. Please end this first with !result <number> or !abort it");
    			return;
       		}
        	sendMessage(user.getChannel(), "/subscribers");
        	sendMessage(user.getChannel(), "A new round of guessing has started. Viewers can now guess the outcome of the current game");
        	startBettingPhase();
        	sendGuessPhaseStartMessage(whatIsGuessed, user);
        	sendMessage(user.getChannel(), "You can now use the command !guess <number> to make a guess. ");
        	writeLater(7, "/me Good luck, let's go!", user, guessroundID);
        	writeLater(8, "/subscribersoff", user, guessroundID);
        	writeLater(180, "/me 2 Minutes left to place your guess", user, guessroundID);
//        	180
        	writeLater(240, "/me 1 Minute left to place your guess", user, guessroundID);
//        	240
        	delayEndBettingPhase(301, guessroundID);
//        	301
        	sendGuessingLockedMessage(300, user, guessroundID);
//        	300
    	}

    	public void sendGuessPhaseStartMessage(int whatIsGuessed, User user){
    		if(whatIsGuessed == 1){
    			sendMessage(user.getChannel(), "/me How often will VITALIC die?");
    		}else if(whatIsGuessed == 2){
    			sendMessage(user.getChannel(), "/me How many kills will VITALIC achieve?");
    		}else if(whatIsGuessed == 3){
    			sendMessage(user.getChannel(), "/me How much GPM will VITALIC achieve in this game? Every guess is allowed to be up to 25 points off of the result to be accepted as correct.");
    		}else if(whatIsGuessed == 4){
    			sendMessage(user.getChannel(), "/me How much XPM will VITALIC achieve in this game? Every guess is allowed to be up to 25 points off of the result to be accepted as correct.");
    		}
    	}
    	
    	public void startBettingPhase(){
    		guessLock = false;
    		guessCount = 0;
    		guessingroundActive = true;
    	}

    	public void endBettingPhase(){
    		guessLock = true;
    	}
    	
       	public void delayEndBettingPhase(int secondsToDelay, int idThatNeedsToStayTheSame){
	    	new java.util.Timer().schedule( 
	    	        new java.util.TimerTask() {
	    	            @Override
	    	            public void run() {
	    	            	if(idThatNeedsToStayTheSame == guessroundID){
	    	            		endBettingPhase();
	    	            	}
	    	            }
	    	        }, 
	    	        1000*secondsToDelay 
	    	);
    	}

       	//      -----Herorequest Related-----
       	
       	public void handleHerorequest(String[] words, User user){
       		if(herorequestActive){
       			sendMessage(user.getChannel(), "Sorry " + user.nick + ", there's already an active herorequest. Please wait until that one is finished.");
       			return;
       		}
       		Integer pointsOfRequestingUser = getPointsof(user.nick);
       		if(pointsOfRequestingUser < HEROREQUEST_PRICE){
       			sendMessage(user.getChannel(), "Sorry " + user.nick + ", herorequests cost " + HEROREQUEST_PRICE.toString() + " points. You've only got " + pointsOfRequestingUser.toString() + ".");
       			return;
       		}
       		
       		if(words.length == 2){
       			requestedHero = words[1];
       		}else{
       			requestedHero = words[1] + " " + words [2]; 
       		}
       		
       		herorequestActive = true;
       		removePointsFromUser(user.nick, HEROREQUEST_PRICE, user);
       		addRedeemedPointsTo(user, HEROREQUEST_PRICE);
       		
       		sendMessage(user.getChannel(), user.nick + " just requested " + requestedHero + " to be played. VITALIC_DOTA2 will play that hero in the next game");
       		herorequestReminder(user, herorequestID);
       	}
       	
       	public void herorequestReminder(User user, int IDThatNeedsToStayTheSame){
       		new java.util.Timer().schedule( 
	    	        new java.util.TimerTask() {
	    	            @Override
	    	            public void run() {
	    	            	if(IDThatNeedsToStayTheSame == herorequestID){
	    	            		sendMessage(user.getChannel(), "There's still an open herorequest from " + user.nick + ". He requested " + requestedHero + " to be played.");
	    	            		herorequestReminder(user, IDThatNeedsToStayTheSame);
	    	            	}
	    	            }
	    	        }, 
	    	        1000*600 
	    	);
       	}

       	public void handleHRDone(){
       		requestedHero = "";
       		herorequestActive = false;
       		herorequestID++;
       	}
       	
       	
       	//     -----ReplayAnalysis Related-----
       	
       	
       	public void handleReplayanalsyse(User user){
       		Integer pointsOfRequestingUser = getPointsof(user.nick);
       		if(pointsOfRequestingUser < REPLAYANALYSIS_PRICE){
       			sendMessage(user.getChannel(), "Sorry " + user.nick + ", a replay analysis costs " + REPLAYANALYSIS_PRICE.toString() + " points. You've only got " + pointsOfRequestingUser.toString() + ".");
       			return;
       		}
       		
       		removePointsFromUser(user.nick, REPLAYANALYSIS_PRICE, user);
       		addRedeemedPointsTo(user, REPLAYANALYSIS_PRICE);
       		
       		sendMessage(user.getChannel(), user.nick + " just requested a replay analysis. VITALIC_DOTA2 will contact you soon.");
       		replayAnalysisReminder(user, 3);
       		
//       		JFrame herorequestReminderWindow = new JFrame();
//       		herorequestReminderWindow.setName("Replayanalysis requested");
//       		herorequestReminderWindow.setSize(300, 100);
//       		JTextArea windowText = new JTextArea("Replayanalysis by " + user.nick);
//       		herorequestReminderWindow.add(windowText);
//       		
//       		herorequestReminderWindow.setVisible(true);
       	}
       	
       	public void replayAnalysisReminder(User user, int howOftenToRepeat){
       		new java.util.Timer().schedule( 
	    	        new java.util.TimerTask() {
	    	            @Override
	    	            public void run() {
	    	            		sendMessage(user.getChannel(), "VITALIC_DOTA2, " +  user.nick + " made a replay analysis request.");
	    	            		herorequestReminder(user, howOftenToRepeat-1);
	    	            }
	    	        }, 
	    	        1000*600 
	    	);
       	}
       	
       	public void handleItembundlerequest(User user){
       		Integer pointsOfRequestingUser = getPointsof(user.nick);
       		if(pointsOfRequestingUser < ITEMBUNDLE_PRICE){
       			sendMessage(user.getChannel(), "Sorry " + user.nick + ", an itembundle costs " + ITEMBUNDLE_PRICE.toString() + " points. You've only got " + pointsOfRequestingUser.toString() + ".");
       			return;
       		}
       		
       		removePointsFromUser(user.nick, ITEMBUNDLE_PRICE, user);
       		addRedeemedPointsTo(user, ITEMBUNDLE_PRICE);
       		
       		sendMessage(user.getChannel(), user.nick + " just requested an itembundle. VITALIC_DOTA2 will contact you soon.");
       		itembundleReminder(user, 3);
       		
//       		JFrame itembundleReminderWindow = new JFrame();
//       		itembundleReminderWindow.setName("itembundle requested");
//       		itembundleReminderWindow.setSize(300, 100);
//       		JTextArea windowText = new JTextArea("Itembundle requested by " + user.nick);
//       		itembundleReminderWindow.add(windowText);
//       		
//       		itembundleReminderWindow.setVisible(true);
       	}
       	
       	public void itembundleReminder(User user, int howOftenToRepeat){
       		new java.util.Timer().schedule( 
	    	        new java.util.TimerTask() {
	    	            @Override
	    	            public void run() {
	    	            		sendMessage(user.getChannel(), "VITALIC_DOTA2, " +  user.nick + " made an itembundle request.");
	    	            		herorequestReminder(user, howOftenToRepeat-1);
	    	            }
	    	        }, 
	    	        1000*600 
	    	);
       	}

       	//      -----MMR Related-----
       	
       	//    	-----MMR Related-----
    	public void updateSoloMMR(int newMMR){
    		String selectSQL = "UPDATE MMR SET VALUE = ? WHERE MMRTYPE = 'SOLO';";
			PreparedStatement preparedStatement;
			try {
				preparedStatement = connection.prepareStatement(selectSQL);
				preparedStatement.setInt(1, newMMR);
				preparedStatement.executeUpdate();
			} catch (SQLException e) {
				System.out.println("We just failed at updating the solo MMR");
				e.printStackTrace();
			}
			
			if(getSoloMMRMax() < newMMR){
				updateSoloMMRMax(newMMR);
			}
    	}
    	
       	public void updateSoloMMRMax(int newMMR){
    		String selectSQL = "UPDATE MMR SET VALUE = ? WHERE MMRTYPE = 'SOLOMAX';";
			PreparedStatement preparedStatement;
			try {
				preparedStatement = connection.prepareStatement(selectSQL);
				preparedStatement.setInt(1, newMMR);
				preparedStatement.executeUpdate();
			} catch (SQLException e) {
				System.out.println("We just failed at updating the solo MMRMAX");
				e.printStackTrace();
			}
    	}
    	
    	public void updatePartyMMR(int newMMR){
    		String selectSQL = "UPDATE MMR SET VALUE = ? WHERE MMRTYPE = 'PARTY';";
			PreparedStatement preparedStatement;
			try {
				preparedStatement = connection.prepareStatement(selectSQL);
				preparedStatement.setInt(1, newMMR);
				preparedStatement.executeUpdate();
			} catch (SQLException e) {
				System.out.println("We just failed at updating the party MMR");
				e.printStackTrace();
			}
			
			if(getPartyMMRMax() < newMMR){
				updatePartyMMRMax(newMMR);
			}
    	}
    	
    	public void updatePartyMMRMax(int newMMR){
    		String selectSQL = "UPDATE MMR SET VALUE = ? WHERE MMRTYPE = 'PARTYMAX';";
			PreparedStatement preparedStatement;
			try {
				preparedStatement = connection.prepareStatement(selectSQL);
				preparedStatement.setInt(1, newMMR);
				preparedStatement.executeUpdate();
			} catch (SQLException e) {
				System.out.println("We just failed at updating the party MMRMAX");
				e.printStackTrace();
			}
    	}
    	
    	public int getSoloMMR(){
    		String selectSQL = "SELECT VALUE FROM MMR WHERE MMRTYPE = 'SOLO'";
			PreparedStatement preparedStatement;
			try {
				preparedStatement = connection.prepareStatement(selectSQL);
				ResultSet resultSet = preparedStatement.executeQuery();
    			if (resultSet.next()) {
    				return resultSet.getInt(1);
    			}else{
    				return 0;
    			}
			} catch (SQLException e) {
				System.out.println("We just failed at retrieving the solo mmr.");
				e.printStackTrace();
			}
			return 9000;
    	}
    	
    	public int getPartyMMR(){
    		String selectSQL = "SELECT VALUE FROM MMR WHERE MMRTYPE = 'PARTY'";
			PreparedStatement preparedStatement;
			try {
				preparedStatement = connection.prepareStatement(selectSQL);
				ResultSet resultSet = preparedStatement.executeQuery();
    			if (resultSet.next()) {
    				return resultSet.getInt(1);
    			}else{
    				return 0;
    			}
			} catch (SQLException e) {
				System.out.println("We just failed at retrieving the party mmr.");
				e.printStackTrace();
			}
			return 9000;
    	}
    	
    	public int getSoloMMRMax(){
    		String selectSQL = "SELECT VALUE FROM MMR WHERE MMRTYPE = 'SOLOMAX'";
			PreparedStatement preparedStatement;
			try {
				preparedStatement = connection.prepareStatement(selectSQL);
				ResultSet resultSet = preparedStatement.executeQuery();
    			if (resultSet.next()) {
    				return resultSet.getInt(1);
    			}else{
    				return 0;
    			}
			} catch (SQLException e) {
				System.out.println("We just failed at retrieving the solo mmrmax.");
				e.printStackTrace();
			}
			return 9000;
    	}

    	public int getPartyMMRMax(){
    		String selectSQL = "SELECT VALUE FROM MMR WHERE MMRTYPE = 'PARTYMAX'";
			PreparedStatement preparedStatement;
			try {
				preparedStatement = connection.prepareStatement(selectSQL);
				ResultSet resultSet = preparedStatement.executeQuery();
    			if (resultSet.next()) {
    				return resultSet.getInt(1);
    			}else{
    				return 0;
    			}
			} catch (SQLException e) {
				System.out.println("We just failed at retrieving the party mmrmax.");
				e.printStackTrace();
			}
			return 9000;
    	}
    	
    	private void handleMMRUpdate(User user, String[] words) {
			if(words[1].equalsIgnoreCase("solo")){
				if(isThisANumber(words[2])){
					updateSoloMMR(Integer.parseInt(words[2]));
					return;
				}else{
					sendMessage(user.getChannel(), "That's no number mate...");
				}
			}
			if(words[1].equalsIgnoreCase("party")){
				if(isThisANumber(words[2])){
					updatePartyMMR(Integer.parseInt(words[2]));
				}else{
					sendMessage(user.getChannel(), "That's no number mate...");
				}
			}
			if(words[1].equalsIgnoreCase("solopeak")){
				if(isThisANumber(words[2])){
					updateSoloMMRMax(Integer.parseInt(words[2]));
					return;
				}else{
					sendMessage(user.getChannel(), "That's no number mate...");
				}
			}
			if(words[1].equalsIgnoreCase("partypeak")){
				if(isThisANumber(words[2])){
					updatePartyMMRMax(Integer.parseInt(words[2]));
					return;
				}else{
					sendMessage(user.getChannel(), "That's no number mate...");
				}
			}
			return;
		}

    	//    	-----General Helpers-----
    	@SuppressWarnings("unused")
		public boolean isThisANumber(String stringToTest){
    		try  
    		{ 
    			double d = Double.parseDouble(stringToTest);  
    		}  
    		catch(NumberFormatException nfe)  
    		{  
    			return false;  
    		}  
    		return true;  
    	}
    	
    	/**
    	 * Returns a pseudo-random number between min and max, inclusive.
    	 * The difference between min and max can be at most
    	 * <code>Integer.MAX_VALUE - 1</code>.
    	 *
    	 * @param min Minimum value
    	 * @param max Maximum value.  Must be greater than min.
    	 * @return Integer between min and max, inclusive.
    	 * @see java.util.Random#nextInt(int)
    	 */
    	public int randInt(int min, int max) {
    	    Random rand = new Random();
    	    int randomNum = rand.nextInt((max - min) + 1) + min;
    	    return randomNum;
    	}
    	
    	public void sendGuessingLockedMessage(int secondsToDelay, User auftraggeber, int idThatNeedsToStayTheSame){
	    	new java.util.Timer().schedule( 
	    	        new java.util.TimerTask() {
	    	            @Override
	    	            public void run() {
	    	            	if(idThatNeedsToStayTheSame == guessroundID){
	    	            		sendMessage(auftraggeber.getChannel(), "/me guessing is now locked. We had " + guessCount + " valid guesses this round. Good luck to everybody!");
	    	            	}
	    	            }
	    	        }, 
	    	        1000*secondsToDelay 
	    	);
    	}
       	public void writeLater(int secondsToDelay, String textToSend, User auftraggeber, int idThatNeedsToStayTheSame){
	    	new java.util.Timer().schedule( 
	    	        new java.util.TimerTask() {
	    	            @Override
	    	            public void run() {
	    	            	if(idThatNeedsToStayTheSame == guessroundID){
	    	            		sendMessage(auftraggeber.getChannel(), textToSend);
	    	            	}
	    	            }
	    	        }, 
	    	        1000*secondsToDelay 
	    	);
    	}
       	
    	//    	-----Points Related-----
    	public void pointsRequestedBy(User user){
    		sendMessage(user.getChannel(), "Hey " + user.nick + ". You've got " + getPointsof(user.nick) + " points.");
    		return;
    	}
    	
    	public void pointsRequestedOf(String username, User theOneWhoAsked){
    		sendMessage(theOneWhoAsked.getChannel(), "Well,  " + username + " got " + getPointsof(username) + " points.");
    		return;
    	}
    	  	
    	public int getPointsof(String username){
    		String selectSQL = "SELECT POINTS FROM USERS WHERE NAME = ?";
			PreparedStatement preparedStatement;
			try {
				preparedStatement = connection.prepareStatement(selectSQL);
				preparedStatement.setString(1, username.toLowerCase());
				ResultSet resultSet = preparedStatement.executeQuery();
    			if (resultSet.next()) {
    				return resultSet.getInt(1);
    			}else{
    				return 0;
    			}
			} catch (SQLException e) {
				System.out.println("We just failed at retrieving the points of user " + username + ". This might be due to problems with the database. Null maybe?");
				e.printStackTrace();
			}
			return 0;
    	}

    	public int getRedeemedPointsof(String username){
    		String selectSQL = "SELECT REDEEMEDPOINTS FROM USERS WHERE NAME = ?";
			PreparedStatement preparedStatement;
			try {
				preparedStatement = connection.prepareStatement(selectSQL);
				preparedStatement.setString(1, username.toLowerCase());
				ResultSet resultSet = preparedStatement.executeQuery();
    			if (resultSet.next()) {
    				return resultSet.getInt(1);
    			}else{
    				return 0;
    			}
			} catch (SQLException e) {
				System.out.println("We just failed at retrieving the redeemed points of user " + username + ". This might be due to problems with the database. Null maybe?");
				e.printStackTrace();
			}
			return 0;
    	}
    	
    	public void removePointsFromUser(String username, int pointsToRemove, User userThatWantsThat){
    		if(isThisUserAlreadyInTheDB(username)){
    			int currentPoints = getPointsof(username);
    			String selectSQL = "UPDATE USERS SET POINTS = ? WHERE NAME = ?;";
    			PreparedStatement preparedStatement;
    			try {
    				preparedStatement = connection.prepareStatement(selectSQL);
    				preparedStatement.setInt(1, currentPoints - pointsToRemove);
    				preparedStatement.setString(2, username.toLowerCase());
    				preparedStatement.executeUpdate();
    			} catch (SQLException e) {
    				System.out.println("We just failed at retrieving the points of user " + username + ". This might be due to problems with the database. Null maybe?");
    				e.printStackTrace();
    			}
    		}else{
    			sendMessage(userThatWantsThat.getChannel(), "Hey " + userThatWantsThat.nick + ", please tell me how to remove something that was never there...");
    		}
    	}
    	
    	public void addPointsToUser(String username, int pointsToAdd){
    		if(isThisUserAlreadyInTheDB(username)){
    			int currentPoints = getPointsof(username);
    			String selectSQL = "UPDATE USERS SET POINTS = ? WHERE NAME = ?;";
    			PreparedStatement preparedStatement;
    			try {
    				preparedStatement = connection.prepareStatement(selectSQL);
    				preparedStatement.setInt(1, currentPoints + pointsToAdd);
    				preparedStatement.setString(2, username.toLowerCase());
    				preparedStatement.executeUpdate();
    			} catch (SQLException e) {
    				System.out.println("We just failed at retrieving the points of user " + username + ". This might be due to problems with the database. Null maybe?");
    				e.printStackTrace();
    			}
    		}else{
    			String selectSQL = "INSERT INTO USERS (NAME,POINTS,REDEEMEDPOINTS) VALUES (?,?,?);";
    			PreparedStatement preparedStatement;
    			try {
    				preparedStatement = connection.prepareStatement(selectSQL);
    				preparedStatement.setString(1, username.toLowerCase());
    				preparedStatement.setInt(2, pointsToAdd);
    				preparedStatement.setInt(3, 0);
    				preparedStatement.executeUpdate();
    			} catch (SQLException e) {
    				System.out.println("We just failed at retrieving the points of user " + username + ". This might be due to problems with the database. Null maybe?");
    				e.printStackTrace();
    			}
    		}
    	}
    	
    	public boolean isThisUserAlreadyInTheDB(String username){
    		username = username.toLowerCase();
    		String selectSQL = "SELECT COUNT(*) FROM USERS WHERE NAME = ?";
			PreparedStatement preparedStatement;
			try {
				preparedStatement = connection.prepareStatement(selectSQL);
				preparedStatement.setString(1, username.toLowerCase());
				ResultSet resultSet = preparedStatement.executeQuery();
    			if (resultSet.next()) {
    				if ( resultSet.getInt("COUNT(*)") == 1 ){
    					return true;
    				}
    				if ( resultSet.getInt("COUNT(*)") == 0 ){
    					return false;
    				}
    			}
			} catch (SQLException e) {
				System.out.println("We just failed at checking whether " + username + " is in the Database already.");
				e.printStackTrace();
			}
    		
			System.out.println("we had an error while checking whether " + username + " exists in the database.");
			return false;
    	}
    	
    	public void handleAddOrRemovePoints(String[] words, User user){
    		if(!isThisANumber(words[3])){
    			sendMessage(user.getChannel(), "You need to use this command as follows: !points <add|remove> <username> <points>. " + words[3] + " should have been a number");
    			return;
    		}
    		String userToProcess = words[2];
    		int pointsToAddOrRemove = Integer.parseInt(words[3]);
    		
    		if(words[1].equals("add")){
    				addPointsToUser(userToProcess, pointsToAddOrRemove);
    				sendMessage(user.getChannel(), "Okay, " + userToProcess + " got " + getPointsof(userToProcess) + " now.");
    		}else if (words[1].equals("remove")){
    				removePointsFromUser(userToProcess, pointsToAddOrRemove, user);
    		}else{
    			sendMessage(user.getChannel(), "You need to use this command as follows: !points <add|remove> <username> <points>");
    		}
    	}
    	
    	public void addRedeemedPointsTo(User user, int pointsToAdd){
    		int currentPoints = getRedeemedPointsof(user.nick);
			String selectSQL = "UPDATE USERS SET REDEEMEDPOINTS = ? WHERE NAME = ?;";
			PreparedStatement preparedStatement;
			try {
				preparedStatement = connection.prepareStatement(selectSQL);
				preparedStatement.setInt(1, currentPoints + pointsToAdd);
				preparedStatement.setString(2, user.nick.toLowerCase());
				preparedStatement.executeUpdate();
			} catch (SQLException e) {
				System.out.println("We just failed at adding some points to redeemed points of " + user.nick );
				e.printStackTrace();
			}
    	}
    
    }
