package io.github.engelberg.dancinglinks;
import java.util.Map.Entry;
import java.util.*;
import java.lang.System;

/**
 *
 * @author Mark Engelberg
 */
public class DancingLinkRoot extends DancingLink {
    // Statistics
    public long steps = 0;
    public long backtrackSteps = 0;
    public long nodes = 0;
    public long decisionNodes = 0;
    public long deadends = 0;

    // Solutions
    public ArrayList listSolutions;
    public Stack<DancingLink> sol;

    // State machine
    public Stack<DancingLink> columnStack;
    public final int CHOOSE_COLUMN = 0;
    public final int NEXT_ROW = 1;
    public final int BACKTRACK = 2;
    public int traversal = 0;

    // Termination criteria
    public boolean timeoutFlag;
    public boolean limitFlag;
    int limit;
    long timeout;
    
    public DancingLinkRoot(Object n) {
        super(n);
    }
    
    DancingLink chooseColumn(){
        int bestSize = this.r.s;
        DancingLink bestLink = this.r;
        for (DancingLink link = this.r.r; link != this; link = link.r) {
            if (link.s < bestSize){
                bestSize = link.s;
                bestLink = link;
            }
        }
	if (bestSize > 1)
	    decisionNodes++;
	else if (bestSize == 0)
	    deadends++;
        return bestLink;
    }

    void clearSolutions () {
        listSolutions = new ArrayList();
        sol = new Stack();
    }
    
    void addSolution () {
        ArrayList solution = new ArrayList();
        for (DancingLink link : sol) {
            solution.add(link.n);
        }
        listSolutions.add(solution);
    }

    public void initSearchOne () {
        sol = new Stack();
	columnStack = new Stack();
	nodes = 1;  // Count root node
    }

    public ArrayList searchOne() {
	// Converts search process into a state machine.
	// columnStack stores a stack of columns chosen.
	// sol tracks rows chosen, uses null to indicate
	// choice of  "no row" for optional cols.
	while (true) {
	    steps++;
	    if (traversal == CHOOSE_COLUMN) {
		// Check to see whether we have a solution		
		if (this == this.r) {
		    ArrayList solution = new ArrayList();
		    for (DancingLink link : sol) {
			if (link != null) {
			    solution.add(link.n);
			}
		    }
		    traversal = NEXT_ROW;
		    return solution;
		}
		// Choose a column to work on
		DancingLink colHeader = this.chooseColumn();
		colHeader.cover();
		columnStack.push(colHeader);
		// Find first row associated with this column
		DancingLink i = colHeader.d;
		// Select this first row for our solution
		if (i != colHeader) {
		    for (DancingLink j = i.r; j != i; j=j.r) {
			j.c.cover();
		    }
		    sol.push(i);
		    nodes++;
		}
		// If no first row was found, but it's an optional col
		// we need to consider the "no row" possiblity
		else if (colHeader.optional) {
		    sol.push(null);
		    nodes++;
		    traversal = CHOOSE_COLUMN;
		}
		// Otherwise, if no row was found, we should backtrack
		else {
		    traversal = BACKTRACK;
		}	    
	    }
	    else if (traversal == NEXT_ROW) {
		// Have we found all solutions?
		if (sol.empty())
		    return null;
		// Remove row we were just working on from solution
		DancingLink i = sol.pop();
		// If we've just considered "no row" for optional column,
		// we're ready to backtrack
		if (i == null) {
		    traversal = BACKTRACK;
		}
		else {
		    // Undo work for row we just processed
		    for (DancingLink j = i.l; j != i; j=j.l) {
			j.c.uncover();
		    }
		    // Go to next row
		    i = i.d;
		    DancingLink colHeader = columnStack.peek();
		    // Select next row for our solution
		    if (i != colHeader) {
			for (DancingLink j = i.r; j != i; j=j.r) {
			    j.c.cover();
			}
			sol.push(i);
			nodes++;
			traversal = CHOOSE_COLUMN;
		    }
		    // If there is no next row, but it is an optional col,
		    // we need to consider the "no row" possibility
		    else if (colHeader.optional) {
			sol.push(null);
			nodes++;
			traversal = CHOOSE_COLUMN;
		    }
		    // If there is no next row and it's not an optional col,
		    // we're ready to backtrack
		    else {
			traversal = BACKTRACK;
		    }
		}
	    }
	    else if (traversal == BACKTRACK) {
		// We've explored every row possiblity for this column
		// so we backtrack to previous column, and start looking
		// for next row associated with the previous column.
		backtrackSteps++;
		DancingLink colHeader = columnStack.pop();
		colHeader.uncover();
		traversal = NEXT_ROW;
	    }
	}
    }
    
    public ArrayList initSearch() {
        clearSolutions();
        this.search();
        return listSolutions;
    }

    void search() {
	nodes++;
        if (this == this.r) {
            addSolution();
        }
        else {
            DancingLink colHeader = this.chooseColumn();
            colHeader.cover();
            for (DancingLink i = colHeader.d; i!=colHeader; i=i.d) {
                for (DancingLink j = i.r; j !=i; j=j.r) {
                    j.c.cover();
                }
                sol.push(i);
                this.search();
                sol.pop();
                for (DancingLink j = i.l; j!=i; j=j.l) {
                    j.c.uncover();
                }
            }
            if (colHeader.optional) {
                this.search();
            }
            colHeader.uncover();
        }
    }
    
    public ArrayList initSearchLimit(int limit) {
        clearSolutions();
        this.limit = limit;
        this.searchLimit();
        return listSolutions;
    }
    // return false when we've hit our search limit
    boolean searchLimit() {
	nodes++;
        if (this == this.r) {
            addSolution();
            if (limit == listSolutions.size()) {
                limitFlag = true;
                return false;
            }
            else 
            	return true;
        }
        else {
            DancingLink colHeader = this.chooseColumn();
            colHeader.cover();
            for (DancingLink i = colHeader.d; i!=colHeader; i=i.d) {
                for (DancingLink j = i.r; j !=i; j=j.r) {
                    j.c.cover();
                }
                sol.push(i);
                if (! this.searchLimit())
                	return false;                			
                sol.pop();
               	for (DancingLink j = i.l; j!=i; j=j.l) {
               		j.c.uncover();
               	}             
            }
            if (colHeader.optional) {
                if (! this.searchLimit()) {
                	return false;
                }
            }
            colHeader.uncover();
            return true;
        }
    }
    
    public ArrayList initSearchLimitTimeout(int limit, long duration) {
        clearSolutions();
        this.limit = limit;
        this.timeout = duration+System.currentTimeMillis();
        this.searchLimitTimeout();
        return listSolutions;
    }
    // return false when we've hit our search limit or time runs out
    boolean searchLimitTimeout() {
	nodes++;
    	if (System.currentTimeMillis()>timeout) {
            timeoutFlag = true;
            return false;
        }
        if (this == this.r) {
            addSolution();
            if (limit == listSolutions.size()) {
                limitFlag = true;
                return false;
            }
            else 
            	return true;
        }
        else {
            DancingLink colHeader = this.chooseColumn();
            colHeader.cover();
            for (DancingLink i = colHeader.d; i!=colHeader; i=i.d) {
                for (DancingLink j = i.r; j !=i; j=j.r) {
                    j.c.cover();
                }
                sol.push(i);
                if (! this.searchLimitTimeout())
                	return false;                			
                sol.pop();
               	for (DancingLink j = i.l; j!=i; j=j.l) {
               		j.c.uncover();
               	}             
            }
            if (colHeader.optional) {
                if (! this.searchLimitTimeout()) {
                	return false;
                }
            }
            colHeader.uncover();
            return true;
        }
    }
}
