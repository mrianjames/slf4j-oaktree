package com.oaktree.core.logging.handlers;

/**
 * File rolling is optional and by default turned off. however it is a useful feature to roll to 
 * another file on size or time constraints, the details of which will be contained in another field.
 * @author Oak Tree Designs Ltd
 *
 */
public enum FileRollingType {
	NONE,MINUTES,HOURS,K,BYTES,MEG,GIG
}
