# Synopsis:

For the challenge, we are to detect cyclic references in the menu. To do this, I do a simple Depth-First search using the
menues obtained from the API endpoints:

* Challenge 1
  * https://backend-challenge-summer-2018.herokuapp.com/challenges.json?id=1&page=1
* Challenge 2
  * https://backend-challenge-summer-2018.herokuapp.com/challenges.json?id=2&page=1
  
# Main File:

The **src/com/company/main.java** is the file with the code.

# Observations:

Each menu item only has one parent; so, either two menus are distinct or they are
part of one larger menu.
  
# Build:

I used IntelliJ IDEA for the project. Make sure to include the java-json.jar in the 
build path. This jar can be found in the jars/ folder

# Runtime:

The setup uses at worst O(|V|), where the V is the set of menus (a.k.a. verticies).
I iterate through all the verticies once to find the root nodes.

The DFS algorithm uses O(|V| + |E|), where E is the set of edges (an edge is the
parent -> child pairing). We iterate through all the nodes and through all the children
of each nodes.


