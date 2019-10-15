/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package turinginterpreter;

import java.util.ArrayList;

/**
 *
 * @author Lukas A single state of the Turing machine. Can be accepting or not
 * and has zero or more transitions away from this state.
 */
public class State {

    private boolean isAccepting;
    private ArrayList<Transition> transitions;
    private String name;

    /**
     *
     * @param isAccepting If computation ends in this state, has the machine
     * truly halted (true) or has an error occurred (false) due to a lack of
     * matching transition?
     * @param transitions The transitions leading OUT of this state.
     */
    public State(boolean isAccepting, ArrayList<Transition> transitions, String name) {
        this.isAccepting = isAccepting;
        this.transitions = transitions;
        this.name = name;
    }

    @Override
    public String toString() {
        String finishedString = Boolean.toString(isAccepting);
        finishedString += TuringInterpreter.TRANSITION_SEPARATOR_CHAR;
        for (Transition transition : transitions) {
            finishedString += transition.toString();
        }
        return finishedString;
    }

    public boolean getIsAccepting() {
        return isAccepting;
    }

    public ArrayList<Transition> getTransitions() {
        return transitions;
    }

    public String getName() {
        return name;
    }

    public void setIsAccepting(boolean isAccepting) {
        this.isAccepting = isAccepting;
    }

    public void addTransition(Transition transition) {
        this.transitions.add(transition);
    }

    /**
     * Removes one or more transitions at the indices specified in the argument.
     *
     * @param indices The indices of transitions to be removed.
     */
    public void removeTransitions(int[] indices) {
        ArrayList<Transition> removingTransitions = new ArrayList<Transition>();
        for (int i = 0; i < indices.length; i++) {
            removingTransitions.add(transitions.get(indices[i]));
        }
        transitions.removeAll(removingTransitions);
    }

    /**
     * Finds the transition with the most specific trigger (fewest '?' wildcard
     * characters) which matches the given content. In case of a tie, the
     * transition which has the least total index of wildcards gets priority,
     * meaning [1, ?, 0, ?] wins out over [1, ?, ?, 0] if both match and there
     * is no transition using 0 or 1 wildcards.
     *
     * @param content The tape content to be matched against.
     * @return The most specific transition. Null if no transition at all
     * matches the given content.
     */
    public Transition findMostSpecificTransition(char[] content) {
        Transition msTransition = null;
        for (Transition transition : transitions) {
            if (transition.triggerMatches(content) && (msTransition == null || transition.getPriority() < msTransition.getPriority())) {
                msTransition = transition;
            }
        }
        return msTransition;
    }

    /**
     * Turns this state's transitions into an array of more human-readable
     * Strings.
     *
     * @return The array of transition Strings.
     */
    public String[] getTransitionStringArray() {
        String[] list = new String[transitions.size()];
        for (int i = 0; i < list.length; i++) {
            list[i] = transitions.get(i).getReadableForm();
        }
        return list;
    }
}
