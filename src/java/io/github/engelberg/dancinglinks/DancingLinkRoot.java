package io.github.engelberg.dancinglinks;
import java.util.Map.Entry;
import java.util.*;
import java.lang.System;

/**
 *
 * @author Mark Engelberg
 */
public class DancingLinkRoot extends DancingLink {
    public ArrayList listSolutions;
    public ArrayList<DancingLink> sol;
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
        return bestLink;
    }

    void clearSolutions () {
        listSolutions = new ArrayList();
        sol = new ArrayList();
    }
    
    void addSolution () {
        ArrayList solution = new ArrayList();
        for (DancingLink link : sol) {
            solution.add(link.n);
        }
        listSolutions.add(solution);
    }

    public ArrayList initSearch() {
        clearSolutions();
        this.search();
        return listSolutions;
    }

    void search() {
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
                sol.add(i);
                this.search();
                sol.remove(sol.size()-1);
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
        if (this == this.r) {
            addSolution();
            if (limit == listSolutions.size())
            	return false;
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
                sol.add(i);
                if (! this.searchLimit())
                	return false;                			
                sol.remove(sol.size()-1);
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
    	if (System.currentTimeMillis()>timeout)
    		return false;    	
        if (this == this.r) {
            addSolution();
            if (limit == listSolutions.size())
            	return false;
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
                sol.add(i);
                if (! this.searchLimitTimeout())
                	return false;                			
                sol.remove(sol.size()-1);
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
