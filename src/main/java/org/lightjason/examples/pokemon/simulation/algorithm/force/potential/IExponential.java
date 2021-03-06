/**
 * @cond LICENSE
 * ######################################################################################
 * # LGPL License                                                                       #
 * #                                                                                    #
 * # This file is part of the LightJason AgentSpeak(L)                                  #
 * # Copyright (c) 2015-16, Philipp Kraus (philipp@lightjason.org)                      #
 * # This program is free software: you can redistribute it and/or modify               #
 * # it under the terms of the GNU Lesser General Public License as                     #
 * # published by the Free Software Foundation, either version 3 of the                 #
 * # License, or (at your option) any later version.                                    #
 * #                                                                                    #
 * # This program is distributed in the hope that it will be useful,                    #
 * # but WITHOUT ANY WARRANTY; without even the implied warranty of                     #
 * # MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the                      #
 * # GNU Lesser General Public License for more details.                                #
 * #                                                                                    #
 * # You should have received a copy of the GNU Lesser General Public License           #
 * # along with this program. If not, see http://www.gnu.org/licenses/                  #
 * ######################################################################################
 * @endcond
 */

package org.lightjason.examples.pokemon.simulation.algorithm.force.potential;


import java.util.function.UnaryOperator;


/**
 * exponential potential function
 */
public abstract class IExponential implements UnaryOperator<Double>
{

    /**
     * returns maximum value of the metric range
     *
     * @return maximum value
     */
    protected abstract double maximum();


    /**
     * returns scaling factor
     *
     * @return scaling factor
     */
    protected abstract double scale();


    @Override
    public final Double apply( final Double p_double )
    {
        return Math.exp( this.maximum() - this.scale() * p_double );
    }

}
