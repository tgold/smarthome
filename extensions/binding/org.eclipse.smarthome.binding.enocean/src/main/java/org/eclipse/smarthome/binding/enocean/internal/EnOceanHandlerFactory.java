/**
 * Copyright (c) 2014 openHAB UG (haftungsbeschraenkt) and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.eclipse.smarthome.binding.enocean.internal;

import java.util.Set;

import org.eclipse.smarthome.binding.enocean.EnOceanBindingConstants;
import org.eclipse.smarthome.binding.enocean.handler.EnOceanHandler;
import org.eclipse.smarthome.core.thing.Thing;
import org.eclipse.smarthome.core.thing.ThingTypeUID;
import org.eclipse.smarthome.core.thing.binding.BaseThingHandlerFactory;
import org.eclipse.smarthome.core.thing.binding.ThingHandler;
import org.osgi.framework.InvalidSyntaxException;
import org.osgi.framework.ServiceReference;
import org.osgi.framework.ServiceRegistration;
import org.osgi.service.component.ComponentContext;
import org.osgi.service.enocean.EnOceanDevice;
import org.osgi.service.enocean.EnOceanHost;
import org.osgi.service.event.EventAdmin;
import org.osgi.util.tracker.ServiceTracker;
import org.osgi.util.tracker.ServiceTrackerCustomizer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.collect.Sets;

/**
 * The {@link EnOceanHandlerFactory} is responsible for creating things and thing
 * handlers.
 *
 * @author Thomas Goldschmidt - Initial contribution
 */
public class EnOceanHandlerFactory extends BaseThingHandlerFactory
        implements ServiceTrackerCustomizer<EnOceanDevice, Object> {

    private Logger logger = LoggerFactory.getLogger(EnOceanHandler.class);

    public final static Set<ThingTypeUID> SUPPORTED_THING_TYPES_UIDS = Sets.newHashSet(
            EnOceanBindingConstants.THING_TYPE_F60201, EnOceanBindingConstants.THING_TYPE_F60201_ROLLERSHUTTER,
            EnOceanBindingConstants.THING_TYPE_A51003);

    private EnOceanHost baseDriver;

    private ServiceTracker<EnOceanDevice, Object> deviceTracker;

    private ServiceRegistration<?> deviceEventSubscription;

    private EventAdmin eventAdmin;

    @Override
    public boolean supportsThingType(ThingTypeUID thingTypeUID) {
        return SUPPORTED_THING_TYPES_UIDS.contains(thingTypeUID);
    }

    @Override
    protected ThingHandler createHandler(Thing thing) {

        ThingTypeUID thingTypeUID = thing.getThingTypeUID();

        if (SUPPORTED_THING_TYPES_UIDS.contains(thingTypeUID)) {
            return new EnOceanHandler(thing);
        }

        return null;
    }

    @Override
    protected void activate(ComponentContext componentContext) {
        // TODO Auto-generated method stub
        super.activate(componentContext);

        logger.debug(
                "IN: org.eclipse.smarthome.protocols.enocean.sample.client.Activator.start(bc: " + bundleContext + ")");

        /* Track device creation */
        try {
            deviceTracker = new ServiceTracker<EnOceanDevice, Object>(bundleContext,
                    bundleContext.createFilter("(&(objectclass=" + EnOceanDevice.class.getName() + "))"), this);

            deviceTracker.open();
        } catch (InvalidSyntaxException e) {
            logger.error("Cannot create ServiceTreacker for filter: " + "(&(objectclass="
                    + EnOceanDevice.class.getName() + "))", e);
        }

        // try {
        // Thread.sleep(3000);
        // } catch (InterruptedException e) {
        // e.printStackTrace();
        // }

        // Display the EnOceanDevice services.
        ServiceReference<?>[] srs;
        try {
            srs = bundleContext.getAllServiceReferences(EnOceanDevice.class.getName(), null);

            logger.debug("srs: " + srs);
            if (srs == null) {
                logger.debug("There is NO service registered with the following class name: "
                        + EnOceanDevice.class.getName());
            } else {
                logger.debug("srs.length: " + srs.length);

                int i = 0;
                while (i < srs.length) {
                    ServiceReference<?> sr = srs[i];
                    logger.debug("sr: " + sr);

                    String[] pks = sr.getPropertyKeys();
                    int j = 0;
                    while (j < pks.length) {
                        logger.debug("pks[" + j + "]: " + pks[j] + ", event.getProperty(" + pks[j] + "): "
                                + sr.getProperty(pks[j]));
                        j = j + 1;
                    }

                    EnOceanDevice eod = (EnOceanDevice) bundleContext.getService(sr);
                    logger.debug("eod: " + eod);
                    logger.debug("eod.getChipId(): " + eod.getChipId());
                    logger.debug("eod.getFunc(): " + eod.getFunc());
                    logger.debug("eod.getManufacturer(): " + eod.getManufacturer());
                    logger.debug("eod.getRollingCode(): " + eod.getRollingCode());
                    logger.debug("eod.getRorg(): " + eod.getRorg());
                    logger.debug("eod.getSecurityLevelFormat(): " + eod.getSecurityLevelFormat());
                    logger.debug("eod.getType(): " + eod.getType());
                    logger.debug("eod.getClass(): " + eod.getClass());
                    logger.debug("eod.getEncryptionKey(): " + eod.getEncryptionKey());
                    logger.debug("eod.getLearnedDevices(): " + eod.getLearnedDevices());
                    logger.debug("eod.getRPCs(): " + eod.getRPCs());

                    // // The following RPC is a copy of:
                    // // org.osgi.test.cases.enocean.rpc.QueryFunction
                    // EnOceanRPC rpc = new EnOceanRPC() {
                    //
                    // // sender='0x0180abb8'
                    //
                    // // propertyNames[0]: enocean.device.profile.func,
                    // // event.getProperty(propertyNames[0]): -1
                    //
                    // // propertyNames[1]: enocean.device.profile.rorg,
                    // // event.getProperty(propertyNames[1]): 165
                    //
                    // // propertyNames[2]: enocean.device.chip_id,
                    // // event.getProperty(propertyNames[2]): 25209784
                    //
                    // // propertyNames[3]: enocean.device.profile.type,
                    // // event.getProperty(propertyNames[3]): -1
                    //
                    // // propertyNames[4]: enocean.message,
                    // // event.getProperty(propertyNames[4]): a5000074080180abb800
                    //
                    // // private int senderId = -1;
                    // private int senderId = 0x0180abb8;
                    //
                    // public void setSenderId(int chipId) {
                    // this.senderId = chipId;
                    // }
                    //
                    // // public void setPayload(byte[] data) {
                    // // // does nothing;
                    // // logger.debug("rpc.setPayLoad(data: " + data + ")");
                    // // }
                    //
                    // public int getSenderId() {
                    // return senderId;
                    // }
                    //
                    // public byte[] getPayload() {
                    // return null;
                    // }
                    //
                    // public int getManufacturerId() {
                    // return 0x07ff;
                    // }
                    //
                    // public int getFunctionId() {
                    // // return 0x0007;
                    // return -1;
                    // }
                    //
                    // public String getName() {
                    // // TODO Auto-generated method stub
                    // return null;
                    // }
                    // };
                    //
                    // EnOceanHandler handler = new EnOceanHandler() {
                    // public void notifyResponse(EnOceanRPC enOceanRPC,
                    // byte[] payload) {
                    // logger.debug("enOceanRPC: " + enOceanRPC
                    // + ", payload: " + payload);
                    // }
                    // };
                    //
                    // logger.debug("BEFORE invoking...");
                    // eod.invoke(rpc, handler);
                    // logger.debug("AFTER invoking...");

                    i = i + 1;

                    // // Let's create an enoceanrpc in order to turn on the plug.
                    // EnOceanRPC turnOnRpc = new EnOceanRPC() {
                    // private int senderId = 0x0180abb8;
                    //
                    // public void setSenderId(int chipId) {
                    // this.senderId = chipId;
                    // }
                    //
                    // public int getSenderId() {
                    // return senderId;
                    // }
                    //
                    // public byte[] getPayload() {
                    // return null;
                    // }
                    //
                    // public int getManufacturerId() {
                    // return -1;
                    // }
                    //
                    // public int getFunctionId() {
                    // return -1;
                    // }
                    //
                    // public String getName() {
                    // return "HARDCODED_TURN_ON";
                    // }
                    // };
                    //
                    // EnOceanHandler handlerTurnOnRpc = new EnOceanHandler() {
                    // public void notifyResponse(EnOceanRPC enOceanRPC,
                    // byte[] payload) {
                    // logger.debug("enOceanRPC: " + enOceanRPC
                    // + ", payload: " + payload);
                    // }
                    // };
                    //
                    // logger.debug("BEFORE invoking...");
                    // eod.invoke(turnOnRpc, handlerTurnOnRpc);
                    // logger.debug("AFTER invoking...");

                }
            }
        } catch (InvalidSyntaxException e) {
            logger.error("Could not get devives from service reference", e);
        }

        // eventAdmin
        ServiceReference<EventAdmin> eventAdminRef = bundleContext.getServiceReference(EventAdmin.class);
        if (eventAdminRef != null) {
            eventAdmin = bundleContext.getService(eventAdminRef);
        } else {
            logger.debug("No event admin service.");
        }

        logger.debug("OUT: org.eclipse.smarthome.protocols.enocean.sample.client.Activator.start(bc: " + bundleContext
                + ")");
    }

    @Override
    public Object addingService(ServiceReference<EnOceanDevice> reference) {
        logger.debug("> addingService(reference: " + reference + ")");
        Object service = bundleContext.getService(reference);
        if (service != null) {
            if (service instanceof EnOceanDevice) {
                EnOceanDevice device = (EnOceanDevice) service;
                logger.debug("> Registered a new EnOceanDevice : " + Utils.printUid(device.getChipId()) + ", device: "
                        + device);
                return service;
            }
        }
        return null;
    }

    @Override
    public void modifiedService(ServiceReference<EnOceanDevice> reference, Object service) {
        logger.debug("> modifiedService(reference: " + reference + ", service: " + service + ")");
        if (service != null) {
            if (service instanceof EnOceanDevice) {
                EnOceanDevice device = (EnOceanDevice) service;
                logger.debug("> modifiedService method. device.getChipId(): " + Utils.printUid(device.getChipId())
                        + ", device: " + device);
            }
        }
    }

    @Override
    public void removedService(ServiceReference<EnOceanDevice> reference, Object service) {
        logger.debug("> removedService(reference: " + reference + ", service: " + service + ")");
        if (service != null) {
            if (service instanceof EnOceanDevice) {
                EnOceanDevice device = (EnOceanDevice) service;
                logger.debug("> removedService method. device.getChipId(): " + Utils.printUid(device.getChipId())
                        + ", device: " + device);
            }
        }
    }
}
