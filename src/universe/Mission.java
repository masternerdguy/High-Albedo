/*
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

/*
 * Represents a mission the player has been assigned by an NPC.
 */
package universe;

import celestial.Ship.Ship;
import celestial.Ship.Station;
import engine.Entity;
import engine.Entity.State;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Random;
import lib.Binling;
import lib.Parser.Term;

/**
 *
 * @author nwiehoff
 */
public class Mission implements Serializable {

    public enum Type {

        BOUNTY_HUNT, //the player is given a ship to destroy
        DESTROY_STATION, //the player is given a station to destroy
    }
    private Type missionType;
    private boolean aborted = false;
    //rng
    Random rnd = new Random();
    //reward and agent
    private long reward;
    private double deltaStanding;
    private Ship agent;
    //briefing
    private String briefing = "NO AIM";
    //targets
    private ArrayList<Entity> targets = new ArrayList<>();

    public Mission(Ship agent) {
        this.agent = agent;
        //generates a random mission
        ArrayList<Term> missions = Universe.getCache().getMissionCache().getTermsOfType("Mission");
        Term pick = missions.get(rnd.nextInt(missions.size()));
        build(pick);
    }

    public Mission(Ship agent, Term pick) {
        this.agent = agent;
        build(pick);
    }

    private void build(Term pick) {
        //get info on min and max
        String sCash = pick.getValue("cash");
        String sDelta = pick.getValue("delta");
        {
            //calculate payment
            long min = Long.parseLong(sCash.split(">")[0]);
            long max = Long.parseLong(sCash.split(">")[1]);
            long dCash = max - min;
            long dR = (long) (rnd.nextFloat() * dCash);
            reward = min + dR;
        }
        //calculate delta
        {
            double min = Double.parseDouble(sDelta.split(">")[0]);
            double max = Double.parseDouble(sDelta.split(">")[1]);
            double dStanding = max - min;
            double dR = (rnd.nextFloat() * dStanding);
            deltaStanding = min + dR;
        }
        //determine mission type
        String rawType = pick.getValue("type");
        if (rawType.matches("DESTROY_STATION")) {
            missionType = Type.DESTROY_STATION;
        }
        if (rawType.matches("BOUNTY_HUNT")) {
            missionType = Type.BOUNTY_HUNT;
        }
        //store briefing
        briefing = pick.getValue("briefing");
        //build more based on type
        if (missionType == Type.DESTROY_STATION) {
            buildDestroyStation();
        } else if (missionType == Type.BOUNTY_HUNT) {
            buildBountyHunt();
        } else {
            preAbort();
        }
    }

    private void buildDestroyStation() {
        /*
         * In these missions a random enemy station is selected somewhere
         * in the universe, and the player has to find a way to blow it up.
         * 
         * There is no time limit, and as long as the station dies the mission
         * will complete.
         */
        //make a list of negative standings
        ArrayList<String> badStandings = new ArrayList<>();
        ArrayList<Binling> raw = agent.getMyFaction().getStandings();
        for (int a = 0; a < raw.size(); a++) {
            if (!raw.get(a).getString().matches("Player") && !raw.get(a).getString().matches("Entities")) {
                if (raw.get(a).getDouble() < 0) {
                    badStandings.add(raw.get(a).getString());
                }
            }
        }
        //safety
        if (badStandings.isEmpty()) {
            preAbort();
        } else {
            //pick a group
            String pick = badStandings.get(rnd.nextInt(badStandings.size()));
            //find one of their stations
            Entity toKill = null;
            //get a list of all their stations
            ArrayList<Entity> options = new ArrayList<>();
            for (int a = 0; a < agent.getUniverse().getSystems().size(); a++) {
                ArrayList<Entity> lStat = agent.getUniverse().getSystems().get(a).getStationList();
                for (int v = 0; v < lStat.size(); v++) {
                    Ship test = (Ship) lStat.get(v);
                    if (test.getFaction().matches(pick) && test.getState() == State.ALIVE) {
                        options.add(lStat.get(v));
                    }
                }
            }
            //pick a station
            if (!options.isEmpty()) {
                toKill = options.get(rnd.nextInt(options.size()));
            }
            //continue
            if (toKill != null) {
                Station tmp = (Station) toKill;
                //add station to target list
                targets.add(toKill);
                //update briefing
                briefing = briefing.replace("<TARGET>", tmp.getName());
                briefing = briefing.replace("<LOCATION>", tmp.getCurrentSystem().getName());
            } else {
                preAbort();
            }
        }
    }

    private void buildBountyHunt() {
        /*
         * In these missions a randomy selected enemy NPC is selected and a
         * bounty placed on it. The player must destroy this NPC before it is
         * destroyed by other causes to get the reward.
         * 
         * There is no time limit, but the player must destroy the target.
         * Otherwise the mission will be aborted.
         */
        //make a list of negative standings
        ArrayList<String> badStandings = new ArrayList<>();
        ArrayList<Binling> raw = agent.getMyFaction().getStandings();
        for (int a = 0; a < raw.size(); a++) {
            if (!raw.get(a).getString().matches("Player") && !raw.get(a).getString().matches("Entities")) {
                if (raw.get(a).getDouble() < 0) {
                    badStandings.add(raw.get(a).getString());
                }
            }
        }
        //safety
        if (badStandings.isEmpty()) {
            preAbort();
        } else {
            //pick a group
            String pick = badStandings.get(rnd.nextInt(badStandings.size()));
            //find one of their stations
            Entity toKill = null;
            //get a list of all their stations
            ArrayList<Entity> options = new ArrayList<>();
            for (int a = 0; a < agent.getUniverse().getSystems().size(); a++) {
                ArrayList<Entity> lStat = agent.getUniverse().getSystems().get(a).getShipList();
                for (int v = 0; v < lStat.size(); v++) {
                    Ship test = (Ship) lStat.get(v);
                    if (test.getFaction().matches(pick) && test.getState() == State.ALIVE) {
                        options.add(lStat.get(v));
                    }
                }
            }
            //pick a ship
            if (!options.isEmpty()) {
                toKill = options.get(rnd.nextInt(options.size()));
            }
            //continue
            if (toKill != null) {
                Ship tmp = (Ship) toKill;
                //add station to target list
                targets.add(toKill);
                //update briefing
                briefing = briefing.replace("<NAME>", tmp.getPilot());
                briefing = briefing.replace("<SHIP>", tmp.getType());
                briefing = briefing.replace("<SHIPNAME>", tmp.getName());
                briefing = briefing.replace("<LOCATION>", tmp.getCurrentSystem().getName());
            } else {
                preAbort();
            }
        }
    }


    /*
     * Used internally to complete or fail a mission based on periodic updates.
     */
    private void abortMission() {
        aborted = true;
        //remove this mission
        agent.getUniverse().getPlayerMissions().remove(this);
        //notify
        agent.composeMessage(agent.getUniverse().getPlayerShip(), "Mission Revoked", "Recent events require us to revoke your contract. You can come back for a new mission at your leisure, however.", null);
    }

    private void failMission() {
        if (!aborted) {
            //update standing
            agent.getUniverse().getPlayerShip().getMyFaction().derivedModification(agent.getMyFaction(), -deltaStanding);
            //remove this mission
            agent.getUniverse().getPlayerMissions().remove(this);
            //notify
            agent.composeMessage(agent.getUniverse().getPlayerShip(), "Mission Failed", "That was pretty sad work you did.", null);
        }
    }

    private void completeMission() {
        if (!aborted) {
            //pay player
            agent.getUniverse().getPlayerShip().setCash(agent.getUniverse().getPlayerShip().getCash() + reward);
            //update standing
            agent.getUniverse().getPlayerShip().getMyFaction().derivedModification(agent.getMyFaction(), deltaStanding);
            //remove this mission
            agent.getUniverse().getPlayerMissions().remove(this);
            //notify
            agent.composeMessage(agent.getUniverse().getPlayerShip(), "Mission Completed", "Payment transfered. Have a nice day.", null);
        }
    }

    private void preAbort() {
        /*
         * Called if the mission cannot be generated for some reason.
         */
        //drop rewards
        reward = 0;
        deltaStanding = 0;
        //remove this mission
        agent.getUniverse().getPlayerMissions().remove(this);
        //notify
        briefing = null;
        agent.composeMessage(agent.getUniverse().getPlayerShip(), "Nevermind", "We don't have anything available at the moment.", null);
    }

    public void periodicUpdate(double tpf) {
        if (!missionComplete()) {
            //waiting
        } else {
            completeMission();
        }
        if (missionFailed()) {
            //fail the mission
            failMission();
        }
    }

    private boolean missionComplete() {
        if (missionType == Type.DESTROY_STATION) {
            return checkDestroyStation();
        } else if (missionType == Type.BOUNTY_HUNT) {
            return checkBountyHunt();
        }
        //undefinded
        return true;
    }

    private boolean missionFailed() {
        return false;
    }

    private boolean checkDestroyStation() {
        //are all targets dead?
        for (int a = 0; a < targets.size(); a++) {
            if (targets.get(a).getState() == State.ALIVE) {
                //nope
                return false;
            }
        }
        //if we made it this far
        return true;
    }

    private boolean checkBountyHunt() {
        //are all targets dead?
        for (int a = 0; a < targets.size(); a++) {
            if (targets.get(a).getState() == State.ALIVE) {
                //nope
                return false;
            } else {
                Ship test = (Ship) targets.get(a);
                if (test.getLastBlow().getFaction().matches("Player")) {
                    //this is good news
                } else {
                    //someone else got it first
                    abortMission();
                    return false;
                }
            }
        }
        //if we made it this far
        return true;
    }

    /*
     * Getters and setters
     */
    public long getReward() {
        return reward;
    }

    public double getDeltaStanding() {
        return deltaStanding;
    }

    public Ship getAgent() {
        return agent;
    }

    public String getBriefing() {
        return briefing;
    }
}
