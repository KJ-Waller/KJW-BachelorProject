package tracks.singlePlayer;

import java.util.Random;

import core.logging.Logger;
import tools.Utils;
import tracks.ArcadeMachine;

/**
 * Created with IntelliJ IDEA. User: Diego Date: 04/10/13 Time: 16:29 This is a
 * Java port from Tom Schaul's VGDL - https://github.com/schaul/py-vgdl
 */
public class Test {

    public static void main(String[] args) {

		// Available tracks:
		String sampleRandomController = "tracks.singlePlayer.simple.sampleRandom.Agent";
		String doNothingController = "tracks.singlePlayer.simple.doNothing.Agent";
		String sampleOneStepController = "tracks.singlePlayer.simple.sampleonesteplookahead.Agent";
		String sampleFlatMCTSController = "tracks.singlePlayer.simple.greedyTreeSearch.Agent";

		String sampleMCTSController = "tracks.singlePlayer.advanced.sampleMCTS.Agent";
        String sampleRSController = "tracks.singlePlayer.advanced.sampleRS.Agent";
        String sampleRHEAController = "tracks.singlePlayer.advanced.sampleRHEA.Agent";
		String sampleOLETSController = "tracks.singlePlayer.advanced.olets.Agent";
		String sampleOMCTSController = "tracks.singlePlayer.mrtndwrd.Agent";
		String samplepddOMCTSController = "tracks.singlePlayer.pddOmcts.Agent";
		String[] controllers = {sampleMCTSController, sampleOMCTSController, samplepddOMCTSController};

		//Load available games
//		String spGamesCollection =  "examples/all_games_sp.csv";
//		String[][] games = Utils.readGames(spGamesCollection);

		//CIG 2014 Validation Set Games
//		String games[] = new String[]{"camelRace", "digdug", "firestorms", "infection", "firecaster",
//				"overload", "pacman", "seaquest", "whackamole", "eggomania", "zelda"};


		String games[] = new String[]{"surround", "infection", "butterflies", "missilecommand", "aliens", "plaqueattack", "plants", "bait", "camelRace", "survivezombies", "seaquest",
				"jaws","frogs","pacman","firestorms","lemmings","boulderdash","overload","boloadventures","roguelike","firecaster","boulderchase","zelda","chase","digdug","eggomania","portals"};

		//Game and level to play
		int gameIdx = 4;
		int levelIdx = 4; //level names from 0 to 4 (game_lvlN.txt).
		String game = "examples/gridphysics/" + games[gameIdx] + ".txt";
		String level1 = "examples/gridphysics/" + games[gameIdx] + "_lvl" + levelIdx +".txt";

		//Game settings
		boolean visuals = true;
		int seed = new Random().nextInt();

		// Game and level to play
//		int gameIdx = 0;
//		int levelIdx = 4; // level names from 0 to 4 (game_lvlN.txt).
//		String gameName = games[gameIdx][1];
//		String game = games[gameIdx][0];
//		String level1 = game.replace(gameName, gameName + "_lvl" + levelIdx);

		String recordActionsFile = null;// "actions_" + games[gameIdx] + "_lvl"
						// + levelIdx + "_" + seed + ".txt";
						// where to record the actions
						// executed. null if not to save.

		// 1. This starts a game, in a level, played by a human.
//		ArcadeMachine.playOneGame(game, level1, recordActionsFile, seed);

		// 2. This plays a game in a level by the controller.
//		ArcadeMachine.runOneGame(game, level1, visuals, samplepddOMCTSController, recordActionsFile, seed, 0);

		// 2.1 This plays all games once for the selected controller
//		String level;
//
//		for (int i = 0; i < games.length; i++) {
//			game = "examples/gridphysics/" + games[i] + ".txt";
//			level = "examples/gridphysics/" + games[i] + "_lvl" + levelIdx + ".txt";
//			ArcadeMachine.runGames(game, new String[]{level}, 1, samplepddOMCTSController, null);
//		}

		// 2.2 This plays all selected games from OMCTS paper n times
//		int num_games = 10;
//		int lvl = 0;
//		for (int i = 0; i < games.length; i++) {
//			String curr_game = "examples/gridphysics/" + games[i] + ".txt";
//			String lvl_path = "examples/gridphysics/" + games[i] + "_lvl" + lvl +".txt";
//			ArcadeMachine.runGames(curr_game, new String[]{lvl_path}, num_games, sampleOMCTSController, null);
//		}


		// 3. This replays a game from an action file previously recorded
	//	 String readActionsFile = recordActionsFile;
	//	 ArcadeMachine.replayGame(game, level1, visuals, readActionsFile);

		// 4. This plays a single game, in N levels, M times :
//		String level2 = new String(game).replace(gameName, gameName + "_lvl" + 1);
//		int M = 10;
//		for(int i=0; i<games.length; i++){
//			game = games[i][0];
//			gameName = games[i][1];
//			level1 = game.replace(gameName, gameName + "_lvl" + levelIdx);
//			ArcadeMachine.runGames(game, new String[]{level1}, M, samplepddOMCTSController, null);
//		}

		//5. This plays N games, in the first L levels, M times each. Actions to file optional (set saveActions to true).
//		int N = games.length, L = 1, M = 2;
//		String gameName;
//		boolean saveActions = false;
//		String[] levels = new String[L];
//		String[] actionFiles = new String[L*M];
//		for(int i = 0; i < N; ++i)
//		{
//			int actionIdx = 0;
//			game = "examples/gridphysics/" + games[i] + ".txt";;
//			gameName = games[i];
//			for(int j = 0; j < L; ++j){
//				levels[j] = game.replace(gameName, gameName + "_lvl" + j);
//				if(saveActions) for(int k = 0; k < M; ++k)
//				actionFiles[actionIdx++] = "actions_game_" + i + "_level_" + j + "_" + k + ".txt";
//			}
//			ArcadeMachine.runGames(game, levels, M, sampleOMCTSController, saveActions? actionFiles:null);
//		}

		//6. This plays N games, in the first L levels, M times each, for all controllers in controllers array. Actions to file optional (set saveActions to true).
		int N = games.length, L = 5, M = 20;
		String gameName;
		String controller;
		boolean saveActions = false;
		String[] levels = new String[L];
		String[] actionFiles = new String[L*M];
		for (int l = 0; l < controllers.length; l++) {
			controller = controllers[l];
			System.out.println("Conteroller: " + controller);

			for(int i = 0; i < N; ++i)
			{
				int actionIdx = 0;
				game = "examples/gridphysics/" + games[i] + ".txt";;
				gameName = games[i];
				System.out.println("Game played: " + gameName);
				for(int j = 0; j < L; ++j){
					levels[j] = game.replace(gameName, gameName + "_lvl" + j);
					if(saveActions) for(int k = 0; k < M; ++k)
						actionFiles[actionIdx++] = "actions_game_" + i + "_level_" + j + "_" + k + ".txt";
				}
				System.out.println("Level: " + levels);
				ArcadeMachine.runGames(game, levels, M, controller, saveActions? actionFiles:null);
			}
		}


    }
}
