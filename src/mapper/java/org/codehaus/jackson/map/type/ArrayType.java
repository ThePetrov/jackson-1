package org.codehaus.jackson.map.type;

import java.lang.reflect.Array;
import java.util.*;

import org.codehaus.jackson.type.JavaType;

/**
 * Array types represent Java arrays, both primitive and object valued.
 * Further, Object-valued arrays can have element type of any other
 * legal {@link JavaType}.
 */
public final class ArrayType
    extends JavaType
{
    /**
     * Type of elements in the array.
     */
    final JavaType _componentType;

    final boolean _fullyTyped;

    /**
     * We will also keep track of shareable instance of empty array,
     * since it usually needs to be constructed any way; and because
     * it is essentially immutable and thus can be shared.
     */
    final Object _emptyArray;

    private ArrayType(JavaType componentType, Object emptyInstance)
    {
        super(emptyInstance.getClass());
        _componentType = componentType;
        _emptyArray = emptyInstance;
        _hashCode += componentType.hashCode();
        _fullyTyped = componentType.isFullyTyped();
    }

    public static ArrayType construct(JavaType componentType)
    {
        /* This is bit messy: there is apparently no other way to
         * reconstruct actual concrete/raw array class from component
         * type, than to construct an instance, get class (same is
         * true for GenericArracyType as well; hence we won't bother
         * passing that in).
         */
        Object emptyInstance = Array.newInstance(componentType.getRawClass(), 0);
        return new ArrayType(componentType, emptyInstance);
    }                                   

    /*
    //////////////////////////////////////////////////////////
    // Methods for narrowing conversions
    //////////////////////////////////////////////////////////
     */

    /**
     * Handling of narrowing conversions for arrays is trickier: for now,
     * it is not even allowed.
     */
    protected JavaType _narrow(Class<?> subclass)
    {
        /* Ok: need a bit of indirection here. First, must replace component
         * type (and check that it is compatible), then re-construct.
         */
        if (!subclass.isArray()) { // sanity check, should never occur
            throw new IllegalArgumentException("Incompatible narrowing operation: trying to narrow "+toString()+" to class "+subclass.getName());
        }
        /* Hmmh. This is an awkward back reference... but seems like the
         * only simple way to do it.
         */
        Class<?> newCompClass = subclass.getComponentType();
        JavaType newCompType = TypeFactory.fromClass(newCompClass);
        return construct(newCompType);
    }

    /**
     * For array types, both main type and content type can be modified;
     * but ultimately they are interchangeable.
     */
    public JavaType narrowContentsBy(Class<?> contentClass)
    {
        // Can do a quick check first:
        if (contentClass == _componentType.getRawClass()) {
            return this;
        }
        JavaType newComponentType = _componentType.narrowBy(contentClass);
        return construct(newComponentType);
    }

    /*
    //////////////////////////////////////////////////////////
    // Public API
    //////////////////////////////////////////////////////////
     */

    /**
     * Simple types are always fully typed: this requires that
     * no instances are ever created for Collection/Map types.
     */
    public boolean isFullyTyped() { return _fullyTyped; }

    public boolean isContainerType() { return true; }

    @Override
    public JavaType getContentType() { return  _componentType; }

    /*
    //////////////////////////////////////////////////////////
    // Standard methods
    //////////////////////////////////////////////////////////
     */

    @Override
        public String toString()
    {
        return "[array type, component type: "+_componentType+"]";
    }

    public boolean equals(Object o)
    {
        if (o == this) return true;
        if (o == null) return false;
        if (o.getClass() != getClass()) return false;

        ArrayType other = (ArrayType) o;
        return _componentType.equals(other._componentType);
    }
}