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

import com.hartrusion.modeling.heatfluid.HeatLinearValve;
/**
 * HeatLinearValve with SetpointIntegrator as actuator and a Monitor for firing
 * state change properties.
 * <p>
 * Received ActionCommands with the set name (to be set with initName) will be
 * used to control the value. Depending on the type, integers can be used to
 * perform opening or closing until a int 0 value is send. Boolean values will
 * either fully open or fully close the valve.
 * <p>
 * The attached monitor will fire property changes to all registered listeners
 * which are defined in
 *
 * @author Viktor Alexander Hartung
 */
public class HeatValve extends BaseAutomatedValve implements Runnable {
    /**
     * The valve element of the model itself.
     */
    protected final HeatLinearValve valve = new HeatLinearValve();

    @Override
    public void initName(String name) {
        super.initName(name);
        valve.setName(name);
    }

    /**
     * Initializes the valves characteristic.
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

    public HeatLinearValve getValveElement() {
        return valve;
    }
}
