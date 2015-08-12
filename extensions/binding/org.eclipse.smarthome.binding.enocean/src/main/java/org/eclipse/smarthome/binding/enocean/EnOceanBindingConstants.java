/**
 * Copyright (c) 2014-2015 openHAB UG (haftungsbeschraenkt) and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.eclipse.smarthome.binding.enocean;

import org.eclipse.smarthome.core.thing.ThingTypeUID;

/**
 * The {@link EnOceanBinding} class defines common constants, which are
 * used across the whole binding.
 *
 * @author Thomas Goldschmidt - Initial contribution
 */
public class EnOceanBindingConstants {

    public static final String BINDING_ID = "enocean";

    // List of all Thing Type UIDs
    public final static ThingTypeUID THING_TYPE_F60201 = new ThingTypeUID(BINDING_ID, "F6-02-01");
    public final static ThingTypeUID THING_TYPE_A51003 = new ThingTypeUID(BINDING_ID, "A5-10-03");
    public static final ThingTypeUID THING_TYPE_FSR14_4X = new ThingTypeUID(BINDING_ID, "FSR14-4x");

    // List of all Channel ids
    public final static String CHANNEL_SWITCH_A = "switchA";
    public final static String CHANNEL_SWITCH_B = "switchB";

    public final static String CHANNEL_TEMPERATURE = "temperature";
    public final static String CHANNEL_SET_POINT = "setPoint";

}
