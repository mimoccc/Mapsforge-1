package org.mapsforge.core;

import org.mapsforge.core.content.Context;

public class ComponentName {

	private final String mPackage;
	private final String mClass;
	/**
     * Create a new component identifier from a Context and Class object.
     *
     * @param pkg A Context for the package implementing the component, from
     * which the actual package name will be retrieved.
     * @param cls The Class object of the desired component, from which the
     * actual class name will be retrieved.
     */
    public ComponentName(Context pkg, Class<?> cls) {
        mPackage = pkg.getPackageName();
        mClass = cls.getName();
    }
    
    /**
     * Return the package name of this component.
     */
    public String getPackageName() {
        return mPackage;
    }

    /**
     * Return the class name of this component.
     */
    public String getClassName() {
        return mClass;
    }

}
