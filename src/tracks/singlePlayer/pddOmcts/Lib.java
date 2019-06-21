package tracks.singlePlayer.pddOmcts;

import core.game.Observation;
import core.game.StateObservation;
import ontology.Types;
import tools.Vector2d;

import java.util.ArrayList;
import java.io.ObjectOutputStream;
import java.io.FileOutputStream;
import java.util.HashMap;
import java.io.FileInputStream;
import java.io.ObjectInputStream;
import java.io.FileNotFoundException;
import java.util.Comparator;
import java.util.Map;
import java.util.Set;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Arrays;
import java.util.Random;

public class Lib
{
	public static final double HUGE_NEGATIVE = -1000000000.0;
	public static final double HUGE_POSITIVE =  1000000000.0;
	/** Only the closest MAX_OBSERVATIONS observations are turned into options */
	// TODO: This may be too much, but who knows?
	public static final int MAX_OBSERVATIONS = 10;

	// This will be used by agents that make files. This postfix can be adjusted
	// by for example the Test class (that's why it's not final...), in order to
	// be able to run several agents at the same time
	public static String filePostfix = "";

	public static String observationToString(Observation obs)
	{
		String s = "";
		s += "obsID: " + obs.obsID + "\n";
		s += "cagegory: " + obs.category + "\n";
		s += "itype: " + obs.itype + "\n";
		s += "position: " + obs.position + "\n";
		s += "reference: " + obs.reference + "\n";
		s += "sqDist: " + obs.sqDist + "\n\n";
		return s;
	}

	public static void printObservationGrid(ArrayList<Observation>[][] observationGrid)
	{
		for (int x = 0; x < observationGrid.length; x++)
		{
			for (int y = 0; y < observationGrid[x].length; y++)
			{
				for (Observation obs : observationGrid[x][y])
				{
					System.out.print(obs.itype);
				}
				System.out.print(" ");
			}
			System.out.println();
		}
	}

	/** Very simple state evaluation, taken from the MCTS code */
	public static double simpleValue(StateObservation so, double difference) 
	{
		boolean gameOver = so.isGameOver();
		Types.WINNER win = so.getGameWinner();
		double rawScore = so.getGameScore();

		if(gameOver && win == Types.WINNER.PLAYER_LOSES)
			rawScore -= difference;

		if(gameOver && win == Types.WINNER.PLAYER_WINS)
			rawScore += difference;

		return rawScore;
	}

	/** Get the nearest observation from an array of ArrayLists of observations,
	 * assuming that all ArrayLists are already ordered. These ArrayList Arrays
	 * come from functions like getNPCPositions()
	 */
	public static Observation getNearestObservation(ArrayList<Observation>[] obala)
	{
		// Sometimes obala isn't instantiated at all!
		if(obala == null)
		{
			return null;
		}
		
		// Hopefully this initializes to a not-null value
		Observation nearest = null;
		for(ArrayList<Observation> obal : obala)
		{
			// if there's observations in this entry
			if(obal.size() > 0)
			{
				// If we don't have a nearest, make this observation nearest
				if(nearest != null)
				{
					// Take the nearest observation of both
					if(nearest.sqDist > obal.get(0).sqDist)
					{
						nearest = obal.get(0);
					}
				}
				else
					nearest = obal.get(0);
			}
		}
		return nearest;
	}

	public static void writeHashMapToFile(HashMap h, String f)
	{
		try
		{
			FileOutputStream fos = new FileOutputStream(f);
			ObjectOutputStream oos = new ObjectOutputStream(fos);
			oos.writeObject(h);
			oos.close();
			System.out.println("HashMap written to file");
		}
		catch (Exception e)
		{
			System.out.println("Couldn't write the hash map to file!");
			e.printStackTrace();
		}
	}

	public static Object loadObjectFromFile(String f)
	{
		try
		{
			FileInputStream fis = new FileInputStream(f);
			ObjectInputStream ois = new ObjectInputStream(fis);
			Object o = ois.readObject();
			System.out.printf("Table loaded from file %s\n", f);
			return o;
		}
		catch (FileNotFoundException e)
		{
			// No file (yet) present:
			System.out.println("No hashMap loaded");
		}
		catch (Exception e)
		{
			// Something's definitely wrong now!
			System.out.println("Couldn't load the hash map from file!");
			e.printStackTrace();
			System.out.println("Returning null");
		}
		// if all else fails
		return null;
	}

	public static enum GETTER_TYPE
	{
		NPC, MOVABLE, IMMOVABLE, RESOURCE, PORTAL, NPC_KILL
	}


	/** Adds options for all actions in the arraylist actions
	 * @param actions all actions possible in this game
	 * @param possibleOptions A list that may allready contain options. New
	 * options are added to this list
	 */
	public static void setOptionsForActions(ArrayList<Types.ACTIONS> actions, ArrayList<Option> possibleOptions)
	{
		for(Types.ACTIONS action : actions)
		{
			possibleOptions.add(new ActionOption(Agent.GAMMA_D[0], Agent.GAMMA_P[0], action));
		}
	}

	public static void setWaitAndShootOptions(StateObservation so, HashSet<Option> possibleOptionSet, int range)
	{
		if(so.getNPCPositions() != null)
		{
			for(ArrayList<Observation> ao : so.getNPCPositions())
			{
				if(ao.size() > 0)
				{
					WaitAndShootOption o = new WaitAndShootOption(Agent.GAMMA_D[5], Agent.GAMMA_P[5],
							ao.get(0).itype, range);
					possibleOptionSet.add(o);
				}
			}
		}
	}

	/** Adds an option per itype currently available in all getters 
	 * @param so the current stateObservation for getting all observations
	 * @param possibleOptionSet all currently available options
	 */
	public static void setGoToNearestOptions(StateObservation so, 
			HashSet<Option> possibleOptionSet)
	{
		int itype;
		ArrayList<Observation>[] oaa;
		for(GETTER_TYPE type : GETTER_TYPE.values())
		{
			// Get all observations for this type
			oaa = getObservationList(so, type);
			if(oaa == null || oaa.length == 0)
			{
				continue;
			}
			// Loop through all observation arrays of different itypes
			for (ArrayList<Observation> oa : oaa)
			{
				if(oa == null || oa.size() == 0)
				{
					continue;
				}
				// Get the itype of this observation array
				itype = oa.get(0).itype;
				if(itype != 0)
				{
					// Try to add non-walls to the possibleOptionSet
					possibleOptionSet.add(new GoToNearestSpriteOfItypeOption(
								Agent.GAMMA_D[3], Agent.GAMMA_P[3], type, itype, so));
				}
			}
		}
	}

	public static ArrayList<Observation>[] getObservationList(StateObservation so, GETTER_TYPE type)
	{
		if(type == GETTER_TYPE.NPC || type == GETTER_TYPE.NPC_KILL)
			return so.getNPCPositions();
		else if(type == GETTER_TYPE.MOVABLE)
			return so.getMovablePositions();
		else if(type == GETTER_TYPE.IMMOVABLE)
			return so.getImmovablePositions();
		else if(type == GETTER_TYPE.RESOURCE)
			return so.getResourcesPositions();
		else if(type == GETTER_TYPE.PORTAL)
			return so.getPortalsPositions();
		return null;
	}

	public static void updateOptions(StateObservation so, 
			ArrayList<Option> possibleOptions)
	{
		// Convert possibleOptions to a HashSet, to avoid double options
		HashSet<Option> possibleOptionSet = new HashSet<Option>(possibleOptions);
		Lib.setOptions(so, possibleOptionSet);
		if(Arrays.asList(Agent.actions).contains(Types.ACTIONS.ACTION_USE))
		{
			Lib.setWaitAndShootOptions(so, possibleOptionSet, 0);
			Lib.setWaitAndShootOptions(so, possibleOptionSet, 2);
			Lib.setWaitAndShootOptions(so, possibleOptionSet, 4);
		}
		Lib.setGoToNearestOptions(so, possibleOptionSet);

		// anno should be added if there are NPCs and removed if there are none
		AvoidNearestNpcOption anno = new AvoidNearestNpcOption(Agent.GAMMA_D[0], Agent.GAMMA_P[0]);
		possibleOptionSet.add(anno);
		if(so.getNPCPositions() == null)
			possibleOptionSet.remove(anno);
		removeFinishedOptions(so, possibleOptionSet);
		// Set the possibleOptions content to the content of the
		// possibleOptionSet
		possibleOptions.clear();
		possibleOptions.addAll(possibleOptionSet);
	}

	/** Adds new GoTo and GoNear options to possibleOptionSet */
	public static void setOptions(StateObservation so, 
			HashSet<Option> possibleOptionSet)
	{
		ArrayList<Types.ACTIONS> act = so.getAvailableActions();
		// Only create path planning options if up, down, left and right are
		// available
		if(act.contains(Types.ACTIONS.ACTION_UP) &&
				act.contains(Types.ACTIONS.ACTION_DOWN) &&
				act.contains(Types.ACTIONS.ACTION_LEFT) &&
				act.contains(Types.ACTIONS.ACTION_RIGHT))
		{
			Vector2d avatarPosition = so.getAvatarPosition();
			// Set options for all types of sprites that exist in this game. If
			// they don't exist, the getter will return null and no options will
			// be created.
			if(so.getNPCPositions() != null)
			{
				createOptions(so.getNPCPositions(avatarPosition), GETTER_TYPE.NPC, 
						so, possibleOptionSet);
			}
			if(so.getMovablePositions() != null)
				createOptions(so.getMovablePositions(avatarPosition), GETTER_TYPE.MOVABLE, 
						so, possibleOptionSet);
			if(so.getResourcesPositions() != null)
				createOptions(so.getResourcesPositions(avatarPosition), GETTER_TYPE.RESOURCE, 
						so, possibleOptionSet);
			if(so.getPortalsPositions() != null)
				createOptions(so.getPortalsPositions(avatarPosition), GETTER_TYPE.PORTAL, 
						so, possibleOptionSet);
		}
	}

	/** Checks isFinished() for all options and removes all the ones that are
	 * finished */
	public static void removeFinishedOptions(StateObservation so, HashSet<Option> possibleOptionSet)
	{
		// Now remove all options that have the obsIDs in optionObsIDs.  We
		// use the iterator, in order to ensure removing while iterating is
		// possible
		for(Iterator<Option> it = possibleOptionSet.iterator(); it.hasNext();)
		{
			Option option = it.next();
			// Remove the options that are still in optionObsIDs.
			if(option.isFinished(so))
			{
				it.remove();
			}
		}
	}

	/** Adds options for everything in observations to the set possibleOptionSet
	 * if they aren't already in there */
	public static void createOptions(ArrayList<Observation>[] observations,
			GETTER_TYPE type,
			StateObservation so,
			HashSet<Option> possibleOptionSet)
	{
		// Loop through all types of NPCs
		for(List<Observation> observationType : observations)
		{
			// Prune to only the nearest observations
			if(observationType.size() > MAX_OBSERVATIONS)
				observationType = observationType.subList(0, MAX_OBSERVATIONS);
			// Loop through all the NPC's of this type
			for(Observation observation : observationType)
			{
				// This is considered to be a wall
				if(observation.itype == 0)
					continue;
				// Check if this is a new obsID
				// Create option for this obsID, either goToMovable or
				// goToPosition
				if(type == GETTER_TYPE.NPC || type == GETTER_TYPE.MOVABLE)
				{
					possibleOptionSet.add(new GoToMovableOption(Agent.GAMMA_D[2], Agent.GAMMA_P[2],
						type, observation.itype, observation.obsID, so));
					possibleOptionSet.add(new GoNearMovableOption(Agent.GAMMA_D[1], Agent.GAMMA_P[1],
						type, observation.itype, observation.obsID, so));
				}
				else
				{
					possibleOptionSet.add(new GoToPositionOption(Agent.GAMMA_D[4], Agent.GAMMA_P[4],
						type, observation.itype, observation.obsID, so));
					possibleOptionSet.add(new GoNearMovableOption(Agent.GAMMA_D[1], Agent.GAMMA_P[1],
						type, observation.itype, observation.obsID, so));
				}
			}
		}
	}

	/** Calculates the orientation based on sprites coming from the avatar. This
	 * is needed because the orientation doesn't always work in the original
	 * games */
	public static Vector2d spriteFromAvatarOrientation(StateObservation so)
	{
		// Get a copy of the state
		StateObservation soCopy = so.copy();
		double blockSize = soCopy.getBlockSize();

		// ACTION_USE to make a new sprite from avatar
		soCopy.advance(Types.ACTIONS.ACTION_USE);
		soCopy.advance(Types.ACTIONS.ACTION_NIL);
		Vector2d avatarPosition = soCopy.getAvatarPosition();
		ArrayList<Observation>[] sprites = 
			soCopy.getFromAvatarSpritesPositions(avatarPosition);

		if(sprites == null || sprites.length == 0 || sprites[0].size() == 0)
		{
			return new Vector2d(0., 0.);
		}
		// assume only 1 sprite is made and get the nearest sprite (.get(0))
		Observation sprite = sprites[0].get(0);
		double x = (sprite.position.x - avatarPosition.x) / blockSize;
		double y = (sprite.position.y - avatarPosition.y) / blockSize;
		return new Vector2d(x, y);
	}

	/** First normalizes array, then returns a weighted random index depending
	 * on the contrents of array */
	public static int weightedRandomIndex(Random random, double[] array, double arraySum)
	{
		// Normalize probs:
		for (int i=0; i < array.length; i++)
		{
			array[i] /= arraySum;
		}

		// Select weighted random prob:
		double rand = random.nextDouble();
		//System.out.println("length: " + array.length + " rand: " + rand);
		double counter = 0;
		for (int i=0; i < array.length; i++)
		{
			counter += array[i];
			if(rand < counter)
			{
				return i;
			}
		}
		System.out.println("WARNING! Nothing returned in weightedRandomIndex, returning 0");
		return 0;
	}
}
