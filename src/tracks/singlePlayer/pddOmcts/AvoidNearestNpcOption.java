package tracks.singlePlayer.pddOmcts;

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
public class AvoidNearestNpcOption extends Option implements Serializable
{
	public final int OPTION_NUM = 0;

	public AvoidNearestNpcOption(double gamma_d, double gamma_p)
	{
		super(gamma_d, gamma_p);
	}

	/** Move away from nearest NPC */
	public Types.ACTIONS act(StateObservation so)
	{
		this.step++;
		// get nearest NPC
		Vector2d avatarPosition = so.getAvatarPosition();
		ArrayList<Observation>[] npcPositions = 
			so.getNPCPositions(avatarPosition);
		if(npcPositions == null)
			return Types.ACTIONS.ACTION_NIL;
		Observation nearest = null;
		double nearestDistance = Double.MAX_VALUE;
		for(ArrayList<Observation> oa : npcPositions)
		{
			if(oa.size() > 0)
			{
				double distance = oa.get(0).position.sqDist(avatarPosition);
				if(nearestDistance > distance)
				{
					nearestDistance = distance;
					nearest = oa.get(0);
				}
			}
		}
		// Sometimes npcPositions is not null, but everything is empty anyways
		if(nearest == null)
			return Types.ACTIONS.ACTION_NIL;
		return Agent.aStar.moveAway(AStar.vectorToBlock(avatarPosition), 
				AStar.vectorToBlock(nearest.position));
	}

	public boolean isFinished(StateObservation so)
	{
		return this.step > 0;
	}

	public Option copy()
	{
		return new AvoidNearestNpcOption(this.gamma_d, this.gamma_p);
	}

	@Override
	public boolean equals(Object o)
	{
		// All avoidNearestNpcOptions are the same
		if(o instanceof AvoidNearestNpcOption)
		{
			return true;
		}
		return false;
	}

	@Override
	public int hashCode()
	{
		// Just a very big prime number, probably okay..?
		return 2922509;
	}

	@Override
	public String toString()
	{
		return "AvoidNearestNpcOption";
	}

}
