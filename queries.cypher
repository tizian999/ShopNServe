// all nodes and edges
MATCH (n)-[r]-(m)
RETURN n, r, m;

// all direct connections to one node
MATCH (c:Container)-[:contains]-(n:Node)
WHERE c.name = 'TCU'
AND n.name = 'Secure SW Update'
MATCH (n:Node)-[r]-(m:Node)
RETURN n, r, m;

// all cycles
MATCH (n:Node)
WITH collect(n) as nodes
CALL apoc.nodes.cycles(nodes)
YIELD path RETURN path;

// all cycles + connection between nodes and the three container components (ECU; TCU; Server)
MATCH (n:Node)
WITH collect(n) AS nodes
CALL apoc.nodes.cycles(nodes)
YIELD path
WITH path, nodes(path) AS cycleNodes
MATCH (m:Node)-[r]-(c:Container)
WHERE m IN cycleNodes
RETURN path, m, r, c;

// clear database
match (n)
detach delete n;

