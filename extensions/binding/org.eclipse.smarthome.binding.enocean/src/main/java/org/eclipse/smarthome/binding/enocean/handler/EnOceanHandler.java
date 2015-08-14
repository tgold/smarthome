/**
 * Copyright (c) 2014-2015 openHAB UG (haftungsbeschraenkt) and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.eclipse.smarthome.binding.enocean.handler;

import java.math.BigInteger;
import java.util.Hashtable;
import java.util.Map;

import org.eclipse.smarthome.binding.enocean.EnOceanBindingConstants;
import org.eclipse.smarthome.binding.enocean.internal.Utils;
import org.eclipse.smarthome.core.library.types.DecimalType;
import org.eclipse.smarthome.core.library.types.OnOffType;
import org.eclipse.smarthome.core.thing.ChannelUID;
import org.eclipse.smarthome.core.thing.Thing;
import org.eclipse.smarthome.core.thing.ThingStatus;
import org.eclipse.smarthome.core.thing.binding.BaseThingHandler;
import org.eclipse.smarthome.core.types.Command;
import org.osgi.framework.ServiceRegistration;
import org.osgi.service.enocean.EnOceanDevice;
import org.osgi.service.enocean.EnOceanEvent;
import org.osgi.service.enocean.EnOceanMessage;
import org.osgi.service.event.Event;
import org.osgi.service.event.EventAdmin;
import org.osgi.service.event.EventConstants;
import org.osgi.service.event.EventHandler;
import org.osgi.util.tracker.ServiceTracker;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * The {@link EnOceanHandler} is responsible for handling commands, which are
 * sent to one of the channels.
 *
 * @author Thomas Goldschmidt - Initial contribution
 */
public class EnOceanHandler extends BaseThingHandler implements org.osgi.service.event.EventHandler {

    private Logger logger = LoggerFactory.getLogger(EnOceanHandler.class);
    private ServiceRegistration<?> deviceEventSubscription;
    private ServiceTracker eventAdminTracker;

    public EnOceanHandler(Thing thing) {
        super(thing);
    }

    @Override
    public void initialize() {
        // TODO: Initialize the thing. If done set status to ONLINE to indicate proper working.
        // Long running initialization should be done asynchronously in background.
        updateStatus(ThingStatus.ONLINE);

        // Note: When initialization can NOT be done set the status with more details for further
        // analysis. See also class ThingStatusDetail for all available status details.
        // Add a description to give user information to understand why thing does not work
        // as expected. E.g.
        // updateStatus(ThingStatus.OFFLINE, ThingStatusDetail.CONFIGURATION_ERROR,
        // "Can not access device as username and/or password are invalid");

        /* Track device events */
        /* Initializes self as EventHandler */
        Hashtable<String, String[]> ht = new Hashtable<String, String[]>();
        ht.put(EventConstants.EVENT_TOPIC, new String[] { EnOceanEvent.TOPIC_MSG_RECEIVED, });
        deviceEventSubscription = bundleContext.registerService(EventHandler.class.getName(), this, ht);

        /* For sending message back to the EnOcean Service */
        eventAdminTracker = new ServiceTracker(bundleContext, EventAdmin.class.getName(), null);
        eventAdminTracker.open();
        logger.debug(
                "A servicetracker for eventadmin service has been created, and opened. EventAdmin is required by the base driver.");
    }

    @Override
    public void handleCommand(ChannelUID channelUID, Command command) {
        if (channelUID.getId().equals(EnOceanBindingConstants.CHANNEL_SET_POINT)) {
            // TODO: handle command

            // Note: if communication with thing fails for some reason,
            // indicate that by setting the status with detail information
            // updateStatus(ThingStatus.OFFLINE, ThingStatusDetail.COMMUNICATION_ERROR,
            // "Could not control device at IP address x.x.x.x");
        } else if (channelUID.getId().equals(EnOceanBindingConstants.CHANNEL_SWITCH_A)) {
            final OnOffType onOffCommand = (OnOffType) command;
            EnOceanMessage msg = new EnOceanMessage() {

                @Override
                public int getType() {
                    return 0xF6;
                }

                @Override
                public int getSubTelNum() {
                    return 1;
                }

                @Override
                public int getStatus() {
                    // TODO Auto-generated method stub
                    return 0;
                }

                @Override
                public int getSenderId() {
                    return Integer.decode("0X" + (String) getThing().getConfiguration().get("localID"));
                }

                @Override
                public int getSecurityLevelFormat() {
                    // TODO Auto-generated method stub
                    return 0;
                }

                @Override
                public int getRorg() {
                    return 0xF6;
                }

                @Override
                public byte[] getPayloadBytes() {
                    byte[] data = new byte[1];
                    if (onOffCommand.equals(OnOffType.ON)) {
                        byte dataDB0InHexAsByte = new Integer(0x10).byteValue();
                    } else if (onOffCommand.equals(OnOffType.OFF)) {
                        byte dataDB0InHexAsByte = new Integer(0x30).byteValue();
                    } else {
                        // do nothing
                    }
                    return data;
                }

                @Override
                public int getFunc() {
                    return 0x02;
                }

                @Override
                public int getDestinationId() {
                    return 0xFFFFFF;
                }

                @Override
                public int getDbm() {
                    // TODO Auto-generated method stub
                    return 0;
                }

                @Override
                public byte[] getBytes() {
                    byte[] data = new byte[14];

                    int i = 0;

                    data[i++] = (byte) getRorg();
                    data[i++] = getPayloadBytes()[0]; // we know it is only one;
                    for (byte b : BigInteger.valueOf(getSenderId()).toByteArray()) {
                        data[i++] = b;
                    }
                    data[i++] = (byte) getStatus();
                    data[i++] = (byte) getSubTelNum();
                    for (byte b : BigInteger.valueOf(getDestinationId()).toByteArray()) {
                        data[i++] = b;
                    }
                    data[i++] = (byte) getDbm();
                    data[i++] = (byte) getSecurityLevelFormat();
                    return data;
                }
            };

            broadcastToEventAdmin(msg);
        }
    }

    @Override
    public void handleEvent(Event event) {
        logger.debug("> handleEvent(event: " + event);

        logger.debug("event.getTopic(): " + event.getTopic());
        // event.getTopic():
        // org/osgi/service/enocean/EnOceanEvent/MESSAGE_RECEIVED
        logger.debug("event.getPropertyNames(): " + event.getPropertyNames());
        // event.getPropertyNames(): [Ljava.lang.String;@194737d

        String[] pns = event.getPropertyNames();
        int i = 0;
        while (i < pns.length) {
            logger.debug(
                    "pns[" + i + "]: " + pns[i] + ", event.getProperty(" + pns[i] + "): " + event.getProperty(pns[i]));
            i = i + 1;

            // pns[0]: enocean.device.profile.func,
            // event.getProperty(enocean.device.profile.func): 2
            // pns[1]: enocean.device.profile.rorg,
            // event.getProperty(enocean.device.profile.rorg): 165
            // pns[2]: enocean.device.chip_id,
            // event.getProperty(enocean.device.chip_id): 8969457
            // pns[3]: enocean.device.profile.type,
            // event.getProperty(enocean.device.profile.type): 5
            // pns[4]: enocean.message, event.getProperty(enocean.message):
            // a5000035080088dcf100
            // pns[5]: event.topics, event.getProperty(event.topics):
            // org/osgi/service/enocean/EnOceanEvent/MESSAGE_RECEIVED
        }

        String topic = event.getTopic();
        // filter only for those messages that match the predefined EnOcean topic
        // and that are not exported by ourselves.
        if (topic.equals(EnOceanEvent.TOPIC_MSG_RECEIVED)
                && event.getProperty(EnOceanEvent.PROPERTY_EXPORTED) == null) {
            String chipId = (String) event.getProperty(EnOceanDevice.CHIP_ID);
            String rorg = (String) event.getProperty(EnOceanDevice.RORG);
            String func = (String) event.getProperty(EnOceanDevice.FUNC);
            String type = (String) event.getProperty(EnOceanDevice.TYPE);
            EnOceanMessage data = (EnOceanMessage) event.getProperty(EnOceanEvent.PROPERTY_MESSAGE);
            String displayId = Utils.printUid(Integer.parseInt(chipId));
            String profile = rorg + "/" + func + "/" + type;
            logger.debug("> MSG_RECEIVED event : sender=" + displayId + ", profile = '" + profile + "'");

            // check if the message was sent from "our thing"
            if (displayId.equalsIgnoreCase(
                    "0x" + thing.getConfiguration().get(EnOceanBindingConstants.CONFIG_PROPERTY_DEVICE_ID))) {
                logger.debug(
                        "Try to identify the device that has sent the just received event (e.g. is it an A5-10-03 device - a temperature sensor range 0°C to +40°C and a temperature set point).");
                if ("165".equals(rorg)) {
                    // hex 0xa5 == int 165.
                    if ("10".equals(func)) {
                        if ("3".equals(type)) {
                            logger.debug("This event has been sent by an A5-10-03 device.");
                            logger.debug(
                                    "The end of page 12, and the beginning of page 13 of EnOcean_Equipment_Profiles_EEP_V2.61_public.pdf specifies how to get the temp°C value starting from an EnOcean telegram.");
                            // propertyNames[4]: enocean.message,
                            // event.getProperty(propertyNames[4]):
                            // a5000035080088dcf100
                            byte[] payload = data.getPayloadBytes();
                            logger.debug("payload: " + payload + ", payload.length: " + payload.length);
                            int j = 0;
                            while (j < payload.length) {
                                logger.debug("payload[" + j + "]: " + payload[j]);
                                j = j + 1;
                            }
                            byte rawTemperatureDB1InHexAsAByte = payload[2];
                            float rawTemperatureDB1InNumberAsADouble = rawTemperatureDB1InHexAsAByte;
                            logger.debug("rawTemperatureDB1InNumberAsADouble: " + rawTemperatureDB1InNumberAsADouble);
                            if (rawTemperatureDB1InNumberAsADouble < 0) {
                                logger.debug(
                                        "rawTemperatureDB1InNumberAsADouble is negative, so let's convert rawTemperatureDB1InNumberAsADouble to unsigned 0..255 value instead of -127..128 one.");
                                rawTemperatureDB1InNumberAsADouble = rawTemperatureDB1InNumberAsADouble * -1
                                        + 2 * (128 + rawTemperatureDB1InNumberAsADouble);
                                logger.debug(
                                        "rawTemperatureDB1InNumberAsADouble: " + rawTemperatureDB1InNumberAsADouble);
                            } else {
                                logger.debug("rawTemperatureDB1InNumberAsADouble is positive, everything is ok.");
                            }

                            // Now let's apply the formula:
                            // (rawTemperatureDB1InNumberAsADouble-255)*-40/255+0 =
                            // temp in celsius.
                            double tempInCelsius = (rawTemperatureDB1InNumberAsADouble - 255) * -40 / 255 + 0;
                            updateState(
                                    new ChannelUID(getThing().getUID(), EnOceanBindingConstants.CHANNEL_TEMPERATURE),
                                    new DecimalType(tempInCelsius));
                            logger.debug("tempInCelsius: " + tempInCelsius);
                        } else {
                            logger.debug("This event has NOT been sent by an A5-02-05 device. TYPE is NOT equal to 5.");
                        }
                    } else {
                        logger.debug("This event has NOT been sent by an A5-02-05 device. FUNC is NOT equal to 2.");
                    }
                } else if ("246".equals(rorg)) {
                    // hex 0xf6 == int 246.
                    logger.debug("This event has been sent by an F6-wx-yz device.");
                    logger.debug(
                            "FUNC, and TYPE are NOT sent by F6-wx-yz device. The system then assumes that the device is an F6-02-01.");
                    logger.debug(
                            "In EnOcean_Equipment_Profiles_EEP_V2.61_public.pdf, pages 13-14, F6-02-01 -> RPS Telegram, Rocker Switch, 2 Rocker, Light and Blind Control - Application Style 1");

                    // byte[] payload = data.getPayloadBytes(); using
                    // getPayloadBytes() is NOT enough here.
                    byte[] payload = data.getBytes();
                    // e.g. f6500029219f3003ffffffff3100 when the button BI of an
                    // F6-02-01 device is pressed.

                    logger.debug("payload: " + payload + ", payload.length: " + payload.length);
                    int j = 0;
                    while (j < payload.length) {
                        logger.debug("payload[" + j + "] & 0xff (value is displayed as an unsigned int): "
                                + (payload[j] & 0xff));
                        j = j + 1;
                    }

                    byte dataDB0InHexAsAByte = payload[1];
                    logger.debug("dataDB0InHexAsAByte: " + dataDB0InHexAsAByte);
                    int dataDB0InHexAsAnInt = dataDB0InHexAsAByte & 0xff;
                    logger.debug("dataDB0InHexAsAnInt: " + dataDB0InHexAsAnInt);

                    byte statusInHexAsAByte = payload[6];
                    logger.debug("statusInHexAsAByte: " + statusInHexAsAByte);
                    int statusInHexAsAsAnInt = statusInHexAsAByte & 0xff;
                    logger.debug("statusInHexAsAsAnInt: " + statusInHexAsAsAnInt);

                    if ((new Integer(0x30).byteValue() & 0xff) == statusInHexAsAsAnInt) {
                        logger.debug("Here, a button has been pressed.");
                        if ((new Integer(0x30).byteValue() & 0xff) == dataDB0InHexAsAnInt) {
                            // Check if A0 button has been pressed --> 0x30
                            logger.debug("A0");
                            updateState(new ChannelUID(getThing().getUID(), EnOceanBindingConstants.CHANNEL_SWITCH_A),
                                    OnOffType.OFF);
                        } else if ((new Integer(0x10).byteValue() & 0xff) == dataDB0InHexAsAnInt) {
                            // Check if AI button has been pressed --> 0x10
                            logger.debug("AI");
                            updateState(new ChannelUID(getThing().getUID(), EnOceanBindingConstants.CHANNEL_SWITCH_A),
                                    OnOffType.ON);
                        } else if ((new Integer(0x70).byteValue() & 0xff) == dataDB0InHexAsAnInt) {
                            // Check if A0 button has been pressed --> 0x70
                            logger.debug("B0");
                            updateState(new ChannelUID(getThing().getUID(), EnOceanBindingConstants.CHANNEL_SWITCH_B),
                                    OnOffType.OFF);
                        } else if ((new Integer(0x50).byteValue() & 0xff) == dataDB0InHexAsAnInt) {
                            // Check if A0 button has been pressed --> 0x50
                            logger.debug("BI");
                            // The switch is used to simulate the smoke
                            updateState(new ChannelUID(getThing().getUID(), EnOceanBindingConstants.CHANNEL_SWITCH_B),
                                    OnOffType.ON);
                        } else {
                            logger.debug("The given Data DB_0 is UNKNOWN; its value is: " + dataDB0InHexAsAnInt);
                        }
                    } else if ((new Integer(0x20).byteValue() & 0xff) == statusInHexAsAsAnInt) {
                        logger.debug("Here, a button has been released (normally, this button was the pressed one.)");
                        // if (alarmIsActive) {
                        // // The switch is used to simulate the smoke
                        // // detector.
                        // // No Alarm
                        // logger.debug("No ALARM");
                        //// reportFireNormalSituation();
                        //// alarmIsActive = false;
                        // }
                    } else {
                        logger.debug(
                                "The given status field of this RPS telegram is UNKNOWN. This status was (as an int): "
                                        + statusInHexAsAsAnInt);
                    }

                } else if ("213".equals(rorg)) {
                    // hex 0xd5 == int 213.
                    logger.debug("This event has been sent by a D5-wx-yz device.");
                    logger.debug(
                            "FUNC, and TYPE are NOT sent by D5-wx-yz device. The system then assumes that the device is an D5-00-01.");
                    logger.debug(
                            "In EnOcean_Equipment_Profiles_EEP_V2.61_public.pdf, pages 24, D5-00-01 -> 1BS Telegram, Contacts and Switches, Single Input Contact.");

                    // logger.debug("data.getBytes(): " + data.getBytes());
                    // logger.debug("Utils.bytesToHexString(data.getBytes()): "
                    // + Utils.bytesToHexString(data.getBytes()));
                    //
                    // logger.debug("data.getDbm(): " + data.getDbm());
                    // logger.debug("data.getDestinationId(): "
                    // + data.getDestinationId());
                    // logger.debug("data.getFunc(): " + data.getFunc());
                    //
                    // logger.debug("data.getPayloadBytes(): "
                    // + data.getPayloadBytes());
                    // Logger.d(TAG,
                    // "Utils.bytesToHexString(data.getPayloadBytes()): "
                    // + Utils.bytesToHexString(data.getPayloadBytes()));
                    //
                    // logger.debug("data.getRorg(): " + data.getRorg());
                    // logger.debug("data.getSecurityLevelFormat(): "
                    // + data.getSecurityLevelFormat());
                    // logger.debug("data.getSenderId(): " + data.getSenderId());
                    // logger.debug("data.getStatus(): " + data.getStatus());
                    // logger.debug("data.getSubTelNum(): " + data.getSubTelNum());
                    // logger.debug("data.getType(): " + data.getType());

                    if (8 == data.getPayloadBytes()[0]) {
                        logger.debug("An opening has been detected.");
                    } else if (9 == data.getPayloadBytes()[0]) {
                        logger.debug("A closing has been detected.");
                    } else if (0 == data.getPayloadBytes()[0]) {
                        logger.debug("The LRN button has been pressed.");
                    } else {
                        logger.debug("The given 1BS's Data DB_0 value (data.getPayloadBytes()[0]: "
                                + data.getPayloadBytes()[0]
                                + " doesn't correspond to anything in EnOcean's specs. There is a pb. The system doesn't know how to handle this message.");
                    }
                } else {
                    logger.debug(
                            "This event has NOT been sent by an A5-02-05 device, nor by a F6-wx-yz device, nor by a D5-wx-yz device. "
                                    + "RORG is NOT equal to a5, nor f6,nor d5 (0xa5 is equal to int 165; 0xf6 -> 246, 0xd5 -> 213).");
                }
            }

        }
    }

    private void broadcastToEventAdmin(EnOceanMessage eoMsg) {
        EventAdmin eventAdmin = (EventAdmin) eventAdminTracker.getService();
        if (eventAdmin == null) {
            logger.error(
                    "The system can NOT find any implementation of EventAdmin. The EnOceanMessage can NOT be broadcasted.");
        } else {
            Map properties = new Hashtable();
            properties.put(EnOceanDevice.CHIP_ID, String.valueOf(eoMsg.getSenderId()));
            properties.put(EnOceanDevice.RORG, String.valueOf(eoMsg.getRorg()));
            properties.put(EnOceanDevice.FUNC, String.valueOf(eoMsg.getFunc()));
            properties.put(EnOceanDevice.TYPE, String.valueOf(eoMsg.getType()));
            properties.put(EnOceanEvent.PROPERTY_EXPORTED, Boolean.TRUE);
            properties.put(EnOceanEvent.PROPERTY_MESSAGE, eoMsg);

            Event event = new Event(EnOceanEvent.TOPIC_MSG_RECEIVED, properties);
            eventAdmin.sendEvent(event);
            logger.error("The following event has been sent via EventAdmin; event: " + event);
        }
    }

}
