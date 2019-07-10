package tracks.singlePlayer.pddOmcts;

import ontology.Types;
import core.game.StateObservation;

import java.io.Serializable;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.IOException;
import java.lang.ClassNotFoundException;
import java.util.Comparator;

/** An option is a sequence of actions. This class enables taking a sequence of
 * actions and saving its result in the q table, enabling a more high-level
 * approach to reinforcement learning */
public abstract class Option implements Serializable
{
	protected double gamma_d;
	protected double gamma_p;
	protected int step;
	protected double cumulativeReward;

	/** If this option has no functionality anymore, we mark it as "finished".
	 * This includes when the destination is reached, when the goal is gone, or
	 * when there is no path to a goal. All actions returned after being
	 * "finished" are ACTION_NIL */
	public boolean finished;

	/** Specifies the obsID of the movable/npc that is tracked by this option.
	 * -1 if its  not available */
	protected int obsID = -1;

	/** specifies the itype of the sprite that is tracked by this option. -1 if
	 * it's not available */
	protected int itype = -1;

	public static OptionComparator optionComparator = new OptionComparator();

	/** Default constructor, creates option, instantiates variables */
	public Option(double gamma_d, double gamma_p)
	{
		this.gamma_d = gamma_d;
		this.gamma_p = gamma_p;
		this.step = 0;
		this.cumulativeReward = 0;
	}

	public Option(double gamma_d, double gamma_p, int step, double cumulativeReward)
	{
		this.gamma_d = gamma_d;
		this.gamma_p = gamma_p;
		this.step = step;
		this.cumulativeReward = cumulativeReward;
	}

	/** Choose the action to be taken now. This should increment this.step! */
	public abstract Types.ACTIONS act(StateObservation so);

	/** True if the option so is a goal state */
	public abstract boolean isFinished(StateObservation so);

	/** True if the goal of this option still exists. For example a
	 * GoToMovableOption where the movable has been removed from the game would
	 * return false.
	 * By default this returns true, since simple options don't have a goal
	 * (which should be treated the same as having an existing goal)
	 */
	public boolean goalExists(StateObservation so)
	{
		return true;
	}

	/** If this option has an obsID of an observation it tracks, that can be
	 * returned by this function. If there's no obsID (default) '-1' will be
	 * returned */
	public int getObsID()
	{
		return this.obsID;
	}

	/** If this option has an obsID of an observation it tracks, that can be
	 * returned by this function. If there's no obsID (default) '-1' will be
	 * returned */
	public int getItype()
	{
		return this.itype;
	}

	/** It's possible to specify a specific subtype with this function */
	protected String getSubtype()
	{
		return "";
	}

	/** Returns string for type and subtype of this option. Used to index what
	 * (sub)types of options are feasible with some kind of uct */
	public String getType()
	{
		return this.getClass().getName() + "." + this.getSubtype();
	}
	
	/** Resets the option values, enabling it to be called again next time
	 * without having interfering values saved in it. */
	public void reset()
	{
		this.step = 0;
		this.cumulativeReward = 0;
		this.finished = false;
	}

	public abstract Option copy();

	/** If your algorithm thinks it knows better how to set the cumulative
	 * reward, use this. */
	public void setCumulativeReward(double cumulativeReward)
	{
		this.cumulativeReward = cumulativeReward;
	}

	public double getReward()
	{
		return this.cumulativeReward;
	}

	/** Returns the step count of this option, representing how long the option
	 * has been in use
	 */
	public int getStep()
	{
		return this.step;
	}

	/** Sets this.finished to true */
	protected void setFinished()
	{
		this.finished = true;
		System.out.println(this.toString() + ' ' + this.getStep());
//		this.toString();
	}

	protected void readObject(ObjectInputStream aInputStream) 
		throws ClassNotFoundException, IOException 
	{
		//always perform the default de-serialization first
		aInputStream.defaultReadObject();
	}

	protected void writeObject(ObjectOutputStream aOutputStream)
		throws IOException 
	{
		// perform the default serialization for all non-transient, non-static
		// fields
		aOutputStream.defaultWriteObject();
	}

	public abstract int hashCode();

	public abstract boolean equals(Object o);

	protected static class OptionComparator implements Comparator<Option>
	{
		public int compare(Option o1, Option o2)
		{
			double v1 = Agent.optionRanking.get(o1.getType());
			double v2 = Agent.optionRanking.get(o2.getType());
			if(v1 > v2)
				return -1;
			else if(v2 > v1)
				return 1;
			else
				return 0;
		}
	}
}
