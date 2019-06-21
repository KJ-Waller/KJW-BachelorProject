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

/** An option that enables the agent to wait until a certain sprite ID
 * (typically an NPC sprite) is in a certain range (somewhere between 0 and 5)
 * and shoots then */
public class WaitAndShootOption extends Option implements Serializable
{
	/** The range from when to shoot. 0 would result in shooting when a sprite
	 * is in front of the agent, higher than that would result in shooting when
	 * a sprite is this many blocks away along the axis that the agent is *not*
	 * oriented in (so the agent is looking right, across the x-axis, the enemy
	 * would have to be _range_ steps away on the y-axis
	 */
	public final int OPTION_NUM = 5;
	private int range = 0;

	/** This option works for this kind of sprite */
	private int itype;

	public WaitAndShootOption(double gamma_d, double gamma_p, int itype, int range)
	{
		super(gamma_d, gamma_p);
		this.range = range;
		this.itype = itype;
	}

	public Types.ACTIONS act(StateObservation so)
	{
		this.step++;
		if(isEnemyInDangerZone(so))
		{
			this.finished = true;
			return Types.ACTIONS.ACTION_USE;
		}
		return Types.ACTIONS.ACTION_NIL;
	}

	public boolean isFinished(StateObservation so)
	{
		return this.finished;
	}

	protected boolean isEnemyInDangerZone(StateObservation so)
	{
		Vector2d avatarOrientation = Agent.avatarOrientation;
		Vector2d avatarPosition = so.getAvatarPosition();
		ArrayList<Observation>[] npcPositions = so.getNPCPositions();
		if(npcPositions == null)
		{
			return false;
		}
		outerloop:
		for(ArrayList<Observation> ao : npcPositions)
		{
			for(Observation o : ao)
			{
				if(o.itype != itype)
					continue outerloop;
				// Calculate the difference in location, to find the direction
				// match:
				double diffX = (o.position.x - avatarPosition.x) / 
					(double) so.getBlockSize();
				double diffY = (o.position.y - avatarPosition.y) / 
					(double) so.getBlockSize();
				// In this case, the avatar is facing the opponent in one axis,
				// so difference in the othre axis should be the same as
				// this.range
				if((diffX * avatarOrientation.x > 0 && 
							(int) Math.round(diffY) == range) ||
						(diffY * avatarOrientation.y > 0 && 
							(int) Math.round(diffX) == range))
				{
					return true;
				}
			}
		}
		return false;
	}

	public int getItype()
	{
		return this.itype;
	}

	public int getRange()
	{
		return this.range;
	}

	// Subtype only checks itype because range effectivity could vary
	// throughout the game
	public String getSubtype()
	{
		return String.format("%s,%d", super.getSubtype(), this.itype);
	}

	public Option copy()
	{
		return new WaitAndShootOption(this.gamma_d, this.gamma_p, this.itype, this.range);
	}

	@Override
	public boolean equals(Object o)
	{
		if(o instanceof WaitAndShootOption)
		{
			WaitAndShootOption oa = (WaitAndShootOption) o;
			if(this.itype == oa.getItype() && this.range == oa.getRange())
				return true;
		}
		return false;
	}

	@Override
	public int hashCode()
	{
		int hash = 1;
		hash = hash * 17 + itype;
		hash = hash * 31 + range;
		return hash;
	}

	@Override
	public String toString()
	{
		return String.format("WaitAndShootOption(%d,%d)", itype, range);
	}

}
