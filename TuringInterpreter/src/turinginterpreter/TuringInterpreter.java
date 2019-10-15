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
public class TuringInterpreter {

    static final char EMPTY_CELL_CHAR = '_';
    static final char WILDCARD_CHAR = '?';
    static final char TRANSITION_SEPARATOR_CHAR = ';';
    static final char FIELD_SEPARATOR_CHAR = '|';
    static final char FIELD_VALUE_SEPARATOR_CHAR = ',';

    static int tapeInitialLength = 50;
    static int currentState = 0;

    static State[] states;
    static Tape[] tapes;

    /**
     * Finds the most specific transition that matches the current tape content
     * from the current state and executes it.
     *
     * @return Status code. On a successful transition, returns 0. Otherwise, if
     * the machine has halted, returns -1.
     */
    static int executeStep() {
        char[] content = getTapeContent();
        Transition nextTransition = states[currentState].findMostSpecificTransition(content);
        if (nextTransition != null) {
            executeTransition(nextTransition);
            return 0;
        } else {
            return -1;
        }
    }

    /**
     * Parses String-based data into the states and tapes arrays. This is
     * intended to be used mostly on data generated by the program (though it
     * doesn't have to be) and thus is rather simple and strict.
     * <br>
     *
     *
     * @param saveData The data to be parsed. Each String represents a line in
     * the saved file and a state of the TM. States are implicitly numbered in
     * increasing order. Format: {Boolean showing whether this state is
     * accepting};{Any number of the following transition block:
     * [{Comma-separated char array describing the trigger}]|[{C-s char array
     * describing the head movement}]|[{C-s char array describing the chars to
     * be written}]|{int index of the state this transitions to};
     * <br>
     * It's easier to show than explain, so here's an example (this is not a
     * real TM.):
     * <br>
     * false;[0,?]|[r,l]|[_,_]|1;[1,_]|[r,r]|[_,_]|9; <br>
     * false;[0,1]|[r,r]|[_,0]|2;[1,0]|[r,r]|[_,1]|8; <br>
     * false;[0,0]|[r,n]|[0,0]|2;[1,1]|[r,r]|[1,1]|3;
     */
    public static void parseDataIntoMachine(ArrayList<String> saveData) {
        //TODO: Fix order such that toBeWritten comes before headMovement.
        int nrOfTapes = 0;
        ArrayList<Transition> transitions;
        states = new State[saveData.size()];
        for (int j = 0; j < saveData.size(); j++) {
            transitions = new ArrayList<>();
            String[] orders = saveData.get(j).split("" + TRANSITION_SEPARATOR_CHAR);
            for (String order : orders) {
                if (!(order.equals("false") | order.equals("true") | order.equals(""))) {
                    String[] stringData = order.split("\\" + FIELD_SEPARATOR_CHAR);
                    for (int k = 0; k < 3; k++) {
                        stringData[k] = stringData[k].substring(1, stringData[k].length() - 1); //remove brackets
                    }
                    if (0 == nrOfTapes) { //Since each line must specify all tapes
                        //even if it contains wildcards, we can pull the number
                        //of tapes used off of any of them.
                        nrOfTapes = (stringData[0].length() + 1) / 2;
                    }
                    char[] trigger = new char[nrOfTapes];
                    char[] headMovement = new char[nrOfTapes];
                    char[] toBeWritten = new char[nrOfTapes];
                    for (int i = 0; i < nrOfTapes; i++) {
                        trigger[i] = stringData[0].charAt(2 * i);
                        headMovement[i] = stringData[1].charAt(2 * i);
                        toBeWritten[i] = stringData[2].charAt(2 * i);
                    }
                    int nextState = Integer.parseInt(stringData[3]);
                    transitions.add(new Transition(trigger, headMovement, toBeWritten, nextState));
                }
            }
            states[j] = new State(Boolean.parseBoolean(orders[0]), transitions, "State " + j);
        }
        tapes = new Tape[nrOfTapes];
        for (int k = 0; k < nrOfTapes; k++) {
            tapes[k] = new Tape();
        }
    }

    /**
     * Turns the machine's current states into a String for saving to a file.
     * Tape content is not saved as that would be rather pointless.
     *
     * @return The finished String. Format example:
     * <br>
     * false;[0, ?]|[r, l]|[_, _]|1;[1, _]|[r, r]|[_, _]|9; <br>
     * false;[0, 1]|[r, r]|[_, 0]|2;[1, 0]|[r, r]|[_, 1]|8; <br>
     * false;[0, 0]|[r, n]|[0, 0]|2;[1, 1]|[r, r]|[1, 1]|3; <br>
     * Note the spaces in the middle of the arrays will be edited out by the
     * text parser and come from how Java stringifies arrays.
     */
    public static String machineToString() {
        String newline = System.getProperty("line.separator");
        String saveData = "";
        for (State state : states) {
            saveData += state.toString();
            saveData += newline;
        }
        saveData = saveData.substring(0, saveData.length());
        return saveData;
    }

    public static String getTapesAsString() {
        String content = "";
        String newline = System.lineSeparator();
        for (int i = 0; i < tapes.length; i++) {
            content += tapes[i].getEntireTapeAsString();
            content += newline;
        }
        return content;
    }

    /**
     * Sets the number of tapes used to argument and creates empty tapes
     * accordingly.
     *
     * @param nrOfTapes The number of tapes the machine should use.
     */
    public static void initialiseNrOfTapes(int nrOfTapes) {
        tapes = new Tape[nrOfTapes];
        for (int i = 0; i < nrOfTapes; i++) {
            tapes[i] = new Tape();
        }
    }

    /**
     * Replaces all tapes with new blank tapes.
     */
    public static void emptyTapes() {
        initialiseNrOfTapes(tapes.length);
    }

    /**
     * Writes the strings in arg array to the tapes, in order. Previous content
     * is erased. Note that array must have exactly as many elements as the
     * machine has tapes.
     *
     * @param array The strings to be written. array[0] is written to tape 0
     * (the first) and so on.
     */
    public static void writeStringArrayToTapes(String[] array) {
        emptyTapes();
        for (int i = 0; i < tapes.length; i++) {
            tapes[i].writeCharArrayToTapeUnderHead(array[i].toCharArray());
            tapes[i].moveHeadToFirstNonEmptyCell();
        }
        //TODO: At least pretend to handle errors.
    }

    /**
     * Writes the ints in arg array to the tapes using binary notation, in
     * order. Previous content is erased. Note that array must have exactly as
     * many elements as the machine has tapes.
     *
     * @param array The ints to be written to the tapes as binary notation.
     */
    public static void writeBinaryArrayToTapes(int[][] array) {
        emptyTapes();
        for (int i = 0; i < tapes.length; i++) {
            for (int j = 0; j < array[i].length; j++) {
                tapes[i].writeBinaryIntToTapeUnderHead(array[i][j]);
            }
            tapes[i].moveHeadToFirstNonEmptyCell();
        }
    }

    /**
     * Collects the char on each tape's currently active cell and returns them
     * all in order as an array.
     *
     * @return The tape content.
     */
    private static char[] getTapeContent() {
        char[] content = new char[tapes.length];
        for (int i = 0; i < tapes.length; i++) {
            content[i] = tapes[i].getCurrentCell();
        }
        return content;
    }

    /**
     * Performs a given transition on each tape involved, writing the new
     * character and moving the head on each tape.
     *
     * @param transition The Transition to be executed.
     */
    private static void executeTransition(Transition transition) {
        char[] headMovement = transition.getHeadMovement();
        char[] toBeWritten = transition.getToBeWritten();
        for (int i = 0; i < headMovement.length; i++) {
            tapes[i].executeStep(toBeWritten[i], headMovement[i]);
        }
        currentState = transition.getNextState();
    }
}