package turinginterpreter;

import java.awt.Color;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.geom.Ellipse2D;
import java.awt.geom.Line2D;
import java.awt.geom.QuadCurve2D;
import javax.swing.*;

class MachineDiagram extends JPanel {

    int startingX = 500, startingY = 400, radius = 50, barb = 20, currentState;
    double phi = Math.PI / 6;
    double[][] contactPointsX, contactPointsY;
    State[] states;
    Timer timer;

    public MachineDiagram(int currentState) {
        this.currentState = currentState;
    }

    public void setCurrentState(int state) {
        currentState = state;
    }

    private void doDrawing(Graphics g) {

        Graphics2D g2d = (Graphics2D) g;
        g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

        g2d.setBackground(Color.white);

        if (null != TuringInterpreter.states) {
            states = TuringInterpreter.states;
            int arrangementRadius = 300;
            double arrangementAngle = 2 * Math.PI / states.length;
            contactPointsX = new double[states.length][states.length + 1];
            contactPointsY = new double[states.length][states.length + 1];
            for (int i = 0; i < states.length; i++) {
                if (i == currentState) {
                    this.drawState(arrangementRadius, arrangementAngle, i, states[i], Color.BLUE, g2d);
                } else {
                    this.drawState(arrangementRadius, arrangementAngle, i, states[i], Color.BLACK, g2d);
                }
            }
            for (int i = 0; i < states.length; i++) {
                for (Transition transition : states[i].getTransitions()) {
                    drawTransition(transition, i, arrangementAngle / 2, Color.BLACK, g2d);
                }
            }
        }

    }

    @Override
    public void paintComponent(Graphics g) {
        super.paintComponent(g);
        doDrawing(g);
    }

    private void drawState(int arrangementRadius, double arrangementAngle, int index, State state, Color color, Graphics2D g2d) {
        double circleX = startingX - radius + arrangementRadius * Math.sin(arrangementAngle * index);
        double circleY = startingY - radius + arrangementRadius * Math.cos(Math.PI + arrangementAngle * index);
        double labelX = circleX + radius - g2d.getFontMetrics().stringWidth(state.getName()) / 2;
        double labelY = circleY + radius;
        g2d.setColor(color);
        g2d.draw(new Ellipse2D.Double(circleX, circleY, 2 * radius, 2 * radius));
        g2d.drawString(state.getName(), (float) labelX, (float) labelY);
        if (state.getIsAccepting()) {
            g2d.draw(new Ellipse2D.Double(circleX + 10, circleY + 10, 2 * radius - 20, 2 * radius - 20));
        }
        generateContactPoints(circleX, circleY, index, arrangementAngle / 2);
    }

    private void drawArrow(Graphics2D g2d, double theta, double x0, double y0) {
        double x = x0 - barb * Math.cos(theta + phi);
        double y = y0 - barb * Math.sin(theta + phi);
        g2d.draw(new Line2D.Double(x0, y0, x, y));
        x = x0 - barb * Math.cos(theta - phi);
        y = y0 - barb * Math.sin(theta - phi);
        g2d.draw(new Line2D.Double(x0, y0, x, y));
    }

    private void drawTransition(Transition transition, int currentState, double cpAngle, Color color, Graphics2D g2d) {
        int targetIndex = transition.getNextState();
        if (targetIndex != currentState) {
            int startCPindex = targetIndex - currentState;
            if (startCPindex < 0) {
                startCPindex += states.length;
            }
            int endCPindex = currentState - targetIndex;
            if (endCPindex < 0) {
                endCPindex += states.length;
            }
            double startCPX = contactPointsX[currentState][startCPindex];
            double startCPY = contactPointsY[currentState][startCPindex];
            double endCPX = contactPointsX[targetIndex][endCPindex];
            double endCPY = contactPointsY[targetIndex][endCPindex];
            double arrowLength = Math.sqrt(Math.pow((startCPX - endCPX), 2) + Math.pow((startCPY - endCPY), 2));
            g2d.setColor(color);
            g2d.draw(new QuadCurve2D.Double(startCPX, startCPY, Math.sin((2 * currentState + startCPindex) * cpAngle) * arrowLength / 10 + (startCPX + endCPX) / 2, Math.cos((2 * currentState + startCPindex) * cpAngle) * arrowLength / 10 + (startCPY + endCPY) / 2, endCPX, endCPY));
            double theta = Math.atan2(endCPY - startCPY, endCPX - startCPX);
            drawArrow(g2d, theta, endCPX, endCPY);
            g2d.drawString(transition.getReadableForm(), (float) (Math.sin((2 * currentState + startCPindex) * cpAngle) * arrowLength / 10 + (startCPX + endCPX) / 2), (float) (Math.cos((2 * currentState + startCPindex) * cpAngle) * arrowLength / 10 + (startCPY + endCPY) / 2));
        } else {
            //TODO: figure out some way to make this do something useful.
        }
    }

    private void generateContactPoints(double circleX, double circleY, int index, double cpAngle) {
        for (int j = 0; j < states.length + 1; j++) {
            contactPointsX[index][j] = circleX + radius + radius * Math.cos((2 * index + j) * cpAngle);
            contactPointsY[index][j] = circleY + radius + radius * Math.sin((2 * index + j) * cpAngle);
        }
    }

}
