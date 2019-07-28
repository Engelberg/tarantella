package io.github.engelberg.dancinglinks;
import java.util.*;

/**
 *
 *
 * @author Mark Engelberg
 */
public class SolutionWithStats {
    public ArrayList solution;
    public long nodes;
    public long decisionNodes;
    public long deadends;

    public SolutionWithStats(ArrayList solution,
			     long nodes, long decisionNodes, long deadends) {
	this.solution = solution;
	this.nodes = nodes;
	this.decisionNodes = decisionNodes;
	this.deadends = deadends;
    }
}
