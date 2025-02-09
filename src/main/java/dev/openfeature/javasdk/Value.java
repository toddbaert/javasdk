package dev.openfeature.javasdk;

import java.time.ZonedDateTime;
import java.util.List;

import lombok.EqualsAndHashCode;
import lombok.ToString;

/**
 * Values server as a return type for provider objects. Providers may deal in protobufs or JSON in the backend and
 * have no reasonable way to convert that into a type that users care about (e.g. an instance of `T`). This
 * intermediate representation should provide a good medium of exchange.
 */
@ToString
@EqualsAndHashCode
@SuppressWarnings("PMD.BeanMembersShouldSerialize")
public class Value {

    private final Object innerObject;

    public Value(Value value) {
        this.innerObject = value.innerObject; 
    }

    public Value(Boolean value) {
        this.innerObject = value; 
    }

    public Value(String value) {
        this.innerObject = value; 
    }

    public Value(Integer value) {
        this.innerObject = value; 
    }

    public Value(Double value) {
        this.innerObject = value; 
    }

    public Value(Structure value) {
        this.innerObject = value; 
    }

    public Value(List<Value> value) {
        this.innerObject = value; 
    }

    public Value(ZonedDateTime value) {
        this.innerObject = value;
    }

    /** 
     * Check if this Value represents a Boolean.
     * 
     * @return boolean
     */
    public boolean isBoolean() {
        return this.innerObject instanceof Boolean;
    }

    /** 
     * Check if this Value represents a String.
     * 
     * @return boolean
     */
    public boolean isString() {
        return this.innerObject instanceof String;
    }

    /** 
     * Check if this Value represents an Integer.
     * 
     * @return boolean
     */
    public boolean isInteger() {
        return this.innerObject instanceof Integer;
    }

    /** 
     * Check if this Value represents a Double.
     * 
     * @return boolean
     */
    public boolean isDouble() {
        return this.innerObject instanceof Double;
    }

    /** 
     * Check if this Value represents a Structure.
     * 
     * @return boolean
     */
    public boolean isStructure() {
        return this.innerObject instanceof Structure;
    }
    
    /** 
     * Check if this Value represents a List.
     * 
     * @return boolean
     */
    public boolean isList() {
        return this.innerObject instanceof List;
    }

    /** 
     * Check if this Value represents a ZonedDateTime.
     * 
     * @return boolean
     */
    public boolean isZonedDateTime() {
        return this.innerObject instanceof ZonedDateTime;
    }
    
    /** 
     * Retrieve the underlying Boolean value, or null.
     * 
     * @return Boolean
     */
    @edu.umd.cs.findbugs.annotations.SuppressFBWarnings(value = "NP_BOOLEAN_RETURN_NULL",
        justification = "This is not a plain true/false method. It's understood it can return null.")
    public Boolean asBoolean() {
        if (this.isBoolean()) {
            return (Boolean)this.innerObject;
        }
        return null;
    }
    
    /** 
     * Retrieve the underlying String value, or null.
     * 
     * @return String
     */
    public String asString() {
        if (this.isString()) {
            return (String)this.innerObject;
        }
        return null;
    }

    /** 
     * Retrieve the underlying Integer value, or null.
     * 
     * @return Integer
     */
    public Integer asInteger() {
        if (this.isInteger()) {
            return (Integer)this.innerObject;
        }
        return null;
    }
    
    /** 
     * Retrieve the underlying Double value, or null.
     * 
     * @return Double
     */
    public Double asDouble() {
        if (this.isDouble()) {
            return (Double)this.innerObject;
        }
        return null;
    }

    /** 
     * Retrieve the underlying Structure value, or null.
     * 
     * @return Structure
     */
    public Structure asStructure() {
        if (this.isStructure()) {
            return (Structure)this.innerObject;
        }
        return null;
    }
    
    /**
     * Retrieve the underlying List value, or null.
     *
     * @return List
     */
    public List<Value> asList() {
        if (this.isList()) {
            //noinspection rawtypes,unchecked
            return (List) this.innerObject;
        }
        return null;
    }

    /** 
     * Retrieve the underlying ZonedDateTime value, or null.
     * 
     * @return ZonedDateTime
     */
    public ZonedDateTime asZonedDateTime() {
        if (this.isZonedDateTime()) {
            return (ZonedDateTime)this.innerObject;
        }
        return null;
    }
}
