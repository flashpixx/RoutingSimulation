// initial-goal
!main.

// initial plan (triggered by the initial-goal) - calculates the initial route route
+!main
    <-
    route/set/start( 140, 140 );
    T = route/estimatedtime();
    generic/print("estimated time of the current route [", T , "]");
    !movement/walk/forward
.



// --- movement plans ------------------------------------------------------------------------------------------------------------------------------------------


// plan to deal with force information
+!movement/force(D)
    : >>( force(F), D )
        <-
            generic/print("force", F)
.


// walk straight forward into the direction of the goal-position
+!movement/walk/forward
    <-
        generic/print( "walk forward in cycle [", Cycle, "]" );
        move/forward();
        !movement/walk/forward
.


// walk straight forward fails than go left
-!movement/walk/forward
    <-
        generic/print( "walk forward fails in cycle [", Cycle, "]" );
        !movement/walk/left;
        !act/attack
.

// walk left - direction 90 degree to the goal position
+!movement/walk/left
    <-
        generic/print( "walk left in cycle [", Cycle, "]" );
        move/left();
        !movement/walk/forward
.

// walk left fails than go right
-!movement/walk/left
    <-
        generic/print( "walk left fails in cycle [", Cycle, "]" );
        !movement/walk/right;
        !act/attack
.

// walk right - direction 90 degree to the goal position
+!movement/walk/right
    <-
        generic/print( "walk right in cycle [", Cycle, "]" );
        move/right();
        !movement/walk/forward
.

// walk right fails than sleep and hope everything will be
// fine later, wakeup plan will be trigger after sleeping
-!movement/walk/right
    <-
        T = math/statistic/randomsimple();
        T = T * 10 + 1;
        T = math/min( T, 5 );
        generic/print( "walk right fails in cycle [", Cycle, "] wait [", T,"] cycles" );
        agent/sleep(T)
.

// if the agent is not walking because speed is
// low the agent increment the current speed
+!movement/standstill
    : >>attribute/speed( S )
        <-
            generic/print( "standstill - increment speed with 1 in cycle [", Cycle, "]" );
            S++;
            +attribute/speed( S );
            !movement/walk/forward
.

// -------------------------------------------------------------------------------------------------------------------------------------------------------------



// --- attack plans --------------------------------------------------------------------------------------------------------------------------------------------

// run a random attack around the current position
+!act/attack
    : >>env/myposition( y(Y), x(X) )
        <-
            A = math/statistic/randomsimple();
            B = math/statistic/randomsimple();
            X = X + A * 4 - 2;
            Y = Y + B * 4 - 2;
            attack/point( "slam", 1, X, Y )
.

// -------------------------------------------------------------------------------------------------------------------------------------------------------------



// --- other calls ---------------------------------------------------------------------------------------------------------------------------------------------

// is called if the pokemon agent increment the level
+!level-up
    <-
        generic/print( "level-up in cycle [", Cycle, "]" )
.

// is called iif || current position - goal-position || <= near-by
// the exact position of the goal will be skipped, so the agent
// is walking to the next position
+!position/achieve(P, D)
    <-
        generic/print( "position [", P, "] achieved with distance [", D, "] in cycle [", Cycle, "]" );
        route/next
.

// is called if the agent walks beyonds the goal-position, than
// the speed is set to 1 and we try go back
+!position/beyond(P)
    <-
        generic/print( "position beyond [", P, "] - set speed to 1 in cycle [", Cycle, "]" );
        +attribute/speed( 1 );
        route/next;
        !movement/walk/forward
.


// if the agent is wake-uped the speed is set to 1 and the agent starts walking
// to the next goal-position
+!wakeup
    <-
        generic/print("wakeup - set speed to 1 in cycle [", Cycle, "]");
        +attribute/speed( 1 );
        !movement/walk/forward
.

// -------------------------------------------------------------------------------------------------------------------------------------------------------------
