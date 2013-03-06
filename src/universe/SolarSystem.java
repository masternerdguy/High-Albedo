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
 * Solar systems are a collection of planets and other celestials in a convenient
 * package. It provides zoning for the universe.
 */
package universe;

import celestial.Jumphole;
import celestial.Planet;
import celestial.Ship.Ship;
import celestial.Ship.Station;
import engine.Entity;
import java.awt.Graphics;
import java.awt.Rectangle;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Random;
import lib.Parser;
import lib.Parser.Term;

/**
 *
 * @author Nathan Wiehoff
 */
public class SolarSystem implements Entity, Serializable {
    //this system

    protected String name;
    int x;
    int y;
    //what it contains
    private ArrayList<Entity> celestials = new ArrayList<>();
    //what contains it
    private Universe universe;

    public SolarSystem(Universe universe, String name, Parser parse) {
        this.name = name; //needed for lookup
        this.universe = universe;
        //generate
        generateSystem(parse);
    }

    private void generateSystem(Parser parse) {
        /*
         * Adds all member objects. Member objects are any object that is
         * a member of this system according to the "system" param and is
         * one of the following
         * 
         * Planet
         * Ship
         * Station
         */
        ArrayList<Term> planets = parse.getTermsOfType("Planet");
        for (int a = 0; a < planets.size(); a++) {
            if (planets.get(a).getValue("system").matches(getName())) {
                //this planet needs to be created and stored
                getCelestials().add(makePlanet(planets.get(a)));
            }
        }
        ArrayList<Term> ships = parse.getTermsOfType("Ship");
        for (int a = 0; a < ships.size(); a++) {
            if (ships.get(a).getValue("system").matches(getName())) {
                //this planet needs to be created and stored
                getCelestials().add(makeShip(ships.get(a)));
            }
        }

        ArrayList<Term> stations = parse.getTermsOfType("Station");
        for (int a = 0; a < stations.size(); a++) {
            if (stations.get(a).getValue("system").matches(getName())) {
                //this planet needs to be created and stored
                getCelestials().add(makeStation(stations.get(a)));
            }
        }

        ArrayList<Term> jumpholes = parse.getTermsOfType("Jumphole");
        for (int a = 0; a < jumpholes.size(); a++) {
            if (jumpholes.get(a).getValue("system").matches(getName())) {
                //this planet needs to be created and stored
                getCelestials().add(makeJumphole(jumpholes.get(a)));
            }
        }
    }

    private Planet makePlanet(Term planetTerm) {
        Planet planet = null;
        {
            //extract terms
            String pName = planetTerm.getValue("name");
            String texture = planetTerm.getValue("texture");
            int diameter = Integer.parseInt(planetTerm.getValue("d"));
            double px = Double.parseDouble(planetTerm.getValue("x"));
            double py = Double.parseDouble(planetTerm.getValue("y"));
            //make planet and store
            planet = new Planet(pName, texture, diameter);
            planet.setX(px);
            planet.setY(py);
        }
        return planet;
    }

    private Ship makeShip(Term shipTerm) {
        Ship ret = null;
        Random rnd = new Random();
        {
            String ship = shipTerm.getValue("ship");
            String near = shipTerm.getValue("near");
            String name = shipTerm.getValue("name");
            String loadout = shipTerm.getValue("install");
            String faction = shipTerm.getValue("faction");
            String cargo = shipTerm.getValue("cargo");
            //create player
            ret = new Ship(name, ship);
            ret.setLoadout(loadout);
            ret.setFaction(faction);
            ret.init(false);
            ret.addInitialCargo(cargo);
            //put it in the right system next to the start object
            if (near != null) {
                for (int b = 0; b < celestials.size(); b++) {
                    if (celestials.get(b).getName().matches(near)) {
                        ret.setX(celestials.get(b).getX() + rnd.nextInt(1600) - 800);
                        ret.setY(celestials.get(b).getY() + rnd.nextInt(1900) - 800);
                        break;
                    }
                }
            } else {
                //it was given specific xy coordinates i guess
                String sx = shipTerm.getValue("x");
                String sy = shipTerm.getValue("y");
                if (sx != null && sy != null) {
                    double tx = Double.parseDouble(sx);
                    double ty = Double.parseDouble(sy);
                    ret.setX(tx);
                    ret.setY(ty);
                } else {
                    //or not? just throw it somewhere.
                    ret.setX(rnd.nextInt(1000000));
                    ret.setY(rnd.nextInt(1000000));
                }
            }
            ret.setCurrentSystem(this);
        }
        return ret;
    }

    private Station makeStation(Term shipTerm) {
        Station ret = null;
        Random rnd = new Random();
        {
            String ship = shipTerm.getValue("ship");
            String near = shipTerm.getValue("near");
            String name = shipTerm.getValue("name");
            String faction = shipTerm.getValue("faction");
            //create player
            ret = new Station(name, ship);
            ret.setFaction(faction);
            //put it in the right system next to the start object
            if (near != null) {
                for (int b = 0; b < celestials.size(); b++) {
                    if (celestials.get(b).getName().matches(near)) {
                        ret.setX(celestials.get(b).getX() + rnd.nextInt(1600) - 800);
                        ret.setY(celestials.get(b).getY() + rnd.nextInt(1600) - 800);
                        break;
                    }
                }
            } else {
                //it was given specific xy coordinates i guess
                String sx = shipTerm.getValue("x");
                String sy = shipTerm.getValue("y");
                if (sx != null && sy != null) {
                    double tx = Double.parseDouble(sx);
                    double ty = Double.parseDouble(sy);
                    ret.setX(tx);
                    ret.setY(ty);
                } else {
                    //or not? just throw it somewhere.
                    ret.setX(rnd.nextInt(1000000));
                    ret.setY(rnd.nextInt(1000000));
                }
            }
            ret.setCurrentSystem(this);
        }
        return ret;
    }

    private Jumphole makeJumphole(Term planetTerm) {
        Jumphole ret = null;
        {
            String pName = planetTerm.getValue("name");
            String out = planetTerm.getValue("out");
            double px = Double.parseDouble(planetTerm.getValue("x"));
            double py = Double.parseDouble(planetTerm.getValue("y"));
            Jumphole hole = new Jumphole(pName, universe);
            hole.setX(px);
            hole.setY(py);
            hole.setOut(out);
            hole.setCurrentSystem(this);
            ret = hole;
        }
        return ret;
    }

    public ArrayList<Entity> getCelestials() {
        return celestials;
    }

    public void setCelestials(ArrayList<Entity> celestials) {
        this.celestials = celestials;
    }

    public void putEntityInSystem(Entity entity) {
        celestials.add(entity);
    }

    public void pullEntityFromSystem(Entity entity) {
        celestials.remove(entity);
    }

    @Override
    public void init(boolean loadedGame) {
        for (int a = 0; a < celestials.size(); a++) {
            celestials.get(a).init(loadedGame);
        }
    }

    @Override
    public void periodicUpdate(double tpf) {
        for (int a = 0; a < celestials.size(); a++) {
            celestials.get(a).periodicUpdate(tpf);
            if (celestials.get(a).getState() == Entity.State.DEAD) {
                //remove the entity
                celestials.remove(a);
            }
        }
    }

    @Override
    public void render(Graphics f, double dx, double dy) {
        //please never call this, talk to the entities directly.
    }

    @Override
    public State getState() {
        return State.ALIVE;
    }

    @Override
    public ArrayList<Rectangle> getBounds() {
        return new ArrayList<>();
    }

    @Override
    public boolean collideWith(Entity target) {
        return false;
    }

    @Override
    public boolean collideWith(Rectangle target) {
        return false;
    }

    @Override
    public double getX() {
        return x;
    }

    @Override
    public double getY() {
        return y;
    }

    @Override
    public void setX(double x) {
        this.x = (int) x;
    }

    @Override
    public void setY(double y) {
        this.y = (int) y;
    }

    @Override
    public String getName() {
        return name;
    }

    @Override
    public void setName(String name) {
        this.name = name;
    }

    @Override
    public void informOfCollisionWith(Entity target) {
        //?!
    }
}
