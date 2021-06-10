package tracking.shape;

import core.settings.ATracking;
import core.settings.Settings;
import javafx.util.Pair;
import util.Annotations;
import util.PointUtils;

import java.awt.*;
import java.awt.geom.Area;
import java.awt.geom.Point2D;
import java.util.*;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Consumer;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

/**
 * 2021-05-28 seems to work for CellShape
 *
 * @param <T>
 * @todo implement FocalAdhesion as TrackingShape and Test it too.
 */
public class ShapeTracker<T extends TrackingShape> {

    private List<TimeTable<T>> uniqueShapes;

    private Map<Pair<T, T>, Double> matchingMap;

    public ShapeTracker() {
        uniqueShapes = new ArrayList<>();
        matchingMap = new ConcurrentHashMap<>();
    }

    public List<TimeTable<T>> getUniqueShapes() {
        return uniqueShapes;
    }

    protected void setUniqueShapes(List<TimeTable<T>> shapes) {
        uniqueShapes = shapes;
    }


    public List<TimeTable<T>> filterUniqueShapes(boolean chkMinLength, int minLength, boolean chkMaxLength, int maxLength) {
        List<TimeTable<T>> result = uniqueShapes;
        if (chkMinLength) {
            result = result.stream().filter(df -> df.getShapes().size() >= minLength).collect(Collectors.toList());
        }
        if (chkMaxLength) {
            result = result.stream().filter(df -> df.getShapes().size() <= maxLength).collect(Collectors.toList());
        }
        return result;
    }


    /**
     * https://stackoverflow.com/questions/25987465/computing-area-of-a-polygon-in-java
     *
     * @param shape
     * @return
     */
    private double calculateArea(Shape shape) {
        List<Point2D> points = PointUtils.getPointsOfShape(shape);

        double sum = 0;
        for (int i = 0; i < points.size(); i++) {
            if (i == 0) {
                sum += points.get(i).getX() * (points.get(i + 1).getY() - points.get(points.size() - 1).getY());
            } else if (i == points.size() - 1) {
                sum += points.get(i).getX() * (points.get(0).getY() - points.get(i - 1).getY());
            } else {
                sum += points.get(i).getX() * (points.get(i + 1).getY() - points.get(i - 1).getY());
            }
        }
        return 0.5 * Math.abs(sum);
    }

    private double getMatchingScore(T t, T t1, Settings settings) {
        //simple matching just check bounding boxes intersection area
        final Shape boundsT = t.getBounds(settings);
        final Shape boundsT1 = t1.getBounds(settings);


        Area areaT = new Area(boundsT);
        //create intersection area
        areaT.intersect(new Area(boundsT1));

        double intersectionArea = calculateArea(areaT);
        //be sure to use the area of the shape as comparison and not the actual area
        final double aT = calculateArea(boundsT);
        final double aT1 = calculateArea(boundsT1);

        double a = Math.min(aT, aT1);//take the smaller area for comparison
        double area = (intersectionArea) / (a);
        if (area > 1) area = 1 / area;
        return area;

        //return intersection.getWidth() * intersection.getHeight();
    }


    private void createMatchingMap(int maxTime, List<List<T>> entries, Settings dp, @Annotations.Nullable Consumer<Integer> progressReporter, int currentProgress) {
        //no error handling currently
        float singleStep = ((float) 50) / maxTime;
        AtomicInteger cnt = new AtomicInteger(0);

        IntStream.range(0, maxTime).parallel().forEach(time -> {
            if (time + 1 < maxTime) {
                List<T> shapesT = entries.get(time);
                List<T> shapesTp1 = entries.get(time + 1);

                for (T shapeT : shapesT) {
                    for (T shapeTp1 : shapesTp1) {
                        matchingMap.put(new Pair<>(shapeT, shapeTp1), getMatchingScore(shapeT, shapeTp1, dp));
                    }
                }
            }
            int cntI = cnt.incrementAndGet();
            if (cntI * singleStep % 5 == 0 && progressReporter != null)
                progressReporter.accept((int) (currentProgress + (cntI * singleStep)));
        });
    }


    protected void handlePairsSizeZero(final int time, final T shapeNow) {
        //no previous match, probably new area or faulty area detection
        TimeTable<T> newCell = new TimeTable<>();
        newCell.getShapes().put(time, new ShapeEvent.ShapeStartEvent<>(shapeNow));
        getUniqueShapes().add(newCell);
    }

    /**
     * One-One Match, CellAliveEvent
     *
     * @param time
     * @param areaNow
     * @param predecessor
     */
    protected void handlePairsSizeOne(final int time, final T areaNow, final T predecessor) {
        getUniqueShapes().stream().filter(d -> d.getContained(predecessor).size() > 0 &&
                d.getContained(predecessor).stream().
                        anyMatch(e -> !(e instanceof ShapeEvent.ShapeFusionEvent) &&
                                !(e instanceof ShapeEvent.ShapeSplitEvent))).findFirst().
                ifPresent(timeTable -> timeTable.getShapes().
                        put(time, new ShapeEvent.ShapeAliveEvent<>(predecessor, areaNow)));

    }

    protected void handlePairsSizeGreaterOne(final int time, final T areaNow, List<T> sources) {
        //pairs.size()>1
        //more than 1 previous area's match the current area which is a fusion/touch

        //get all life-lines matching, except fusion and split
        List<TimeTable<T>> dynAreas = sources.stream().
                map(p -> getUniqueShapes().stream().
                        filter(d -> d.getContained(p).size() > 0 && d.getContained(p).stream()
                                .anyMatch(e -> !(e instanceof ShapeEvent.ShapeFusionEvent) &&
                                        !(e instanceof ShapeEvent.ShapeSplitEvent))).findAny().
                        orElse(null)).
                filter(Objects::nonNull).collect(Collectors.toList());

        //on fusion or split create a new dynamic area the old one ends with cellFusion
        //and the new one starts with cellstart
        TimeTable<T> fused = new TimeTable<>();
        fused.getShapes().put(time, new ShapeEvent.ShapeStartEvent<>(areaNow));
        getUniqueShapes().add(fused);

        dynAreas.forEach(dynArea -> {
            if (dynArea.getShapes().containsKey(time)) {//this case should not exist
                //already contains key... handle
            } else {
                dynArea.getShapes().put(time, new ShapeEvent.ShapeFusionEvent<>(sources, areaNow));
                dynArea.setRelated(fused);
            }
        });

    }


    public void process(List<List<T>> shapesPerTime, Settings dp, @Annotations.Nullable Consumer<Integer> progressReporter) {

        int maxTime = shapesPerTime.size();
        final double intersectTolerance = dp.getValueAsDouble(ATracking.intersectTolerance);

        uniqueShapes.clear();

        AtomicInteger progress = new AtomicInteger(0);

        //create a matching map Map<Pair<Shape,Shape>,Double> for each pair of Shape
        //only time and time+1 comparisons each area of time gets compared to each area of time+1
        //this part could be done in parallel the resulting map will be used to get the best matches and create the
        //TimeTable object
        createMatchingMap(maxTime, shapesPerTime, dp, progressReporter, 10);

        //has to be matched with previous cell, since we only know cell t-1


        float singleStep = ((float) 40) / maxTime;

        IntStream.range(0, maxTime).forEach(time -> {
            int cntI = progress.incrementAndGet();
            if (cntI * singleStep % 5 == 0 && progressReporter != null)
                progressReporter.accept((int) (60 + (cntI * singleStep)));
            if (time == 0) {
                //init
                List<T> shapesT = shapesPerTime.get(time);
                shapesT.forEach(area -> {
                    TimeTable<T> timeTable = new TimeTable<>();
                    timeTable.setBirth(0);
                    timeTable.getShapes().put(0, new ShapeEvent.ShapeStartEvent<>(area));
                    uniqueShapes.add(timeTable);
                });
            } else {
                List<T> areasNow = shapesPerTime.get(time);
                //Retrieve all current areas with their matching predecessors
                //Map<AreaNow,List<Pair<AreaNow,List<Areas Matching>>>>
                Map<T, List<Pair<T, List<T>>>> results = areasNow.stream().map(shapeNow ->
                        new Pair<>(shapeNow,
                                matchingMap.entrySet().stream().
                                        filter(e -> e.getValue() > intersectTolerance && e.getKey().getValue().
                                                equals(shapeNow)).map(e -> e.getKey().getKey()).
                                        collect(Collectors.toList())
                        )
                ).collect(Collectors.groupingBy(Pair::getKey));


                //gather all cells having the same predecessor (cell split)
                //2020-12-09 19:41 seems to work
                //Map<PreCell,List<Pair<AreaNow,PreCell>>>
                Map<T, List<Pair<T, T>>> splitCells = results.values().stream().flatMap(Collection::stream).map(result -> {
                    List<T> other = result.getValue();
                    return results.values().stream().flatMap(Collection::stream).filter(p -> p.getValue() != other).
                            flatMap(p -> p.getValue().stream().
                                    map(pi -> new Pair<>(p.getKey(), pi))).
                            filter(p -> other.contains(p.getValue())).collect(Collectors.toList());
                }).flatMap(Collection::stream).collect(Collectors.groupingBy(Pair::getValue));

                //remove the possible splits from result map (they will be processed differently)
                splitCells.values().stream().flatMap(p -> p.stream().map(Pair::getKey)).forEach(results::remove);

                splitCells.forEach((key, value) -> {
                    //get the dynamic areas
                    TimeTable<T> timeTable = getUniqueShapes().stream().filter(d -> d.getContained(key).size() > 0 &&
                            d.getContained(key).stream().
                                    anyMatch(e -> !(e instanceof ShapeEvent.ShapeFusionEvent) &&
                                            !(e instanceof ShapeEvent.ShapeSplitEvent))).findFirst().orElse(null);
                    if (timeTable != null) {
                        //create the split event in old time line
                        timeTable.getShapes().put(time,
                                new ShapeEvent.ShapeSplitEvent<>(key,
                                        value.stream().map(Pair::getKey).collect(Collectors.toList())));
                        //create new time lines for each splitted area
                        value.stream().map(Pair::getKey).forEach(cell -> {
                            TimeTable<T> tmp = new TimeTable<>();
                            tmp.getShapes().put(time, new ShapeEvent.ShapeStartEvent<>(cell));
                            //set relation to concatenate life lines
                            tmp.setRelated(timeTable);
                            getUniqueShapes().add(tmp);
                        });
                    }
                });

                results.forEach((areaNow, value) -> {
                    //List<AreaNow,List<Matching Areas>>
                    List<T> pairs = value.stream().flatMap(p -> p.getValue().stream()).collect(Collectors.toList());
                    //touch/detouch is handled via post processing
                    if (pairs.size() == 0) {
                        handlePairsSizeZero(time, areaNow);
                    } else if (pairs.size() == 1) {
                        handlePairsSizeOne(time, areaNow, pairs.get(0));
                    } else {
                        handlePairsSizeGreaterOne(time, areaNow, pairs);
                    }
                });
            }
        });


        //after first processing step add a post processing which checks all fusion and split,
        //switches with touch de-touch, add cellEnd events and merge life-lines which belong together
        postProcessing(dp);
        if (progressReporter != null) progressReporter.accept(100);
        //empty matching map
        matchingMap.clear();
    }

    /**
     * This method changes fusion/touch and split/detouch events and connects life lines
     * it also sets parameters like birth, death etc.
     */
    protected void postProcessing(Settings dp) {

        List<TimeTable<T>> postProcessedAreas = new ArrayList<>(getUniqueShapes());

        //check all life line "endings" for split/fusion, check connected life lines
        getUniqueShapes().stream().filter(d -> {
            int maxTime = d.getShapes().keySet().stream().mapToInt(i -> i).max().orElse(-1);
            if (maxTime != -1) {
                return d.getShapes().get(maxTime) instanceof ShapeEvent.ShapeFusionEvent;
            }
            return false;
        }).forEach(d -> {
            //only life lines which end with fusion
            //a touch starts with fusion and ends with split, we want the starting point so we start with the fusion
            //real fusions don't happen that often and can be ignored for now
            //means all fusions are touch
            int maxTimeD = d.getShapes().keySet().stream().mapToInt(i -> i).max().orElse(-1);
            TimeTable<T> related = d.getRelated();//the life line with the possible split event
            if (related != null) {
                int maxTimeRelated = related.getShapes().keySet().stream().mapToInt(i -> i).max().orElse(-1);
                if (maxTimeRelated != -1 && related.getShapes().get(maxTimeRelated) instanceof ShapeEvent.ShapeSplitEvent) {
                    postProcessedAreas.remove(related);//remove the fused life line from postProcessed Areas

                    //found a split event to the fusion -> most likely touch/detouch
                    //the life lines should be connected

                    //in here the life line of the splitted cells have to be found
                    //getRelated==related can be the source of the touch/detouch line or the result(result of split)
                    ShapeEvent.ShapeSplitEvent<T> split = (ShapeEvent.ShapeSplitEvent<T>) related.getShapes().get(maxTimeRelated);

                    //this should be the life lines of the detouch event
                    List<TimeTable<T>> detouchLines = postProcessedAreas.stream().
                            filter(dynamicArea -> dynamicArea.getRelated() == related &&
                                    dynamicArea.getShapes().containsKey(maxTimeRelated) &&
                                    dynamicArea.getShapes().get(maxTimeRelated) instanceof ShapeEvent.ShapeStartEvent).
                            collect(Collectors.toList());

                    //get the best match for the original life line and combine the life lines
                    ShapeEvent.ShapeFusionEvent<T> fusion = (ShapeEvent.ShapeFusionEvent<T>) d.getShapes().get(maxTimeD);

                    //should contain all matches of the source shapes with target shapes
                    Map<Pair<T, T>, Double> relationMap = new HashMap<>();

                    ShapeEvent<T> sourceEvent = d.getShapes().get(maxTimeD - 1);
                    T tmpSourceShape = null;
                    if (sourceEvent instanceof ShapeEvent.ShapeAliveEvent) {
                        tmpSourceShape = ((ShapeEvent.ShapeAliveEvent<T>) sourceEvent).getTarget();
                    } else if (sourceEvent instanceof ShapeEvent.ShapeStartEvent) {
                        tmpSourceShape = ((ShapeEvent.ShapeStartEvent<T>) sourceEvent).getSource();
                    }
                    if (tmpSourceShape != null) {
                        final T sourceShape = tmpSourceShape;
                        fusion.getSource().forEach(shapeSource ->
                                split.getTarget().forEach(shapeTarget ->
                                        relationMap.put(new Pair<>(shapeSource, shapeTarget),
                                                getMatchingScore(shapeSource, shapeTarget, dp))));

                        //get disjunct combinations of the pairs, sum the matching and take the one with max sum
                        //requirement each combination value has to be larger than 0
                        List<List<Pair<T, T>>> sortedPairs = relationMap.entrySet().stream().filter(entry -> entry.getValue() > 0).map(entry ->
                                new Pair<>(new Pair<>(entry.getKey(), entry.getValue()),
                                        relationMap.entrySet().stream().filter(entry2 -> entry2.getValue() > 0 &&
                                                entry2.getKey().getKey() != entry.getKey().getKey() &&
                                                entry2.getKey().getKey() != entry.getKey().getValue() &&
                                                entry2.getKey().getValue() != entry.getKey().getValue() &&
                                                entry2.getKey().getValue() != entry.getKey().getValue()
                                        ).map(entry2 -> new Pair<>(entry2.getKey(), entry2.getValue())).collect(Collectors.toList()))
                        ).filter(entry2 -> entry2.getValue().size() > 0).
                                sorted(Comparator.comparingDouble(e -> e.getKey().getValue() + e.getValue().stream().
                                        mapToDouble(Pair::getValue).sum())).
                                map(entry -> {
                                    List<Pair<T, T>> pairs = entry.getValue().stream().map(Pair::getKey).
                                            collect(Collectors.toList());
                                    pairs.add(entry.getKey().getKey());
                                    return pairs;
                                }).collect(Collectors.toList());
                        //pair.getKey() --- fusion, pair.getValue()  --- split
                        List<Pair<T, T>> highestScore = sortedPairs.get(sortedPairs.size() - 1);
                        highestScore.forEach(pair -> {
                            if (pair.getKey() == sourceShape) {
                                //stitch life lines
                                related.getShapes().keySet().forEach(key -> {
                                    //we do not need to copy the starting point
                                    //copy all other events to the source
                                    //the fusion part will be in all cells that has been fused
                                    if (!d.getShapes().containsKey(key)) {
                                        d.getShapes().put(key, related.getShapes().get(key));
                                    } else {
                                        //starting point the "fusion" should be changed to touch event
                                        ShapeEvent.ShapeFusionEvent<T> eventFusion = (ShapeEvent.ShapeFusionEvent<T>) d.getShapes().get(key);
                                        d.getShapes().put(key, new ShapeEvent.ShapeTouchEvent<>(eventFusion.getSource(), eventFusion.getTarget()));
                                    }
                                });
                                d.setRelated(null);
                                TimeTable<T> splittedMatch = detouchLines.stream().filter(detouchArea -> detouchArea.contains(pair.getValue())).findAny().orElse(null);
                                if (splittedMatch != null) {
                                    //add this life line to original
                                    splittedMatch.getShapes().keySet().forEach(key -> {
                                        //we do not need to copy the starting point
                                        //copy all other events to the source
                                        //the fusion part will be in all cells that has been fused
                                        if (!d.getShapes().containsKey(key)) {
                                            d.getShapes().put(key, splittedMatch.getShapes().get(key));
                                        } else {
                                            //starting point the "split" should be changed to detouch event
                                            ShapeEvent.ShapeSplitEvent<T> eventSplit = (ShapeEvent.ShapeSplitEvent<T>) d.getShapes().get(key);
                                            d.getShapes().put(key, new ShapeEvent.ShapeDeTouchEvent<>(eventSplit.getSource(), eventSplit.getTarget()));

                                        }
                                    });
                                    splittedMatch.setRelated(null);
                                    postProcessedAreas.remove(splittedMatch);
                                }

                            }
                        });
                    }
                }
            }
        });

        //set parameters in dynamic areas  (birth, death etc.)
        AtomicInteger counter = new AtomicInteger(0);
        postProcessedAreas.forEach(dynamicArea -> {
            dynamicArea.setIdentifier(counter.getAndIncrement());

            int maxTime = dynamicArea.getShapes().keySet().stream().mapToInt(i -> i).max().orElse(-1);
            int minTime = dynamicArea.getShapes().keySet().stream().mapToInt(i -> i).min().orElse(-1);
            dynamicArea.setBirth(0);
            dynamicArea.setDeath(0);
            if (maxTime != -1) {
                ShapeEvent<T> event = dynamicArea.getShapes().get(maxTime);
                //maybe remove the cellendevent and let it stay as a cellaliveevent
                if (event instanceof ShapeEvent.ShapeAliveEvent) {
                    //change to CellEndEvent
                    dynamicArea.getShapes().put(maxTime, new ShapeEvent.ShapeEndEvent<T>(((ShapeEvent.ShapeAliveEvent<T>) event).getTarget()));
                }
                dynamicArea.setDeath(maxTime);
            }
            if (minTime != -1) {
                dynamicArea.setBirth(minTime);
            }
            dynamicArea.setLength((dynamicArea.getDeath() - dynamicArea.getBirth()) + 1);//+1 because see below
            //for example: birth=0 death=4 which means it exists 5 time units (the "death" time still includes the object)
            dynamicArea.updateIdentifiers();

        });

        setUniqueShapes(postProcessedAreas);
    }


}
