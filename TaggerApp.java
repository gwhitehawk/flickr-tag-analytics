/* This app allows to build a graph from arrays of picture tags, and display 
 * the closely related ones for each tag. 
 * Data format: Map with keys equal to tags. Tag _a_ is mapped into a pair 
 * (int weight, List neighborhood). _a_.weight is the number of occurences of 
 * tag _a_ among the set of flickr photos. Each item in _a_.neighborhood  is 
 * an instance of Edge(String label, int weight),  
 * where edge.label denotes the related tag, and edge.weight is the number of common 
 * occurences of tags _a_ and edge.label. Items in the list are sorted according 
 * to their label in the lexicographic order, to allow for efficient logarithmic 
 * updates.  
 *
 * Methods of Tagger class:
 *
 * updateGraph(String[] tags):
 * Updates the graph by plugging in a new set of tags.
 *
 * displaykNN(String targetFile, int k, int weightThreshold):
 * Displays each tag with its nearest neighbors, and the corresponding weights.
 *
 * Efficiency:
 * Good: 
 * Easy to access items to update at each step (TreeMap constant access).
 * Easy to display k nearest tags for each tag (Sorted lists -> constant).
 */

import java.io.*;
import java.util.*;
import java.lang.*;
import java.nio.charset.Charset;

// class: Edge
// fields: String label, int weight
// For tag _a_, lists a related tag _b_ (label), and number of common 
// occurences (weight).
class Edge {
    public String label;
    public int weight;

    public Edge(String label) {
        this.label = label;
        this.weight = 1;
    }
}

// class: NodeDescription
// fields: int nodeWeight, List neighborhood
// For tag _a_, lists number of occurences (nodeWeight), and list of edges
// (neighborhood). 
class NodeDescription {
    public int nodeWeight;
    List<Edge> neighborhood;

    public NodeDescription(List<Edge> neighborhood) {
        this.nodeWeight = 1;
        this.neighborhood = neighborhood;
    }
}

// class LabelComparator: compares two edges by labels (lexicographically)
class LabelComparator implements Comparator<Edge> {
    public int compare(Edge e1, Edge e2) {
        return e1.label.compareTo(e2.label);
    }
}

// class WeightComparator: compares two edges by weights (in descending order)
class WeightComparator implements Comparator<Edge> {
    public int compare(Edge e1, Edge e2) {
        return e2.weight - e1.weight;
    }
}

// class Tagger:
// field graph: Maps tag -> (weight, neighborhood)
// method void updateGraph(String[] tags): updates graph with tags in the String argument 
// method void displaykNN(String filename, int k, int weightThreshold): prints tags with
// at least weightThreshold occurences, and at most k of their neighbors with maximum 
// number of common occurences
class Tagger {
    // Map graph: Maps tag -> (weight, neighborhood)
    private Map<String, NodeDescription> graph = new TreeMap<String, NodeDescription>();
    
    // printToFile: Prints StringBuffer data into file filename, append if append=true. 
    private static void printToFile(StringBuffer data, String filename, boolean append) {
        try {
            FileOutputStream out = new FileOutputStream(filename, append);
            PrintStream pPrint = new PrintStream(out);
            pPrint.println(data);
            pPrint.close();
        }   
        catch (FileNotFoundException e) {
        }   
    }   
    
    // updateGraph: Injects an array of tags into graph
    // example graph created from [a, b], [a, c]:
    // "a" -> nodeWeight = 2, neighborhood = ((label = "b", weight = 1), (label = "c", weight = 1))
    // "b" -> nodeWeight = 1, neighborhood = ((label = "a", weight = 1))
    // "c" -> nodeWeight = 1, neighborhood = ((label = "a", weight = 1))
    public void updateGraph(String[] tags) {
        List<Edge> neighborhood;
        NodeDescription currentNode;
        Edge currentEdge;
        Comparator<Edge> c = new LabelComparator();

        int index;
    
        // for each artist
        for (String tag : tags) {
            index = 0;

            // if graph doesn't contain key artist, initialize neighborhood,
            // else get the neighborhood, and increase node weight
            if (!graph.containsKey(tag)) {
                neighborhood = new ArrayList<Edge>();
                currentNode = new NodeDescription(neighborhood);    
            } else {
                currentNode = graph.get(tag);
                currentNode.nodeWeight++;
                neighborhood = currentNode.neighborhood;
            }   

            // for each neighbor other than artist
            for (String neighbor : tags) {
                if (!tag.equals(neighbor)) {
                    currentEdge = new Edge(neighbor);
    
                    // find neighbor in neigborhood
                    index = Collections.binarySearch(neighborhood, currentEdge, c); 
    
                    // if neighbor not in neighborhood, set insert index, insert neighbor,
                    // else increase edge.weight
                    if (index < 0) {
                        index = -index - 1;
                        neighborhood.add(index, currentEdge);
                    } else {
                        currentEdge = neighborhood.get(index);
                        currentEdge.weight++;
                        neighborhood.set(index, currentEdge);
                    }   
                }   
            }   
            
            // if neighborhood non-empty, put artist to graph
            if (!neighborhood.isEmpty()) {
                currentNode.neighborhood = neighborhood;
                graph.put(tag, currentNode);
            }
        }
    }

    // displaykNN: Displays (standard output or file) tags and at most k-tuple
    // of related tags with max number of common occurences, to filter out noisy 
    // tags, weightThreshold can be set (minimum number of occurences)
    // 
    // output example for graph build from ["a", "b"], ["a", "c"]:
    // a: 2: (b, 1) (c, 1)
    // b: 1: (a, 1)
    // c: 1: (a, 1)
    public void displaykNN(String targetFile, int k, int weightThreshold) {
        Set<String> keys = graph.keySet();
        Iterator iter = keys.iterator();
        Comparator<Edge> c = new WeightComparator();

        // clean-up file if it exists
        if (targetFile != null) 
            printToFile(new StringBuffer(""), targetFile, false);

        while (iter.hasNext()) {
            String currentKey = (String)iter.next();
            NodeDescription currentVal = graph.get(currentKey);
            Collections.sort(currentVal.neighborhood, c);

            StringBuffer output = new StringBuffer(200);
            int count = 0;

            if (currentVal.nodeWeight >= weightThreshold) {
                output.append(String.format("%s: %d: ", currentKey, currentVal.nodeWeight));

                for (Edge item : currentVal.neighborhood) {
                    if (count < k) {
                        output.append(String.format("(%s,%d) ", item.label, item.weight));
                    } else break;
                    
                    count++;
                }
                output.append("\n");
                
                if (targetFile != null) {
                    printToFile(output, targetFile, true);
                } else {
                    System.out.println(output);
                }
            }
        }
    }
}

public class TaggerApp {
    private static final int MAX_TAGS = 100;

    public static void main(String[] args) throws IOException {
        String[] status = { "LINK", "TITLE", "TAGS" };
        int stateIndex;
        String[] tagList = new String[MAX_TAGS];
        int tagListIndex;
        String[] toBuild;
       
        int numberOfNeighbors = Integer.parseInt(args[0]);
        int weightThreshold = Integer.parseInt(args[1]);
        String path = args[2];

        Tagger buildTags = new Tagger();
     
        InputStream fis;
        BufferedReader br;
        String line;
        
        File dir = new File(path);
        String[] fileList;

        if (dir.isDirectory()) {
            fileList = dir.list();
        } else {
            fileList = new String[1];
            path = "";
            fileList[0] = dir.getPath();
        }
        
        for (String filename : fileList) {
            stateIndex = 0;
            tagListIndex = 0;

            try {
                fis = new FileInputStream(path + filename);
                br = new BufferedReader(new InputStreamReader(fis, Charset.forName("UTF-8")));
        
                while ((line = br.readLine()) != null) {
                    if (line.equals("")) {
                        if (stateIndex == 2) { 
                            toBuild = Arrays.copyOf(tagList, tagListIndex);
                            buildTags.updateGraph(toBuild);
                        }       
                        stateIndex = 0;
                        tagListIndex = 0;
                    } else if (stateIndex < 2) {
                        stateIndex++;
                    } else {
                        if (tagListIndex < MAX_TAGS) {
                            tagList[tagListIndex] = line;
                            tagListIndex++;
                        }
                    } 
                }
            } catch (FileNotFoundException e) {
                System.out.println("File not found.");
            }
        } 
         
        if (args.length == 3) {
            buildTags.displaykNN(null, numberOfNeighbors, weightThreshold);
        } else if (args.length > 3) {
            buildTags.displaykNN(args[3], numberOfNeighbors, weightThreshold);
        } 
    }
}
