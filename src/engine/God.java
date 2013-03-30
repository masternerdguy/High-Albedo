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
 * Responsible for keeping the universe active. Specifically,
 * 1. Manage patrols and traders - spawn replacements as needed - TODO
 * 2. Manage stations - spawn replacements as needed - TODO
 * 3. Add 'fun' disasters to the universe. - TODO
 */
package engine;

import celestial.Celestial;
import celestial.Jumphole;
import celestial.Ship.Ship;
import celestial.Ship.Ship.Behavior;
import java.awt.geom.Point2D;
import java.util.ArrayList;
import java.util.Random;
import lib.Binling;
import lib.Faction;
import lib.Parser;
import lib.Parser.Term;
import lib.SuperFaction;
import universe.SolarSystem;
import universe.Universe;

/**
 *
 * @author nwiehoff
 */
public class God implements EngineElement {

    private Universe universe;
    private ArrayList<SuperFaction> factions = new ArrayList<>();
    private Random rnd = new Random();

    public God(Universe universe) {
        this.universe = universe;
        //generate lists
        initFactions();
    }

    private void initFactions() {
        //make a list of all factions
        Parser fParse = new Parser("FACTIONS.txt");
        ArrayList<Term> terms = fParse.getTermsOfType("Faction");
        for (int a = 0; a < terms.size(); a++) {
            factions.add(new SuperFaction(universe, terms.get(a).getValue("name")));
        }
    }

    @Override
    public void periodicUpdate() {
        try {
            checkPatrols();
            checkStations();
        } catch (Exception e) {
            System.out.println("Error manipulating dynamic universe.");
            e.printStackTrace();
        }
    }

    /*
     * Hooks
     */
    private void checkPatrols() {
        //iterate through each faction
        for (int a = 0; a < factions.size(); a++) {
            doPatrols(factions.get(a));
        }
    }

    private void checkStations() {
        //TODO
    }

    /*
     * Implementations
     */
    private void doPatrols(SuperFaction faction) {
        /*
         * 1. Make sure this faction has patrols
         * 2. Count the number of each loadout
         * 3. Spawn more of each loadout as needed
         */
        //make sure this faction has patrols
        if (faction.getPatrols().size() > 0) {
            //for storing loadout totals
            int count[] = new int[faction.getPatrols().size()];
            //get a count of the number of patrols in each system
            for (int a = 0; a < universe.getSystems().size(); a++) {
                SolarSystem sys = universe.getSystems().get(a);
                if (sys.getOwner().matches(faction.getName())) {
                    /*
                     * Increment count[] by the total number of each loadout
                     * found in this system. This will be used as a universe
                     * wide total later on.
                     */
                    for (int v = 0; v < count.length; v++) {
                        String loadout = faction.getPatrols().get(v).getString();
                        int num = countShipsByLoadout(faction, sys, loadout);
                        count[v] += num;
                    }
                }
            }
            //do they meet the required density?
            for (int a = 0; a < count.length; a++) {
                double density = faction.getPatrols().get(a).getDouble();
                //System.out.println(faction.getPatrols().get(a).getString() + " " + count[a]);
                if (count[a] < density) {
                    //pick a system this faction owns
                    ArrayList<SolarSystem> sov = faction.getSov();
                    SolarSystem pick = sov.get(rnd.nextInt(sov.size()));
                    //pick a planet in this system
                    ArrayList<Entity> planets = pick.getCelestialList();
                    Celestial host = (Celestial) planets.get(rnd.nextInt(planets.size()));
                    //pick a point near the jump hole
                    double x = host.getX() + rnd.nextInt(8000) - 4000;
                    double y = host.getY() + rnd.nextInt(8000) - 4000;
                    //make a point
                    Point2D.Double pnt = new Point2D.Double(x, y);
                    //spawn
                    spawnShip(faction, pick, pnt, faction.getPatrols().get(a), Behavior.PATROL);
                }
            }
        }
    }

    /*
     * Tools
     */
    public int countShipsByLoadout(Faction faction, SolarSystem system, String loadout) {
        int count = 0;
        {
            //get ship list
            ArrayList<Entity> ships = system.getShipList();
            for (int a = 0; a < ships.size(); a++) {
                Ship tmp = (Ship) ships.get(a);
                if (tmp.getFaction().matches(faction.getName())) {
                    if (tmp.getTemplate().matches(loadout)) {
                        count++;
                    }
                }
            }
        }
        return count;
    }

    public int countShipsByRole(Faction faction, SolarSystem system, Behavior behavior) {
        int count = 0;
        {
            //get ship list
            ArrayList<Entity> ships = system.getShipList();
            for (int a = 0; a < ships.size(); a++) {
                Ship tmp = (Ship) ships.get(a);
                if (tmp.getFaction().matches(faction.getName())) {
                    if (tmp.getBehavior() == behavior) {
                        count++;
                    }
                }
            }
        }
        return count;
    }

    private Ship makeShip(String template, String name, String faction) {
        /*
         * Generates a ship from a template.
         */
        Ship ret = null;
        {
            String cargo = "";
            String install = "";
            String ship = "Mass Testing Brick";
            if (template != null) {
                //load this template
                Parser lParse = new Parser("LOADOUTS.txt");
                ArrayList<Term> lods = lParse.getTermsOfType("Loadout");
                for (int a = 0; a < lods.size(); a++) {
                    if (lods.get(a).getValue("name").matches(template)) {
                        //get terms
                        cargo = lods.get(a).getValue("cargo");
                        install = lods.get(a).getValue("install");
                        ship = lods.get(a).getValue("ship");
                        break;
                    }
                }
            }

            //create player
            ret = new Ship(name, ship);
            if (template != null) {
                ret.setTemplate(template);
            }
            //check template
            ret.setEquip(install);
            ret.setFaction(faction);
            ret.init(false);
            ret.addInitialCargo(cargo);
        }
        return ret;
    }

    public void spawnShip(Faction faction, SolarSystem system, Point2D.Double loc, Binling loadout, Behavior behavior) {
        String name = loadout.getString();
        //get a basic ship to work with
        Ship tmp = makeShip(loadout.getString(), name, faction.getName());
        //push coordinates
        tmp.setX(loc.getX());
        tmp.setY(loc.getY());
        //push behavior
        tmp.setBehavior(behavior);
        //finalize
        tmp.setCurrentSystem(system);
        system.putEntityInSystem(tmp);
        //report
        System.out.println("Spawned "+loadout.getString()+" in "+system.getName()+" for "+faction.getName());
    }
}