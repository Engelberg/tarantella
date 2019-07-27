package io.github.engelberg.dancinglinks;
import java.util.Map.Entry;
import java.util.*;

/**
 *
 * @author Mark Engelberg
 */
public class DancingLink {
    public DancingLink l;
    public DancingLink r;
    public DancingLink u;
    public DancingLink d;
    public int s;
    public DancingLink c;
    public Object n;
    public boolean optional;

    public DancingLink(Object n) {
        this.n = n;
    }

    public DancingLink(int s, Object n, boolean optional) {
	// Optional columns have one more choice, i.e.,
	// choice to not cover column at all, so we add 1 to s
	if (optional)
	    this.s = s + 1;
	else
	    this.s = s;
        this.n = n;
        this.optional = optional;
    }

    static void linkHorizontally(ArrayList<DancingLink> links) {
    	if (links.size()==0)
    		return;
        DancingLink firstLink = links.get(0);
        DancingLink lastLink = links.get(links.size()-1);
        lastLink.r = firstLink;
        firstLink.l = lastLink;
        for (int i = 0; i < links.size()-1; i++) {
            links.get(i).r = links.get(i+1);
            links.get(i+1).l = links.get(i);
        }
    }

    static void linkVertically(ArrayList<DancingLink> links) {
        DancingLink firstLink = links.get(0);
        DancingLink lastLink = links.get(links.size()-1);
        lastLink.d = firstLink;
        firstLink.u = lastLink;
        for (DancingLink link : links) {
            link.c = firstLink;
        }
        for (int i = 0; i < links.size()-1; i++) {
            links.get(i).d = links.get(i+1);
            links.get(i+1).u = links.get(i);            
        }
    }
    
    static Object[] makeColumnHeaders (Map<Object,Collection> colMap, Set<Object> optionalCols, Set<Object> coveredCols) {
        Map headers = new java.util.HashMap();
        ArrayList headersToCoverList = new ArrayList();
        for (Iterator<Entry<Object, Collection>> it = colMap.entrySet().iterator(); it.hasNext();) {
            java.util.Map.Entry cr = it.next();
            Object col = cr.getKey();
            Collection rows = (Collection) cr.getValue();
            DancingLink header = new DancingLink(rows.size(),col.toString(), optionalCols.contains(col)); 
            headers.put(col, header);
            if (coveredCols.contains(col))
            	headersToCoverList.add(header);
        }        
        DancingLinkRoot root = new DancingLinkRoot("root");
        ArrayList headerList = new ArrayList();
        headerList.add(root);        
        headerList.addAll(headers.values());
        linkHorizontally(headerList);
        Object[] rh = {root,headers,headersToCoverList};
        return rh;
    }
    
    public static DancingLink makeTapestry(Map<Object,Collection> rowMap, Map<Object,Collection> colMap){
        return makeTapestry(rowMap,colMap,new java.util.HashSet(), new java.util.HashSet(), false);
    }
    
    public static DancingLink makeTapestry(Map<Object,Collection> rowMap, Map<Object,Collection> colMap,
    		Set<Object> optionalCols){
    	return makeTapestry(rowMap,colMap,optionalCols, new java.util.HashSet(), false);
    }
    
    public static DancingLinkRoot makeTapestry(Map<Object,Collection> rowMap, Map<Object,Collection> colMap,
            Set<Object> optionalCols, Set<Object> coveredCols, boolean shuffle){
        Object[] rh = makeColumnHeaders(colMap, optionalCols, coveredCols);
        DancingLinkRoot root = (DancingLinkRoot) rh[0];
        Map<Object,DancingLink> headers = (Map<Object,DancingLink>) rh[1];
        ArrayList<DancingLink> headersToCoverList = (ArrayList<DancingLink>) rh[2];
        Map<Object,Map<Object,DancingLink>> links = new java.util.HashMap();
        for (Object row : rowMap.keySet()) {
            Map<Object,DancingLink> m = new java.util.HashMap();
            links.put(row, m);
            for (Object col : colMap.keySet()) {
                m.put(col, new DancingLink(row));
            }
        }
	Iterator<Entry<Object, Collection>> columnIterator;
	if (shuffle) {
	    List<Map.Entry<Object, Collection>> shuffledColMapEntries = new ArrayList<> (colMap.entrySet());
	    Collections.shuffle(shuffledColMapEntries);
	    columnIterator = shuffledColMapEntries.iterator();
	}
	else {
	    columnIterator = colMap.entrySet().iterator();
	}
        for (Iterator<Entry<Object, Collection>> colIt = columnIterator; colIt.hasNext();) {
            java.util.Map.Entry cr = colIt.next();
            Object col = cr.getKey();
            Collection rows = (Collection) cr.getValue();
	    Iterator<Object> rowIterator;
	    if (shuffle) {
		List shuffledRows = new ArrayList<> (rows);
		Collections.shuffle(shuffledRows);
		rowIterator = shuffledRows.iterator();
	    }
	    else {
		rowIterator = rows.iterator();
	    }
            ArrayList colLinks = new ArrayList();
            colLinks.add(headers.get(col));
            for (Iterator<Object> rowIt = rowIterator; rowIt.hasNext();) {
		Object row = rowIt.next();
                colLinks.add(links.get(row).get(col));
            }
            linkVertically(colLinks);
        }
        for (Iterator<Entry<Object, Collection>> it = rowMap.entrySet().iterator(); it.hasNext();) {
            java.util.Map.Entry rc = it.next();
            Object row = rc.getKey();
            Collection cols = (Collection) rc.getValue();
            ArrayList rowLinks = new ArrayList();
            for (Object col : cols) {
                rowLinks.add(links.get(row).get(col));
            }
            linkHorizontally(rowLinks);
        }
        for (DancingLink header : headersToCoverList) {
        	header.cover();
        }
        return root;
    }

    
    // Call on column header
    void cover() {
        this.r.l = this.l;
        this.l.r = this.r;
        for (DancingLink i = this.d; i!=this; i=i.d) {
            for (DancingLink j = i.r; j !=i; j=j.r) {
                j.d.u = j.u;
                j.u.d = j.d;
                j.c.s--;
            }
        }
    }

    // Call on column header
    void uncover() {
        for (DancingLink i = this.u; i!=this; i=i.u) {
            for (DancingLink j = i.l; j!=i; j=j.l) {
                j.c.s++;
                j.d.u = j;
                j.u.d = j;
            }
        }
        this.r.l = this;
        this.l.r = this;
    }
}
