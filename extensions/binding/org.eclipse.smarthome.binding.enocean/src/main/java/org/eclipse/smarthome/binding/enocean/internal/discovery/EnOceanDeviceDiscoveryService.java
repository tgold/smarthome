package org.eclipse.smarthome.binding.enocean.internal.discovery;

import java.util.Date;

import org.apache.commons.lang.StringUtils;
import org.eclipse.smarthome.binding.enocean.EnOceanBindingConstants;
import org.eclipse.smarthome.binding.enocean.internal.EnOceanHandlerFactory;
import org.eclipse.smarthome.binding.enocean.internal.Utils;
import org.eclipse.smarthome.config.discovery.AbstractDiscoveryService;
import org.eclipse.smarthome.config.discovery.DiscoveryResult;
import org.eclipse.smarthome.config.discovery.DiscoveryResultBuilder;
import org.eclipse.smarthome.core.thing.ThingUID;
import org.osgi.framework.BundleContext;
import org.osgi.framework.FrameworkUtil;
import org.osgi.framework.InvalidSyntaxException;
import org.osgi.framework.ServiceReference;
import org.osgi.service.enocean.EnOceanDevice;
import org.osgi.service.enocean.descriptions.EnOceanMessageDescription;
import org.osgi.service.enocean.descriptions.EnOceanMessageDescriptionSet;
import org.osgi.util.tracker.ServiceTracker;
import org.osgi.util.tracker.ServiceTrackerCustomizer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class EnOceanDeviceDiscoveryService extends AbstractDiscoveryService implements ServiceTrackerCustomizer {

    private Logger logger = LoggerFactory.getLogger(EnOceanDeviceDiscoveryService.class);

    private ServiceTracker deviceTracker;

    private BundleContext bc;

    public EnOceanDeviceDiscoveryService() throws IllegalArgumentException {
        super(EnOceanHandlerFactory.SUPPORTED_THING_TYPES_UIDS, 15);
    }

    @Override
    protected void startBackgroundDiscovery() {
        bc = FrameworkUtil.getBundle(EnOceanDeviceDiscoveryService.class).getBundleContext();
        /* Track device creation */
        try {
            deviceTracker = new ServiceTracker(bc,
                    bc.createFilter("(&(objectclass=" + EnOceanDevice.class.getName() + "))"), this);
            deviceTracker.open();
        } catch (InvalidSyntaxException e) {
            logger.error("Could not instantiate device tracker for EnOceanDevice class", e);
        }
    }

    @Override
    public void stopBackgroundDiscovery() {
        removeOlderResults(new Date().getTime());
        deviceTracker.close();
    }

    @Override
    protected void startScan() {
        try {
            // Display the EnOceanDevice services.
            ServiceReference[] srs;
            srs = bc.getAllServiceReferences(EnOceanDevice.class.getName(), null);

            logger.debug("srs: " + srs);
            if (srs == null) {
                logger.debug("There is NO service registered with the following class name: "
                        + EnOceanDevice.class.getName());
            } else {
                logger.debug("srs.length: " + srs.length);

                int i = 0;
                while (i < srs.length) {
                    ServiceReference sr = srs[i];
                    logger.debug("sr: " + sr);

                    String[] pks = sr.getPropertyKeys();
                    int j = 0;
                    while (j < pks.length) {
                        logger.debug("pks[" + j + "]: " + pks[j] + ", event.getProperty(" + pks[j] + "): "
                                + sr.getProperty(pks[j]));
                        j = j + 1;
                    }

                    EnOceanDevice eod = (EnOceanDevice) bc.getService(sr);
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

                    i = i + 1;
                    DiscoveryResult discoveryResult = createDiscoveryResult(eod);
                    thingDiscovered(discoveryResult);
                }
            }
        } catch (InvalidSyntaxException e) {
            logger.error("Cannot get service reference for EnOceanDevice.class.getName().", e);
        }
    }

    @Override
    public Object addingService(ServiceReference ref) {
        Object service = this.bc.getService(ref);
        if (service == null) {
            return null;
        } else {
            if (service instanceof EnOceanDevice) {
                EnOceanDevice eod = (EnOceanDevice) bc.getService(ref);
                DiscoveryResult discoveryResult = createDiscoveryResult(eod);
                thingDiscovered(discoveryResult);
            }
            return service;
        }
    }

    @Override
    public void modifiedService(ServiceReference arg0, Object service) {

    }

    @Override
    public void removedService(ServiceReference arg0, Object service) {

    }

    private DiscoveryResult createDiscoveryResult(EnOceanDevice eod) {
        ThingUID thingUID = getUID(eod);

        String label = getEnOceanMessageDescription(eod.getRorg(), eod.getFunc(), eod.getType());

        if (StringUtils.isBlank(label)) {
            String deviceType = getDeviceTypeAsString(eod);
            label = "EnOcean " + deviceType;
        }

        return DiscoveryResultBuilder.create(thingUID).withLabel(label).build();
    }

    private String getDeviceTypeAsString(EnOceanDevice eod) {
        String rorg = Utils.bytesToHexString(Utils.intTo1Byte(eod.getRorg()));
        String func = Utils.bytesToHexString(Utils.intTo1Byte(eod.getFunc()));
        String type = Utils.bytesToHexString(Utils.intTo1Byte(eod.getType()));

        if ("f6".equalsIgnoreCase(rorg)) {
            if ("ff".equalsIgnoreCase(type)) {
                // assume F6-02-01 as default
                func = "02";
                type = "01";
            }
        }
        String deviceType = rorg + "-" + func + "-" + type;
        return deviceType.toUpperCase();
    }

    private String getChipIdAsString(EnOceanDevice eod) {
        String chipId = Utils.bytesToHexString(Utils.intTo4Bytes(eod.getChipId()));
        return chipId.toUpperCase();
    }

    private ThingUID getUID(EnOceanDevice eod) {
        ThingUID thingUID = new ThingUID(EnOceanBindingConstants.BINDING_ID, getDeviceTypeAsString(eod),
                getChipIdAsString(eod));
        return thingUID;
    }

    private String getEnOceanMessageDescription(int rorg, int func, int type) {
        try {
            ServiceReference[] srs = bc.getAllServiceReferences(EnOceanMessageDescriptionSet.class.getName(), null);
            logger.debug("srs: " + srs);
            if (srs == null) {
                logger.debug("There is NO service registered with the following class name: "
                        + EnOceanMessageDescriptionSet.class.getName());
            } else {
                logger.debug("srs.length: " + srs.length);

                int i = 0;
                while (i < srs.length) {
                    ServiceReference sRef = srs[i];
                    logger.debug("sRef: " + sRef);
                    EnOceanMessageDescriptionSet eomds = (EnOceanMessageDescriptionSet) bc.getService(sRef);
                    logger.debug("eomds: " + eomds);
                    EnOceanMessageDescription eomd = eomds.getMessageDescription(rorg, func, type, -1);
                    logger.debug("eomd: " + eomd);
                    if (eomd != null) {
                        return eomd.getMessageDescription();
                    }
                    i = i + 1;
                }
            }
        } catch (InvalidSyntaxException e) {
            e.printStackTrace();
        }
        return null;
    }

}
