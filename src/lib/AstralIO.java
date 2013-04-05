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
 * Reads/writes file data. Nuff said. Nathan Wiehoff, masternerdguy@yahoo.com
 */
package lib;

import java.applet.Applet;
import java.applet.AudioClip;
import java.awt.Image;
import java.awt.Toolkit;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.net.URISyntaxException;
import java.net.URL;
import javax.sound.sampled.AudioInputStream;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.Clip;
import universe.Universe;

public class AstralIO implements Serializable {

    public static final String RESOURCE_DIR = "/resource/";

    /*
     * Text
     */
    public static String readFile(String target, boolean local) {
        String ret = "";
        //Attemps to load an external file (local = false) or a file from within the archive
        if (local) {
            try {
                ret = readTextFromJar(target);
            } catch (Exception e) {
                e.printStackTrace();
            }
        } else {
            try {
                try (BufferedReader in = new BufferedReader(new FileReader(target))) {
                    String str;
                    while ((str = in.readLine()) != null) {
                        ret += str + "\n";
                    }
                }

            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        return ret;
    }

    public void writeFile(String target, String text) {
        try {
            FileWriter fstream = new FileWriter(target);
            BufferedWriter out = new BufferedWriter(fstream);
            out.write(text);
            out.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static String readTextFromJar(String target) {
        InputStream is = null;
        BufferedReader br = null;
        String line;
        String ret = "";

        try {
            is = AstralIO.class.getResourceAsStream(RESOURCE_DIR + target);
            br = new BufferedReader(new InputStreamReader(is));
            while (null != (line = br.readLine())) {
                ret = ret + line + "\n";
            }
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            try {
                if (br != null) {
                    br.close();
                }
                if (is != null) {
                    is.close();
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        return ret;
    }

    /*
     * Images
     */
    public Image loadImage(String target) throws NullPointerException, URISyntaxException {
        Image tmp = null;
        {
            System.out.println("Loading image resource " + RESOURCE_DIR + target);
            URL url = getClass().getResource(RESOURCE_DIR + target);
            File file = new File(url.toURI());
            if (file.exists()) {
                tmp = Toolkit.getDefaultToolkit().getImage(url);
            } else {
                throw new NullPointerException();
            }
        }
        return tmp;
    }

    /*
     * Audio
     */
    public static synchronized Clip getClip(String target) {
        try {
            Clip clip = AudioSystem.getClip();
            AudioInputStream inputStream = AudioSystem.getAudioInputStream(
                    AstralIO.class.getResourceAsStream(RESOURCE_DIR + target));
            clip.open(inputStream);
            return clip;
        } catch (Exception e) {
            System.err.println(e.getMessage());
        }
        return null;
    }
    
    public synchronized AudioClip getAudioClip(String target) {
        URL url = getClass().getResource(RESOURCE_DIR + target);
        return Applet.newAudioClip(url);
    }

    public static synchronized void playSound(Clip clip) {
        clip.start();
    }

    public static synchronized void playSound(String target) {
        //new Thread(new Runnable() {
        //public void run() {
        try {
            Clip clip = AudioSystem.getClip();
            AudioInputStream inputStream = AudioSystem.getAudioInputStream(
                    AstralIO.class.getResourceAsStream(RESOURCE_DIR + target));
            clip.open(inputStream);
            clip.start();
        } catch (Exception e) {
            System.err.println(e.getMessage());
        }
        // }
        //}).start();
    }

    /*
     * Binary data
     */
    public static InputStream getStream(String target) {
        return AstralIO.class.getResourceAsStream(RESOURCE_DIR + "/" + target);
    }

    public void saveGame(Universe universe, String gameName) throws Exception {
        String home = System.getProperty("user.home") + "/.highalbedo/";
        //create the subfolder
        File folder = new File(home);
        if (!folder.exists()) {
            folder.mkdir();
        }
        //generate serializable universe
        Everything everything = new Everything(universe);
        //serialize universe
        FileOutputStream fos = new FileOutputStream(home + gameName);
        ObjectOutputStream oos = new ObjectOutputStream(fos);
        oos.writeObject(everything);
    }

    public class Everything implements Serializable {
        /*
         * This class contains everything in the universe in a temporary container
         * useful for serialization.
         */

        protected Universe universe;

        public Everything(Universe universe) {
            this.universe = universe;
        }

        public Universe getUniverse() {
            return universe;
        }

        public void setUniverse(Universe universe) {
            this.universe = universe;
        }
    }
}
