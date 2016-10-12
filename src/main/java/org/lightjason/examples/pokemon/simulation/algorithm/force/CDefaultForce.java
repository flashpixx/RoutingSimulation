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

package org.lightjason.examples.pokemon.simulation.algorithm.force;

import java.util.stream.Stream;


/**
 * defines a default force model
 */
public final class CDefaultForce<N extends Number, M extends IForce<N, ?>> implements IForceModel<N, M>
{

    @Override
    public final N apply( final M p_object, final Stream<M> p_stream )
    {
        /*
        p_stream
            .map( i -> {
                      final N l_distance = 0.5 * p_object.metric().apply( i );

                      return p_object.objectscale().apply( p_object, i,
                      Stream.of(
                                                p_object.potentialscale().apply( p_object.potential().apply( l_distance ) ),
                                                p_object.potentialscale().apply( i.potential().apply( l_distance ) )
                                            ).collect( p_object.potentialreduce() )
                                            ).collection( p_object.objectreduce() )
            } )
        */

        return null;
    }

}
