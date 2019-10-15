/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package turinginterpreter;

import java.util.Arrays;

/**
 *
 * @author Lukas A transition, that is, moving from the original state to
 * another one. This class stores the parameters needed to update the machine's
 * state in each step.
 */
public class Transition {

    private final char[] trigger;
    private final char[] headMovement;
    private final char[] toBeWritten;
    private final int nextState;
    private final int priority;

    /**
     * Create new Transition object.
     *
     * @param trigger What has to be on each tape to trigger this transition.
     * @param headMovement In which direction each tape's head should move.
     * @param toBeWritten What this transition should write to each tape.
     * @param nextState The state this transition leads into.
     *
     * Note that "?" (question mark character) is a wildcard, meaning: In
     * trigger, any character is accepted for tapes with a wildcard in their
     * slot. In toBeWritten, tapes with a wildcard in their slot are not
     * modified.
     */
    public Transition(char[] trigger, char[] headMovement, char[] toBeWritten, int nextState) {
        this.trigger = trigger;
        this.headMovement = headMovement;
        this.toBeWritten = toBeWritten;
        this.nextState = nextState;
        this.priority = calculatePriority();
    }

    public char[] getHeadMovement() {
        return headMovement;
    }

    public char[] getToBeWritten() {
        return toBeWritten;
    }

    public int getNextState() {
        return nextState;
    }

    public int getPriority() {
        return priority;
    }

    /**
     * Checks if this transition would trigger on the given content, that is,
     * all characters of trigger are either '?' or the same as what is in
     * content in the corresponding space.
     *
     * @param content The content to be evaluated for triggering.
     * @return True if it would trigger, false otherwise.
     */
    public boolean triggerMatches(char[] content) {
        boolean matches = true; //Consideration: Throw exception or something if
        //length of content and trigger don't match? Currently not possible due
        //to earlier validation.
        for (int i = 0; i < content.length; i++) {
            if (trigger[i] != TuringInterpreter.WILDCARD_CHAR) {
                matches = matches && (content[i] == trigger[i]);
            } //If it is ?, ignore this spot as it is a wildcard.
        }
        return matches;
    }

    /**
     * Gets the number of TuringInterpreter.WILDCARD_CHARs present in this
     * transition's trigger.
     *
     * @return the number of wildcards.
     */
    private int getNumberOfWildcardsInTrigger() {
        int wildcards = 0;
        for (char character : trigger) {
            if (character == TuringInterpreter.WILDCARD_CHAR) {
                wildcards++;
            }
        }
        return wildcards;
    }

    @Override
    public String toString() {
        //TODO: Fix order such that toBeWritten comes before headMovement.
        String finishedString = Arrays.toString(trigger);
        finishedString += TuringInterpreter.FIELD_SEPARATOR_CHAR;
        finishedString += Arrays.toString(headMovement);
        finishedString += TuringInterpreter.FIELD_SEPARATOR_CHAR;
        finishedString += Arrays.toString(toBeWritten);
        finishedString += TuringInterpreter.FIELD_SEPARATOR_CHAR;
        finishedString += Integer.toString(nextState);
        finishedString += TuringInterpreter.TRANSITION_SEPARATOR_CHAR;
        return finishedString;
    }

    /**
     * Turns this transition into a more human-readable String.
     *
     * @return The readable String formatted as [trigger] -> [toBeWritten],
     * [head movement] -> nextState. Example: [0, 0] -> [2, M], [r, r] -> 1
     */
    public String getReadableForm() {
        String readableForm = Arrays.toString(trigger);
        readableForm += " -> ";
        readableForm += Arrays.toString(toBeWritten);
        readableForm += ", ";
        readableForm += Arrays.toString(headMovement);
        readableForm += " -> ";
        readableForm += nextState;
        return readableForm;
    }

    public String getTriggerAsString() {
        String triggerString = "";
        triggerString += trigger[0];
        for (int i = 1; i < trigger.length; i++) {
            triggerString += ",";
            triggerString += trigger[i];
        }
        return triggerString;
    }

    /**
     * Calculates this transition's priority based on its number and position of
     * wildcards and returns it.
     *
     * @return The transition's priority. 0 if no wildcards are present,
     * otherwise above 0. A transition with fewer wildcards than another one
     * will always return a lesser number. In the case of a tie, the transition
     * whose wildcards are at greater indices will return a lesser number.
     */
    private int calculatePriority() {
        int nrOfWildcards = getNumberOfWildcardsInTrigger();
        int prio = nrOfWildcards * trigger.length;
        for (int i = 0; i < trigger.length; i++) {
            if (trigger[i] == TuringInterpreter.WILDCARD_CHAR) {
                prio += trigger.length - i;
            }
        }
        return prio;
    }
}
