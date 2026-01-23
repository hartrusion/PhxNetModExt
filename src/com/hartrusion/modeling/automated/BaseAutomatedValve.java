/*
 * The MIT License
 *
 * Copyright 2025 Viktor Alexander Hartung.
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 * THE SOFTWARE.
 */
package com.hartrusion.modeling.automated;

import com.hartrusion.values.ValueHandler;
import com.hartrusion.control.SetpointIntegrator;
import com.hartrusion.control.ValveActuatorMonitor;
import com.hartrusion.mvc.ActionCommand;
import java.beans.PropertyChangeListener;
import java.beans.PropertyChangeSupport;
import java.util.function.BooleanSupplier;

/**
 * Base class with common valve control elements.
 *
 * @author Viktor Alexander Hartung
 */
public abstract class BaseAutomatedValve {

    /**
     * Generates limited values to mimic motor drive behavior.
     */
    protected final SetpointIntegrator swControl
            = new SetpointIntegrator();

    protected final ValveActuatorMonitor monitor
            = new ValveActuatorMonitor();

    protected final PropertyChangeSupport pcs = new PropertyChangeSupport(this);

    protected ValueHandler outputValues;

    protected boolean safeOpen = true;
    protected BooleanSupplier safeOpenProvider;
    protected boolean safeClosed = true;
    protected BooleanSupplier safeClosedProvider;

    public BaseAutomatedValve() {
        swControl.setMaxRate(25);
        swControl.setUpperLimit(100);
        swControl.setLowerLimit(-5.0);
    }

    protected String name = "unnamedAutomatedValve";

    public void initName(String name) {
        this.name = name;
        monitor.setName(name);
    }

    /**
     * Makes the signal listener instance known to this class.
     *
     * @param signalListener Instance that will receive the event changes from
     * valves and pumps.
     */
    public void registerSignalListener(PropertyChangeListener signalListener) {
        pcs.addPropertyChangeListener(signalListener);
        monitor.addPropertyChangeListener(signalListener);
    }

    /**
     * Sets a ParameterHandler that will get the valve position on each run
     * call.
     *
     * @param h reference to ParameterHandler
     */
    public void registerParameterHandler(ValueHandler h) {
        outputValues = h;
    }

    /**
     * Allows processing received events by the class itself. The Property name
     * must begin with the same name as this classes elements, which was
     * initialized with the initName function call.
     *
     * @param ac ActionCommand, will be further checked if it's matching.
     * @return true if event was processed by this instance.
     */
    public boolean handleAction(ActionCommand ac) {
        if (!ac.getPropertyName().equals(name)) {
            return false;
        }
        // Int values are sent from so-called Integral switches, as long as they
        // are pressed, value integrates. The press sends a +1 or -1 and the
        // release of the button sends a 0, but this is done with default.
        if (ac.getValue() instanceof Integer) {
            switch ((int) ac.getValue()) {
                case -1 ->
                    operateCloseValve();
                case +1 ->
                    operateOpenValve();
                default ->
                    stopValve();
            }
        } else if (ac.getValue() instanceof Boolean) {
            if ((boolean) ac.getValue()) {
                operateOpenValve();
            } else {
                operateCloseValve();
            }
        }
        return true;
    }

    public void operateOpenValve() {
        swControl.setInputMax();
    }

    public void operateCloseValve() {
        swControl.setInputMin();
    }

    public void operateSetOpening(double opening) {
        swControl.setInput(opening);
    }

    public void stopValve() {
        swControl.setStop();
    }

    public double getOpening() {
        return swControl.getOutput();
    }

    public SetpointIntegrator getIntegrator() {
        return swControl;
    }

    /**
     * Adds an external provider which defines criteria for safe open of this
     * valve. Note that safety logic is reversed, meaning the boolean value must
     * be true for the valve element to be able to close it. Can be used with
     * anonymous classes or lambda expression.
     *
     * @param safeOpenProvider a BooleanSupplier which returns FALSE for OPEN
     */
    public void addSafeOpenProvider(BooleanSupplier safeOpenProvider) {
        this.safeOpenProvider = safeOpenProvider;
    }

    /**
     * Adds an external provider which defines criteria for safe shut of this
     * valve. Note that safety logic is reversed, meaning the boolean value must
     * be true to be able to open the valve. Can be used with anonymous classes
     * or lambda expression.
     *
     * @param safeClosedProvider a BooleanSupplier which returns FALSE for SHUT
     */
    public void addSafeClosedProvider(BooleanSupplier safeClosedProvider) {
        this.safeClosedProvider = safeClosedProvider;
    }
}
