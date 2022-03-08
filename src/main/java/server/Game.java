package server;

import java.util.Scanner;
import java.util.*;
import java.io.*;

/**
 * Class: Game Description: Game class that can load an ascii image Class can be
 * used to hold the persistent state for a game for different threads
 * synchronization is not taken care of . You can change this Class in any way
 * you like or decide to not use it at all I used this class in my
 * SockBaseServer to create a new game and keep track of the current image
 * evenon differnt threads. My threads each get a reference to this Game
 */

public class Game {
    private int idx = 0; // current index where x could be replaced with original
    private int idxMax; // max index of image
    private char[][] original; // the original image
    private char[][] hidden; // the hidden image
    private int col; // columns in original, approx
    private int row; // rows in original and hidden
    private boolean won; // if the game is won or not
    private List<String> files = new ArrayList<String>(); // list of files, each file has one image

    public Game() {
        // you can of course add more or change this setup completely. You are totally
        // free to also use just Strings in your Server class instead of this class
        won = true; // setting it to true, since then in newGame() a new image will be created
        files.add("battle1.txt");
        files.add("battle2.txt");
        files.add("battle3.txt");

    }

    /**
     * Sets the won flag to true
     * 
     * @param args Unused.
     * @return Nothing.
     */
    public synchronized void setWon() {
        won = true;
    }

    /**
     * Method loads in a new image from the specified files and creates the hidden
     * image for it.
     * 
     * @return Nothing.
     */
    public synchronized void newGame() throws Exception {
        System.out.println("started a new game");
        idx = 0;
        won = false;
        List<String> rows = new ArrayList<String>();

        try {
            // loads one random image from list
            Random rand = new Random();
            col = 0;
            int randInt = rand.nextInt(files.size());
            System.out.println("File " + files.get(randInt));
            File file = new File(Game.class.getResource("/" + files.get(randInt)).getFile());
            BufferedReader br = new BufferedReader(new FileReader(file));
            String line;
            while ((line = br.readLine()) != null) {
                if (col < line.length()) {
                    col = line.length();
                }
                rows.add(line);
            }
        } catch (Exception e) {
            System.out.println("File load error: " + e); // extremely simple error handling, you can do better if
                                                         // you like.
        }

        // this handles creating the orinal array and the hidden array in the correct
        // size
        String[] rowsASCII = rows.toArray(new String[0]);

        row = rowsASCII.length;

        // Generate original array by splitting each row in the original array.
        original = new char[row][col];
        for (int i = 0; i < row; i++) {
            char[] splitRow = rowsASCII[i].toCharArray();
            for (int j = 0; j < splitRow.length; j++) {
                original[i][j] = splitRow[j];
            }
        }

        // Generate Hidden array with X's (this is the minimal size for columns)
        hidden = new char[row][col];
        for (int i = 0; i < row; i++) {
            for (int j = 0; j < col; j++) {
                hidden[i][j] = 'X';
            }
        }
        setIdxMax(col * row);
    }

    public synchronized String getAnswer() {
        StringBuilder sb = new StringBuilder();
        for (char[] subArray : original) {
            sb.append(subArray);
            sb.append("\n");
        }
        return sb.toString();
    }
    
    /**
     * Method returns the String of the current hidden image
     * 
     * @return String of the current hidden image
     */
    public synchronized String getImage() {
        StringBuilder sb = new StringBuilder();
        for (char[] subArray : hidden) {
            sb.append(subArray);
            sb.append("\n");
        }
        return sb.toString();
    }

    /**
     * Method replaces the row and column value given with te value of the original.
     * If it was a part of the ship the idx will be incremented, so it can keep
     * track of how many ship parts were already found
     */
    public synchronized String replaceOneCharacter(int row, int column) {
        hidden[row][column] = original[row][column];
        if (hidden[row][column] == '.') {
            hidden[row][column] = ' ';
        }
        if (original[row][column] == 'x') {
            idx++;
            hidden[row][column] = '!';
            System.out.println("hit");
        }

        return (getImage());
    }

    public synchronized int getIdxMax() {
        return idxMax;
    }

    public synchronized void setIdxMax(int idxMax) {
        this.idxMax = idxMax;
    }

    public synchronized int getIdx() {
        return idx;
    }

    public synchronized void setIdx(int idx) {
        this.idx = idx;
    }
}
