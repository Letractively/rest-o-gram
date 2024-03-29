/*
 * This software is released under the GNU Lesser General Public License v3.
 * For more information see http://www.gnu.org/licenses/lgpl.html
 *
 * Copyright (c) 2011, Peter Knego & Matjaz Tercelj
 * All rights reserved.
 */

package com.leanengine;

import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

/**
 * LeanEntity is a basic data unit that can be stored on the server. It can be saved, retrieved, deleted and queried.
 * Entities can contain properties of various types: long, double, boolean, String and Date.
 * <br/><br/>
 * Future enhancements: new properties will be Entity reference, Blob, GeoPoint, Image..
 * <br/><br/>
 * Basic usage is to create a named entity via {@link #initPrivateEntity(String, long)} Entity(String, String)} method and then use  {@link #save()} to store it
 * to server. Other methods are {@link #get(String)}, {@link #getAll(String)} and {@link #delete()}.
 * <br/><br/>
 * All method have their asynchronous counterparts, designated by added 'InBackground' to method name, which allow
 * performing the method in the background thread.
 */
public class LeanEntity {
    protected final String kind;
    protected Long id = Long.MIN_VALUE;
    protected String uniqueName;
    protected Long accountID;
    protected Map<String, Object> properties = new HashMap<>();

    LeanEntity(String kind) {
        this.kind = kind;
    }

    LeanEntity(String kind, long id) {
        this.kind = kind;
        this.id = id;
    }

    LeanEntity(String kind, long id, long accountID) {
        this(kind, id);
        this.accountID = accountID;
    }

    LeanEntity(String kind, String uniqueName) {
        this.kind = kind;
        this.uniqueName = uniqueName;
    }

    /**
     * Static method to create an entity of given kind.
     * Entities created via {@code init()} method do not have their {@code id} and {@code accountID} fields set,
     * even when they are saved to server.
     *
     * @param kind The kind of the entity.
     * @return
     */
//    public static LeanEntity initPublicEntity(String kind, String uniqueName) {
//        return new LeanEntity(kind, uniqueName);
//    }

    public static LeanEntity initPrivateEntity(String kind) {
        return new LeanEntity(kind);
    }

    public static LeanEntity initPrivateEntity(String kind, long id) {
        return new LeanEntity(kind, id);
    }

    /**
     * Saves this entity to the server. Saving is performed on the background thread.
     * <br/><br/>
     * This method does not set the  {@code ID} and {@code accountID} fields of this entity.
     *
     * @param callback NetworkCallback that on success returns ID of the saved Entity.
     */
    public void saveInBackground(NetworkCallback<Long> callback) {
        RestService.getInstance().putPrivateEntityAsync(this, callback);
    }

    /**
     * Saves this entity to the server. This method call blocks until result is available.
     * <br/><br/>
     * This method does not set the  {@code ID} and {@code accountID} fields of this entity.
     *
     * @return ID of the saved Entity.
     * @throws LeanException In case of authentication, network and data parsing errors.
     */
    public long save() throws LeanException {
        return RestService.getInstance().putPrivateEntity(this);
    }

    /**
     * Retrieves from server an entity of given kind and ID.
     * Only returns entity if it belongs to the current user account.
     * This is a blocking operation - it block the execution of current thread until result is returned.
     *
     * @param kind The kind of the Entity to be retrieved.
     * @param id   ID of the Entity
     * @return Returns LeanEntity saved under the given {@code id} and of given {@code kind}.
     * @throws IllegalArgumentException If parameter {@code id} or {@code kind} is null.
     * @throws LeanException            If entity could not be found or in case of authentication, network and data
     * parsing errors.
     */
    public static LeanEntity get(String kind, long id) throws LeanException, IllegalArgumentException {
        return RestService.getInstance().getPrivateEntity(kind, id);
    }

    /**
     * Retrieves from server an entities of certain kind and ID.
     * Only returns entity if it belongs to the current user account.
     * Operation is performed in the background thread.
     *
     * @param kind     The kind of the Entity to be retrieved.
     * @param id       ID of the Entity
     * @param callback Callback to be invoked in case of result or error.
     */
    public static void getInBackground(String kind, long id, NetworkCallback<LeanEntity> callback) {
        RestService.getInstance().getPrivateEntityAsync(kind, id, callback);
    }

    /**
     * Retrieves from server all entities of certain kind.
     * Returns only entities belonging to current user account.
     * Operation is performed in the background thread.
     *
     * @param kind     The kind of the Entities to be retrieved.
     * @param callback Callback to be invoked in case of result or error.
     */
    public static void getAllInBackground(String kind, NetworkCallback<LeanEntity> callback) {
        RestService.getInstance().getPrivateEntitiesAsync(kind, callback);
    }

    /**
     * Retrieves from server all entities of certain kind.
     * Returns only entities belonging to current user account (private).
     * This is a blocking operation - it block the execution of current thread until result is returned.
     *
     * @param kind The kind of the entities to be retrieved.
     * @return An array of LeanEntity
     * @throws LeanException In case of authentication, network and data parsing errors.
     */
    public static LeanEntity[] getAll(String kind) throws LeanException {
        return RestService.getInstance().getPrivateEntities(kind);
    }

    /**
     * Deletes an entity of the given kind an ID fro the server datastore..
     *
     * @param kind     The kind of the entity to delete.
     * @param entityId The ID of the entity to delete.
     * @throws LeanException In case of authentication, network and data parsing errors.
     */
    public static void delete(String kind, long entityId) throws LeanException {
        RestService.getInstance().deletePrivateEntity(kind, entityId);
    }

    public void delete() throws LeanException {
        RestService.getInstance().deletePrivateEntity(this.kind, this.id);
    }

    /**
     * Returns a {@link Set} of properties' keys .
     *
     * @return {@link Set} of properties' keys.
     */
    public Set<String> getKeySet() {
        return properties.keySet();
    }

    /**
     * @return Returns the kind of the entity.
     */
    public String getKind() {
        return kind;
    }

    /**
     * @return Returns the ID of the entity.
     */
    public Long getId() {
        return id;
    }

    public boolean hasId() {
        return id != Long.MIN_VALUE;
    }

    public String getUniqueName() {
        return uniqueName;
    }

    /**
     * Returns the user account ID that this entity belongs to.
     * Account ID is only available for entities retrieved from server.
     *
     * @return ID of the account this entity belongs to. {@code Null} if entity was not retrieved from server.
     */
    public Long getAccountID() {
        return accountID;
    }

    /**
     * Gets the property with the specified key.
     * <br/><br/>
     * The value returned may not be the same type as originally set via {@code putXYZ(String, Object)}.
     * Object types are internally converted to types supported by LeanEngine server: //todo insert LE Docs link here
     *
     * @param key Key (name) of the property.
     * @return Value of the property or {@code null} if property with given key does not exist.
     */
    public Object get(String key) {
        return properties.get(key);
    }

    /**
     * Gets the property with the specified key.
     * {@code Null} is returned if key does not exist or if property is not of type {@link String}.
     * <br/><br/>
     * The value returned may not be the same type as originally set via {@link #put(String, String)}.
     * Object types are internally converted to types supported by LeanEngine server: //todo insert LE Docs link here
     *
     * @param key Key under which property was stored.
     * @return Value of property or {@code null} if key does not exist or if property is not of type {@link String}.
     */
    public String getString(String key) {
        Object val = properties.get(key);
        return (val != null && val.getClass() == String.class) ? (String) val : null;
    }

    /**
     * Gets the property with the specified key.
     * {@code Null} is returned if key does not exist or if property is not of type {@link String}.
     * <br/><br/>
     * This method is to be used to retrieve properties that were saved via {@link #putText(String, String)}.
     *
     * @param key Key under which property was stored.
     * @return Value of property or {@code null} if key does not exist or if property is not of type {@link String}.
     */
    public String getText(String key) {
        Object val = properties.get(key);
        return (val != null && val.getClass() == LeanText.class) ? ((LeanText) val).getValue() : null;
    }

    /**
     * Gets the property with the specified key.
     * {@code Null} is returned if key does not exist or if property is not of type {@link Long}.
     * <br/><br/>
     * The value returned may not be the same type as originally set via {@link #put(String, long)}.
     * Object types are internally converted to types supported by LeanEngine server: //todo insert LE Docs link here
     *
     * @param key Key under which property was stored.
     * @return Value of property or {@code null} if key does not exist or if property is not of type {@link Long}.
     */
    public Long getLong(String key) {
        Object val = properties.get(key);
        return (val != null && val.getClass() == Long.class) ? (Long) val : null;
    }

    /**
     * Gets the property with the specified key.
     * {@code Null} is returned if key does not exist or if property is not of type {@link Double}.
     * <br/><br/>
     * The value returned may not be the same type as originally set via {@link #put(String, double)}.
     * Object types are internally converted to types supported by LeanEngine server: //todo insert LE Docs link here
     *
     * @param key Key under which property was stored.
     * @return Value of property or {@code null} if key does not exist or if property is not of type {@link Double}.
     */
    public Double getDouble(String key) {
        Object val = properties.get(key);
        return (val != null && val.getClass() == Double.class) ? (Double) val : null;
    }

    /**
     * Gets the property with the specified key.
     * {@code Null} is returned if key does not exist or if property is not of type {@link java.util.Date}.
     * <br/><br/>
     * The value returned may not be the same type as originally set via {@link #put(String, java.util.Date)}.
     * Object types are internally converted to types supported by LeanEngine server: //todo insert LE Docs link here
     *
     * @param key Key under which property was stored.
     * @return Value of property or {@code null} if key does not exist or if property is not of type {@link java.util.Date}.
     */
    public Date getDate(String key) {
        Object val = properties.get(key);
        return (val != null && val.getClass() == Date.class) ? (Date) val : null;
    }

    /**
     * Gets the property with the specified key.
     * {@code Null} is returned if key does not exist or if property is not of type {@link Boolean}.
     * <br/><br/>
     * The value returned may not be the same type as originally set via {@link #put(String, boolean)}.
     * Vales are internally converted to types supported by LeanEngine server: //todo insert LE Docs link here
     *
     * @param key Key under which property was stored.
     * @return Value of property or {@code null} if key does not exist or if property is not of type {@link Boolean}.
     */
    public Boolean getBoolean(String key) {
        Object val = properties.get(key);
        return (val != null && val.getClass() == Boolean.class) ? (Boolean) val : null;
    }

    /**
     * Sets the property with given {@code key} to {@code value}.
     * <br/><br/>
     * Vales are internally converted to types supported by LeanEngine server: //todo insert LE Docs link here
     *
     * @param key   Key under which property will be stored.
     * @param value {@link long} value to be stored.
     */
    public void put(String key, long value) {
        properties.put(key, value);
    }

    /**
     * Sets the property with given {@code key} to {@code value}.
     * <br/><br/>
     * Vales are internally converted to types supported by LeanEngine server: //todo insert LE Docs link here
     *
     * @param key   Key under which property will be stored.
     * @param value {@link double} value to be stored.
     */
    public void put(String key, double value) {
        properties.put(key, value);
    }

    /**
     * Sets the property with given {@code key} to {@code value}.
     * <br/><br/>
     * Value is stored on server as short string value, which can be indexed (via server configuration) and can be
     * used in queries.
     * <br/><br/>
     * {@code Value} must be shorter that 500 Unicode characters. Use {@link #putText(String, String)} if longer
     * string values need to be stored.
     *
     * @param key   Key under which property will be stored.
     * @param value {@link String} value to be stored.
     * @throws IllegalArgumentException If {@code value} is longer that 500 Unicode characters.
     */
    public void put(String key, String value) throws IllegalArgumentException {
        if (value.length() >= 500) {
            throw new IllegalArgumentException("Value to be stored is too long. Max 500 chars.");
        }
        properties.put(key, value);
    }

    /**
     * Sets the property with given {@code key} to {@code value}.
     * <br/><br/>
     * This method supports saving String values that are longer that 500 Unicode characters.
     * Property will be saved on the server as an unindexed text value, meaning that it can not be used in queries.
     *
     * @param key   Key under which property will be stored.
     * @param value {@link String} value to be stored.
     */
    public void putText(String key, String value) {
        properties.put(key, new LeanText(value));
    }

    /**
     * Sets the property with given {@code key} to {@code value}.
     * <br/><br/>
     * Vales are internally converted to types supported by LeanEngine server: //todo insert LE Docs link here
     *
     * @param key   Key under which property will be stored.
     * @param value {@link Date} value to be stored.
     */
    public void put(String key, Date value) {
        properties.put(key, value);
    }

    /**
     * Sets the property with given {@code key} to {@code value}.
     * <br/><br/>
     * Vales are internally converted to types supported by LeanEngine server: //todo insert LE Docs link here
     *
     * @param key   Key under which property will be stored.
     * @param value {@link boolean} value to be stored.
     */
    public void put(String key, boolean value) {
        properties.put(key, value);
    }

    /**
     * Checks if property with given {@code key} exists.
     *
     * @param key Key of the property.
     * @return {@code True} if property exists, {@code false} otherwise.
     */
    public boolean hasProperty(String key) {
        return properties.containsKey(key);
    }

    /**
     * Checks if entity contains any properties.
     *
     * @return {@code True} if entity is empty, i.e. contains no properties, {@code false} otherwise.
     */
    public boolean isEmpty() {
        return properties.isEmpty();
    }
}
