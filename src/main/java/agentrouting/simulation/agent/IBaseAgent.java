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


package agentrouting.simulation.agent;

import agentrouting.simulation.environment.EDirection;
import agentrouting.simulation.environment.IEnvironment;
import agentrouting.simulation.algorithm.force.IForce;
import cern.colt.matrix.DoubleMatrix1D;
import cern.colt.matrix.impl.DenseDoubleMatrix1D;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.Pixmap;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.Sprite;
import org.lightjason.agentspeak.action.binding.IAgentActionAllow;
import org.lightjason.agentspeak.action.binding.IAgentActionBlacklist;
import org.lightjason.agentspeak.action.binding.IAgentActionName;
import org.lightjason.agentspeak.common.CPath;
import org.lightjason.agentspeak.configuration.IAgentConfiguration;
import org.lightjason.agentspeak.language.CCommon;
import org.lightjason.agentspeak.language.CLiteral;
import org.lightjason.agentspeak.language.CRawTerm;
import org.lightjason.agentspeak.language.ILiteral;
import org.lightjason.agentspeak.language.instantiable.plan.trigger.CTrigger;
import org.lightjason.agentspeak.language.instantiable.plan.trigger.ITrigger;

import java.text.MessageFormat;
import java.util.Queue;
import java.util.Random;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.stream.IntStream;
import java.util.stream.Stream;


/**
 * agent class for modelling individual behaviours
 */
@IAgentActionBlacklist
abstract class IBaseAgent extends org.lightjason.agentspeak.agent.IBaseAgent<IAgent> implements IAgent
{
    /**
     * name of the beliefbase for individual preferences
     */
    public static final String PREFERENCE = "preferences";
    /**
     * random generator
     */
    private final Random m_random = new Random();
    /**
     * current position of the agent
     */
    private final DoubleMatrix1D m_position;
    /**
     * reference to the environment
     */
    private final IEnvironment m_environment;
    /**
     * color
     */
    private final Color m_color;
    /**
     * sprite object for painting
     */
    private Sprite m_sprite;
    /**
     * current moving speed
     */
    private int m_speed = 1;
    /**
     * route
     */
    private final Queue<DoubleMatrix1D> m_route = new ConcurrentLinkedQueue<>();




    /**
     * ctor
     *
     * @param p_environment environment
     * @param p_agentconfiguration agent configuration
     * @param p_force force model
     * @param p_position initialize position
     * @param p_color color string in RRGGBBAA
     */
    IBaseAgent( final IEnvironment p_environment, final IAgentConfiguration<IAgent> p_agentconfiguration,
                final IForce p_force, final DoubleMatrix1D p_position, final String p_color
    )
    {
        super( p_agentconfiguration );
        if ( p_color.isEmpty() )
            throw new RuntimeException( "color need not to be empty" );

        m_position = p_position;
        m_environment = p_environment;
        m_color = Color.valueOf( p_color );

        // create a random route
        this.routerandom( Math.min( m_environment.column(), m_environment.row() ) / 2 );
        //m_route.add( new DenseDoubleMatrix1D( new double[]{m_environment.column() - 5, m_environment.row() - 5} ) );
    }

    @Override
    public IAgent call() throws Exception
    {
        // cache current position to generate non-moving trigger
        final DenseDoubleMatrix1D l_position = new DenseDoubleMatrix1D( m_position.toArray() );

        // --- visualization -----------------------------------------------------------------------

        // update sprite for painting (sprit position is x/y position, but position storing is row / column)
        if ( m_sprite != null )
            m_sprite.setPosition( (float) l_position.get( 1 ), (float) l_position.get( 0 ) );


        // --- agent-cycle to create goal-trigger --------------------------------------------------

        // call cycle
        super.call();

        // if position is not changed run not-moved plan
        if ( m_position.equals( l_position ) )
            this.trigger( CTrigger.from( ITrigger.EType.ADDGOAL, CLiteral.from( "movement/standstill" ) ) );

        // check if the agent reaches the goal-position, if it reachs, remove it from the route queue
        final DoubleMatrix1D l_goalposition = this.goal();
        if ( m_position.equals( l_goalposition ) )
        {
            this.trigger( CTrigger.from( ITrigger.EType.ADDGOAL, CLiteral.from( "goal/achieve-position", Stream.of( CRawTerm.from( m_position ) ) ) ) );
            m_route.poll();
        }
        else
        {
            // otherwise check "near-by(D)" preference for the current position and the goal
            // position, D is the radius (in cells) so we trigger the goal "near-by(Y)" and
            // Y is a literal with distance
            final double l_distance = EDirection.distance( m_position, l_goalposition );
            if ( l_distance <= this.preference( "near-by", 0 ).doubleValue() )
                this.trigger( CTrigger.from( ITrigger.EType.ADDGOAL, CLiteral.from( "goal/near-by", Stream.of( CRawTerm.from( l_distance ) ) ) ) );
        }

        return this;
    }



    // --- object getter ---------------------------------------------------------------------------------------------------------------------------------------

    @Override
    public final DoubleMatrix1D position()
    {
        return m_position;
    }

    @Override
    public final DoubleMatrix1D goal()
    {
        return m_route.isEmpty()
            ? m_position
            : m_route.peek();
    }

    @Override
    public final Stream<ILiteral> preferences()
    {
        return this.beliefbase().stream( CPath.from( PREFERENCE ) );
    }



    // --- agent actions ---------------------------------------------------------------------------------------------------------------------------------------
    // https://en.wikipedia.org/wiki/Fitness_proportionate_selection to calculate the direction

    @IAgentActionName( name = "speed/set" )
    @IAgentActionAllow( classes = CMovingAgent.class )
    protected final void setspeed( final Number p_speed )
    {
        if ( p_speed.intValue() < 1 )
            throw new RuntimeException( "speed cannot be less than one" );
        m_speed = p_speed.intValue();
    }

    @IAgentActionName( name = "speed/increment" )
    @IAgentActionAllow( classes = CMovingAgent.class )
    protected final void incrementspeed( final Number p_speed )
    {
        if ( p_speed.intValue() < 1 )
            throw new RuntimeException( "speed cannot be less than one" );
        m_speed += p_speed.intValue();
    }

    @IAgentActionName( name = "speed/decrement" )
    @IAgentActionAllow( classes = CMovingAgent.class )
    protected final void decrementspeed( final Number p_speed )
    {
        if ( ( p_speed.intValue() < 1 ) || ( m_speed - p_speed.intValue() < 1 ) )
            throw new RuntimeException( "speed cannot be less than one or cannot be smaler than one" );
        m_speed -= p_speed.intValue();
    }

    /**
     * route calculation
     *
     * @param p_row row position
     * @param p_column column position
     */
    @IAgentActionAllow
    @IAgentActionName( name = "route/set" )
    protected final void route( final Number p_row, final Number p_column )
    {
        m_route.addAll( m_environment.route( this, new DenseDoubleMatrix1D( new double[]{p_row.doubleValue(), p_column.doubleValue()} ) ) );
    }

    /**
     * creates a new route depend on the
     * distance around the current position
     *
     * @param p_radius distance (in cells)
     */
    @IAgentActionAllow
    @IAgentActionName( name = "route/random" )
    protected final void routerandom( final Number p_radius )
    {
        if ( p_radius.intValue() < 1 )
            throw new RuntimeException( "radius must be greater than zero" );

        m_route.addAll(
            m_environment.route(
                this,
                new DenseDoubleMatrix1D(
                    new double[]{
                        m_position.getQuick( 0 ) + m_random.nextInt( p_radius.intValue() * 2 ) - p_radius.intValue(),
                        m_position.getQuick( 1 ) + m_random.nextInt( p_radius.intValue() * 2 ) - p_radius.intValue()
                    }
                )
            )
        );
    }

    /**
     * skips the current goal-position of the routing queue
     */
    @IAgentActionAllow
    @IAgentActionName( name = "route/skipcurrent" )
    protected final void routskipcurrent()
    {
        m_route.poll();
    }

    /**
     * skips the current n-elements of the routing queue
     * @param p_value number of elements
     */
    @IAgentActionAllow
    @IAgentActionName( name = "route/skipelements" )
    protected final void routskipcurrent( final Number p_value )
    {
        if ( p_value.intValue() < 1 )
            throw new RuntimeException( "value must be greater than zero" );

        IntStream.range( 0, p_value.intValue() ).forEach( i -> m_route.poll() );
    }

    /**
     * move forward into goal direction
     */
    @IAgentActionName( name = "move/forward" )
    @IAgentActionAllow( classes = CMovingAgent.class )
    protected final void moveforward()
    {
        this.move( EDirection.FORWARD );
    }

    /**
     * move left forward into goal direction
     */
    @IAgentActionName( name = "move/forwardright" )
    @IAgentActionAllow( classes = CMovingAgent.class )
    protected final void moveforwardright()
    {
        this.move( EDirection.FORWARDRIGHT );
    }

    /**
     * move right to the goal direction
     */
    @IAgentActionName( name = "move/right" )
    @IAgentActionAllow( classes = CMovingAgent.class )
    protected final void moveright()
    {
        this.move( EDirection.RIGHT );
    }

    /**
     * move backward right from goal direction
     */
    @IAgentActionName( name = "move/backwardright" )
    @IAgentActionAllow( classes = CMovingAgent.class )
    protected final void movebackwardright()
    {
        this.move( EDirection.BACKWARDRIGHT );
    }

    /**
     * move backward from goal direction
     */
    @IAgentActionName( name = "move/backward" )
    @IAgentActionAllow( classes = CMovingAgent.class )
    protected final void movebackward()
    {
        this.move( EDirection.BACKWARD );
    }

    /**
     * move backward right from goal direction
     */
    @IAgentActionName( name = "move/backwardleft" )
    @IAgentActionAllow( classes = CMovingAgent.class )
    protected final void movebackwardleft()
    {
        this.move( EDirection.BACKWARDLEFT );
    }

    /**
     * move left to the goal
     */
    @IAgentActionName( name = "move/left" )
    @IAgentActionAllow( classes = CMovingAgent.class )
    protected final void moveleft()
    {
        this.move( EDirection.LEFT );
    }

    /**
     * move forward left into goal direction
     */
    @IAgentActionName( name = "move/forwardleft" )
    @IAgentActionAllow( classes = CMovingAgent.class )
    protected final void moveforwardleft()
    {
        this.move( EDirection.FORWARDLEFT );
    }

    /**
     * helper method for moving
     *
     * @param p_direction direction
     */
    private void move( final EDirection p_direction )
    {
        final DoubleMatrix1D l_goalposition = this.goal();
        if ( l_goalposition.equals( m_position ) )
            return;

        if ( !this.equals( m_environment.position( this, p_direction.position( m_position, l_goalposition, m_speed ) ) ) )
            throw new RuntimeException( MessageFormat.format( "cannot move {0}", p_direction ) );
    }

    @Override
    public final <N> N preference( final String p_name, final N p_default )
    {
        return CCommon.raw(
            this.beliefbase().stream( CPath.from( MessageFormat.format( "{0}/{1}", PREFERENCE, p_name ) ) )
                .findFirst()
                .orElseGet( () -> CLiteral.from( MessageFormat.format( "{0}/{1}", PREFERENCE, p_name ), Stream.of( CRawTerm.from( p_default ) ) ) )
                .values()
                .findFirst()
                .orElse( CRawTerm.from( p_default ) )
        );
    }

    // --- visualization ---------------------------------------------------------------------------------------------------------------------------------------

    @Override
    public final Sprite sprite()
    {
        return m_sprite;
    }

    @Override
    public final Sprite spriteinitialize( final int p_rows, final int p_columns, final int p_cellsize )
    {
        // create a colored sequare for the agent
        final Pixmap l_pixmap = new Pixmap( p_cellsize, p_cellsize, Pixmap.Format.RGBA8888 );
        l_pixmap.setColor( m_color );
        l_pixmap.fillRectangle( 0, 0, p_cellsize, p_cellsize );

        // add the square to a sprite (for visualization) and scale it to 80% of cell size
        m_sprite = new Sprite( new Texture( l_pixmap ), 0, 0, p_cellsize, p_cellsize );
        m_sprite.setSize( 0.9f * p_cellsize, 0.9f * p_cellsize );
        m_sprite.setOrigin( 1.5f / p_cellsize, 1.5f / p_cellsize );

        return m_sprite;
    }

}
