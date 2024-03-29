/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package turinginterpreter;

import java.awt.Component;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import javax.swing.JFileChooser;
import javax.swing.SpinnerNumberModel;
import javax.swing.Timer;

class MyCustomFilter extends javax.swing.filechooser.FileFilter {

    @Override
    public boolean accept(File file) {
        // Allow only directories, or files with ".txt" extension
        return file.isDirectory() || file.getAbsolutePath().endsWith(".txt");
    }

    @Override
    public String getDescription() {
        // This description will be displayed in the dialog,
        // hard-coded = ugly, should be done via I18N
        return "Text documents (*.txt)";
    }
}

/**
 *
 * @author Lukas
 */
public class TuringInterpreterUI extends javax.swing.JFrame {

    private static State[] states = TuringInterpreter.states;
    private static Tape[] tapes = TuringInterpreter.tapes;
    private static int currentState = 0;
    private Timer timer;

    /**
     * Creates new form TuringInterpreterUI
     */
    public TuringInterpreterUI() {
        initComponents();
        timer = new Timer(1000, new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent ae) {
                int retval = executeMachineStep();
                if (retval != 0) {
                    timer.stop();
                    runMachineButton.setText("Run machine");
                }
            }
        });
        timer.setInitialDelay(0);
    }

    private void resetNrOfTapesDialog() {
        nrOfTapesTapeSpinner.setValue(1);
        nrOfTapesStateSpinner.setValue(1);
    }

    private void resetStateBuilderDialog() {
        currentState = 0;
    }

    private void resetTransitionBuilderDialog() {
        transitionBuilderTriggerTF.setText("");
        transitionBuilderHeadMovTF.setText("");
        transitionBuilderToBeWrittenTF.setText("");
        transitionBuilderNextStateDropdown.setSelectedIndex(0);
    }

    private void resetMachine() {
        TuringInterpreter.currentState = 0;
        currentState = 0;
        ((MachineDiagram) machineDisplayPanel).setCurrentState(0);
        machineDisplayPanel.repaint();
        TuringInterpreter.emptyTapes();
        updateTapesDisplayTA();
    }

    private void updateTapesDisplayTA() {
        this.tapeDisplayTextArea.setText(TuringInterpreter.getTapesAsString());
    }

    private void enableInputs() {
        modMachineMI.setEnabled(true);
        modifyMachineButton.setEnabled(true);
        resetMachineMI.setEnabled(true);
        stepMachineButton.setEnabled(true);
        runMachineButton.setEnabled(true);
        this.executionSpeedlabel.setEnabled(true);
        executionSpeedDropdown.setEnabled(true);
        this.tapeMenu.setEnabled(true);
        this.resetMachineButton.setEnabled(true);
    }

    private int executeMachineStep() {
        int retval = TuringInterpreter.executeStep();
        currentState = TuringInterpreter.currentState;
        ((MachineDiagram) machineDisplayPanel).setCurrentState(currentState);
        this.machineDisplayPanel.repaint();
        this.updateTapesDisplayTA();
        if (retval == -1) {
            this.handleMachineHalting();
        }
        return retval;
    }

    private void updateStateBuilderStateTransitionList() {
        if (null != states && 0 < states.length) {
            stateBuilderStateTransitionList.setListData(states[currentState].getTransitionStringArray());
        } else {
            stateBuilderStateTransitionList.setListData(new String[0]);
        }
    }

    private String getAcceptingLabelForCurrentState() {
        if (null == states) {
            return "State 0 is currently NOT ACCEPTING.";
        }
        String label = "State " + currentState + " is currently";
        if (!states[currentState].getIsAccepting()) {
            label += " NOT";
        }
        label += " ACCEPTING.";
        return label;
    }

    private void nrOfTapesDialogHandleConfirm() {
        TuringInterpreter.initialiseNrOfTapes((int) nrOfTapesTapeSpinner.getValue());
        tapes = TuringInterpreter.tapes;
        if (null == states) {
            states = new State[(int) nrOfTapesStateSpinner.getValue()];
            for (int i = 0; i < states.length; i++) {
                states[i] = new State(false, new ArrayList<Transition>(), "State " + i);
            }
        } else {
            State[] holderArray = states;
            states = new State[(int) nrOfTapesStateSpinner.getValue()];
            for (int i = 0; i < holderArray.length && i < states.length; i++) {
                states[i] = holderArray[i];
            }
            for (int i = holderArray.length; i < states.length; i++) {
                states[i] = new State(false, new ArrayList<Transition>(), "State " + i);
            }
        }
        nrOfTapesDialog.setVisible(false);
        stateBuilderDialog.setVisible(true);
    }

    private void transitionBuilderDialogHandleConfirm() {
        boolean transitionIsUnique = true, lengthFits = true, formatMatches = true;
        String[] inputData = new String[3];
        inputData[0] = this.transitionBuilderTriggerTF.getText();
        inputData[1] = this.transitionBuilderToBeWrittenTF.getText();
        inputData[2] = this.transitionBuilderHeadMovTF.getText();
        for (int i = 0; i < 3; i++) {
            inputData[i] = inputData[i].replaceAll(" +", "");
            lengthFits = lengthFits && (2 * tapes.length - 1 == inputData[i].length());
        }
        inputData[2] = inputData[2].toUpperCase();
        for (Transition transition : states[currentState].getTransitions()) {
            transitionIsUnique = transitionIsUnique && !(transition.getTriggerAsString().equals(inputData[0]));
        }
        formatMatches = formatMatches && inputData[0].matches("[^,](,[^,])*");
        formatMatches = formatMatches && inputData[1].matches("[^,](,[^,])*");
        formatMatches = formatMatches && inputData[2].matches("[LNR](,[LNR])*");
        if (transitionIsUnique && lengthFits && formatMatches) {
            char[][] data = new char[3][tapes.length];
            for (int i = 0; i < 3; i++) {
                for (int j = 0; j < tapes.length; j++) {
                    data[i][j] = inputData[i].charAt(2 * j);
                }
            }
            int nextState = this.transitionBuilderNextStateDropdown.getSelectedIndex();
            states[currentState].addTransition(new Transition(data[0], data[1], data[2], nextState));
            resetTransitionBuilderDialog();
            updateStateBuilderStateTransitionList();
            transitionBuilderDialog.setVisible(false);
        } else {
            //Display error message to be assembled.
            String errorMessage = "This transition is not valid. Possible causes:\n\n";
            if (!lengthFits) {
                errorMessage += "- One or more input fields have too few or too many characters in them. They should contain exactly ";
                errorMessage += 2 * tapes.length - 1;
                errorMessage += " characters (not counting spaces): ";
                errorMessage += tapes.length;
                errorMessage += " input/output characters and ";
                errorMessage += tapes.length - 1;
                errorMessage += " commas.\n\n";
            }
            if (!transitionIsUnique) {
                errorMessage += "- A transition with that trigger from this state already exists. Delete that transition first.\n\n";
            }
            if (!formatMatches) {
                errorMessage += "- One or more input fields is formatted incorrectly. Please use only comma-separated single characters.\n";
                errorMessage += "Additionally, the head movement field must contain only L, N, or R, separated by commas.";
            }
            transitionBuilderErrorDialogTF.setText(errorMessage);
            transitionBuilderErrorDialog.pack();
            transitionBuilderErrorDialog.setVisible(true);
        }
    }

    private void handleMachineHalting() {
        String message = "The machine has halted in state ";
        message += currentState;
        if (states[currentState].getIsAccepting()) {
            message += ". This is an accepting state, so presumably all is well.";
        } else {
            message += ". This is not an accepting state.";
        }
        message += " Save tape content?";
        machineHasHaltedDialogText.setText(message);
        machineHasHaltedDialog.pack();
        machineHasHaltedDialog.setVisible(true);
        stepMachineButton.setEnabled(false);
        runMachineButton.setEnabled(false);
    }

    /**
     * This method is called from within the constructor to initialise the form.
     * WARNING: Do NOT modify this code. The content of this method is always
     * regenerated by the Form Editor.
     */
    @SuppressWarnings("unchecked")
    // <editor-fold defaultstate="collapsed" desc="Generated Code">//GEN-BEGIN:initComponents
    private void initComponents() {

        fileChooserWindow = new javax.swing.JFileChooser();
        nrOfTapesDialog = new javax.swing.JDialog();
        nrOfTapesOKButton = new javax.swing.JButton();
        nrOfTapesLabel = new javax.swing.JLabel();
        nrOfTapesCancelButton = new javax.swing.JButton();
        nrOfTapesTapeLabel = new javax.swing.JLabel();
        nrOfTapesStateLabel = new javax.swing.JLabel();
        nrOfTapesStateSpinner = new javax.swing.JSpinner();
        nrOfTapesTapeSpinner = new javax.swing.JSpinner();
        stateBuilderDialog = new javax.swing.JDialog();
        stateBuilderStateSelectorDropdown = new javax.swing.JComboBox<>();
        stateBuilderStateSelectorLabel = new javax.swing.JLabel();
        stateBuilderToggleAcceptingButton = new javax.swing.JButton();
        stateBuilderAcceptingLabel = new javax.swing.JLabel();
        stateBuilderStateTransitionsLabel = new javax.swing.JLabel();
        jScrollPane1 = new javax.swing.JScrollPane();
        stateBuilderStateTransitionList = new javax.swing.JList<>();
        stateBuilderAddTransitionButton = new javax.swing.JButton();
        stateBuilderRemoveTransitionButton = new javax.swing.JButton();
        stateBuilderChangeNrOfTapesStatesButton = new javax.swing.JButton();
        stateBuilderFinishMachineButton = new javax.swing.JButton();
        stateBuilderCancelButton = new javax.swing.JButton();
        transitionBuilderDialog = new javax.swing.JDialog();
        transitionBuilderContainerPanel = new javax.swing.JPanel();
        transitionBuilderHeadMovLabel = new javax.swing.JLabel();
        transitionBuilderHeadMovTF = new javax.swing.JTextField();
        transitionBuilderToBeWrittenLabel = new javax.swing.JLabel();
        transitionBuilderToBeWrittenTF = new javax.swing.JTextField();
        transitionBuilderOpenHelpButton = new javax.swing.JButton();
        transitionBuilderNextStateLabel = new javax.swing.JLabel();
        transitionBuilderNextStateDropdown = new javax.swing.JComboBox<>();
        transitionBuilderDoneButton = new javax.swing.JButton();
        transitionBuilderTriggerLabel = new javax.swing.JLabel();
        transitionBuilderTriggerTF = new javax.swing.JTextField();
        transitionBuilderCancelButton = new javax.swing.JButton();
        transitionBuilderHelpDialog = new javax.swing.JDialog();
        jScrollPane2 = new javax.swing.JScrollPane();
        transitionBuilderHelpText = new javax.swing.JTextArea();
        transitionBuilderHelpOKButton = new javax.swing.JButton();
        transitionBuilderErrorDialog = new javax.swing.JDialog();
        jScrollPane3 = new javax.swing.JScrollPane();
        transitionBuilderErrorDialogTF = new javax.swing.JTextArea();
        transitionBuilderErrorDialogOKButton = new javax.swing.JButton();
        machineHasHaltedDialog = new javax.swing.JDialog();
        jScrollPane4 = new javax.swing.JScrollPane();
        machineHasHaltedDialogText = new javax.swing.JTextArea();
        machineHasHaltedDialogDisplayTapesButton = new javax.swing.JButton();
        machineHasHaltedialogSaveTapesButton = new javax.swing.JButton();
        machineHasHaltedDialogOKButton = new javax.swing.JButton();
        tapeContentDisplayDialog = new javax.swing.JDialog();
        jScrollPane5 = new javax.swing.JScrollPane();
        tapeContentDisplayText = new javax.swing.JTextArea();
        tapeContentDisplayOKButton = new javax.swing.JButton();
        itPlaintextDialog = new javax.swing.JDialog();
        itPlaintextDialogLabel1 = new javax.swing.JLabel();
        itPlaintextDialogLabel2 = new javax.swing.JLabel();
        jScrollPane6 = new javax.swing.JScrollPane();
        itPlaintextTextArea = new javax.swing.JTextArea();
        itPlaintextOKButton = new javax.swing.JButton();
        itPlaintextCancelButton = new javax.swing.JButton();
        itPlaintextDialogLabel3 = new javax.swing.JLabel();
        itBinaryDialog = new javax.swing.JDialog();
        itBinaryDialogLabel1 = new javax.swing.JLabel();
        itBinaryDialogLabel2 = new javax.swing.JLabel();
        jScrollPane7 = new javax.swing.JScrollPane();
        itBinaryTextArea = new javax.swing.JTextArea();
        itBinaryOKButton = new javax.swing.JButton();
        itBinaryCancelButton = new javax.swing.JButton();
        itBinaryDialogLabel3 = new javax.swing.JLabel();
        itBinaryDialogLabel4 = new javax.swing.JLabel();
        itUnaryDialog = new javax.swing.JDialog();
        itUnaryDialogLabel1 = new javax.swing.JLabel();
        itUnaryDialogLabel2 = new javax.swing.JLabel();
        jScrollPane8 = new javax.swing.JScrollPane();
        itUnaryTextArea = new javax.swing.JTextArea();
        itUnaryOKButton = new javax.swing.JButton();
        itUnaryCancelButton = new javax.swing.JButton();
        itUnaryDialogLabel3 = new javax.swing.JLabel();
        itUnaryDialogLabel4 = new javax.swing.JLabel();
        turingMachineExplanatoryDialog = new javax.swing.JDialog();
        jScrollPane9 = new javax.swing.JScrollPane();
        turingMachineExplanatoryTextField = new javax.swing.JTextPane();
        turingMachineExplanatoryOKButton = new javax.swing.JButton();
        machineDisplayPanel = new MachineDiagram(currentState);
        stepMachineButton = new javax.swing.JButton();
        runMachineButton = new javax.swing.JButton();
        executionSpeedDropdown = new javax.swing.JComboBox<>();
        executionSpeedlabel = new javax.swing.JLabel();
        resetMachineButton = new javax.swing.JButton();
        tapeDisplayScrollPane = new javax.swing.JScrollPane();
        tapeDisplayTextArea = new javax.swing.JTextArea();
        newMachineButton = new javax.swing.JButton();
        modifyMachineButton = new javax.swing.JButton();
        openFileButton = new javax.swing.JButton();
        mainMenuBar = new javax.swing.JMenuBar();
        machineMenu = new javax.swing.JMenu();
        newMachineMI = new javax.swing.JMenuItem();
        modMachineMI = new javax.swing.JMenuItem();
        resetMachineMI = new javax.swing.JMenuItem();
        openFileMI = new javax.swing.JMenuItem();
        saveMachineMI = new javax.swing.JMenuItem();
        exitMI = new javax.swing.JMenuItem();
        tapeMenu = new javax.swing.JMenu();
        initialiseTapesSubMenu = new javax.swing.JMenu();
        itPlaintextMI = new javax.swing.JMenuItem();
        itBinaryMI = new javax.swing.JMenuItem();
        itUnaryMI = new javax.swing.JMenuItem();
        helpMenu = new javax.swing.JMenu();
        helpWhatIsTuringMachineMI = new javax.swing.JMenuItem();

        fileChooserWindow.setFileFilter(new MyCustomFilter());

        nrOfTapesDialog.setMinimumSize(new java.awt.Dimension(379, 190));
        nrOfTapesDialog.setModal(true);
        nrOfTapesDialog.addComponentListener(new java.awt.event.ComponentAdapter() {
            public void componentShown(java.awt.event.ComponentEvent evt) {
                nrOfTapesDialogComponentShown(evt);
            }
        });

        nrOfTapesOKButton.setText("OK");
        nrOfTapesOKButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                nrOfTapesOKButtonActionPerformed(evt);
            }
        });

        nrOfTapesLabel.setText("Select the number of tapes and states for the machine to use.");

        nrOfTapesCancelButton.setText("Cancel");
        nrOfTapesCancelButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                nrOfTapesCancelButtonActionPerformed(evt);
            }
        });

        nrOfTapesTapeLabel.setText("Tapes:");

        nrOfTapesStateLabel.setText("States:");

        nrOfTapesStateSpinner.setMinimumSize(new java.awt.Dimension(55, 22));
        nrOfTapesStateSpinner.setOpaque(false);
        nrOfTapesStateSpinner.setPreferredSize(new java.awt.Dimension(55, 22));
        nrOfTapesStateSpinner.setModel(new SpinnerNumberModel(1, 1, 99, 1));

        nrOfTapesTapeSpinner.setMinimumSize(new java.awt.Dimension(55, 22));
        nrOfTapesTapeSpinner.setOpaque(false);
        nrOfTapesTapeSpinner.setPreferredSize(new java.awt.Dimension(55, 22));
        nrOfTapesTapeSpinner.setModel(new SpinnerNumberModel(1, 1, 99, 1));
        nrOfTapesTapeSpinner.addKeyListener(new java.awt.event.KeyAdapter() {
            public void keyReleased(java.awt.event.KeyEvent evt) {
                nrOfTapesTapeSpinnerKeyReleased(evt);
            }
            public void keyTyped(java.awt.event.KeyEvent evt) {
                nrOfTapesTapeSpinnerKeyTyped(evt);
            }
        });

        javax.swing.GroupLayout nrOfTapesDialogLayout = new javax.swing.GroupLayout(nrOfTapesDialog.getContentPane());
        nrOfTapesDialog.getContentPane().setLayout(nrOfTapesDialogLayout);
        nrOfTapesDialogLayout.setHorizontalGroup(
            nrOfTapesDialogLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(nrOfTapesDialogLayout.createSequentialGroup()
                .addGroup(nrOfTapesDialogLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addGroup(nrOfTapesDialogLayout.createSequentialGroup()
                        .addContainerGap(javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                        .addComponent(nrOfTapesLabel))
                    .addGroup(nrOfTapesDialogLayout.createSequentialGroup()
                        .addGap(65, 65, 65)
                        .addGroup(nrOfTapesDialogLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.TRAILING)
                            .addGroup(nrOfTapesDialogLayout.createSequentialGroup()
                                .addComponent(nrOfTapesTapeLabel)
                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                                .addComponent(nrOfTapesTapeSpinner, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                            .addComponent(nrOfTapesCancelButton))
                        .addGap(18, 18, 18)
                        .addGroup(nrOfTapesDialogLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                            .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, nrOfTapesDialogLayout.createSequentialGroup()
                                .addComponent(nrOfTapesStateLabel)
                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                                .addComponent(nrOfTapesStateSpinner, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                            .addComponent(nrOfTapesOKButton, javax.swing.GroupLayout.Alignment.TRAILING, javax.swing.GroupLayout.PREFERRED_SIZE, 61, javax.swing.GroupLayout.PREFERRED_SIZE))))
                .addContainerGap(javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
        );
        nrOfTapesDialogLayout.setVerticalGroup(
            nrOfTapesDialogLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, nrOfTapesDialogLayout.createSequentialGroup()
                .addContainerGap()
                .addComponent(nrOfTapesLabel)
                .addGap(27, 27, 27)
                .addGroup(nrOfTapesDialogLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(nrOfTapesTapeLabel)
                    .addComponent(nrOfTapesStateLabel)
                    .addComponent(nrOfTapesStateSpinner, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(nrOfTapesTapeSpinner, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED, 64, Short.MAX_VALUE)
                .addGroup(nrOfTapesDialogLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(nrOfTapesOKButton, javax.swing.GroupLayout.PREFERRED_SIZE, 35, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(nrOfTapesCancelButton, javax.swing.GroupLayout.PREFERRED_SIZE, 35, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addContainerGap())
        );

        stateBuilderDialog.setMinimumSize(new java.awt.Dimension(557, 330));
        stateBuilderDialog.addComponentListener(new java.awt.event.ComponentAdapter() {
            public void componentHidden(java.awt.event.ComponentEvent evt) {
                stateBuilderDialogComponentHidden(evt);
            }
            public void componentShown(java.awt.event.ComponentEvent evt) {
                stateBuilderDialogComponentShown(evt);
            }
        });

        stateBuilderStateSelectorDropdown.setModel(new javax.swing.DefaultComboBoxModel<>(new String[] { "lul" }));
        stateBuilderStateSelectorDropdown.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                stateBuilderStateSelectorDropdownActionPerformed(evt);
            }
        });

        stateBuilderStateSelectorLabel.setText("Select which state to modify:");

        stateBuilderToggleAcceptingButton.setText("Toggle accepting status");
        stateBuilderToggleAcceptingButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                stateBuilderToggleAcceptingButtonActionPerformed(evt);
            }
        });

        stateBuilderAcceptingLabel.setText("State is currently NOT ACCEPTING");
        stateBuilderAcceptingLabel.setText(getAcceptingLabelForCurrentState());

        stateBuilderStateTransitionsLabel.setText("State transitions:");

        stateBuilderStateTransitionList.setToolTipText("[Trigger] -> [Head movement] + [Cells after transition] -> [New state after transition]");
        stateBuilderStateTransitionList.setFocusable(false);
        jScrollPane1.setViewportView(stateBuilderStateTransitionList);
        updateStateBuilderStateTransitionList();

        stateBuilderAddTransitionButton.setText("Add new transition...");
        stateBuilderAddTransitionButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                stateBuilderAddTransitionButtonActionPerformed(evt);
            }
        });

        stateBuilderRemoveTransitionButton.setText("Remove selected transition(s)");
        stateBuilderRemoveTransitionButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                stateBuilderRemoveTransitionButtonActionPerformed(evt);
            }
        });

        stateBuilderChangeNrOfTapesStatesButton.setText("Change number of tapes / states");
        stateBuilderChangeNrOfTapesStatesButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                stateBuilderChangeNrOfTapesStatesButtonActionPerformed(evt);
            }
        });

        stateBuilderFinishMachineButton.setText("Finish machine");
        stateBuilderFinishMachineButton.addComponentListener(new java.awt.event.ComponentAdapter() {
            public void componentShown(java.awt.event.ComponentEvent evt) {
                stateBuilderFinishMachineButtonComponentShown(evt);
            }
        });
        stateBuilderFinishMachineButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                stateBuilderFinishMachineButtonActionPerformed(evt);
            }
        });

        stateBuilderCancelButton.setText("Cancel setup and forget changes");
        stateBuilderCancelButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                stateBuilderCancelButtonActionPerformed(evt);
            }
        });

        javax.swing.GroupLayout stateBuilderDialogLayout = new javax.swing.GroupLayout(stateBuilderDialog.getContentPane());
        stateBuilderDialog.getContentPane().setLayout(stateBuilderDialogLayout);
        stateBuilderDialogLayout.setHorizontalGroup(
            stateBuilderDialogLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(stateBuilderDialogLayout.createSequentialGroup()
                .addContainerGap()
                .addGroup(stateBuilderDialogLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addGroup(stateBuilderDialogLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING, false)
                        .addGroup(stateBuilderDialogLayout.createSequentialGroup()
                            .addComponent(stateBuilderStateSelectorLabel)
                            .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                            .addComponent(stateBuilderStateSelectorDropdown, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                        .addComponent(jScrollPane1))
                    .addComponent(stateBuilderAcceptingLabel)
                    .addComponent(stateBuilderStateTransitionsLabel))
                .addGap(18, 18, 18)
                .addGroup(stateBuilderDialogLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING, false)
                    .addComponent(stateBuilderToggleAcceptingButton, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                    .addComponent(stateBuilderAddTransitionButton, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                    .addComponent(stateBuilderRemoveTransitionButton, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                    .addComponent(stateBuilderChangeNrOfTapesStatesButton, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                    .addComponent(stateBuilderFinishMachineButton, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                    .addComponent(stateBuilderCancelButton, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
                .addContainerGap(92, Short.MAX_VALUE))
        );
        stateBuilderDialogLayout.setVerticalGroup(
            stateBuilderDialogLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(stateBuilderDialogLayout.createSequentialGroup()
                .addContainerGap()
                .addGroup(stateBuilderDialogLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(stateBuilderStateSelectorLabel)
                    .addComponent(stateBuilderStateSelectorDropdown, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(stateBuilderToggleAcceptingButton))
                .addGap(14, 14, 14)
                .addGroup(stateBuilderDialogLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(stateBuilderAcceptingLabel)
                    .addComponent(stateBuilderAddTransitionButton))
                .addGap(18, 18, 18)
                .addGroup(stateBuilderDialogLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addGroup(stateBuilderDialogLayout.createSequentialGroup()
                        .addComponent(stateBuilderStateTransitionsLabel)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(jScrollPane1, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                    .addGroup(stateBuilderDialogLayout.createSequentialGroup()
                        .addComponent(stateBuilderRemoveTransitionButton)
                        .addGap(18, 18, 18)
                        .addComponent(stateBuilderChangeNrOfTapesStatesButton)
                        .addGap(18, 18, 18)
                        .addComponent(stateBuilderFinishMachineButton)
                        .addGap(18, 18, 18)
                        .addComponent(stateBuilderCancelButton)))
                .addContainerGap(javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
        );

        transitionBuilderDialog.setMinimumSize(new java.awt.Dimension(540, 170));
        transitionBuilderDialog.addComponentListener(new java.awt.event.ComponentAdapter() {
            public void componentShown(java.awt.event.ComponentEvent evt) {
                transitionBuilderDialogComponentShown(evt);
            }
        });

        transitionBuilderHeadMovLabel.setText("Head movement:");

        transitionBuilderHeadMovTF.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                transitionBuilderHeadMovTFActionPerformed(evt);
            }
        });

        transitionBuilderToBeWrittenLabel.setText("To be written:");

        transitionBuilderOpenHelpButton.setText("Help");
        transitionBuilderOpenHelpButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                transitionBuilderOpenHelpButtonActionPerformed(evt);
            }
        });

        transitionBuilderNextStateLabel.setText("Next state after transition:");

        transitionBuilderNextStateDropdown.setModel(new javax.swing.DefaultComboBoxModel<>(new String[] { "State 0" }));
        transitionBuilderNextStateDropdown.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                transitionBuilderNextStateDropdownActionPerformed(evt);
            }
        });

        transitionBuilderDoneButton.setText("Done");
        transitionBuilderDoneButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                transitionBuilderDoneButtonActionPerformed(evt);
            }
        });

        transitionBuilderTriggerLabel.setText("Trigger:");

        transitionBuilderCancelButton.setText("Cancel");
        transitionBuilderCancelButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                transitionBuilderCancelButtonActionPerformed(evt);
            }
        });

        javax.swing.GroupLayout transitionBuilderContainerPanelLayout = new javax.swing.GroupLayout(transitionBuilderContainerPanel);
        transitionBuilderContainerPanel.setLayout(transitionBuilderContainerPanelLayout);
        transitionBuilderContainerPanelLayout.setHorizontalGroup(
            transitionBuilderContainerPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(transitionBuilderContainerPanelLayout.createSequentialGroup()
                .addContainerGap()
                .addGroup(transitionBuilderContainerPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(transitionBuilderTriggerLabel)
                    .addComponent(transitionBuilderTriggerTF, javax.swing.GroupLayout.PREFERRED_SIZE, 98, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addGap(18, 18, 18)
                .addGroup(transitionBuilderContainerPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addGroup(transitionBuilderContainerPanelLayout.createSequentialGroup()
                        .addGroup(transitionBuilderContainerPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                            .addComponent(transitionBuilderToBeWrittenTF, javax.swing.GroupLayout.PREFERRED_SIZE, 98, javax.swing.GroupLayout.PREFERRED_SIZE)
                            .addComponent(transitionBuilderToBeWrittenLabel))
                        .addGap(18, 18, 18)
                        .addGroup(transitionBuilderContainerPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                            .addGroup(transitionBuilderContainerPanelLayout.createSequentialGroup()
                                .addComponent(transitionBuilderHeadMovTF, javax.swing.GroupLayout.PREFERRED_SIZE, 98, javax.swing.GroupLayout.PREFERRED_SIZE)
                                .addGap(18, 18, 18)
                                .addComponent(transitionBuilderNextStateDropdown, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                            .addGroup(transitionBuilderContainerPanelLayout.createSequentialGroup()
                                .addComponent(transitionBuilderHeadMovLabel)
                                .addGap(18, 18, 18)
                                .addComponent(transitionBuilderNextStateLabel, javax.swing.GroupLayout.PREFERRED_SIZE, 181, javax.swing.GroupLayout.PREFERRED_SIZE))))
                    .addGroup(transitionBuilderContainerPanelLayout.createSequentialGroup()
                        .addComponent(transitionBuilderCancelButton)
                        .addGap(18, 18, 18)
                        .addComponent(transitionBuilderOpenHelpButton)
                        .addGap(18, 18, 18)
                        .addComponent(transitionBuilderDoneButton)))
                .addContainerGap(javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
        );
        transitionBuilderContainerPanelLayout.setVerticalGroup(
            transitionBuilderContainerPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(transitionBuilderContainerPanelLayout.createSequentialGroup()
                .addContainerGap()
                .addGroup(transitionBuilderContainerPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(transitionBuilderTriggerLabel)
                    .addComponent(transitionBuilderToBeWrittenLabel)
                    .addComponent(transitionBuilderNextStateLabel)
                    .addComponent(transitionBuilderHeadMovLabel))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(transitionBuilderContainerPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(transitionBuilderTriggerTF, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(transitionBuilderToBeWrittenTF, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(transitionBuilderNextStateDropdown, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(transitionBuilderHeadMovTF, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addGap(30, 30, 30)
                .addGroup(transitionBuilderContainerPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(transitionBuilderDoneButton)
                    .addComponent(transitionBuilderCancelButton)
                    .addComponent(transitionBuilderOpenHelpButton))
                .addContainerGap(javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
        );

        javax.swing.GroupLayout transitionBuilderDialogLayout = new javax.swing.GroupLayout(transitionBuilderDialog.getContentPane());
        transitionBuilderDialog.getContentPane().setLayout(transitionBuilderDialogLayout);
        transitionBuilderDialogLayout.setHorizontalGroup(
            transitionBuilderDialogLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addComponent(transitionBuilderContainerPanel, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
        );
        transitionBuilderDialogLayout.setVerticalGroup(
            transitionBuilderDialogLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addComponent(transitionBuilderContainerPanel, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
        );

        transitionBuilderHelpDialog.setMinimumSize(new java.awt.Dimension(430, 498));

        transitionBuilderHelpText.setColumns(20);
        transitionBuilderHelpText.setLineWrap(true);
        transitionBuilderHelpText.setRows(5);
        transitionBuilderHelpText.setText("A transition has four parts:\n\nA trigger, what must be on each tape to trigger this transition,\n\nWhat should be written to each tape (in the original\ncell, where the transition was triggered),\n\nHow each tape's head should move, with l meaning left, n meaning\nno movement, and r meaning right.\n\nAnd the next state the machine should be in afterwards.\n\nEnter the values for each of these parts into the corresponding\ntext field separated by commas, choose the next state from the\ndropdown, and click the Done button to add the transition.\n\nTapes are initialised with space characters (' ') in all fields\nthat were not otherwise written to. You can use the question\nmark character ('?') as a wildcard, meaning that any character is accepted if used in the trigger, or whichever character was already in the cell is left there if used in the \"to be written\" field.");
        transitionBuilderHelpText.setWrapStyleWord(true);
        jScrollPane2.setViewportView(transitionBuilderHelpText);

        transitionBuilderHelpDialog.getContentPane().add(jScrollPane2, java.awt.BorderLayout.CENTER);

        transitionBuilderHelpOKButton.setText("OK");
        transitionBuilderHelpOKButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                transitionBuilderHelpOKButtonActionPerformed(evt);
            }
        });
        transitionBuilderHelpDialog.getContentPane().add(transitionBuilderHelpOKButton, java.awt.BorderLayout.PAGE_END);

        transitionBuilderErrorDialogTF.setColumns(20);
        transitionBuilderErrorDialogTF.setLineWrap(true);
        transitionBuilderErrorDialogTF.setRows(5);
        transitionBuilderErrorDialogTF.setWrapStyleWord(true);
        jScrollPane3.setViewportView(transitionBuilderErrorDialogTF);

        transitionBuilderErrorDialogOKButton.setText("OK");
        transitionBuilderErrorDialogOKButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                transitionBuilderErrorDialogOKButtonActionPerformed(evt);
            }
        });

        javax.swing.GroupLayout transitionBuilderErrorDialogLayout = new javax.swing.GroupLayout(transitionBuilderErrorDialog.getContentPane());
        transitionBuilderErrorDialog.getContentPane().setLayout(transitionBuilderErrorDialogLayout);
        transitionBuilderErrorDialogLayout.setHorizontalGroup(
            transitionBuilderErrorDialogLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addComponent(jScrollPane3)
            .addGroup(transitionBuilderErrorDialogLayout.createSequentialGroup()
                .addGap(165, 165, 165)
                .addComponent(transitionBuilderErrorDialogOKButton)
                .addContainerGap(186, Short.MAX_VALUE))
        );
        transitionBuilderErrorDialogLayout.setVerticalGroup(
            transitionBuilderErrorDialogLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(transitionBuilderErrorDialogLayout.createSequentialGroup()
                .addComponent(jScrollPane3, javax.swing.GroupLayout.PREFERRED_SIZE, 230, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addGap(18, 18, 18)
                .addComponent(transitionBuilderErrorDialogOKButton)
                .addGap(0, 27, Short.MAX_VALUE))
        );

        machineHasHaltedDialogText.setColumns(20);
        machineHasHaltedDialogText.setLineWrap(true);
        machineHasHaltedDialogText.setRows(5);
        machineHasHaltedDialogText.setText("The machine has halted!");
        machineHasHaltedDialogText.setWrapStyleWord(true);
        jScrollPane4.setViewportView(machineHasHaltedDialogText);

        machineHasHaltedDialogDisplayTapesButton.setText("Display tape content");
        machineHasHaltedDialogDisplayTapesButton.setAlignmentX(Component.CENTER_ALIGNMENT);
        machineHasHaltedDialogDisplayTapesButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                machineHasHaltedDialogDisplayTapesButtonActionPerformed(evt);
            }
        });

        machineHasHaltedialogSaveTapesButton.setText("Save tapes to file");
        machineHasHaltedialogSaveTapesButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                machineHasHaltedialogSaveTapesButtonActionPerformed(evt);
            }
        });

        machineHasHaltedDialogOKButton.setText("OK");
        machineHasHaltedDialogOKButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                machineHasHaltedDialogOKButtonActionPerformed(evt);
            }
        });

        javax.swing.GroupLayout machineHasHaltedDialogLayout = new javax.swing.GroupLayout(machineHasHaltedDialog.getContentPane());
        machineHasHaltedDialog.getContentPane().setLayout(machineHasHaltedDialogLayout);
        machineHasHaltedDialogLayout.setHorizontalGroup(
            machineHasHaltedDialogLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addComponent(jScrollPane4)
            .addGroup(machineHasHaltedDialogLayout.createSequentialGroup()
                .addGap(28, 28, 28)
                .addComponent(machineHasHaltedDialogOKButton)
                .addGap(18, 18, 18)
                .addComponent(machineHasHaltedDialogDisplayTapesButton)
                .addGap(18, 18, 18)
                .addComponent(machineHasHaltedialogSaveTapesButton)
                .addContainerGap(javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
        );
        machineHasHaltedDialogLayout.setVerticalGroup(
            machineHasHaltedDialogLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(machineHasHaltedDialogLayout.createSequentialGroup()
                .addComponent(jScrollPane4, javax.swing.GroupLayout.DEFAULT_SIZE, 244, Short.MAX_VALUE)
                .addGap(18, 18, 18)
                .addGroup(machineHasHaltedDialogLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(machineHasHaltedDialogDisplayTapesButton)
                    .addComponent(machineHasHaltedialogSaveTapesButton)
                    .addComponent(machineHasHaltedDialogOKButton))
                .addContainerGap())
        );

        tapeContentDisplayText.setColumns(20);
        tapeContentDisplayText.setFont(new java.awt.Font("Courier New", 0, 13)); // NOI18N
        tapeContentDisplayText.setRows(5);
        jScrollPane5.setViewportView(tapeContentDisplayText);

        tapeContentDisplayOKButton.setText("OK");
        tapeContentDisplayOKButton.setAlignmentX(Component.CENTER_ALIGNMENT);
        tapeContentDisplayOKButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                tapeContentDisplayOKButtonActionPerformed(evt);
            }
        });

        javax.swing.GroupLayout tapeContentDisplayDialogLayout = new javax.swing.GroupLayout(tapeContentDisplayDialog.getContentPane());
        tapeContentDisplayDialog.getContentPane().setLayout(tapeContentDisplayDialogLayout);
        tapeContentDisplayDialogLayout.setHorizontalGroup(
            tapeContentDisplayDialogLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addComponent(jScrollPane5)
            .addGroup(tapeContentDisplayDialogLayout.createSequentialGroup()
                .addGap(161, 161, 161)
                .addComponent(tapeContentDisplayOKButton)
                .addContainerGap(190, Short.MAX_VALUE))
        );
        tapeContentDisplayDialogLayout.setVerticalGroup(
            tapeContentDisplayDialogLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(tapeContentDisplayDialogLayout.createSequentialGroup()
                .addComponent(jScrollPane5, javax.swing.GroupLayout.PREFERRED_SIZE, 259, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                .addComponent(tapeContentDisplayOKButton)
                .addContainerGap())
        );

        itPlaintextDialogLabel1.setText("For each tape, enter its content below on a new line. Use empty cell chars ('_') to control offset");

        itPlaintextDialogLabel2.setText("The first tape is filled by the first line, and so on. Lines can be empty.");

        itPlaintextTextArea.setColumns(20);
        itPlaintextTextArea.setRows(5);
        jScrollPane6.setViewportView(itPlaintextTextArea);

        itPlaintextOKButton.setText("OK");
        itPlaintextOKButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                itPlaintextOKButtonActionPerformed(evt);
            }
        });

        itPlaintextCancelButton.setText("Cancel");
        itPlaintextCancelButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                itPlaintextCancelButtonActionPerformed(evt);
            }
        });

        itPlaintextDialogLabel3.setText("After writing, each tape's head will be placed at the first non-empty cell from the left.");

        javax.swing.GroupLayout itPlaintextDialogLayout = new javax.swing.GroupLayout(itPlaintextDialog.getContentPane());
        itPlaintextDialog.getContentPane().setLayout(itPlaintextDialogLayout);
        itPlaintextDialogLayout.setHorizontalGroup(
            itPlaintextDialogLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(itPlaintextDialogLayout.createSequentialGroup()
                .addContainerGap()
                .addGroup(itPlaintextDialogLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(jScrollPane6)
                    .addGroup(itPlaintextDialogLayout.createSequentialGroup()
                        .addGroup(itPlaintextDialogLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                            .addComponent(itPlaintextDialogLabel1)
                            .addComponent(itPlaintextDialogLabel2)
                            .addComponent(itPlaintextDialogLabel3))
                        .addGap(0, 40, Short.MAX_VALUE)))
                .addContainerGap())
            .addGroup(itPlaintextDialogLayout.createSequentialGroup()
                .addGap(209, 209, 209)
                .addComponent(itPlaintextCancelButton)
                .addGap(18, 18, 18)
                .addComponent(itPlaintextOKButton)
                .addContainerGap(javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
        );
        itPlaintextDialogLayout.setVerticalGroup(
            itPlaintextDialogLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(itPlaintextDialogLayout.createSequentialGroup()
                .addContainerGap()
                .addComponent(itPlaintextDialogLabel1)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(itPlaintextDialogLabel2)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(itPlaintextDialogLabel3)
                .addGap(18, 18, 18)
                .addComponent(jScrollPane6, javax.swing.GroupLayout.PREFERRED_SIZE, 200, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addGap(18, 18, 18)
                .addGroup(itPlaintextDialogLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(itPlaintextCancelButton)
                    .addComponent(itPlaintextOKButton))
                .addContainerGap(javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
        );

        itBinaryDialogLabel1.setText("For each tape, enter the numbers to write on each tape on a new line, separated by commas.");

        itBinaryDialogLabel2.setText("The first tape is filled by the first line, and so on. Lines can be empty.");

        itBinaryTextArea.setColumns(20);
        itBinaryTextArea.setRows(5);
        jScrollPane7.setViewportView(itBinaryTextArea);

        itBinaryOKButton.setText("OK");
        itBinaryOKButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                itBinaryOKButtonActionPerformed(evt);
            }
        });

        itBinaryCancelButton.setText("Cancel");
        itBinaryCancelButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                itBinaryCancelButtonActionPerformed(evt);
            }
        });

        itBinaryDialogLabel3.setText("After writing, each tape's head will be placed at the first non-empty cell from the left.");

        itBinaryDialogLabel4.setText("The numbers will be converted to binary notation and written to their respective tapes.");

        javax.swing.GroupLayout itBinaryDialogLayout = new javax.swing.GroupLayout(itBinaryDialog.getContentPane());
        itBinaryDialog.getContentPane().setLayout(itBinaryDialogLayout);
        itBinaryDialogLayout.setHorizontalGroup(
            itBinaryDialogLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(itBinaryDialogLayout.createSequentialGroup()
                .addContainerGap()
                .addGroup(itBinaryDialogLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addGroup(itBinaryDialogLayout.createSequentialGroup()
                        .addGroup(itBinaryDialogLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                            .addComponent(itBinaryDialogLabel1)
                            .addComponent(itBinaryDialogLabel2)
                            .addComponent(itBinaryDialogLabel4)
                            .addComponent(itBinaryDialogLabel3))
                        .addGap(0, 453, Short.MAX_VALUE))
                    .addComponent(jScrollPane7))
                .addContainerGap())
            .addGroup(itBinaryDialogLayout.createSequentialGroup()
                .addGap(273, 273, 273)
                .addComponent(itBinaryCancelButton)
                .addGap(34, 34, 34)
                .addComponent(itBinaryOKButton)
                .addContainerGap(javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
        );
        itBinaryDialogLayout.setVerticalGroup(
            itBinaryDialogLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(itBinaryDialogLayout.createSequentialGroup()
                .addContainerGap()
                .addComponent(itBinaryDialogLabel1)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(itBinaryDialogLabel2)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(itBinaryDialogLabel4)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(itBinaryDialogLabel3)
                .addGap(18, 18, 18)
                .addComponent(jScrollPane7, javax.swing.GroupLayout.PREFERRED_SIZE, 200, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addGap(18, 18, 18)
                .addGroup(itBinaryDialogLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(itBinaryCancelButton)
                    .addComponent(itBinaryOKButton))
                .addContainerGap(javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
        );

        itUnaryDialogLabel1.setText("For each tape, enter the numbers to write on each tape on a new line, separated by commas.");

        itUnaryDialogLabel2.setText("The first tape is filled by the first line, and so on. Lines can be empty.");

        itUnaryTextArea.setColumns(20);
        itUnaryTextArea.setRows(5);
        jScrollPane8.setViewportView(itUnaryTextArea);

        itUnaryOKButton.setText("OK");
        itUnaryOKButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                itUnaryOKButtonActionPerformed(evt);
            }
        });

        itUnaryCancelButton.setText("Cancel");
        itUnaryCancelButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                itUnaryCancelButtonActionPerformed(evt);
            }
        });

        itUnaryDialogLabel3.setText("After writing, each tape's head will be placed at the first non-empty cell from the left.");

        itUnaryDialogLabel4.setText("The numbers will be converted to unary notation and written to their respective tapes.");

        javax.swing.GroupLayout itUnaryDialogLayout = new javax.swing.GroupLayout(itUnaryDialog.getContentPane());
        itUnaryDialog.getContentPane().setLayout(itUnaryDialogLayout);
        itUnaryDialogLayout.setHorizontalGroup(
            itUnaryDialogLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(itUnaryDialogLayout.createSequentialGroup()
                .addContainerGap()
                .addGroup(itUnaryDialogLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addGroup(itUnaryDialogLayout.createSequentialGroup()
                        .addGroup(itUnaryDialogLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                            .addComponent(itUnaryDialogLabel1)
                            .addComponent(itUnaryDialogLabel2)
                            .addComponent(itUnaryDialogLabel4)
                            .addComponent(itUnaryDialogLabel3))
                        .addGap(0, 453, Short.MAX_VALUE))
                    .addComponent(jScrollPane8))
                .addContainerGap())
            .addGroup(itUnaryDialogLayout.createSequentialGroup()
                .addGap(273, 273, 273)
                .addComponent(itUnaryCancelButton)
                .addGap(34, 34, 34)
                .addComponent(itUnaryOKButton)
                .addContainerGap(javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
        );
        itUnaryDialogLayout.setVerticalGroup(
            itUnaryDialogLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(itUnaryDialogLayout.createSequentialGroup()
                .addContainerGap()
                .addComponent(itUnaryDialogLabel1)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(itUnaryDialogLabel2)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(itUnaryDialogLabel4)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(itUnaryDialogLabel3)
                .addGap(18, 18, 18)
                .addComponent(jScrollPane8, javax.swing.GroupLayout.PREFERRED_SIZE, 200, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addGap(18, 18, 18)
                .addGroup(itUnaryDialogLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(itUnaryCancelButton)
                    .addComponent(itUnaryOKButton))
                .addContainerGap(javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
        );

        turingMachineExplanatoryDialog.setMinimumSize(new java.awt.Dimension(430, 498));

        turingMachineExplanatoryTextField.setText("A Turing machine, or state machine, is a state-based processing algorithm. It has a given number of tapes of infinite length divided into single-character cells, a set of states that determine which transitions are currently active, and transitions between these states (or back to the same state) which process the tape's contents.\n\nThe tapes are the memory of the Turing machine, where it receives its initial input and eventually writes its output if any. Each tape has a read/write head that is over one cell at a time. It reports that cell's contents for the purposes of triggering a transition and writes new content into that cell during a transition. It may then move one cell left or right.\n\nEach Turing machine has one or more states. It starts in state 0, changing depending on the transition used. Each state can be defined as accepting or not. This has no intrinsic meaning, but usually implies something about the result of the computation such as only input of a given format ending in an accepting state. A Turing machine should generally halt in an accepting state if nothing went wrong and the input is as desired (return value 0). Halting in an unaccepting state implies something went wrong or the input is incorrect in some form.\n\nTransitions are the major computing component of a Turing machine. Each state has zero or more transitions leading out of it. A transition has four parts:\n\n- Its trigger, what must be written on the current cell of each tape to trigger the transition (The wildcard character ? will cause any input to be accepted on that tape)\n- Its output, what will be written to the current cell of each tape when the transition occurs (The wildcard character ? will cause the current cell content to remain in that cell)\n- Its head movement, whether each tape's head will move left, right, or not at all\n- Its next state, into which the machine will go after the transition is done. This may be the same state as the transition leads out of.\n\nDue to wildcards, multiple transitions may match the same tape content in a given state. If this is the case, the most specific transition trigger with the fewest wildcards is chosen. In the event that there are multiple triggers with the fewest amount of wildcards, the transition whose wildcards appear the latest will be chosen. ([0, 1, ?] over [0, ?, 0])\n\nIf a state has no transition that matches the current tape content, the machine will halt. When it does, it checks whether its current state is accepting and allows its tape content to be read out.");
        jScrollPane9.setViewportView(turingMachineExplanatoryTextField);

        turingMachineExplanatoryOKButton.setText("OK");
        turingMachineExplanatoryOKButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                turingMachineExplanatoryOKButtonActionPerformed(evt);
            }
        });

        javax.swing.GroupLayout turingMachineExplanatoryDialogLayout = new javax.swing.GroupLayout(turingMachineExplanatoryDialog.getContentPane());
        turingMachineExplanatoryDialog.getContentPane().setLayout(turingMachineExplanatoryDialogLayout);
        turingMachineExplanatoryDialogLayout.setHorizontalGroup(
            turingMachineExplanatoryDialogLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addComponent(jScrollPane9, javax.swing.GroupLayout.DEFAULT_SIZE, 475, Short.MAX_VALUE)
            .addComponent(turingMachineExplanatoryOKButton, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
        );
        turingMachineExplanatoryDialogLayout.setVerticalGroup(
            turingMachineExplanatoryDialogLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(turingMachineExplanatoryDialogLayout.createSequentialGroup()
                .addComponent(jScrollPane9, javax.swing.GroupLayout.DEFAULT_SIZE, 440, Short.MAX_VALUE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(turingMachineExplanatoryOKButton, javax.swing.GroupLayout.PREFERRED_SIZE, 25, javax.swing.GroupLayout.PREFERRED_SIZE))
        );

        setDefaultCloseOperation(javax.swing.WindowConstants.EXIT_ON_CLOSE);
        setMinimumSize(new java.awt.Dimension(1100, 800));
        addComponentListener(new java.awt.event.ComponentAdapter() {
            public void componentShown(java.awt.event.ComponentEvent evt) {
                formComponentShown(evt);
            }
        });

        machineDisplayPanel.setBackground(new java.awt.Color(255, 255, 255));
        machineDisplayPanel.setMinimumSize(new java.awt.Dimension(800, 800));

        javax.swing.GroupLayout machineDisplayPanelLayout = new javax.swing.GroupLayout(machineDisplayPanel);
        machineDisplayPanel.setLayout(machineDisplayPanelLayout);
        machineDisplayPanelLayout.setHorizontalGroup(
            machineDisplayPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGap(0, 800, Short.MAX_VALUE)
        );
        machineDisplayPanelLayout.setVerticalGroup(
            machineDisplayPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGap(0, 800, Short.MAX_VALUE)
        );

        stepMachineButton.setText("Step machine");
        stepMachineButton.setEnabled(false);
        stepMachineButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                stepMachineButtonActionPerformed(evt);
            }
        });

        runMachineButton.setText("Run machine");
        runMachineButton.setEnabled(false);
        runMachineButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                runMachineButtonActionPerformed(evt);
            }
        });

        executionSpeedDropdown.setModel(new javax.swing.DefaultComboBoxModel<>(new String[] { "Slow", "Medium", "Fast", "Instant" }));
        executionSpeedDropdown.setEnabled(false);

        executionSpeedlabel.setText("Execution speed");
        executionSpeedlabel.setEnabled(false);

        resetMachineButton.setText("Reset machine");
        resetMachineButton.setEnabled(false);
        resetMachineButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                resetMachineButtonActionPerformed(evt);
            }
        });

        tapeDisplayScrollPane.setMinimumSize(new java.awt.Dimension(800, 150));

        tapeDisplayTextArea.setColumns(20);
        tapeDisplayTextArea.setFont(new java.awt.Font("Courier New", 0, 13)); // NOI18N
        tapeDisplayTextArea.setRows(5);
        tapeDisplayTextArea.setText("Once a machine is loaded, the tape contents will be displayed here.");
        tapeDisplayTextArea.setEnabled(false);
        tapeDisplayScrollPane.setViewportView(tapeDisplayTextArea);

        newMachineButton.setText("New machine");
        newMachineButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                newMachineButtonActionPerformed(evt);
            }
        });

        modifyMachineButton.setText("Modify machine");
        modifyMachineButton.setEnabled(false);
        modifyMachineButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                modifyMachineButtonActionPerformed(evt);
            }
        });

        openFileButton.setText("Open from file");
        openFileButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                openFileButtonActionPerformed(evt);
            }
        });

        machineMenu.setText("Machine");
        machineMenu.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                machineMenuActionPerformed(evt);
            }
        });

        newMachineMI.setAccelerator(javax.swing.KeyStroke.getKeyStroke(java.awt.event.KeyEvent.VK_N, java.awt.event.InputEvent.CTRL_MASK));
        newMachineMI.setText("New machine");
        newMachineMI.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                newMachineMIActionPerformed(evt);
            }
        });
        machineMenu.add(newMachineMI);

        modMachineMI.setAccelerator(javax.swing.KeyStroke.getKeyStroke(java.awt.event.KeyEvent.VK_M, java.awt.event.InputEvent.CTRL_MASK));
        modMachineMI.setText("Modify current machine");
        modMachineMI.setEnabled(false);
        modMachineMI.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                modMachineMIActionPerformed(evt);
            }
        });
        machineMenu.add(modMachineMI);

        resetMachineMI.setAccelerator(javax.swing.KeyStroke.getKeyStroke(java.awt.event.KeyEvent.VK_R, java.awt.event.InputEvent.CTRL_MASK));
        resetMachineMI.setText("Reset machine");
        resetMachineMI.setEnabled(false);
        resetMachineMI.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                resetMachineMIActionPerformed(evt);
            }
        });
        machineMenu.add(resetMachineMI);

        openFileMI.setAccelerator(javax.swing.KeyStroke.getKeyStroke(java.awt.event.KeyEvent.VK_O, java.awt.event.InputEvent.CTRL_MASK));
        openFileMI.setText("Open from file");
        openFileMI.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                openFileMIActionPerformed(evt);
            }
        });
        machineMenu.add(openFileMI);

        saveMachineMI.setAccelerator(javax.swing.KeyStroke.getKeyStroke(java.awt.event.KeyEvent.VK_S, java.awt.event.InputEvent.CTRL_MASK));
        saveMachineMI.setText("Save to file");
        saveMachineMI.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                saveMachineMIActionPerformed(evt);
            }
        });
        machineMenu.add(saveMachineMI);

        exitMI.setAccelerator(javax.swing.KeyStroke.getKeyStroke(java.awt.event.KeyEvent.VK_Q, java.awt.event.InputEvent.ALT_MASK));
        exitMI.setText("Exit");
        exitMI.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                exitMIActionPerformed(evt);
            }
        });
        machineMenu.add(exitMI);

        mainMenuBar.add(machineMenu);

        tapeMenu.setText("Tapes");
        tapeMenu.setEnabled(false);

        initialiseTapesSubMenu.setText("Initialise tapes");

        itPlaintextMI.setText("... with plaintext");
        itPlaintextMI.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                itPlaintextMIActionPerformed(evt);
            }
        });
        initialiseTapesSubMenu.add(itPlaintextMI);

        itBinaryMI.setText("... with binary numbers");
        itBinaryMI.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                itBinaryMIActionPerformed(evt);
            }
        });
        initialiseTapesSubMenu.add(itBinaryMI);

        itUnaryMI.setText("... with unary numbers");
        itUnaryMI.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                itUnaryMIActionPerformed(evt);
            }
        });
        initialiseTapesSubMenu.add(itUnaryMI);

        tapeMenu.add(initialiseTapesSubMenu);

        mainMenuBar.add(tapeMenu);

        helpMenu.setText("Help");

        helpWhatIsTuringMachineMI.setText("What is a Turing machine?");
        helpWhatIsTuringMachineMI.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                helpWhatIsTuringMachineMIActionPerformed(evt);
            }
        });
        helpMenu.add(helpWhatIsTuringMachineMI);

        mainMenuBar.add(helpMenu);

        setJMenuBar(mainMenuBar);

        javax.swing.GroupLayout layout = new javax.swing.GroupLayout(getContentPane());
        getContentPane().setLayout(layout);
        layout.setHorizontalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, layout.createSequentialGroup()
                .addContainerGap()
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.TRAILING)
                    .addComponent(tapeDisplayScrollPane, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                    .addComponent(machineDisplayPanel, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
                .addGap(18, 18, 18)
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING, false)
                    .addGroup(layout.createSequentialGroup()
                        .addComponent(executionSpeedlabel, javax.swing.GroupLayout.PREFERRED_SIZE, 107, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addGap(0, 0, Short.MAX_VALUE))
                    .addComponent(resetMachineButton, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                    .addComponent(executionSpeedDropdown, 0, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                    .addComponent(modifyMachineButton, javax.swing.GroupLayout.DEFAULT_SIZE, 140, Short.MAX_VALUE)
                    .addComponent(stepMachineButton, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                    .addComponent(newMachineButton, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                    .addComponent(runMachineButton, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                    .addComponent(openFileButton, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
                .addContainerGap())
        );
        layout.setVerticalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(layout.createSequentialGroup()
                .addContainerGap()
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(machineDisplayPanel, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addGroup(layout.createSequentialGroup()
                        .addComponent(newMachineButton)
                        .addGap(18, 18, 18)
                        .addComponent(openFileButton)
                        .addGap(18, 18, 18)
                        .addComponent(modifyMachineButton)
                        .addGap(56, 56, 56)
                        .addComponent(stepMachineButton)
                        .addGap(18, 18, 18)
                        .addComponent(runMachineButton)
                        .addGap(18, 18, 18)
                        .addComponent(executionSpeedlabel, javax.swing.GroupLayout.PREFERRED_SIZE, 16, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(executionSpeedDropdown, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addGap(18, 18, 18)
                        .addComponent(resetMachineButton)))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                .addComponent(tapeDisplayScrollPane, javax.swing.GroupLayout.PREFERRED_SIZE, 150, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addContainerGap(javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
        );

        pack();
    }// </editor-fold>//GEN-END:initComponents

    private void openFileMIActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_openFileMIActionPerformed
        int returnVal = fileChooserWindow.showOpenDialog(this);
        if (returnVal == JFileChooser.APPROVE_OPTION) {
            File file = fileChooserWindow.getSelectedFile();
            try (BufferedReader br = new BufferedReader(new FileReader(file))) {
                ArrayList<String> saveData = new ArrayList<>();
                String line = br.readLine();
                while (line != null) {
                    line = line.replaceAll("(?m)^[ \t]*\r?\n", "");
                    if (line != "") {
                        saveData.add(line);
                    }
                    line = br.readLine();
                }
                TuringInterpreter.parseDataIntoMachine(saveData);
                states = TuringInterpreter.states;
                tapes = TuringInterpreter.tapes;
                resetMachine();
                this.enableInputs();
                this.machineDisplayPanel.repaint();
                this.updateTapesDisplayTA();
            } catch (IOException e) {
                //TODO: handle error. Probably throw something or show a popup.
            }
        } else {
            System.out.println("File access cancelled by user.");
        }
    }//GEN-LAST:event_openFileMIActionPerformed

    private void exitMIActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_exitMIActionPerformed
        System.exit(0);
    }//GEN-LAST:event_exitMIActionPerformed

    private void machineMenuActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_machineMenuActionPerformed
        // TODO add your handling code here:
    }//GEN-LAST:event_machineMenuActionPerformed

    private void saveMachineMIActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_saveMachineMIActionPerformed
        int returnVal = fileChooserWindow.showOpenDialog(this);
        if (returnVal == JFileChooser.APPROVE_OPTION) {
            File file = fileChooserWindow.getSelectedFile();
            try (BufferedWriter w = new BufferedWriter(new FileWriter(file))) {
                w.write(TuringInterpreter.machineToString());
            } catch (IOException e) {
                //TODO: handle error. Probably throw something or show a popup.
            }
        } else {
            System.out.println("File access cancelled by user.");
        }
    }//GEN-LAST:event_saveMachineMIActionPerformed

    private void newMachineMIActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_newMachineMIActionPerformed
        resetNrOfTapesDialog();
        tapes = null;
        states = null;
        nrOfTapesDialog.setVisible(true);
    }//GEN-LAST:event_newMachineMIActionPerformed

    private void nrOfTapesOKButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_nrOfTapesOKButtonActionPerformed
        nrOfTapesDialogHandleConfirm();
    }//GEN-LAST:event_nrOfTapesOKButtonActionPerformed

    private void nrOfTapesCancelButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_nrOfTapesCancelButtonActionPerformed
        nrOfTapesDialog.setVisible(false);
    }//GEN-LAST:event_nrOfTapesCancelButtonActionPerformed

    private void stateBuilderToggleAcceptingButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_stateBuilderToggleAcceptingButtonActionPerformed
        State state = states[this.stateBuilderStateSelectorDropdown.getSelectedIndex()];
        state.setIsAccepting(!state.getIsAccepting());
        this.stateBuilderAcceptingLabel.setText(getAcceptingLabelForCurrentState());
    }//GEN-LAST:event_stateBuilderToggleAcceptingButtonActionPerformed

    private void stateBuilderStateSelectorDropdownActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_stateBuilderStateSelectorDropdownActionPerformed
        currentState = stateBuilderStateSelectorDropdown.getSelectedIndex();
        stateBuilderAcceptingLabel.setText(getAcceptingLabelForCurrentState());
        updateStateBuilderStateTransitionList();
    }//GEN-LAST:event_stateBuilderStateSelectorDropdownActionPerformed

    private void stateBuilderChangeNrOfTapesStatesButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_stateBuilderChangeNrOfTapesStatesButtonActionPerformed
        stateBuilderDialog.setVisible(false);
        nrOfTapesTapeSpinner.setValue(tapes.length);
        nrOfTapesStateSpinner.setValue(states.length);
        nrOfTapesDialog.setVisible(true);
    }//GEN-LAST:event_stateBuilderChangeNrOfTapesStatesButtonActionPerformed

    private void modMachineMIActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_modMachineMIActionPerformed
        tapes = TuringInterpreter.tapes;
        states = TuringInterpreter.states;
        currentState = TuringInterpreter.currentState;
        stateBuilderDialog.setVisible(true);
    }//GEN-LAST:event_modMachineMIActionPerformed

    private void nrOfTapesDialogComponentShown(java.awt.event.ComponentEvent evt) {//GEN-FIRST:event_nrOfTapesDialogComponentShown
        this.nrOfTapesTapeSpinner.requestFocusInWindow();
    }//GEN-LAST:event_nrOfTapesDialogComponentShown

    private void stateBuilderCancelButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_stateBuilderCancelButtonActionPerformed
        this.stateBuilderDialog.setVisible(false);
    }//GEN-LAST:event_stateBuilderCancelButtonActionPerformed

    private void stateBuilderFinishMachineButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_stateBuilderFinishMachineButtonActionPerformed
        TuringInterpreter.tapes = tapes;
        TuringInterpreter.states = states;
        this.resetMachine();
        this.enableInputs();
        stateBuilderDialog.setVisible(false);
        this.machineDisplayPanel.repaint();
        this.tapeDisplayTextArea.setText(TuringInterpreter.getTapesAsString());
    }//GEN-LAST:event_stateBuilderFinishMachineButtonActionPerformed

    private void stateBuilderAddTransitionButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_stateBuilderAddTransitionButtonActionPerformed
        transitionBuilderTriggerTF.requestFocus();
        transitionBuilderDialog.setVisible(true);
    }//GEN-LAST:event_stateBuilderAddTransitionButtonActionPerformed

    private void stateBuilderDialogComponentShown(java.awt.event.ComponentEvent evt) {//GEN-FIRST:event_stateBuilderDialogComponentShown
        String[] stateList = new String[states.length];
        for (int i = 0; i < stateList.length; i++) {
            stateList[i] = states[i].getName();
        }
        stateBuilderStateSelectorDropdown.setModel(new javax.swing.DefaultComboBoxModel<>(stateList));
        stateBuilderStateSelectorDropdown.requestFocus();
        this.stateBuilderStateSelectorDropdown.setSelectedIndex(currentState);
        updateStateBuilderStateTransitionList();
        stateBuilderAcceptingLabel.setText(getAcceptingLabelForCurrentState());
        stateBuilderDialog.pack();
    }//GEN-LAST:event_stateBuilderDialogComponentShown

    private void stateBuilderFinishMachineButtonComponentShown(java.awt.event.ComponentEvent evt) {//GEN-FIRST:event_stateBuilderFinishMachineButtonComponentShown

    }//GEN-LAST:event_stateBuilderFinishMachineButtonComponentShown

    private void transitionBuilderDialogComponentShown(java.awt.event.ComponentEvent evt) {//GEN-FIRST:event_transitionBuilderDialogComponentShown
        String[] stateList = new String[states.length];
        for (int i = 0; i < stateList.length; i++) {
            stateList[i] = states[i].getName();
        }
        this.transitionBuilderNextStateDropdown.setModel(new javax.swing.DefaultComboBoxModel<>(stateList));
    }//GEN-LAST:event_transitionBuilderDialogComponentShown

    private void transitionBuilderHelpOKButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_transitionBuilderHelpOKButtonActionPerformed
        transitionBuilderHelpDialog.setVisible(false);
    }//GEN-LAST:event_transitionBuilderHelpOKButtonActionPerformed

    private void nrOfTapesTapeSpinnerKeyTyped(java.awt.event.KeyEvent evt) {//GEN-FIRST:event_nrOfTapesTapeSpinnerKeyTyped

    }//GEN-LAST:event_nrOfTapesTapeSpinnerKeyTyped

    private void nrOfTapesTapeSpinnerKeyReleased(java.awt.event.KeyEvent evt) {//GEN-FIRST:event_nrOfTapesTapeSpinnerKeyReleased

    }//GEN-LAST:event_nrOfTapesTapeSpinnerKeyReleased

    private void stateBuilderRemoveTransitionButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_stateBuilderRemoveTransitionButtonActionPerformed
        states[currentState].removeTransitions(stateBuilderStateTransitionList.getSelectedIndices());
        this.updateStateBuilderStateTransitionList();
    }//GEN-LAST:event_stateBuilderRemoveTransitionButtonActionPerformed

    private void stateBuilderDialogComponentHidden(java.awt.event.ComponentEvent evt) {//GEN-FIRST:event_stateBuilderDialogComponentHidden
        resetStateBuilderDialog();
    }//GEN-LAST:event_stateBuilderDialogComponentHidden

    private void formComponentShown(java.awt.event.ComponentEvent evt) {//GEN-FIRST:event_formComponentShown

    }//GEN-LAST:event_formComponentShown

    private void transitionBuilderDoneButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_transitionBuilderDoneButtonActionPerformed
        this.transitionBuilderDialogHandleConfirm();
    }//GEN-LAST:event_transitionBuilderDoneButtonActionPerformed

    private void transitionBuilderNextStateDropdownActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_transitionBuilderNextStateDropdownActionPerformed
        // TODO add your handling code here:
    }//GEN-LAST:event_transitionBuilderNextStateDropdownActionPerformed

    private void transitionBuilderOpenHelpButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_transitionBuilderOpenHelpButtonActionPerformed
        transitionBuilderHelpDialog.setVisible(true);
    }//GEN-LAST:event_transitionBuilderOpenHelpButtonActionPerformed

    private void transitionBuilderHeadMovTFActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_transitionBuilderHeadMovTFActionPerformed
        // TODO add your handling code here:
    }//GEN-LAST:event_transitionBuilderHeadMovTFActionPerformed

    private void transitionBuilderErrorDialogOKButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_transitionBuilderErrorDialogOKButtonActionPerformed
        transitionBuilderErrorDialog.setVisible(false);
    }//GEN-LAST:event_transitionBuilderErrorDialogOKButtonActionPerformed

    private void transitionBuilderCancelButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_transitionBuilderCancelButtonActionPerformed
        transitionBuilderDialog.setVisible(false);
        this.resetTransitionBuilderDialog();
    }//GEN-LAST:event_transitionBuilderCancelButtonActionPerformed

    private void stepMachineButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_stepMachineButtonActionPerformed
        executeMachineStep();
    }//GEN-LAST:event_stepMachineButtonActionPerformed

    private void machineHasHaltedDialogOKButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_machineHasHaltedDialogOKButtonActionPerformed
        machineHasHaltedDialog.setVisible(false);
    }//GEN-LAST:event_machineHasHaltedDialogOKButtonActionPerformed

    private void tapeContentDisplayOKButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_tapeContentDisplayOKButtonActionPerformed
        tapeContentDisplayDialog.setVisible(false);
    }//GEN-LAST:event_tapeContentDisplayOKButtonActionPerformed

    private void machineHasHaltedDialogDisplayTapesButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_machineHasHaltedDialogDisplayTapesButtonActionPerformed
        this.tapeContentDisplayText.setText(TuringInterpreter.getTapesAsString());
        this.tapeContentDisplayDialog.pack();
        this.tapeContentDisplayDialog.setVisible(true);
    }//GEN-LAST:event_machineHasHaltedDialogDisplayTapesButtonActionPerformed

    private void machineHasHaltedialogSaveTapesButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_machineHasHaltedialogSaveTapesButtonActionPerformed
        int returnVal = fileChooserWindow.showOpenDialog(this);
        if (returnVal == JFileChooser.APPROVE_OPTION) {
            String newline = System.getProperty("line.separator");
            File file = fileChooserWindow.getSelectedFile();
            try (BufferedWriter w = new BufferedWriter(new FileWriter(file))) {
                w.write(TuringInterpreter.getTapesAsString());
                machineHasHaltedDialogText.setText(machineHasHaltedDialogText.getText() + newline + newline + "Tape content saved successfully.");
            } catch (IOException e) {
                //TODO: handle error. Probably throw something or show a popup.
            }
        } else {
            System.out.println("File access cancelled by user.");
        }
    }//GEN-LAST:event_machineHasHaltedialogSaveTapesButtonActionPerformed

    private void runMachineButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_runMachineButtonActionPerformed
        int delay = 0;
        if (timer.isRunning()) {
            timer.stop();
            runMachineButton.setText("Run machine");
        } else {
            switch (executionSpeedDropdown.getSelectedItem().toString()) {
                case "Slow":
                    delay = 500;
                    break;
                case "Medium":
                    delay = 250;
                    break;
                case "Fast":
                    delay = 100;
                    break;
                case "Instant":
                    break;
            }
            timer.setDelay(delay);
            timer.setInitialDelay(delay / 2);
            timer.start();
            runMachineButton.setText("Stop machine");
        }
    }//GEN-LAST:event_runMachineButtonActionPerformed

    private void resetMachineMIActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_resetMachineMIActionPerformed
        this.resetMachine();
        this.runMachineButton.setEnabled(true);
        this.stepMachineButton.setEnabled(true);
    }//GEN-LAST:event_resetMachineMIActionPerformed

    private void itPlaintextMIActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_itPlaintextMIActionPerformed
        itPlaintextDialog.pack();
        itPlaintextDialog.setVisible(true);
    }//GEN-LAST:event_itPlaintextMIActionPerformed

    private void itPlaintextOKButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_itPlaintextOKButtonActionPerformed
        TuringInterpreter.writeStringArrayToTapes(itPlaintextTextArea.getText().split("\r?\n", tapes.length));
        itPlaintextTextArea.setText("");
        this.updateTapesDisplayTA();
        itPlaintextDialog.setVisible(false);
    }//GEN-LAST:event_itPlaintextOKButtonActionPerformed

    private void resetMachineButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_resetMachineButtonActionPerformed
        resetMachine();
        this.runMachineButton.setEnabled(true);
        this.stepMachineButton.setEnabled(true);
    }//GEN-LAST:event_resetMachineButtonActionPerformed

    private void itBinaryMIActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_itBinaryMIActionPerformed
        itBinaryDialog.pack();
        itBinaryDialog.setVisible(true);
    }//GEN-LAST:event_itBinaryMIActionPerformed

    private void itBinaryOKButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_itBinaryOKButtonActionPerformed
        String[] lines = itBinaryTextArea.getText().split("\r?\n", tapes.length);
        String[] numberStrings;
        int[] numbers;
        int[][] tapeContents = new int[tapes.length][];
        for (int i = 0; i < tapes.length; i++) {
            numberStrings = lines[i].split(",");
            numbers = new int[numberStrings.length];
            for (int j = 0; j < numberStrings.length; j++) {
                numberStrings[j] = numberStrings[j].replaceAll("[ \t]", "");
                numbers[j] = Integer.parseInt(numberStrings[j]);
            }
            tapeContents[i] = numbers;
        }
        TuringInterpreter.writeBinaryArrayToTapes(tapeContents);
        itBinaryTextArea.setText("");
        this.updateTapesDisplayTA();
        this.itBinaryDialog.setVisible(false);
    }//GEN-LAST:event_itBinaryOKButtonActionPerformed

    private void itUnaryOKButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_itUnaryOKButtonActionPerformed
        String[] lines = itUnaryTextArea.getText().split("\r?\n", tapes.length);
        String[] numberStrings;
        int[] numbers;
        int[][] tapeContents = new int[tapes.length][];
        for (int i = 0; i < tapes.length; i++) {
            numberStrings = lines[i].split(",");
            numbers = new int[numberStrings.length];
            for (int j = 0; j < numberStrings.length; j++) {
                numberStrings[j] = numberStrings[j].replaceAll("[ \t]", "");
                numbers[j] = Integer.parseInt(numberStrings[j]);
            }
            tapeContents[i] = numbers;
        }
        TuringInterpreter.writeUnaryArrayToTapes(tapeContents);
        itUnaryTextArea.setText("");
        this.updateTapesDisplayTA();
        this.itUnaryDialog.setVisible(false);
    }//GEN-LAST:event_itUnaryOKButtonActionPerformed

    private void itUnaryMIActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_itUnaryMIActionPerformed
        this.itUnaryDialog.pack();
        this.itUnaryDialog.setVisible(true);
    }//GEN-LAST:event_itUnaryMIActionPerformed

    private void itUnaryCancelButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_itUnaryCancelButtonActionPerformed
        this.itUnaryTextArea.setText("");
        this.itUnaryDialog.setVisible(false);
    }//GEN-LAST:event_itUnaryCancelButtonActionPerformed

    private void itBinaryCancelButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_itBinaryCancelButtonActionPerformed
        this.itBinaryTextArea.setText("");
        this.itBinaryDialog.setVisible(false);
    }//GEN-LAST:event_itBinaryCancelButtonActionPerformed

    private void itPlaintextCancelButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_itPlaintextCancelButtonActionPerformed
        this.itPlaintextTextArea.setText("");
        this.itPlaintextDialog.setVisible(false);
    }//GEN-LAST:event_itPlaintextCancelButtonActionPerformed

    private void newMachineButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_newMachineButtonActionPerformed
        this.newMachineMIActionPerformed(evt);
    }//GEN-LAST:event_newMachineButtonActionPerformed

    private void modifyMachineButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_modifyMachineButtonActionPerformed
        this.modMachineMIActionPerformed(evt);
    }//GEN-LAST:event_modifyMachineButtonActionPerformed

    private void turingMachineExplanatoryOKButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_turingMachineExplanatoryOKButtonActionPerformed
        turingMachineExplanatoryDialog.setVisible(false);
    }//GEN-LAST:event_turingMachineExplanatoryOKButtonActionPerformed

    private void helpWhatIsTuringMachineMIActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_helpWhatIsTuringMachineMIActionPerformed
        this.turingMachineExplanatoryDialog.setVisible(true);
    }//GEN-LAST:event_helpWhatIsTuringMachineMIActionPerformed

    private void openFileButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_openFileButtonActionPerformed
        this.openFileMIActionPerformed(evt);
    }//GEN-LAST:event_openFileButtonActionPerformed

    /**
     * @param args the command line arguments
     */
    public static void main(String args[]) {
        /* Set the Nimbus look and feel */
        //<editor-fold defaultstate="collapsed" desc=" Look and feel setting code (optional) ">
        /* If Nimbus (introduced in Java SE 6) is not available, stay with the default look and feel.
         * For details see http://download.oracle.com/javase/tutorial/uiswing/lookandfeel/plaf.html
         */
        try {
            for (javax.swing.UIManager.LookAndFeelInfo info : javax.swing.UIManager.getInstalledLookAndFeels()) {
                if ("Nimbus".equals(info.getName())) {
                    javax.swing.UIManager.setLookAndFeel(info.getClassName());
                    break;
                }
            }
        } catch (ClassNotFoundException ex) {
            java.util.logging.Logger.getLogger(TuringInterpreterUI.class.getName()).log(java.util.logging.Level.SEVERE, null, ex);
        } catch (InstantiationException ex) {
            java.util.logging.Logger.getLogger(TuringInterpreterUI.class.getName()).log(java.util.logging.Level.SEVERE, null, ex);
        } catch (IllegalAccessException ex) {
            java.util.logging.Logger.getLogger(TuringInterpreterUI.class.getName()).log(java.util.logging.Level.SEVERE, null, ex);
        } catch (javax.swing.UnsupportedLookAndFeelException ex) {
            java.util.logging.Logger.getLogger(TuringInterpreterUI.class.getName()).log(java.util.logging.Level.SEVERE, null, ex);
        }
        //</editor-fold>

        /* Create and display the form */
        java.awt.EventQueue.invokeLater(new Runnable() {
            public void run() {
                new TuringInterpreterUI().setVisible(true);
            }
        });
    }

    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.JComboBox<String> executionSpeedDropdown;
    private javax.swing.JLabel executionSpeedlabel;
    private javax.swing.JMenuItem exitMI;
    private javax.swing.JFileChooser fileChooserWindow;
    private javax.swing.JMenu helpMenu;
    private javax.swing.JMenuItem helpWhatIsTuringMachineMI;
    private javax.swing.JMenu initialiseTapesSubMenu;
    private javax.swing.JButton itBinaryCancelButton;
    private javax.swing.JDialog itBinaryDialog;
    private javax.swing.JLabel itBinaryDialogLabel1;
    private javax.swing.JLabel itBinaryDialogLabel2;
    private javax.swing.JLabel itBinaryDialogLabel3;
    private javax.swing.JLabel itBinaryDialogLabel4;
    private javax.swing.JMenuItem itBinaryMI;
    private javax.swing.JButton itBinaryOKButton;
    private javax.swing.JTextArea itBinaryTextArea;
    private javax.swing.JButton itPlaintextCancelButton;
    private javax.swing.JDialog itPlaintextDialog;
    private javax.swing.JLabel itPlaintextDialogLabel1;
    private javax.swing.JLabel itPlaintextDialogLabel2;
    private javax.swing.JLabel itPlaintextDialogLabel3;
    private javax.swing.JMenuItem itPlaintextMI;
    private javax.swing.JButton itPlaintextOKButton;
    private javax.swing.JTextArea itPlaintextTextArea;
    private javax.swing.JButton itUnaryCancelButton;
    private javax.swing.JDialog itUnaryDialog;
    private javax.swing.JLabel itUnaryDialogLabel1;
    private javax.swing.JLabel itUnaryDialogLabel2;
    private javax.swing.JLabel itUnaryDialogLabel3;
    private javax.swing.JLabel itUnaryDialogLabel4;
    private javax.swing.JMenuItem itUnaryMI;
    private javax.swing.JButton itUnaryOKButton;
    private javax.swing.JTextArea itUnaryTextArea;
    private javax.swing.JScrollPane jScrollPane1;
    private javax.swing.JScrollPane jScrollPane2;
    private javax.swing.JScrollPane jScrollPane3;
    private javax.swing.JScrollPane jScrollPane4;
    private javax.swing.JScrollPane jScrollPane5;
    private javax.swing.JScrollPane jScrollPane6;
    private javax.swing.JScrollPane jScrollPane7;
    private javax.swing.JScrollPane jScrollPane8;
    private javax.swing.JScrollPane jScrollPane9;
    private javax.swing.JPanel machineDisplayPanel;
    private javax.swing.JDialog machineHasHaltedDialog;
    private javax.swing.JButton machineHasHaltedDialogDisplayTapesButton;
    private javax.swing.JButton machineHasHaltedDialogOKButton;
    private javax.swing.JTextArea machineHasHaltedDialogText;
    private javax.swing.JButton machineHasHaltedialogSaveTapesButton;
    private javax.swing.JMenu machineMenu;
    private javax.swing.JMenuBar mainMenuBar;
    private javax.swing.JMenuItem modMachineMI;
    private javax.swing.JButton modifyMachineButton;
    private javax.swing.JButton newMachineButton;
    private javax.swing.JMenuItem newMachineMI;
    private javax.swing.JButton nrOfTapesCancelButton;
    private javax.swing.JDialog nrOfTapesDialog;
    private javax.swing.JLabel nrOfTapesLabel;
    private javax.swing.JButton nrOfTapesOKButton;
    private javax.swing.JLabel nrOfTapesStateLabel;
    private javax.swing.JSpinner nrOfTapesStateSpinner;
    private javax.swing.JLabel nrOfTapesTapeLabel;
    private javax.swing.JSpinner nrOfTapesTapeSpinner;
    private javax.swing.JButton openFileButton;
    private javax.swing.JMenuItem openFileMI;
    private javax.swing.JButton resetMachineButton;
    private javax.swing.JMenuItem resetMachineMI;
    private javax.swing.JButton runMachineButton;
    private javax.swing.JMenuItem saveMachineMI;
    private javax.swing.JLabel stateBuilderAcceptingLabel;
    private javax.swing.JButton stateBuilderAddTransitionButton;
    private javax.swing.JButton stateBuilderCancelButton;
    private javax.swing.JButton stateBuilderChangeNrOfTapesStatesButton;
    private javax.swing.JDialog stateBuilderDialog;
    private javax.swing.JButton stateBuilderFinishMachineButton;
    private javax.swing.JButton stateBuilderRemoveTransitionButton;
    private javax.swing.JComboBox<String> stateBuilderStateSelectorDropdown;
    private javax.swing.JLabel stateBuilderStateSelectorLabel;
    private javax.swing.JList<String> stateBuilderStateTransitionList;
    private javax.swing.JLabel stateBuilderStateTransitionsLabel;
    private javax.swing.JButton stateBuilderToggleAcceptingButton;
    private javax.swing.JButton stepMachineButton;
    private javax.swing.JDialog tapeContentDisplayDialog;
    private javax.swing.JButton tapeContentDisplayOKButton;
    private javax.swing.JTextArea tapeContentDisplayText;
    private javax.swing.JScrollPane tapeDisplayScrollPane;
    private javax.swing.JTextArea tapeDisplayTextArea;
    private javax.swing.JMenu tapeMenu;
    private javax.swing.JButton transitionBuilderCancelButton;
    private javax.swing.JPanel transitionBuilderContainerPanel;
    private javax.swing.JDialog transitionBuilderDialog;
    private javax.swing.JButton transitionBuilderDoneButton;
    private javax.swing.JDialog transitionBuilderErrorDialog;
    private javax.swing.JButton transitionBuilderErrorDialogOKButton;
    private javax.swing.JTextArea transitionBuilderErrorDialogTF;
    private javax.swing.JLabel transitionBuilderHeadMovLabel;
    private javax.swing.JTextField transitionBuilderHeadMovTF;
    private javax.swing.JDialog transitionBuilderHelpDialog;
    private javax.swing.JButton transitionBuilderHelpOKButton;
    private javax.swing.JTextArea transitionBuilderHelpText;
    private javax.swing.JComboBox<String> transitionBuilderNextStateDropdown;
    private javax.swing.JLabel transitionBuilderNextStateLabel;
    private javax.swing.JButton transitionBuilderOpenHelpButton;
    private javax.swing.JLabel transitionBuilderToBeWrittenLabel;
    private javax.swing.JTextField transitionBuilderToBeWrittenTF;
    private javax.swing.JLabel transitionBuilderTriggerLabel;
    private javax.swing.JTextField transitionBuilderTriggerTF;
    private javax.swing.JDialog turingMachineExplanatoryDialog;
    private javax.swing.JButton turingMachineExplanatoryOKButton;
    private javax.swing.JTextPane turingMachineExplanatoryTextField;
    // End of variables declaration//GEN-END:variables
}
