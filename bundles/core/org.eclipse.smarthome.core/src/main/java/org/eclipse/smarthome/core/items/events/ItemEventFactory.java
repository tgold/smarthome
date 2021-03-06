/**
 * Copyright (c) 2014-2015 openHAB UG (haftungsbeschraenkt) and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.eclipse.smarthome.core.items.events;

import java.lang.reflect.Method;
import java.util.LinkedList;
import java.util.List;

import org.eclipse.smarthome.core.events.AbstractEventFactory;
import org.eclipse.smarthome.core.events.Event;
import org.eclipse.smarthome.core.items.Item;
import org.eclipse.smarthome.core.items.dto.ItemDTO;
import org.eclipse.smarthome.core.items.dto.ItemDTOMapper;
import org.eclipse.smarthome.core.types.Command;
import org.eclipse.smarthome.core.types.RefreshType;
import org.eclipse.smarthome.core.types.State;
import org.eclipse.smarthome.core.types.Type;
import org.eclipse.smarthome.core.types.UnDefType;

import com.google.common.base.Preconditions;
import com.google.common.collect.Sets;

/**
 * An {@link ItemEventFactory} is responsible for creating item event instances, e.g. {@link ItemCommandEvent}s and
 * {@link ItemStateEvent}s.
 *
 * @author Stefan Bußweiler - Initial contribution
 */
public class ItemEventFactory extends AbstractEventFactory {

    private static final String CORE_LIBRARY_PACKAGE = "org.eclipse.smarthome.core.library.types.";

    private static final String ITEM_COMAND_EVENT_TOPIC = "smarthome/items/{itemName}/command";

    private static final String ITEM_STATE_EVENT_TOPIC = "smarthome/items/{itemName}/state";

    private static final String ITEM_ADDED_EVENT_TOPIC = "smarthome/items/{itemName}/added";

    private static final String ITEM_REMOVED_EVENT_TOPIC = "smarthome/items/{itemName}/removed";

    private static final String ITEM_UPDATED_EVENT_TOPIC = "smarthome/items/{itemName}/updated";

    /**
     * Constructs a new ItemEventFactory.
     */
    public ItemEventFactory() {
        super(Sets.newHashSet(ItemCommandEvent.TYPE, ItemStateEvent.TYPE, ItemAddedEvent.TYPE, ItemUpdatedEvent.TYPE,
                ItemRemovedEvent.TYPE));
    }

    @Override
    protected Event createEventByType(String eventType, String topic, String payload, String source) throws Exception {
        Event event = null;
        if (eventType.equals(ItemCommandEvent.TYPE)) {
            event = createCommandEvent(topic, payload, source);
        } else if (eventType.equals(ItemStateEvent.TYPE)) {
            event = createStateEvent(topic, payload, source);
        } else if (eventType.equals(ItemAddedEvent.TYPE)) {
            event = createAddedEvent(topic, payload);
        } else if (eventType.equals(ItemUpdatedEvent.TYPE)) {
            event = createUpdatedEvent(topic, payload);
        } else if (eventType.equals(ItemRemovedEvent.TYPE)) {
            event = createRemovedEvent(topic, payload);
        }
        return event;
    }

    private Event createCommandEvent(String topic, String payload, String source) {
        String itemName = getItemName(topic);
        ItemEventPayloadBean bean = deserializePayload(payload, ItemEventPayloadBean.class);
        Command command = null;
        try {
            command = (Command) parse(bean.getType(), bean.getValue());
        } catch (Exception e) {
            throw new IllegalArgumentException("Parsing of item command event failed.", e);
        }
        return new ItemCommandEvent(topic, payload, itemName, command, source);
    }

    private Event createStateEvent(String topic, String payload, String source) {
        String itemName = getItemName(topic);
        ItemEventPayloadBean bean = deserializePayload(payload, ItemEventPayloadBean.class);
        State state = null;
        try {
            state = (State) parse(bean.getType(), bean.getValue());
        } catch (Exception e) {
            throw new IllegalArgumentException("Parsing of item state event failed.", e);
        }
        return new ItemStateEvent(topic, payload, itemName, state, source);
    }

    private String getItemName(String topic) {
        String[] topicElements = getTopicElements(topic);
        if (topicElements.length != 4)
            throw new IllegalArgumentException("Event creation failed, invalid topic: " + topic);
        return topicElements[2];
    }

    private Object parse(String typeName, String valueToParse) throws Exception {
        if (typeName.equals(UnDefType.class.getSimpleName())) {
            return UnDefType.valueOf(valueToParse);
        }
        if (typeName.equals(RefreshType.class.getSimpleName())) {
            return RefreshType.valueOf(valueToParse);
        }
        Class<?> stateClass = Class.forName(CORE_LIBRARY_PACKAGE + typeName);
        Method valueOfMethod = stateClass.getMethod("valueOf", String.class);
        return valueOfMethod.invoke(stateClass, valueToParse);
    }

    private Event createAddedEvent(String topic, String payload) {
        ItemDTO itemDTO = deserializePayload(payload, ItemDTO.class);
        return new ItemAddedEvent(topic, payload, itemDTO);
    }

    private Event createRemovedEvent(String topic, String payload) {
        ItemDTO itemDTO = deserializePayload(payload, ItemDTO.class);
        return new ItemRemovedEvent(topic, payload, itemDTO);
    }

    private Event createUpdatedEvent(String topic, String payload) {
        ItemDTO[] itemDTOs = deserializePayload(payload, ItemDTO[].class);
        if (itemDTOs.length != 2) {
            throw new IllegalArgumentException("ItemUpdateEvent creation failed, invalid payload: " + payload);
        }
        return new ItemUpdatedEvent(topic, payload, itemDTOs[0], itemDTOs[1]);
    }

    /**
     * Creates an item command event.
     *
     * @param itemName the name of the item to send the command for
     * @param command the command to send
     * @param source the name of the source identifying the sender (can be null)
     *
     * @return the created item command event
     *
     * @throws IllegalArgumentException if itemName or command is null
     */
    public static ItemCommandEvent createCommandEvent(String itemName, Command command, String source) {
        assertValidArguments(itemName, command, "command");
        String topic = buildTopic(ITEM_COMAND_EVENT_TOPIC, itemName);
        ItemEventPayloadBean bean = new ItemEventPayloadBean(command.getClass().getSimpleName(), command.toString());
        String payload = serializePayload(bean);
        return new ItemCommandEvent(topic, payload, itemName, command, source);
    }

    /**
     * Creates an item command event.
     *
     * @param itemName the name of the item to send the command for
     * @param command the command to send
     *
     * @return the created item command event
     *
     * @throws IllegalArgumentException if itemName or command is null
     */
    public static ItemCommandEvent createCommandEvent(String itemName, Command command) {
        return createCommandEvent(itemName, command, null);
    }

    /**
     * Creates an item state event.
     *
     * @param itemName the name of the item to send the state update for
     * @param state the new state to send
     * @param source the name of the source identifying the sender (can be null)
     *
     * @return the created item state event
     *
     * @throws IllegalArgumentException if itemName or state is null
     */
    public static ItemStateEvent createStateEvent(String itemName, State state, String source) {
        assertValidArguments(itemName, state, "state");
        String topic = buildTopic(ITEM_STATE_EVENT_TOPIC, itemName);
        ItemEventPayloadBean bean = new ItemEventPayloadBean(state.getClass().getSimpleName(), state.toString());
        String payload = serializePayload(bean);
        return new ItemStateEvent(topic, payload, itemName, state, source);
    }

    /**
     * Creates an item state event.
     *
     * @param itemName the name of the item to send the state update for
     * @param state the new state to send
     *
     * @return the created item state event
     *
     * @throws IllegalArgumentException if itemName or state is null
     */
    public static ItemStateEvent createStateEvent(String itemName, State state) {
        return createStateEvent(itemName, state, null);
    }

    /**
     * Creates an item added event.
     *
     * @param item the item
     *
     * @return the created item added event
     *
     * @throws IllegalArgumentException if item is null
     */
    public static ItemAddedEvent createAddedEvent(Item item) {
        assertValidArgument(item, "item");
        String topic = buildTopic(ITEM_ADDED_EVENT_TOPIC, item.getName());
        ItemDTO itemDTO = map(item);
        String payload = serializePayload(itemDTO);
        return new ItemAddedEvent(topic, payload, itemDTO);
    }

    /**
     * Creates an item removed event.
     *
     * @param item the item
     *
     * @return the created item removed event
     *
     * @throws IllegalArgumentException if item is null
     */
    public static ItemRemovedEvent createRemovedEvent(Item item) {
        assertValidArgument(item, "item");
        String topic = buildTopic(ITEM_REMOVED_EVENT_TOPIC, item.getName());
        ItemDTO itemDTO = map(item);
        String payload = serializePayload(itemDTO);
        return new ItemRemovedEvent(topic, payload, itemDTO);
    }

    /**
     * Creates an item updated event.
     *
     * @param item the item
     * @param oldItem the old item
     *
     * @return the created item updated event
     *
     * @throws IllegalArgumentException if item or oldItem is null
     */
    public static ItemUpdatedEvent createUpdateEvent(Item item, Item oldItem) {
        assertValidArgument(item, "item");
        assertValidArgument(oldItem, "oldItem");
        String topic = buildTopic(ITEM_UPDATED_EVENT_TOPIC, item.getName());
        ItemDTO itemDTO = map(item);
        ItemDTO oldItemDTO = map(oldItem);
        List<ItemDTO> itemDTOs = new LinkedList<ItemDTO>();
        itemDTOs.add(itemDTO);
        itemDTOs.add(oldItemDTO);
        String payload = serializePayload(itemDTOs);
        return new ItemUpdatedEvent(topic, payload, itemDTO, oldItemDTO);
    }

    private static String buildTopic(String topic, String itemName) {
        return topic.replace("{itemName}", itemName);
    }

    private static ItemDTO map(Item item) {
        return ItemDTOMapper.map(item, false);
    }

    private static void assertValidArguments(String itemName, Type type, String typeArgumentName) {
        Preconditions.checkArgument(itemName != null && !itemName.isEmpty(),
                "The argument 'itemName' must not be null or empty.");
        Preconditions.checkArgument(type != null, "The argument '" + typeArgumentName + "' must not be null or empty.");
    }

    private static void assertValidArgument(Item item, String argumentName) {
        Preconditions.checkArgument(item != null, "The argument '" + argumentName + "' must no be null.");
    }

    /**
     * This is a java bean that is used to serialize/deserialize item event payload.
     */
    private static class ItemEventPayloadBean {
        private String type;
        private String value;

        public ItemEventPayloadBean(String type, String value) {
            this.type = type;
            this.value = value;
        }

        public String getType() {
            return type;
        }

        public String getValue() {
            return value;
        }
    }

}
