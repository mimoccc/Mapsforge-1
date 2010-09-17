package org.mapsforge.android.map.intervaltree;

/*************************************************************************
 *  Compilation:  javac Interval.java
 *  Execution:    java Internval
 *  
 *  Interval ADT for integer coordinates.
 *
 *************************************************************************/


public class Interval<Type> {
    public Type value;
	public final int low;   
    public final int high;  

    // precondition: left <= right
    public Interval(int low, int high) {
        if (low <= high) {
            this.low  = low;
            this.high = high;
            this.value=null;
        }
        else throw new RuntimeException("Illegal interval");
    }
    
    public Interval(int left, int right,Type value) {
        if (left <= right) {
            this.low  = left;
            this.high = right;
            this.value= value;
        }
        else throw new RuntimeException("Illegal interval");
    }

    // does this interval a intersect b?
    public boolean intersects(Interval b) {
        Interval a = this;        
		 if (((a.low < b.low) || (a.low <
		 b.high))
		 && ((a.high > b.low) || (a.high >
		 b.high)))return true;
		 return false;
    }

    // does this interval a intersect b?
    public boolean contains(int x) {
        return (low <= x) && (x <= high);
    }

        
    public String toString() {
        return "[" + low + ", " + high + "]";
    }
    
    @Override
    public boolean equals(Object obj) {
    	if(!(obj instanceof Interval))
    	return false;	
    	Interval b=(Interval) obj;
    	if(this.low!=b.low)return false;
    	if(this.high!=b.high)return false;
    	
    	
    	return true;
    }


}


