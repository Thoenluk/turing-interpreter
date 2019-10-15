/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package turinginterpreter;

import java.util.ArrayList;

/**
 *
 * @author Lukas
 */
public class Tape {

    private ArrayList<Character> content;
    private int headPosition;

    /**
     * Create new Tape object with given initial size and head position in the
     * middle of the list.
     *
     * @param initialLength The size for the tape to begin with. Note that all
     * tapes are functionally infinite, this is only to save computational time.
     * If initialLength is negative or less than 3, use 10 (standard) instead to
     * avoid provoking errors further down the line. (This program assumes there
     * is always at least one cell on either side of the head.)
     */
    public Tape(int initialLength) {
        if (initialLength < 3) {
            initialLength = 10;
        }
        this.content = new ArrayList<>(initialLength);
        for (int i = 0; i < initialLength; i++) {
            content.add(TuringInterpreter.EMPTY_CELL_CHAR);
        }
        this.headPosition = initialLength / 2;
    }

    /**
     * Create new Tape object with initial size 20, for when initial size isn't
     * relevant enough to specify.
     */
    public Tape() {
        this(20);
    }

    /**
     * Check if head is at either end of the tape and add empty cells (content
     * '_') to either side if needed.
     *
     * If this inserts a cell at the 0-index, increments headPosition to reflect
     * the tape shifting underneath the head.
     */
    private void extendTapeIfNecessary() {
        if (0 == headPosition) {
            content.add(0, TuringInterpreter.EMPTY_CELL_CHAR);
            headPosition++;
        }
        if (content.size() - 1 == headPosition) {
            content.add(TuringInterpreter.EMPTY_CELL_CHAR);
        }
    }

    /**
     * Returns the character currently under this tape's head.
     *
     * @return The character under the head, aka the active cell content.
     */
    public Character getCurrentCell() {
        return content.get(headPosition);
    }

    /**
     * Replace the char in the cell currently under the head with the given
     * char, unless the argument is WILDCARD_CHAR ('?') (That's not an emoji.)
     *
     * @param character The char to insert. If this is WILDCARD_CHAR ('?'), do
     * nothing.
     */
    private void setCurrentCell(Character character) {
        if (character != TuringInterpreter.WILDCARD_CHAR) {
            content.set(headPosition, character);
        }
    }

    /**
     * Moves the head one cell in the specified direction.
     *
     * @param direction Expected cases: 'l' for left (towards index 0), 'r' for
     * right (increasing index), 'n' to do nothing.
     */
    private void moveHead(char direction) {
        direction = Character.toLowerCase(direction);
        switch (direction) {
            case 'l':
                headPosition--;
                break;
            case 'r':
                headPosition++;
                break;
            case 'n':
                break;
            default:
                break; //NOP; Do nothing.
        }
        extendTapeIfNecessary();
    }

    /**
     * Sets the current cell to arg character and moves head in direction of arg
     * direction. See doc of setCurrentCell and moveHead for details.
     *
     * @param character The character to insert at this space.
     * @param direction The direction for the head to move.
     */
    public void executeStep(char character, char direction) {
        setCurrentCell(character);
        moveHead(direction);
    }

    /**
     * Reads a section of (range) characters to either side of the head and
     * returns it as a string. If there is not enough space on the tape to go in
     * the given distance, extends the tape.
     *
     * @param range How many chars to go to either side.
     * @return The assembled string.
     */
    public String getSectionOfTapeAsString(int range) {
        String tape = "";
        String newline = System.getProperty("line.separator");
        int i;
        for (i = 0; i < range; i++) {
            tape += " "; //Generate head position offset.
            moveHead('l');
        }
        tape += "V" + newline; //Denote head position.
        for (i = 0; i < 2 * range; i++) {
            tape += getCurrentCell();
            moveHead('r');
        }
        for (i = 0; i < range; i++) {
            moveHead('l');
        }
        return tape;
    }

    /**
     * Plonks the entire tape into a string and returns it.
     *
     * @return The tape's content as a string. Note this will inevitably contain
     * EMPTY_CELL_CHARs ('_'), so handle those.
     */
    public String getEntireTapeAsString() {
        String tape = "";
        String newline = System.getProperty("line.separator");
        for (int i = 0; i < headPosition; i++) {
            tape += " ";
        }
        tape += "V" + newline;
        for (int i = 0; i < content.size(); i++) {
            tape += content.get(i);
        }
        return tape;
    }

    /**
     * Starting at the current head position and proceeding right, write the
     * content of the given array to the tape.
     *
     * @param array The char array (usually a converted String) to be written.
     */
    public void writeCharArrayToTapeUnderHead(char[] array) {
        for (char character : array) {
            setCurrentCell(character);
            moveHead('r');
        }
        moveHead('r');
    }

    /**
     * Convert a given integer to binary form and write it to the tape, starting
     * under the head's current position and moving right.
     *
     * @param integer The integer to be written.
     */
    public void writeBinaryIntToTapeUnderHead(int integer) {
        writeCharArrayToTapeUnderHead(Integer.toBinaryString(integer).toCharArray());
    }

    /**
     * Convert a given integer to unary form and write it to the tape, starting
     * under the head's current position and moving right.
     *
     * @param integer The integer to be written.
     */
    public void writeUnaryIntToTapeUnderHead(int integer) {
        char[] unary = new char[integer + 1];
        for (int i = 0; i < integer; i++) {
            unary[i] = '0';
        }
        writeCharArrayToTapeUnderHead(unary);
    }

    public void moveHeadToFirstNonEmptyCell() {
        headPosition = content.size() / 2;
        for (int i = 0; i < content.size(); i++) {
            if (content.get(i) != TuringInterpreter.EMPTY_CELL_CHAR) {
                headPosition = i;
                break;
            }
        }
    }
}
