package edu.nps.moves.dis;

/**
 * Domain.java created on May 7, 2019
 * MOVES Institute Naval Postgraduate School, Monterey, CA, USA www.nps.edu
 *
 * Marker interface to polymorphize category field in Pdus
 * @author Mike Bailey, jmbailey@nps.edu
 * @version $Id$
 */
public interface Category
{
    int getValue();
    String getDescription();
}