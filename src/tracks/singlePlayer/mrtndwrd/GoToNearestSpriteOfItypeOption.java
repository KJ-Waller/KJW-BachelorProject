package tracks.singlePlayer.mrtndwrd;

import ontology.Types;
import tools.Vector2d;
import core.game.StateObservation;
import core.game.Observation;

import java.io.Serializable;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.lang.ClassNotFoundException;
import java.lang.Double;

/** An option that enables the agent to wait until a certain sprite ID
 * (typically an NPC sprite) is in a certain range (somewhere between 0 and 5)
 * and shoots then */
public class GoToNearestSpriteOfItypeOption extends GoToMovableOption implements Serializable
{
	public GoToNearestSpriteOfItypeOption(double gamma, Lib.GETTER_TYPE type, 
			int itype, StateObservation so)
	{
		super(gamma, type, itype, -1, so);
	}

	/** Constructor mainly for use by copy. By supplying the current goal
	 * position, the need for a StateObservation vanishes. */
	public GoToNearestSpriteOfItypeOption(double gamma, int step, double cumulativeReward,
			Lib.GETTER_TYPE type, int itype,
			SerializableTuple<Integer, Integer> goal)
	{
		super(gamma, step, cumulativeReward, type, itype, -1, goal);
	}

	/** Returns the location of the thing that is tracked, based on type, itype
	 * and obsID */
	protected SerializableTuple<Integer, Integer>
		getGoalLocationFromSo(StateObservation so)
	{
		ArrayList<Observation> observations;
		observations = getObservations(so, itype, so.getAvatarPosition());
		if(observations == null || observations.size() == 0)
		{
			return null;
		}
		return Agent.aStar.vectorToBlock(observations.get(0).position);
	}

	@Override
	public Option copy()
	{
		return new GoToNearestSpriteOfItypeOption(gamma, step, cumulativeReward,
				type, itype, goal);
	}

	@Override
	public String toString()
	{
		return String.format("GoToNearestSpriteOfItypeOption(%s,%d)", 
				type, itype);
	}

	public boolean equals(Object o)
	{
		if(o instanceof GoToNearestSpriteOfItypeOption)
		{
			GoToNearestSpriteOfItypeOption oa = (GoToNearestSpriteOfItypeOption) o;
			return this.type.equals(oa.type) && 
				this.itype == oa.itype && this.obsID == oa.obsID;
		}
		return false;
	}
}
