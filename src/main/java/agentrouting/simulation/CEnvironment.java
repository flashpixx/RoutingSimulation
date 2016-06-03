/**
 * @cond LICENSE
 * ######################################################################################
 * # LGPL License                                                                       #
 * #                                                                                    #
 * # This file is part of the LightJason Gridworld                                      #
 * # Copyright (c) 2015-16, Philipp Kraus (philipp.kraus@tu-clausthal.de)               #
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

package agentrouting.simulation;

import agentrouting.simulation.agent.IAgent;
import agentrouting.simulation.algorithm.routing.IRouting;
import cern.colt.matrix.DoubleMatrix1D;
import cern.colt.matrix.ObjectMatrix2D;
import cern.colt.matrix.impl.DenseDoubleMatrix1D;
import cern.colt.matrix.impl.SparseObjectMatrix2D;
import cern.colt.matrix.linalg.Algebra;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.Pixmap;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.maps.tiled.TiledMap;
import com.badlogic.gdx.maps.tiled.TiledMapTile;
import com.badlogic.gdx.maps.tiled.TiledMapTileLayer;
import com.badlogic.gdx.maps.tiled.tiles.StaticTiledMapTile;
import org.lightjason.agentspeak.language.CLiteral;
import org.lightjason.agentspeak.language.CRawTerm;
import org.lightjason.agentspeak.language.instantiable.plan.trigger.CTrigger;
import org.lightjason.agentspeak.language.instantiable.plan.trigger.ITrigger;

import java.text.MessageFormat;
import java.util.List;
import java.util.logging.Logger;
import java.util.stream.IntStream;
import java.util.stream.Stream;


/**
 * environment class
 */
public final class CEnvironment implements IEnvironment
{

    /**
     * logger
     */
    private final static Logger LOGGER = Logger.getLogger( CEnvironment.class.getName() );
    /**
     * algebra object
     */
    private static final Algebra ALGEBRA = new Algebra();
    /**
     * routing algorithm
     */
    private final IRouting m_routing;
    /**
     * row number
     */
    private final int m_row;
    /**
     * column number
     */
    private final int m_column;
    /**
     * cell size
     */
    private final int m_cellsize;
    /**
     * matrix with object positions
     */
    private final ObjectMatrix2D m_positions;


    /**
     * create environment
     *
     * @param p_cellrows number of row cells
     * @param p_cellcolumns number of column cells
     * @param p_cellsize cell size
     * @param p_routing routing algorithm
     */
    public CEnvironment( final int p_cellrows, final int p_cellcolumns, final int p_cellsize, final IRouting p_routing )
    {
        if ( ( p_cellcolumns < 1 ) || ( p_cellrows < 1 ) || ( p_cellsize < 1 ) )
            throw new IllegalArgumentException( "environment size must be greater or equal than one" );

        m_row = p_cellrows;
        m_column = p_cellcolumns;
        m_routing = p_routing;
        m_cellsize = p_cellsize;
        m_positions = new SparseObjectMatrix2D( m_row, m_column );
        LOGGER.info( MessageFormat.format( "create environment with size [{0}x{1}] and cell size [{2}]", m_row, m_column, p_cellsize ) );
    }

    @Override
    public final IEnvironment initialize()
    {
        m_routing.initialize( m_positions );
        return this;
    }

    @Override
    public final IEnvironment execute( final int p_step )
    {
        return this;
    }

    @Override
    public final int row()
    {
        return m_row;
    }

    @Override
    public final int column()
    {
        return m_column;
    }

    @Override
    public final int cellsize()
    {
        return m_cellsize;
    }


    // --- grid-access (routing & position) --------------------------------------------------------------------------------------------------------------------

    @Override
    public final List<DoubleMatrix1D> route( final IElement<?> p_element, final DoubleMatrix1D p_target )
    {
        return m_routing.route( m_positions, p_element, p_target );
    }

    @Override
    @SuppressWarnings( "unchecked" )
    public final synchronized IElement<?> position( final IElement<?> p_element, final DoubleMatrix1D p_position )
    {
        final DoubleMatrix1D l_position = this.clip( new DenseDoubleMatrix1D( p_position.toArray() ) );

        // check of the target position is free, if not return object, which blocks the cell
        final IElement<?> l_object = (IElement<?>) m_positions.getQuick( (int) l_position.getQuick( 0 ), (int) l_position.getQuick( 1 ) );
        if ( l_object != null )
            return l_object;

        // cell is free, move the position and return updated object
        m_positions.set( (int) l_position.getQuick( 0 ), (int) l_position.getQuick( 1 ), null );
        p_element.position().setQuick( 0, l_position.getQuick( 0 ) );
        p_element.position().setQuick( 1, l_position.getQuick( 1 ) );

        m_positions.set( (int) p_element.position().get( 0 ), (int) p_element.position().get( 1 ), p_element );

        return p_element;
    }

    @Override
    public final DoubleMatrix1D clip( final DoubleMatrix1D p_position )
    {
        // clip position values if needed
        p_position.setQuick( 0, clip( p_position.getQuick( 0 ), m_row ) );
        p_position.setQuick( 1, clip( p_position.getQuick( 1 ), m_column ) );

        return p_position;
    }

    /**
     * value clipping
     *
     * @param p_value value
     * @param p_max maximum
     * @return modifed value
     */
    private static double clip( final double p_value, final double p_max )
    {
        if ( p_value < 0 )
            return p_max + p_value;

        if ( p_value >= p_max )
            return p_value - p_max;

        return p_value;
    }


    // --- visualization ---------------------------------------------------------------------------------------------------------------------------------------

    @Override
    public final TiledMap map()
    {
        // create background checkerboard with a tile map
        final Pixmap l_pixmap = new Pixmap( 2 * m_cellsize, m_cellsize, Pixmap.Format.RGBA8888 );
        l_pixmap.setColor( new Color( 0.1f, 0.1f, 0.1f, 1 ) );
        l_pixmap.fillRectangle( 0, 0, m_cellsize, m_cellsize );
        l_pixmap.setColor( new Color( 0.3f, 0.3f, 0.3f, 1 ) );
        l_pixmap.fillRectangle( m_cellsize, 0, m_cellsize, m_cellsize );

        final Texture l_texture = new Texture( l_pixmap );
        final TiledMapTile l_region1 = new StaticTiledMapTile( new TextureRegion( l_texture, 0, 0, m_cellsize, m_cellsize ) );
        final TiledMapTile l_region2 = new StaticTiledMapTile( new TextureRegion( l_texture, m_cellsize, 0, m_cellsize, m_cellsize ) );

        // create tilemap
        final TiledMap l_map = new TiledMap();
        final TiledMapTileLayer l_layer = new TiledMapTileLayer( m_column, m_row, m_cellsize, m_cellsize );
        l_map.getLayers().add( l_layer );

        IntStream
            .range( 0, m_column )
            .forEach( x ->
                      {
                          IntStream
                              .range( 0, m_row )
                              .forEach( y ->
                                        {

                                            final TiledMapTileLayer.Cell l_cell = new TiledMapTileLayer.Cell();
                                            l_layer.setCell( x, y, l_cell );
                                            l_cell.setTile(
                                                y % 2 != 0
                                                ? x % 2 != 0 ? l_region1 : l_region2
                                                : x % 2 != 0 ? l_region2 : l_region1
                                            );
                                        } );
                      } );

        return l_map;
    }


    // --- agent behaviour / access ----------------------------------------------------------------------------------------------------------------------------

    @Override
    public final IElement<IAgent> perceive( final IElement<IAgent> p_agent )
    {
        // check if the agent reaches the viewpont and returns the goal "viewpointreach"
        if ( p_agent.position().equals( p_agent.viewpoint() ) )
            p_agent.trigger( CTrigger.from( ITrigger.EType.ADDGOAL, CLiteral.from( "viewpointreach", Stream.of( CRawTerm.from( p_agent.position() ) ) ) ) );

        //else
        // otherwise check "near(D)" preference for the current position and the view
        // point, D is the distances (in cells) so we trigger the goal "nearby(Y)" and
        // Y is a literal with distance e.g. "viewpoint(D)"
        //if ( ALGEBRA.mult( p_agent.position(), p_agent.viewpoint() ) )

        return p_agent;
    }

}
