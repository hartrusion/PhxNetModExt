/*
 * Copyright (C) 2025 Viktor Alexander Hartung
 *
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
package com.hartrusion.modeling.automated;

import com.hartrusion.values.ValueHandler;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.beans.PropertyChangeSupport;
import com.hartrusion.control.SetpointIntegrator;
import com.hartrusion.control.ValveActuatorMonitor;
import com.hartrusion.modeling.phasedfluid.PhasedLinearValve;
import com.hartrusion.mvc.ActionCommand;

/**
 * HeatLinearValve with SetpointIntegrator as actuator and a Monitor for firing
 * state change properties.
 *
 * @author Viktor Alexander Hartung
 */
public class PhasedValve extends BaseAutomatedValve implements Runnable {

    /**
     * The valve element of the model itself.
     */
    protected final PhasedLinearValve valve = new PhasedLinearValve();

    public void initName(String name) {
        super.initName(name);
        valve.setName(name);
    }

    /**
     * Initializes the valves characterisitc.
     *
     * @param resistanceFullOpen Flow resistance on 100 % opening state, given
     * in Pa/kg*s
     * @param closedFactor set to less than 1.0 for default behavior, values
     * higher than 1.0 will be set as closedFactor of the valve.
     *
     */
    public void initCharacteristic(double resistanceFullOpen, double closedFactor) {
        valve.setResistanceFullOpen(resistanceFullOpen);
        if (closedFactor > 1.0) {
            valve.setCharacteristic(false, closedFactor);
        } else {
            valve.setCharacteristic(true, 0.0);
        }

    }

    public void initOpening(double opening) {
        valve.setOpening(opening);
        swControl.forceOutputValue(opening);
        // Todo - dirty workaround - the not closed signal has to be sent 
        // if the valve is not closed initially to have the mnemonic gui 
        // showing it.
        if (opening > 5.0) {
            pcs.firePropertyChange(new PropertyChangeEvent(this,
                    valve.toString() + "_Closed",
                    null, false));
        }

    }

    @Override
    public void run() {
        if (safeOpenProvider != null) {
            safeOpen = safeOpenProvider.getAsBoolean();
        }
        if (safeClosedProvider != null) {
            safeClosed = safeClosedProvider.getAsBoolean();
        }
        
        // Force valve open or closed if safety signal is missing
        if (!safeClosed) {
            swControl.setInputMin();
        } else if (!safeOpen) {
            swControl.setInputMax();
        }
        
        swControl.run();
        valve.setOpening(swControl.getOutput());
        monitor.setInput(swControl.getOutput());
        monitor.run();

        // Send valve position as parameter value for monitoring
        if (outputValues != null) {
            outputValues.setParameterValue(valve.toString(),
                    valve.getOpening());
        }
    }

    public PhasedLinearValve getValveElement() {
        return valve;
    }
}
