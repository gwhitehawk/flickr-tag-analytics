#!/usr/bin/env python
import urllib
import datetime
import re
import os
import sys 
import math

MIN_NODE_WEIGHT = 40
EDGE_RATIO = 5
NODE_SIZE_FACTOR = 0.05
FONT_SIZE_MIN = 3
FONT_SIZE_MAX = 11

source_file = sys.argv[1]
dot_file = sys.argv[2]

def make_dot():
    sourcef = open(source_file, "r")
    dotf = open(dot_file, "w")
    
    dotf.write("""graph Tags {
    \t ratio = "compress";
    \t size = 8;
    \t nodesep = 0.1;
    \t ranksep = 1.5;
    """)

    for line in sourcef.readlines():
        line = line.strip()
        if (line != ""):
            sline = line.split(":")
        
            node_name = sline[0]
            node_weight = int(sline[1].strip())
            
            node_size = NODE_SIZE_FACTOR*node_weight/MIN_NODE_WEIGHT
            font_size = min(math.floor(FONT_SIZE_MIN*node_weight/MIN_NODE_WEIGHT), FONT_SIZE_MAX)

            dotf.write("\t\"%s\" [width = %.2f, height = %.2f, fontsize = %d];\n" % (node_name, 2*node_size, node_size, font_size))
            neighbors = sline[2].strip().split()
 
            for neighbor in neighbors:
                neighbor = neighbor.strip("()")
            
                sneighbor = neighbor.split(",")
                edge_name = sneighbor[0]
                
                if (edge_name > node_name):
                    edge_weight = int(sneighbor[1])
                
                    edge_weight_param = math.floor(EDGE_RATIO*edge_weight/node_weight);
                    if (edge_weight_param > 0):
                        dotf.write("\t\"%s\" -- \"%s\" [weight = %d];\n" % (node_name, edge_name, edge_weight_param)) 
        
            dotf.write("\n")

    dotf.write("}")
    sourcef.close()
    dotf.close()

make_dot()
