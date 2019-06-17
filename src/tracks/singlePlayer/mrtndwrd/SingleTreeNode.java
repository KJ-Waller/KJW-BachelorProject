package tracks.singlePlayer.mrtndwrd;

import core.game.StateObservation;
import ontology.Types;
import tools.ElapsedCpuTimer;
import tools.Utils;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Random;
import java.lang.Math;

@SuppressWarnings("unchecked")
public class SingleTreeNode
{
	private static final double HUGE_NEGATIVE = -10000000.0;

	private static final double HUGE_POSITIVE =  10000000.0;

	/** mctsSearch continues until there are only so many miliseconds left */
	public static final int REMAINING_LIMIT = 15;

	/** Decides whether to use the mean reward, or the mean value of a root node
	 * for the optionRanking. Mean reward works well in games where 1 option is
	 * the best option. In games where a combination of options has to be used,
	 * this option should be false */
	public static boolean USE_MEAN_REWARD = true;

	/** Decides weather rollouts are done at random, or following options */
	public static boolean RANDOM_ROLLOUT = false;

	/** Decides after how many node visits exploration using Crazy Stone's
	 * algorithm is stopped and exploitation using UCT starts */
	public static int UCT_START_VISITS = 40;

	public static double STEEPNESS = 0.5;

	public static double epsilon = 1e-6;

	public StateObservation state;

	public SingleTreeNode parent;

	public SingleTreeNode[] children;

	public double totValue;

	public int nVisits;

	/** If this is true, the "currentOption" has finished somewhere in the tree
	 * under this node. That means we'll update its values */
	public boolean optionFinished = false;

	private ArrayList<Option> possibleOptions;

	/** When this is true, this node is new and should be backed up */
	private boolean expanded; 

	/** The option that is chosen in this node. This option is followed until it
	 * is finished, thereby representing a specific subtree in the whole */
	private Option chosenOption;

	/** The option that the agent is already following. This gets an extra
	 * score. Only the root node has this as not null */
	private Option agentOption;

	public static Random random;
	/** The depth in the rollout of this node (initialized as parent.node+1) */
	public int nodeDepth;

	/** Depth reached by final rollout */
	public int rollOutDepth;

	private boolean chosenOptionFinished;

	/** The value of the best possible option in the optionRanking */
	private double mu0;
	private double muLast;
	/** The value of the best possible option in the optionRankingVariance */
	private double sigma0;

	//protected static double[] bounds = 
	//	new double[]{Double.MAX_VALUE, -Double.MAX_VALUE};

	/** Root node constructor */
	public SingleTreeNode(ArrayList<Option> possibleOptions, Random rnd, Option currentOption)
	{
		this(null, null, null, possibleOptions, rnd);
		this.agentOption = currentOption;
	}

	/** normal constructor */
	public SingleTreeNode(StateObservation state, 
			SingleTreeNode parent, 
			Option chosenOption, 
			ArrayList<Option> possibleOptions, 
			Random rnd)
	{
		this.state = state;
		this.parent = parent;
		this.random = rnd;
		this.possibleOptions = possibleOptions;
		this.chosenOption = chosenOption;
		this.expanded = true;

		// Sort possible options by optionRanking:
		if(!Agent.NAIVE_PLANNING)
		{
			Collections.sort(this.possibleOptions, Option.optionComparator);

			// Set mu0 to the value of the best option in the optionRanking
			this.mu0 = Agent.optionRanking.get(
					this.possibleOptions.get(0).getType());
			this.muLast = Agent.optionRanking.get(this.possibleOptions.get(this.possibleOptions.size()-1).getType());
			this.sigma0 = Agent.optionRankingVariance.get(
					this.possibleOptions.get(0).getType());
		}

		// Create the possibility of chosing new options
		if(chosenOption == null || chosenOption.isFinished(state))
		{
			this.chosenOptionFinished = true;
			// FIXME: Should this be removed or not!??!?!!
			// Update the option ranking if needed
			if(chosenOption != null && chosenOption.isFinished(state))
			{
				if(this.USE_MEAN_REWARD)
					chosenOption.updateOptionRanking();
				else if(parent != null)
					parent.setOptionFinished(chosenOption);
			}
			children = new SingleTreeNode[possibleOptions.size()];
		}
		// The only child is the continuation of this option.
		else
		{
			this.chosenOptionFinished = false;
			children = new SingleTreeNode[1];
		}

		totValue = 0.0;
		if(parent != null)
			nodeDepth = parent.nodeDepth+1;
		else
			nodeDepth = 0;
	}

	public void mctsSearch(ElapsedCpuTimer elapsedTimer) 
	{
		double avgTimeTaken = 0;
		double acumTimeTaken = 0;
		long remaining = elapsedTimer.remainingTimeMillis();
		int numIters = 0;
		ElapsedCpuTimer elapsedTimerIteration;
		while(remaining > 3 * avgTimeTaken && remaining > REMAINING_LIMIT)
		{
			elapsedTimerIteration = new ElapsedCpuTimer();

			// Select the node to explore (either expanding unexpanded node, or
			// selecting the best one with UCT or crazyStone)
			//System.out.printf("Remaining before treePolicy: %d\n", elapsedTimer.remainingTimeMillis());
			SingleTreeNode selected; 
			if(Agent.NAIVE_PLANNING)
				selected = treePolicyNaive();
			else
				selected = treePolicyCrazyStone();

			// Get node value using a max-depth rollout
			//System.out.printf("Remaining before rollOut: %d\n", elapsedTimer.remainingTimeMillis());
			double delta = selected.rollOut();

			//System.out.printf("Remaining before rollOut: %d\n", elapsedTimer.remainingTimeMillis());
			// Set values for parents of current node, using new rollout value
			backUp(selected, delta, selected.rollOutDepth);
			//System.out.printf("Remaining after backUp: %d\n", elapsedTimer.remainingTimeMillis());

			numIters++;
			acumTimeTaken += (elapsedTimerIteration.elapsedMillis()) ;

			avgTimeTaken = acumTimeTaken/numIters;
			remaining = elapsedTimer.remainingTimeMillis();

		}
		// This is the rootnode, because where else would this function be
		// called!?!! So here we call setCumulativeRewardsForChildren, if needed
		if(!Agent.NAIVE_PLANNING)
			setCumulativeRewardsForChildren();
	}

	/** For each child, set its chosen options cumulative reward to the totValue
	 * of that child node. */
	public void setCumulativeRewardsForChildren()
	{
		// This isn't needed if USE_MEAN_REWARD is true, the reward is then set
		// all over this file
		if(SingleTreeNode.USE_MEAN_REWARD)
			return;
		Option o;
		for(SingleTreeNode c : this.children)
		{
			if(c != null)
			{
				if(c.optionFinished)
				{
					o = c.getChosenOption();
					o.setCumulativeReward(c.totValue/(c.nVisits + this.epsilon));
					o.updateOptionRanking();
					//System.out.println("Setting value for option " + o + " to " + c.totValue / (c.nVisits + this.epsilon));
				}
			}
		}
	}

	/** Expand the current treenode, if it's not fully expanded. Else, return
	 * the best node using uct
	 */
	public SingleTreeNode treePolicyCrazyStone() 
	{
		SingleTreeNode cur = this;
		SingleTreeNode next;
		while (!cur.state.isGameOver() && cur.nodeDepth < Agent.ROLLOUT_DEPTH)
		{
			if(cur.nVisits < UCT_START_VISITS)
			{
				next = cur.crazyStone();
				// If we have expanded, return the new node for rollouts
				if(next.expanded)
				{
					// Also reset expanded to false
					next.expanded = false;
					return next;
				}
			}
			// Else: continue with this node
			else
			{
				next = cur.uct();
			}
			cur = next;
		}
		return cur;
	}

	public SingleTreeNode treePolicyNaive()
	{
		SingleTreeNode cur = this;
		SingleTreeNode next;
		while (!cur.state.isGameOver() && cur.nodeDepth < Agent.ROLLOUT_DEPTH)
		{
			if (cur.notFullyExpanded())
			{
				return cur.expand();
			}
			// Else: continue with this node
			else
			{
				next = cur.uct();
			}
			cur = next;
		}
		return cur;
	}

	public SingleTreeNode expand() 
	{
		int bestOption = 0;
		double bestValue = -1;
		for (int i = 0; i < children.length; i++) 
		{
			double x = random.nextDouble();
			if (x > bestValue && children[i] == null) 
			{
				bestOption = i;
				bestValue = x;
			}
		}
		Option nextOption = this.possibleOptions.get(bestOption).copy();
		SingleTreeNode tn = expandChild(bestOption, nextOption);
		//StateObservation nextState = state.copy();
		//nextState.advance(Agent.actions[bestOption]);

		//SingleTreeNode tn = new SingleTreeNode(nextState, this, this.random);
		children[bestOption] = tn;
		return tn;
	}

	public SingleTreeNode expandChild(int id, Option nextOption)
	{
		StateObservation nextState = this.state.copy();
		Types.ACTIONS action = nextOption.act(nextState);

		// Step 1: Follow the option:
		nextState.advance(action);
		// Step 2: Update the option's values:
		if(USE_MEAN_REWARD)
		{
			nextOption.addReward(nextState.getGameScore() - state.getGameScore());
			if(nextOption.isFinished(nextState) && !Agent.NAIVE_PLANNING)
				nextOption.updateOptionRanking();
		}
		else if(nextOption.isFinished(nextState) && this.parent != null)
		{
			this.parent.setOptionFinished(nextOption);
		}

		// Step 3: get the new option set
		ArrayList<Option> newOptions = (ArrayList<Option>) this.possibleOptions.clone();
		Lib.updateOptions(nextState, newOptions);

		// Step 4: create a child node
		SingleTreeNode tn = new SingleTreeNode(nextState, this, nextOption, newOptions, this.random);
		this.children[id] = tn;

		// Step 5: Set the observation grid to the new grid:
		Agent.aStar.setLastObservationGrid(nextState.getObservationGrid());
		Agent.aStar.checkForWalls(state, action, nextState);
		return tn;
	}

	/** Choses a child node using the crazy stone algorithm, following
	 * "Efficient Selectivity and Backup Operators in Monte-Carlo Tree Search",
	 * Coulom 2006. The mu and sigma are not calculated as in the paper, because
	 * this is not a game of go. */
	public SingleTreeNode crazyStone()
	{
		// For speeding up the situation where an option is being followed, and
		// just 1 child exists
		if(!chosenOptionFinished)
		{
			if(this.children[0] == null)
			{
				this.expandChild(0, chosenOption);
			}
			return this.children[0];
		}

		double N = this.possibleOptions.size();
		int selectedId = 0;
		double[] probs = new double[(int) N];
		double probsSum = 0;
		int agentOptionIndex = -1;
		double alpha;
		if(agentOption != null)
		{
			agentOptionIndex = possibleOptions.indexOf(agentOption);
		}
		//System.out.println("Possible options (in order): " + possibleOptions);
		for(int i = 0; i < N; i++)
		{
			Option o = this.possibleOptions.get(i);
			// The second equation in sect. 3.2:
			// It is possible to add an alpha value here, increasing the
			// probability of it being chosen. The formula would become this:
			// (0.1 + Math.pow(2, (-i)) + alpha) / N
			// alpha being 1 if something good's going on, and 0 otherwise.
			// Alpha is now 1 when the agent is already following this option.
			// This encourages the tree search to explore the agent's current
			// option
			if(i == agentOptionIndex)
				alpha = 1;
			else
				alpha = 0;
			double paperEpsilon = (0.1 + Math.pow(2, (-i)) + alpha) / N;
			// Prepare values for the next equation
			//double mu = Utils.normalise(Agent.optionRanking.get(o.getType()), muLast, mu0);
			double mu = Agent.optionRanking.get(o.getType());
			double sigma = Agent.optionRankingVariance.get(o.getType());

			// The first equation in sect. 3.2:
			probs[i] = Math.exp(-STEEPNESS * (
						//(1 - mu) /
						(mu0 - mu) /
						(Math.sqrt(2 * (sigma0 + sigma)) + this.epsilon)) 
					+ paperEpsilon);
			probsSum += probs[i];
			//System.out.printf("\nMu0: %f\nMu: %f\nSigma0: %f\nSigma: %f\nespilon: %f\n\n",
			//		mu0, mu, sigma0, sigma, paperEpsilon);
			//System.out.println("Prob for option '" + o + "' is '" + probs[i] + "'");
		}
		//System.out.println("\n\n");
		// Get the randomly selected best id to expand/select
		selectedId = Lib.weightedRandomIndex(random, probs, probsSum);

		// The return-variable
		SingleTreeNode selected;

		// Check if the selected option is already expanded, if not, expand
		// first, then return the child
		if(children[selectedId] == null)
		{
			Option nextOption = this.possibleOptions.get(selectedId).copy();
			selected = this.expandChild(selectedId, nextOption);
		}
		else
		{
			selected = children[selectedId];
		}
		return selected;
	}

	public SingleTreeNode uct()
	{
		// For speeding up the situation where an option is being followed, and
		// just 1 child exists
		SingleTreeNode selected = null;
		double bestValue = -Double.MAX_VALUE;
		SingleTreeNode child = null;

		// Loop through existing children:
		for (int i=0; i<this.children.length; i++)
		{
			child = children[i];
			// Some children might not be initialized by the exploration
			// strategy
			if(child == null)
				continue;

			// Get discounted child value
			double hvVal = child.totValue;

			double childValue =  hvVal / (child.nVisits + this.epsilon);

			double uctValue = childValue +
					Agent.K * Math.sqrt(Math.log(this.nVisits + 1) / (child.nVisits + this.epsilon));

			// small sampleRandom numbers: break ties randomly
			uctValue = Utils.noise(uctValue, this.epsilon, this.random.nextDouble());

			if (uctValue > bestValue) 
			{
				selected = child;
				bestValue = uctValue;
			}
		}
		return selected;
	}

	/** Perform a rollout with actions taken from chosenOption, and when that's
	 * finished random actions, on the current node of maximally
	 * Agent.ROLLOUT_DEPTH. 
	 * @return Delta is the "simpleValue" of the last state the rollOut
	 * arrives in if USE_MEAN_REWARD is true, else it's the difference in reward
	 * of this state
	 */
	public double rollOut()
	{
		StateObservation rollerState = state.copy();
		rollOutDepth = this.nodeDepth;
		Option rollerOption = null;
		// Decides how many points should be awarded or subtracted for a win or
		// loss respectively
		//TODO: Check if this is needed
		double scoreExtra = 10.;
		if(USE_MEAN_REWARD)
			scoreExtra = 100;
		// Save copy-time when RANDOM_ROLLOUT is true
		if(chosenOption != null && !RANDOM_ROLLOUT)
			rollerOption = chosenOption.copy();
		// Instantiate "rollerOptionFinished" to whether it's null:
		// If RANDOM_ROLLOUT is turned on, we say that the "roller option is
		// finished" If there is no rollerOption it is also finished. And of
		// course, it's finished if it is finished
		boolean rollerOptionFinished = rollerOption == null || RANDOM_ROLLOUT 
			|| rollerOption.isFinished(rollerState);
		//double lastScore = Lib.simpleValue(rollerState);
		// Initialize lastScore with the parent's state's score, because the
		// expansion to this node could also result in score change
		double lastScore = Lib.simpleValue(this.parent.state, scoreExtra);
		while (!finishRollout(rollerState)) 
		{
			// System.out.println("Roller depth " + rollOutDepth);
			// if(this.parent != null)
			// 	System.out.println(this.parent);

			Types.ACTIONS action;
			if(!rollerOptionFinished)
			{
				double score = rollerState.getGameScore();

				// If the option is finished, update the Agent's option ranking
				if(rollerOption.isFinished(rollerState))
				{
					if(USE_MEAN_REWARD)
						rollerOption.updateOptionRanking();
					else if(this.parent != null)
						parent.setOptionFinished(rollerOption);
					rollerOptionFinished = true;
				}
				// If possible follow this node's optio
				action = rollerOption.act(rollerState);
				rollerState.advance(action);
				// Update the option's reward
				//rollerOption.addReward(Lib.simpleValue(rollerState) - lastScore);
				if(USE_MEAN_REWARD)
					rollerOption.addReward(rollerState.getGameScore() - score);
			}
			else
			{
				action = Agent.actions[random.nextInt(Agent.actions.length)];
				rollerState.advance(action);
			}
			rollOutDepth++;
		}
		return Lib.simpleValue(rollerState, scoreExtra) - lastScore;
	}

	public boolean finishRollout(StateObservation rollerState)
	{
		if(rollOutDepth >= Agent.ROLLOUT_DEPTH)	  //rollout end condition.
			return true;

		if(rollerState.isGameOver())			   //end of game
			return true;

		return false;
	}

	public void backUp(SingleTreeNode node, double result, int furthestDepth)
	{
		SingleTreeNode n = node;
		while(n != null)
		{
			n.nVisits++;
			n.totValue += Math.pow(Agent.GAMMA, furthestDepth - n.nodeDepth) * result;
			n = n.parent;
		}
	}

	public int mostVisitedAction() 
	{
		int selected = -1;
		double bestValue = -Double.MAX_VALUE;
		boolean allEqual = true;
		double first = -1;

		for (int i=0; i<children.length; i++) {

			if(children[i] != null)
			{
				if(first == -1)
					first = children[i].nVisits;
				else if(first != children[i].nVisits)
				{
					allEqual = false;
				}

				double childValue = children[i].nVisits;
				childValue = Utils.noise(childValue, this.epsilon, this.random.nextDouble());	 //break ties randomly
				if (childValue > bestValue) {
					bestValue = childValue;
					selected = i;
				}
			}
		}

		if (selected == -1)
		{
			System.out.println("Unexpected selection!");
			selected = 0;
		}
		else if(allEqual)
		{
			//If all are equal, we opt to choose for the one with the best Q.
			if(Agent.NAIVE_PLANNING)
				selected = bestActionNaive();
			else
				selected = bestActionLearning();
		}
		return selected;
	}

	public int bestActionNaive()
	{
		int selected = -1;
		double bestValue = -Double.MAX_VALUE;
		for (int i=0; i<children.length; i++) 
		{
			if(children[i] != null) 
			{
				double childValue = children[i].totValue / (children[i].nVisits + this.epsilon);
				childValue = Utils.noise(childValue, this.epsilon, this.random.nextDouble());	 //break ties randomly
				if (childValue > bestValue)
				{
					bestValue = childValue;
					selected = i;
				}
			}
		}
		if (selected == -1)
		{
			System.out.println("Unexpected selection!");
			selected = 0;
		}
		return selected;
	}

	public int bestActionLearning()
	{
		int selected = -1;
		double bestValue = -Double.MAX_VALUE;
		double bestOptionValue = -Double.MAX_VALUE;
		for (int i=0; i<children.length; i++) 
		{
			if(children[i] != null) 
			{
				double childValue = children[i].totValue / (children[i].nVisits + this.epsilon);
				double optionValue = Agent.optionRanking.get(
						this.possibleOptions.get(i).getType());
				//break ties randomly
				optionValue = Utils.noise(
						optionValue, this.epsilon, this.random.nextDouble());
				// Go for the best child value
				if (childValue > bestValue)
				{
					bestValue = childValue;
					bestOptionValue = optionValue;
					selected = i;
				}
				// But break ties using optionValue.
				else if(childValue == bestValue)
				{
					if(optionValue > bestOptionValue)
					{
						selected = i;
						bestOptionValue = optionValue;
					}
				}
			}
		}
		if (selected == -1)
		{
			System.out.println("Unexpected selection!");
			selected = 0;
		}
		return selected;
	}

	public boolean notFullyExpanded() 
	{
		for (SingleTreeNode tn : children) 
		{
			if (tn == null) 
			{
				return true;
			}
		}

		return false;
	}

	public void setOptionFinished(Option o)
	{
		if(o.equals(this.chosenOption))
		{
			//System.out.println("Setting option '" + o + "' to finished");
			this.optionFinished = true;
			if(parent != null)
			{
				parent.setOptionFinished(o);
			}
		}
		else
		{
			//System.out.println("Not settinp option " + o + " to finished in " + this);
		}
	}
	
	public String print(int depth)
	{
		String s = "";
		if(this.parent == null)
			s += String.format("root (%d visits)", nVisits);
		else
			s += String.format("%s (%f, %d visits)", chosenOption, totValue / (nVisits + this.epsilon), nVisits);
		for(SingleTreeNode node : children)
		{
			if(node != null)
			{
				s += "\n";
				for(int i=0; i<depth; i++)
				{
					s += "|\t";
				}
				s += "\\- ";
				s += node.print(depth+1);
			}
		}
		return s;
	}

	public Option getChosenOption()
	{
		return this.chosenOption;
	}

	public String toString()
	{
		return print(0);
	}

}
