// This Java API uses camelCase instead of the snake_case as documented in the API docs.
//     Otherwise the names of methods are consistent.

import hlt.*;

import java.util.ArrayList;
import java.util.Random;
import java.util.*;

public class MyBot {
    public static void main(final String[] args) {
        final long rngSeed;
        if (args.length > 1) {
            rngSeed = Integer.parseInt(args[1]);
        } else {
            rngSeed = System.nanoTime();
        }
        final Random rng = new Random(rngSeed);

        Game game = new Game();
        ArrayList<Stack<Direction>> history = new ArrayList<>();  
        ArrayList<Boolean> back = new ArrayList<>();
        // At this point "game" variable is populated with initial map data.
        // This is a good place to do computationally expensive start-up pre-processing.
        // As soon as you call "ready" function below, the 2 second per turn timer will start.
        game.ready("MyJavaBot");

        Log.log("Successfully created bot! My Player ID is " + game.myId + ". Bot rng seed is " + rngSeed + ".");

        
        game.updateFrame();
	//Adaugam primul bot
        final ArrayList<Command> commandQueue_start = new ArrayList<>();
        commandQueue_start.add(game.me.shipyard.spawn());        
	back.add(new Boolean(false));
        history.add(new Stack<Direction>());

        game.endTurn(commandQueue_start);
        
        int coord_x=0,coord_y=0;
        float max;
        for (;;) {
            game.updateFrame();
             Player me = game.me;
             GameMap gameMap = game.gameMap;
            final ArrayList<Command> commandQueue = new ArrayList<>();                   

            for (final Ship ship : me.ships.values()) {
		//Calculam cea mai buna pozitie               
		max = 0;
                for(int y = 0; y < gameMap.height;++y)
                    for (int x = 0; x < gameMap.width; ++x) {
                            if(gameMap.calculateDistance(new Position(x,y),ship.position)!=0 && gameMap.cells[y][x].halite>Constants.MAX_HALITE/5)
                            if(gameMap.cells[y][x].halite/gameMap.calculateDistance(new Position(x,y),ship.position) > max) {
                                max = gameMap.cells[y][x].halite/gameMap.calculateDistance(new Position(x,y),ship.position);
                                coord_x = x; coord_y = y; 
				//coord celulei cu cel mai bun raport le retin in coord_x si coord_y
                            }
                    }
		//daca vaporul e full, trebuie sa ma intorc la baza
                if(ship.isFull())
                    back.set(ship.id.id, new Boolean(true));

		//daca a ajuns la baza sterg istoricul si setez back pe false
                if(ship.position.x == me.shipyard.position.x && ship.position.y == me.shipyard.position.y) {
                    history.get(ship.id.id).clear();
                    back.set(ship.id.id, new Boolean(false));
                    }

		//daca back este false inseamna ca el colecteaza halite in etapa asta
                if(back.get(ship.id.id).equals(new Boolean(false))) {

		    //verifica daca e necesar sa mearga la alta celula sa colecteze halite
                    if (gameMap.at(ship).halite < Constants.MAX_HALITE / 11 && ship.halite >= gameMap.at(ship).halite/10 || ship.position.x == me.shipyard.position.x && ship.position.y == me.shipyard.position.y) {

			//vezi ce directie trebuie sa faci ca sa ajungi la celula aia
                        final Direction randomDirection = gameMap.naiveNavigate(ship, new Position(coord_x,coord_y),back);

			//daca urmatoarea celula in care ajung are halite>=500 iar in vapor am deja 800 de halite, ma intorc in baza sa depun
			//si ma intorc cu comanda din istoric
                        if(gameMap.at(ship.position.directionalOffset(randomDirection)).halite>=500 && ship.halite>=800) {
                            back.set(ship.id.id, new Boolean(true));

				//daca functia naiveNavigate imi da sa stau still in urma incercarii de a ma deplasa cu ultima comanda din istoric
                                if(gameMap.naiveNavigate(ship, new Position(ship.position.x,ship.position.y).directionalOffset(history.get(ship.id.id).peek()),back)!=Direction.STILL)
                                	commandQueue.add(ship.move(history.get(ship.id.id).pop()));
				 	//prin codul de pe linia anterioara ma deplasez cu ultima comanda
                            	else commandQueue.add(ship.stayStill()); //<-stau still
                        }
			else {   
			//else-ul asta e de la if-ul cu 500 si 800
                        	if(randomDirection!=Direction.STILL) //daca comanda  nu e still atunci adauga opusul directiei in istoric pt a sti cum ma
                                	history.get(ship.id.id).push(randomDirection.invertDirection()); //intorc in baza cand va trebui
                        	commandQueue.add(ship.move(randomDirection)); //indiferent daca directia e still sau nu o da ca si comanda
                        }
                    }
		    else 
			//acesta e else-ul if-ului care verifica daca se merita sa mearga la alta celula sau nu
                        commandQueue.add(ship.stayStill());
                }
                else {
		//aici sunt pe ramura in care back este true, deci se intoarce la baza pe unde a venit, folosind comenzile din istoric 
                    	//daca functia naiveNavigate imi da sa stau still in urma incercarii de a ma deplasa cu ultima comanda din istoric
			if(gameMap.naiveNavigate(ship, new Position(ship.position.x,ship.position.y).directionalOffset(history.get(ship.id.id).peek()),back)!=Direction.STILL)   
                        	commandQueue.add(ship.move(history.get(ship.id.id).pop())); //ma deplasez cu ultima comanda din istoric
                    	else 
				commandQueue.add(ship.stayStill()); //stau still
                }
            }	// aici se termina for-ul ce parcurge vapoarele

	   //daca sunt la turul 4 sau 8 adaug un nou vapor cu un nou back si un nou istoric pt vapor
            if(game.turnNumber==4 || game.turnNumber==8) { 
                commandQueue.add(me.shipyard.spawn());
                back.add(new Boolean(false));
                history.add(new Stack<Direction>());
            }
            game.endTurn(commandQueue);
        }
    }
}