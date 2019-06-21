package tracks.singlePlayer.mrtndwrd;

import core.game.StateObservation;
import tools.ElapsedCpuTimer;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.Random;

/**
 * Created with IntelliJ IDEA.
 * User: Diego
 * Date: 07/11/13
 * Time: 17:13
 */
public class SingleMCTSPlayer
{
	/**
	 * Root of the tree.
	 */
	private SingleTreeNode rootNode;

	/**
	 * Random generator.
	 */
	private Random random;

	/**
	 * Creates the MCTS player with a sampleRandom generator object.
	 * @param random sampleRandom generator object.
	 */
	public SingleMCTSPlayer(Random random)
	{
		this.random = random;
	}

	/**
	 * Inits the tree with the new observation state in the root.
	 * @param gameState current state of the game.
	 */
	public void init(StateObservation gameState, ArrayList<Option> possibleOptions, Option currentOption)
	{
		//Set the game observation to a new root node.
		rootNode = new SingleTreeNode(possibleOptions, random, currentOption);
		rootNode.state = gameState;
	}

	/**
	 * Runs MCTS to decide the action to take. It does not reset the tree.
	 * @param elapsedTimer Timer when the action returned is due.
	 * @return the action to execute in the game.
	 */
	public int run(ElapsedCpuTimer elapsedTimer)
	{
		//Do the search within the available time.
		rootNode.mctsSearch(elapsedTimer);
		//Determine the best action to take and return it.
		// TODO: Compare with mostVisitedAction
		int action;
		if(Agent.NAIVE_PLANNING)
			action = rootNode.bestActionNaive();
		else
			action = rootNode.bestActionLearning();
		//int action = rootNode.mostVisitedAction();
		return action;
	}

	public String printRootNode()
	{
		return rootNode.toString();
	}
}
