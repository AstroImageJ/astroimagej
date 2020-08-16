// InvalidDateException.java
// $Id: InvalidDateException.java,v 1.1 2000/09/26 13:54:04 bmahe Exp $
// (c) COPYRIGHT MIT, INRIA and Keio, 2000.
// Please first read the full copyright statement in file COPYRIGHT.html

// package org.w3c.util;

package astroj;

/**
 * @version $Revision: 1.1 $
 * @author  Benot Mahe (bmahe@w3.org)
 */
public class InvalidDateException extends Exception
	{
	public InvalidDateException(String msg)
		{
		super(msg);
		}
	}

