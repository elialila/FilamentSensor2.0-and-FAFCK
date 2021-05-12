package util;


import java.awt.*;
import java.awt.geom.AffineTransform;
import java.awt.geom.Line2D;
import java.awt.geom.PathIterator;
import java.awt.geom.Point2D;
import java.util.*;
import java.util.List;
import java.util.stream.Collectors;

public class PointUtils {
    //protected constructor, this class does not need any instances
    protected PointUtils() {
    }


    /**
     * Get the Endpoints from neighbor-map. In this case a endpoint is a point which has only one neighbor.
     * The neighbor map is based on a skeletal line(1pixel thickness)
     *
     * @param neighbors neighbor map which key = Point and value = all neighbors of this point (distanceSq <=2)
     * @return Returns a Collection of Points which are "endpoints"
     */
    public static Collection<Point2D> getEndpoints(Map<Point2D, Collection<Point2D>> neighbors) {
        return neighbors.keySet().stream().filter(key -> neighbors.get(key).size() == 1).collect(Collectors.toList());
    }

    public static Collection<Point2D> getEndpoints(Map<Point2D, Collection<Point2D>> neighbors, Collection<Point2D> line) {
        return line.stream().filter(p -> {
            Collection<Point2D> tmp = neighbors.get(p);
            if (tmp == null) return false;
            return tmp.size() < 2;
        }).collect(Collectors.toList());
    }


    /**
     * Get the Pair of Points in Collection(points) which have distance(point A,point B)=MAX
     *
     * @param points Set of points from which the output will be calculated
     * @return Pair of points with max distance to each other
     */
    public static Pair<Point2D, Point2D> getMaxDist(Collection<Point2D> points) {
        //currently just do a stupid O(n²) solution
        Pair<Point2D, Point2D> result = new Pair<>(null, null);
        double maxDist = 0;

        List<Point2D> test = new ArrayList<>(points);
        for (int i = 0; i < test.size(); i++) {
            for (int j = i + 1; j < test.size(); j++) {
                double dist = test.get(i).distanceSq(test.get(j));
                if (dist > maxDist) {
                    result.setKey(test.get(i));
                    result.setValue(test.get(j));
                    if (result.getKey().getX() > result.getValue().getX()) {
                        //swap key and value
                        Point2D tmp = result.getKey();
                        result.setKey(result.getValue());
                        result.setValue(tmp);
                    }
                    maxDist = dist;
                }
            }
        }
        test.clear();

        return result;
    }


    /**
     * Clean up the list of endpoints - it is base on the assumption that a line has only two endpoints
     * and it returns the resulting line from the pair of endpoints which produce the longest path
     *
     * @param endpoints collection of points which contain the endpoints
     * @param line      collection of points which contain the whole line
     * @return returns the longest path on the line from one endpoint to another
     */
    public static List<Point2D> cleanUpEndpoints(Collection<Point2D> endpoints, Collection<Point2D> line) {
        List<List<Point2D>> paths = new ArrayList<>();
        endpoints.forEach(ep -> {
            //iterate the path from endpoint to endpoint and count the points, the longest path will be kept
            List<Point2D> predecessor = new ArrayList<>();
            predecessor.add(ep);
            List<Point2D> path = longestPath(new ArrayList<>(line), predecessor);
            if (path != null) paths.add(path);
        });
        return paths.stream().max(Comparator.comparingInt(List::size)).orElse(null);
    }


    /**
     * Creates and Returns a Map of Neighbor Points Key=Point and Value= a list of its neighbors
     * defined by a distanceSq <=2
     *
     * @param pointsForDistMap collection of points for which the distance map should be created
     * @return Returns a neighbor map of a point and all its neighbors
     */
    public static Map<Point2D, Collection<Point2D>> getNeighborsMap(Collection<Point2D> pointsForDistMap) {
        Map<Point2D, Collection<Point2D>> distanceMapForArea = new HashMap<>();

        //this for loop is faster than the for-each loop below
        List<Point2D> test = new ArrayList<>(pointsForDistMap);
        for (int i = 0; i < pointsForDistMap.size(); i++) {
            for (int j = i + 1; j < pointsForDistMap.size(); j++) {
                double dist = test.get(i).distanceSq(test.get(j));
                if (dist <= 2) {//neighbor
                    if (!distanceMapForArea.containsKey(test.get(i)))
                        distanceMapForArea.put(test.get(i), new ArrayList<>());
                    if (!distanceMapForArea.containsKey(test.get(j)))
                        distanceMapForArea.put(test.get(j), new ArrayList<>());
                    distanceMapForArea.get(test.get(i)).add(test.get(j));
                    distanceMapForArea.get(test.get(j)).add(test.get(i));
                }
            }
        }
        test.clear();
        /*time=System.currentTimeMillis();
        pointsForDistMap.forEach(p1->{
            pointsForDistMap.forEach(p2->{
                double dist = p1.distanceSq(p2);
                if (dist <= 2 && !p1.equals(p2)) {//neighbor
                    if (!distanceMapForArea.containsKey(p1)) distanceMapForArea.put(p1, new ArrayList<>());
                    distanceMapForArea.get(p1).add(p2);
                }
            });
        });
        System.out.println("ForEach-Loop:"+((double)(System.currentTimeMillis()-time)/1000)+"s");*/
        return distanceMapForArea;
    }

    /**
     * Returns a single cluster based on the input collection "current"
     * the cluster contains all neighbors which are connected
     *
     * @param current      current collection of the cluster
     * @param neighborsMap the neighborsMap resulting from clustering
     * @return
     */
    private static Collection<Point2D> getCluster(Collection<Point2D> current, Map<Point2D, Collection<Point2D>> neighborsMap) {
        Collection<Collection<Point2D>> pointsToAdd = new ArrayList<>();
        current.forEach(p -> {
            if (neighborsMap.containsKey(p)) {
                pointsToAdd.add(neighborsMap.get(p));
                neighborsMap.remove(p);
            }
        });
        pointsToAdd.forEach(current::addAll);
        if (pointsToAdd.size() > 0)
            return getCluster(current, neighborsMap);
        return current;
    }

    /**
     * Gets all clusters from given neighborsMap
     * Attention: it changes the input Map
     *
     * @param neighborsMap map containing all neighbors related to a point
     * @return
     */
    public static Collection<Collection<Point2D>> getClusters(Map<Point2D, Collection<Point2D>> neighborsMap) {
        Collection<Collection<Point2D>> clusters = new ArrayList<>();
        List<Point2D> tmp = new ArrayList<>(neighborsMap.keySet());//create this collection because changing a
        //looped collection inside the loop is producing an exception
        tmp.forEach(tmpKey -> {
            if (neighborsMap.containsKey(tmpKey)) {
                //set is used to remove the possibility of adding duplicates
                Collection<Point2D> currentCluster = new HashSet<>(neighborsMap.get(tmpKey));
                neighborsMap.remove(tmpKey);
                clusters.add(getCluster(currentCluster, neighborsMap));
            }
        });

        return clusters;
    }

    /**
     * Not in use, test for a simple "line" clustering method
     *
     * @param neighborsMap
     * @return this method should also work with lines thicker than one pixel
     * first attempt for skeletal lines
     */
    public static Collection<Collection<Point2D>> getLines(Map<Point2D, Collection<Point2D>> neighborsMap) {
        Collection<Collection<Point2D>> lines = new ArrayList<>();
        Map<Point2D, Collection<Point2D>> copy = new HashMap<>(neighborsMap);

        Collection<Collection<Point2D>> clusters = getClusters(copy);//get clusters
        //now inspect clusters for lines: assumption its a list of lines
        //find overlapping lines and separate them

        clusters.parallelStream().forEach(cluster -> {

            Collection<Point2D> endpoints = getEndpoints(neighborsMap, cluster);
            System.out.println("Endpoints:" + endpoints);
            if (endpoints.size() > 3) {//if two lines cross there should be more than 3 endpoint
                //separation needed
                System.out.println("Separation needed:" + cluster);

                //iterate points, check neighborhood, if there are too many direct neighbors choose the one with smaller angle in the next few points

                //think about that part and correct it/implement it
                endpoints.forEach(ep -> {
                    List<Point2D> line = new ArrayList<>();

                    line.add(ep);
                    line.addAll(neighborsMap.get(ep));//since its an endpoint there should only be one added
                    int lSize = 0;
                    do {
                        lSize = line.size();
                        Point2D last = line.get(line.size() - 1);
                        Collection<Point2D> points = neighborsMap.get(last).stream().filter(p -> !line.contains(p)).collect(Collectors.toList());
                        if (points.size() <= 2 && points.contains(last)) {
                            points.stream().filter(p -> !p.equals(last)).findAny().ifPresent(line::add);
                        } else {

                            List<Pair<Point2D, Double>> nextPoints = points.stream().map(p -> new Pair<>(p, getAngleRAD(last, p))).sorted(Comparator.comparingDouble(Pair::getValue)).collect(Collectors.toList());
                            //for now just take the first
                            line.add(nextPoints.get(0).key);

                        }

                    } while (lSize != line.size());


                });


            } else if (endpoints.size() > 2) {//its a fork one line touches the other but does not cross


            }


        });


        return lines;
    }


    /**
     * https://stackoverflow.com/questions/57366574/how-to-calculate-a-point-on-a-rotated-ellipse
     *
     * @param p
     * @param center
     * @param rotation in RAD
     * @return
     */
    public static Point2D getPointRotated(Point2D p, Point2D center, double rotation) {
        double cosA = Math.cos(rotation);
        double sinA = Math.sin(rotation);
        double dx = p.getX() - center.getX();
        double dy = p.getY() - center.getY();
        return new Point2D.Double(center.getX() + dx * cosA - dy * sinA, center.getY() + dx * sinA + dy * cosA);
    }

    /**
     * https://courses.lumenlearning.com/boundless-algebra/chapter/the-circle-and-the-ellipse/
     * https://math.stackexchange.com/questions/22064/calculating-a-point-that-lies-on-an-ellipse-given-an-angle
     *
     * @param center
     * @param majorAxis
     * @param minorAxis
     * @param rotation  in RAD
     */
    public static Polygon getPointsOnEllipse(Point2D center, double majorAxis, double minorAxis, double rotation) {
        //rotation seems to be ok

        final int numberOfPoints = 120;
        double step = 2 * Math.PI / numberOfPoints;

        final double a = (majorAxis) / 2, b = (minorAxis) / 2;

        Polygon polygon = new Polygon();
        polygon.npoints = numberOfPoints;
        polygon.xpoints = new int[numberOfPoints];
        polygon.ypoints = new int[numberOfPoints];

        for (int i = 0; i < numberOfPoints; i++) {
            double currentAngle = i * step;
            double x = -1, y = -1;

            if ((currentAngle >= 0 && currentAngle < Math.PI / 2) || (currentAngle > 3 * Math.PI / 2 && currentAngle <= 2 * Math.PI)) {
                double div = Math.sqrt(Math.pow(b, 2) + Math.pow(a, 2) * Math.pow(Math.tan(currentAngle), 2));
                x = (a * b) / div;
                y = (a * b * Math.tan(currentAngle)) / div;
            } else if (currentAngle > Math.PI / 2 && currentAngle < 3 * Math.PI / 2) {
                double div = Math.sqrt(Math.pow(b, 2) + Math.pow(a, 2) * Math.pow(Math.tan(currentAngle), 2));
                x = -1 * (a * b) / div;
                y = -1 * (a * b * Math.tan(currentAngle)) / div;
            } else if (currentAngle == Math.PI / 2) {
                x = 0;
                y = -b;
            } else if (currentAngle == 3 * Math.PI / 2) {
                x = 0;
                y = b;
            }
            if (x != -1 && y != -1) {
                x += center.getX();
                y += center.getY();
                Point2D point = PointUtils.getPointRotated(new Point2D.Double(x, y), center, rotation);
                polygon.xpoints[i] = (int) Math.round(point.getX());
                polygon.ypoints[i] = (int) Math.round(point.getY());
            }
        }
        return polygon;
    }


    /**
     * Get a List of Lines (represented by Set<Point>) from a given List of Points
     *
     * @param points
     * @return
     * @deprecated its probably not used since getClusters does a similar job
     */
    public static List<Set<Point2D>> getLines(List<Point2D> points) {
        List<Set<Point2D>> listLines = new ArrayList<>();
        while (points.size() > 0) {
            Set<Point2D> base = new HashSet<>();
            base.add(points.get(0));
            points.removeAll(base);
            int sizeBase;
            do {
                sizeBase = base.size();
                Set<Point2D> tmp = new HashSet<>(base);
                base.forEach(p0 -> points.stream().filter(p -> p0.distanceSq(p) <= 2).forEach(tmp::add));
                points.removeAll(tmp);
                base.clear();
                base.addAll(tmp);
            } while (sizeBase != base.size());
            //one line should be finished after this loop
            listLines.add(base);
        }
        return listLines;
    }

    /**
     * Get the longest path from a specified point(content of predecessor) from a "set" of points(points)
     *
     * @param points
     * @param predecessor
     * @return
     */
    public static List<Point2D> longestPath(Collection<Point2D> points, List<Point2D> predecessor) {
        List<Point2D> input = new ArrayList<>(points);//copy input collection to preserve the content
        input.removeAll(predecessor);
        List<Point2D> result = new ArrayList<>(predecessor);

        List<Point2D> tmpResult = input.stream().filter(p2 -> predecessor.get(predecessor.size() - 1).distanceSq(p2) <= 2).collect(Collectors.toList());
        if (tmpResult.size() > 1) {
            //double parameters -> two path's
            List<List<Point2D>> paths = new ArrayList<>();
            tmpResult.forEach(p -> {
                List<Point2D> tmpPred = new ArrayList<>(predecessor);
                tmpPred.add(p);
                paths.add(longestPath(input, tmpPred));
            });
            return paths.stream().max(Comparator.comparingInt(List::size)).orElse(null);
        } else if (tmpResult.size() == 1) {
            predecessor.add(tmpResult.get(0));
            return longestPath(input, predecessor);
        }

        return result;
    }


    public static double getAngleDegree(Point2D source, Point2D target) {
        double angle = Math.toDegrees(Math.atan2(target.getY() - source.getY(), target.getX() - source.getX()));
        if (angle < 0) {
            angle += 360;
        }
        return angle;
    }

    public static double getAngleRAD(Point2D source, Point2D target) {
        double angle = Math.atan2(target.getY() - source.getY(), target.getX() - source.getX());
        if (angle < 0) {
            angle += 2 * Math.PI;
        }
        return angle;
    }

    //#region @source https://www.geeksforgeeks.org/convex-hull-set-1-jarviss-algorithm-or-wrapping/ [2019-10-07]

    // To find orientation of ordered triplet (p, q, r).
    // The function returns following values
    // 0 --> p, q and r are colinear
    // 1 --> Clockwise
    // 2 --> Counterclockwise
    public static int orientation(Point2D p, Point2D q, Point2D r) {
        int val = (int) ((q.getY() - p.getY()) * (r.getX() - q.getX()) -
                (q.getX() - p.getX()) * (r.getY() - q.getY()));

        if (val == 0) return 0;  // collinear
        return (val > 0) ? 1 : 2; // clock or counterclock wise
    }

    /**
     * Calculates and returns the convex hull of a set of points
     *
     * @param points Points for which the convex hull should be calculated
     * @return List of Points of the convex hull (sorted neighbor-wise)
     */
    public static List<Point2D> getConvexHull(List<Point2D> points) {
        int n = points.size();
        // There must be at least 3 points
        if (n < 3) return null;
        // Initialize Result
        List<Point2D> hull = new ArrayList<>();

        // Find the leftmost point
        int l = 0;
        for (int i = 1; i < n; i++)
            if (points.get(i).getX() < points.get(l).getX())
                l = i;
        // Start from leftmost point, keep moving
        // counterclockwise until reach the start point
        // again. This loop runs O(h) times where h is
        // number of points in result or output.
        int p = l, q;
        do {
            // Add current point to result
            hull.add(points.get(p));

            // Search for a point 'q' such that
            // orientation(p, x, q) is counterclockwise
            // for all points 'x'. The idea is to keep
            // track of last visited most counterclock-
            // wise point in q. If any point 'i' is more
            // counterclock-wise than q, then update q.
            q = (p + 1) % n;

            for (int i = 0; i < n; i++) {
                // If i is more counterclockwise than
                // current q, then update q
                if (orientation(points.get(p), points.get(i), points.get(q)) == 2)
                    q = i;
            }
            // Now q is the most counterclockwise with
            // respect to p. Set p as q for next iteration,
            // so that q is added to result 'hull'
            p = q;

        } while (p != l);  // While we don't come to first
        // point
        // Print Result


        //could be used for making sure its the min hull
        /*
        List<Point2D> hullUpdated = new ArrayList<>(hull);
        //test for possible points that could be removed without changing the polygon
        for (int i = 0; i < hull.size(); i++) {
            //line from P(i) to P(i+2) if contains P(i+1) remove P(i+1)
            Point2D a = hull.get(i),
                    b = hull.get((i + 1) % hull.size()),
                    c = hull.get((i + 2) % hull.size());

            if (isBetween(a, b, c)) {//remove P(i+1)
                hullUpdated.remove(hull.get((i + 1) % hull.size()));
            }
        }*/
        return hull;
    }
    //endregion

    // is BC inline with AC or visa-versa
    public static boolean isBetween(Point2D A, Point2D B, Point2D C) {
        // if AC is vertical
        if (A.getX() == C.getX()) return B.getX() == C.getX();
        // if AC is horizontal
        if (A.getY() == C.getY()) return B.getY() == C.getY();
        // match the gradients
        return (A.getX() - C.getX()) * (A.getY() - C.getY()) == (C.getX() - B.getX()) * (C.getY() - B.getY());
    }


    public static List<Point2D> getPointsOfShape(Shape shape) {
        double[] coords = new double[6];
        List<Point2D> points = new ArrayList<>();
        PathIterator pathIterator = shape.getPathIterator(new AffineTransform());
        while (!pathIterator.isDone()) {
            switch (pathIterator.currentSegment(coords)) {
                case PathIterator.SEG_MOVETO:
                case PathIterator.SEG_LINETO:
                    points.add(new Point2D.Double(coords[0], coords[1]));
                    break;
                case PathIterator.SEG_QUADTO:
                    points.add(new Point2D.Double(coords[0], coords[1]));
                    points.add(new Point2D.Double(coords[2], coords[3]));
                    break;
                case PathIterator.SEG_CUBICTO:
                    points.add(new Point2D.Double(coords[0], coords[1]));
                    points.add(new Point2D.Double(coords[2], coords[3]));
                    points.add(new Point2D.Double(coords[4], coords[5]));
                    break;
                case PathIterator.SEG_CLOSE:
                    break;
            }
            pathIterator.next();
        }
        return points;
    }


}
