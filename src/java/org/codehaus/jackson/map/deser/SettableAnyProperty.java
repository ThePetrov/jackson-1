package org.codehaus.jackson.map.deser;

import java.io.IOException;
import java.lang.reflect.*;

import org.codehaus.jackson.*;
import org.codehaus.jackson.map.DeserializationContext;
import org.codehaus.jackson.map.JsonDeserializer;
import org.codehaus.jackson.map.JsonMappingException;
import org.codehaus.jackson.type.JavaType;

/**
 * Class that represents a "wildcard" set method which can be used
 * to generically set values of otherwise unmapped (aka "unknown")
 * properties read from Json content.
 *<p>
 * !!! Note: might make sense to refactor to share some code
 * with {@link SettableBeanProperty}?
 */
public final class SettableAnyProperty
{
    final Method _setter;

    final JavaType _type;

    JsonDeserializer<Object> _valueDeserializer;

    public SettableAnyProperty(JavaType type, Method setter)
    {
        _type = type;
        _setter = setter;
    }

    /*
    /////////////////////////////////////////////////////////
    // Public API
    /////////////////////////////////////////////////////////
     */

    public boolean hasValueDeserializer() { return (_valueDeserializer != null); }

    public void setValueDeserializer(JsonDeserializer<Object> deser)
    {
        if (_valueDeserializer != null) { // sanity check
            throw new IllegalStateException("Already had assigned deserializer for SettableAnyProperty");
        }
        _valueDeserializer = deser;
    }

    public JavaType getType() { return _type; }

    /**
     * Method called to deserialize appropriate value, given parser (and
     * context), and set it using appropriate method (a setter method).
     */
    public final void deserializeAndSet(JsonParser jp, DeserializationContext ctxt,
                                        Object instance, String propName)
        throws IOException, JsonProcessingException
    {
        JsonToken t = jp.nextToken();
        Object value = (t == JsonToken.VALUE_NULL) ? null : _valueDeserializer.deserialize(jp, ctxt);
        try {
            _setter.invoke(instance, propName, value);
        } catch (Exception e) {
            _throwAsIOE(e, propName, value);
        }
    }

    /*
    /////////////////////////////////////////////////////////
    // Helper methods
    /////////////////////////////////////////////////////////
     */

    /**
     * @param propName Name of property (from Json input) to set
     * @param instance Bean to set property on
     * @param value Value of the property
     */
    protected void _throwAsIOE(Exception e, String propName, Object value)
        throws IOException
    {
        if (e instanceof IllegalArgumentException) {
            String actType = (value == null) ? "[NULL]" : value.getClass().getName();
            StringBuilder msg = new StringBuilder("Problem deserializing \"any\" property '").append(propName);
            msg.append("' of class "+getClassName()+" (expected type: ").append(_type);
            msg.append("; actual type: ").append(actType).append(")");
            String origMsg = e.getMessage();
            if (origMsg != null) {
                msg.append(", problem: ").append(origMsg);
            } else {
                msg.append(" (no error message provided)");
            }
            throw new JsonMappingException(msg.toString(), null, e);
        }
        if (e instanceof IOException) {
            throw (IOException) e;
        }
        if (e instanceof RuntimeException) {
            throw (RuntimeException) e;
        }
        // let's wrap the innermost problem
        Throwable t = e;
        while (t.getCause() != null) {
            t = t.getCause();
        }
        throw new JsonMappingException(t.getMessage(), null, t);
    }

    private String getClassName() { return _setter.getDeclaringClass().getName(); }

    @Override public String toString() { return "[any property on class "+getClassName()+"]"; }
}
