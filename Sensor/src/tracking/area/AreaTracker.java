package tracking.area;

import core.settings.ATracking;
import core.settings.Settings;
import javafx.util.Pair;
import core.cell.CellShape;
import core.cell.ShapeContainer;
import core.image.ImageWrapper;
import tracking.filament.DynamicFilament;
import util.Annotations;

import java.awt.geom.Rectangle2D;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Consumer;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

/**
 * 2020-12-21 Seems to work with current data-set,
 * quality highly dependant to the quality of area calculation
 */
public class AreaTracker {

    private List<DynamicArea> uniqueAreas;

    private Map<Pair<CellShape, CellShape>, Double> matchingMap;

    public AreaTracker() {
        uniqueAreas = new ArrayList<>();
        matchingMap = new ConcurrentHashMap<>();
    }

    public List<DynamicArea> getUniqueAreas() {
        return uniqueAreas;
    }

    protected void setUniqueAreas(List<DynamicArea> areas) {
        uniqueAreas = areas;
    }


    public List<DynamicArea> filterUniqueAreas(boolean chkMinLength, int minLength, boolean chkMaxLength, int maxLength) {
        List<DynamicArea> result = uniqueAreas;
        if (chkMinLength) {
            result = result.stream().filter(df -> df.getAreas().size() >= minLength).collect(Collectors.toList());
        }
        if (chkMaxLength) {
            result = result.stream().filter(df -> df.getAreas().size() <= maxLength).collect(Collectors.toList());
        }
        return result;
    }


    private double getMatchingScore(CellShape t, CellShape t1) {
        //simple matching just check bounding boxes intersection area
        Rectangle2D intersection = t.getBounds().createIntersection(t1.getBounds());


        double a1 = t.getBounds().getWidth() * t.getBounds().getHeight();
        double a2 = t1.getBounds().getWidth() * t1.getBounds().getHeight();
        double a = Math.min(a1, a2);//take the smaller area for comparison
        double area = (intersection.getWidth() * intersection.getHeight()) / (a);
        if (area > 1) area = 1 / area;
        return area;

        //return intersection.getWidth() * intersection.getHeight();
    }


    private void createMatchingMap(int maxTime, ImageWrapper wrapper, Settings dp, @Annotations.Nullable Consumer<Integer> progressReporter, int currentProgress) {
        //no error handling currently

        float singleStep = ((float) 50) / maxTime;
        AtomicInteger cnt = new AtomicInteger(0);

        IntStream.range(0, maxTime).parallel().forEach(time -> {
            if (time + 1 < maxTime) {
                ShapeContainer containerT = wrapper.getEntryList().get(time).getShape();
                ShapeContainer containerTp1 = wrapper.getEntryList().get(time + 1).getShape();
                List<CellShape> areasT = containerT.getAreas();
                List<CellShape> areasTp1 = containerTp1.getAreas();

                for (CellShape areaT : areasT) {
                    for (CellShape areaTp1 : areasTp1) {
                        matchingMap.put(new Pair<>(areaT, areaTp1), getMatchingScore(areaT, areaTp1));
                    }
                }
            }
            int cntI = cnt.incrementAndGet();
            if (cntI * singleStep % 5 == 0 && progressReporter != null)
                progressReporter.accept((int) (currentProgress + (cntI * singleStep)));
        });


    }


    protected void handlePairsSizeZero(final int time, final CellShape areaNow) {
        //no previous match, probably new area or faulty area detection
        DynamicArea newCell = new DynamicArea();
        newCell.getAreas().put(time, new CellEvent.CellStartEvent(areaNow));
        getUniqueAreas().add(newCell);
    }

    /**
     * One-One Match, CellAliveEvent
     *
     * @param time
     * @param areaNow
     * @param predecessor
     */
    protected void handlePairsSizeOne(final int time, final CellShape areaNow, final CellShape predecessor) {
        getUniqueAreas().stream().filter(d -> d.getContained(predecessor).size() > 0 &&
                d.getContained(predecessor).stream().
                        anyMatch(e -> !(e instanceof CellEvent.CellFusionEvent) &&
                                !(e instanceof CellEvent.CellSplitEvent))).findFirst().
                ifPresent(dynamicArea -> dynamicArea.getAreas().
                        put(time, new CellEvent.CellAliveEvent(predecessor, areaNow)));

    }

    protected void handlePairsSizeGreaterOne(final int time, final CellShape areaNow, List<CellShape> sources) {
        //pairs.size()>1
        //more than 1 previous area's match the current area which is a fusion/touch

        //get all life-lines matching, except fusion and split
        List<DynamicArea> dynAreas = sources.stream().
                map(p -> getUniqueAreas().stream().
                        filter(d -> d.getContained(p).size() > 0 && d.getContained(p).stream()
                                .anyMatch(e -> !(e instanceof CellEvent.CellFusionEvent) &&
                                        !(e instanceof CellEvent.CellSplitEvent))).findAny().
                        orElse(null)).
                filter(Objects::nonNull).collect(Collectors.toList());

        //on fusion or split create a new dynamic area the old one ends with cellFusion
        //and the new one starts with cellstart
        DynamicArea fused = new DynamicArea();
        fused.getAreas().put(time, new CellEvent.CellStartEvent(areaNow));
        getUniqueAreas().add(fused);

        dynAreas.forEach(dynArea -> {
            if (dynArea.getAreas().containsKey(time)) {//this case should not exist
                //already contains key... handle
            } else {
                dynArea.getAreas().put(time, new CellEvent.CellFusionEvent(sources, areaNow));
                dynArea.setRelated(fused);
            }
        });

    }


    public void process(ImageWrapper wrapper, Settings dp, @Annotations.Nullable Consumer<Integer> progressReporter) {

        int maxTime = wrapper.getSize();
        final double intersectTolerance = dp.getValueAsDouble(ATracking.intersectTolerance);

        uniqueAreas.clear();

        AtomicInteger progress = new AtomicInteger(0);

        //create a matching map Map<Pair<CellShape,CellShape>,Double> for each pair of CellShape's
        //only time and time+1 comparisons each area of time gets compared to each area of time+1
        //this part could be done in parallel the resulting map will be used to get the best matches and create the
        //DynamicArea object
        createMatchingMap(maxTime, wrapper, dp, progressReporter, 10);

        //has to be matched with previous cell, since we only know cell t-1


        float singleStep = ((float) 40) / maxTime;

        IntStream.range(0, maxTime).forEach(time -> {
            int cntI = progress.incrementAndGet();
            if (cntI * singleStep % 5 == 0 && progressReporter != null)
                progressReporter.accept((int) (60 + (cntI * singleStep)));
            if (time == 0) {
                //init
                ShapeContainer containerT = wrapper.getEntryList().get(time).getShape();
                List<CellShape> areasT = containerT.getAreas();
                areasT.forEach(area -> {
                    DynamicArea dynArea = new DynamicArea();
                    dynArea.setBirth(0);
                    dynArea.getAreas().put(0, new CellEvent.CellStartEvent(area));
                    uniqueAreas.add(dynArea);
                });
            } else {
                ShapeContainer containerNow = wrapper.getEntryList().get(time).getShape();
                List<CellShape> areasNow = containerNow.getAreas();

                //Retrieve all current areas with their matching predecessors
                //Map<AreaNow,List<Pair<AreaNow,List<Areas Matching>>>>
                Map<CellShape, List<Pair<CellShape, List<CellShape>>>> results = areasNow.stream().map(areaNow ->
                        new Pair<>(areaNow,
                                matchingMap.entrySet().stream().
                                        filter(e -> e.getValue() > intersectTolerance && e.getKey().getValue().
                                                equals(areaNow)).map(e -> e.getKey().getKey()).
                                        collect(Collectors.toList())
                        )
                ).collect(Collectors.groupingBy(Pair::getKey));


                //gather all cells having the same predecessor (cell split)
                //2020-12-09 19:41 seems to work
                //Map<PreCell,List<Pair<AreaNow,PreCell>>>
                Map<CellShape, List<Pair<CellShape, CellShape>>> splitCells = results.values().stream().flatMap(Collection::stream).map(result -> {
                    List<CellShape> other = result.getValue();
                    return results.values().stream().flatMap(Collection::stream).filter(p -> p.getValue() != other).
                            flatMap(p -> p.getValue().stream().
                                    map(pi -> new Pair<>(p.getKey(), pi))).
                            filter(p -> other.contains(p.getValue())).collect(Collectors.toList());
                }).flatMap(Collection::stream).collect(Collectors.groupingBy(Pair::getValue));

                //remove the possible splits from result map (they will be processed differently)
                splitCells.values().stream().flatMap(p -> p.stream().map(Pair::getKey)).forEach(results::remove);

                splitCells.forEach((key, value) -> {
                    //get the dynamic areas
                    DynamicArea dynamicArea = getUniqueAreas().stream().filter(d -> d.getContained(key).size() > 0 &&
                            d.getContained(key).stream().
                                    anyMatch(e -> !(e instanceof CellEvent.CellFusionEvent) &&
                                            !(e instanceof CellEvent.CellSplitEvent))).findFirst().orElse(null);
                    if (dynamicArea != null) {
                        //create the split event in old time line
                        dynamicArea.getAreas().put(time,
                                new CellEvent.CellSplitEvent(key,
                                        value.stream().map(Pair::getKey).collect(Collectors.toList())));
                        //create new time lines for each splitted area
                        value.stream().map(Pair::getKey).forEach(cell -> {
                            DynamicArea tmp = new DynamicArea();
                            tmp.getAreas().put(time, new CellEvent.CellStartEvent(cell));
                            //set relation to concatenate life lines
                            tmp.setRelated(dynamicArea);
                            getUniqueAreas().add(tmp);
                        });
                    }
                });

                results.forEach((areaNow, value) -> {
                    //List<AreaNow,List<Matching Areas>>
                    List<CellShape> pairs = value.stream().flatMap(p -> p.getValue().stream()).collect(Collectors.toList());
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
        postProcessing();
        if (progressReporter != null) progressReporter.accept(100);
        //empty matching map
        matchingMap.clear();
    }

    /**
     * This method changes fusion/touch and split/detouch events and connects life lines
     * it also sets parameters like birth, death etc.
     */
    protected void postProcessing() {

        List<DynamicArea> postProcessedAreas = new ArrayList<>(getUniqueAreas());

        //check all life line "endings" for split/fusion, check connected life lines
        getUniqueAreas().stream().filter(d -> {
            int maxTime = d.getAreas().keySet().stream().mapToInt(i -> i).max().orElse(-1);
            if (maxTime != -1) {
                return d.getAreas().get(maxTime) instanceof CellEvent.CellFusionEvent;
            }
            return false;
        }).forEach(d -> {
            //only life lines which end with fusion
            //a touch starts with fusion and ends with split, we want the starting point so we start with the fusion
            //real fusions don't happen that often and can be ignored for now
            //means all fusions are touch
            int maxTimeD = d.getAreas().keySet().stream().mapToInt(i -> i).max().orElse(-1);
            DynamicArea related = d.getRelated();//the life line with the possible split event
            if (related != null) {
                int maxTimeRelated = related.getAreas().keySet().stream().mapToInt(i -> i).max().orElse(-1);
                if (maxTimeRelated != -1 && related.getAreas().get(maxTimeRelated) instanceof CellEvent.CellSplitEvent) {
                    postProcessedAreas.remove(related);//remove the fused life line from postProcessed Areas

                    //found a split event to the fusion -> most likely touch/detouch
                    //the life lines should be connected

                    //in here the life line of the splitted cells have to be found
                    //getRelated==related can be the source of the touch/detouch line or the result(result of split)
                    CellEvent.CellSplitEvent split = (CellEvent.CellSplitEvent) related.getAreas().get(maxTimeRelated);

                    //this should be the life lines of the detouch event
                    List<DynamicArea> detouchLines = postProcessedAreas.stream().
                            filter(dynamicArea -> dynamicArea.getRelated() == related &&
                                    dynamicArea.getAreas().containsKey(maxTimeRelated) &&
                                    dynamicArea.getAreas().get(maxTimeRelated) instanceof CellEvent.CellStartEvent).
                            collect(Collectors.toList());

                    //get the best match for the original life line and combine the life lines
                    CellEvent.CellFusionEvent fusion = (CellEvent.CellFusionEvent) d.getAreas().get(maxTimeD);

                    //should contain all matches of the source shapes with target shapes
                    Map<Pair<CellShape, CellShape>, Double> relationMap = new HashMap<>();

                    CellEvent sourceEvent = d.getAreas().get(maxTimeD - 1);
                    CellShape tmpSourceShape = null;
                    if (sourceEvent instanceof CellEvent.CellAliveEvent) {
                        tmpSourceShape = ((CellEvent.CellAliveEvent) sourceEvent).getTarget();
                    } else if (sourceEvent instanceof CellEvent.CellStartEvent) {
                        tmpSourceShape = ((CellEvent.CellStartEvent) sourceEvent).getSource();
                    }
                    if (tmpSourceShape != null) {
                        final CellShape sourceShape = tmpSourceShape;
                        /*split.getTarget().forEach(shapeTarget->
                                relationMap.put(new Pair<>(sourceShape,shapeTarget),
                                        getMatchingScore(sourceShape,shapeTarget)));*/

                        fusion.getSource().forEach(shapeSource ->
                                split.getTarget().forEach(shapeTarget ->
                                        relationMap.put(new Pair<>(shapeSource, shapeTarget),
                                                getMatchingScore(shapeSource, shapeTarget))));


                        //get disjunct combinations of the pairs, sum the matching and take the one with max sum
                        //requirement each combination value has to be larger than 0
                        List<List<Pair<CellShape, CellShape>>> sortedPairs = relationMap.entrySet().stream().filter(entry -> entry.getValue() > 0).map(entry ->
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
                                    List<Pair<CellShape, CellShape>> pairs = entry.getValue().stream().map(Pair::getKey).
                                            collect(Collectors.toList());
                                    pairs.add(entry.getKey().getKey());
                                    return pairs;
                                }).collect(Collectors.toList());
                        //pair.getKey() --- fusion, pair.getValue()  --- split
                        List<Pair<CellShape, CellShape>> highestScore = sortedPairs.get(sortedPairs.size() - 1);
                        highestScore.forEach(pair -> {
                            if (pair.getKey() == sourceShape) {
                                //stitch life lines
                                related.getAreas().keySet().forEach(key -> {
                                    //we do not need to copy the starting point
                                    //copy all other events to the source
                                    //the fusion part will be in all cells that has been fused
                                    if (!d.getAreas().containsKey(key)) {
                                        d.getAreas().put(key, related.getAreas().get(key));
                                    } else {
                                        //starting point the "fusion" should be changed to touch event
                                        CellEvent.CellFusionEvent eventFusion = (CellEvent.CellFusionEvent) d.getAreas().get(key);
                                        d.getAreas().put(key, new CellEvent.CellTouchEvent(eventFusion.getSource(), eventFusion.getTarget()));
                                    }
                                });
                                d.setRelated(null);
                                DynamicArea splittedMatch = detouchLines.stream().filter(detouchArea -> detouchArea.contains(pair.getValue())).findAny().orElse(null);
                                if (splittedMatch != null) {
                                    //add this life line to original
                                    splittedMatch.getAreas().keySet().forEach(key -> {
                                        //we do not need to copy the starting point
                                        //copy all other events to the source
                                        //the fusion part will be in all cells that has been fused
                                        if (!d.getAreas().containsKey(key)) {
                                            d.getAreas().put(key, splittedMatch.getAreas().get(key));
                                        } else {
                                            //starting point the "split" should be changed to detouch event
                                            CellEvent.CellSplitEvent eventSplit = (CellEvent.CellSplitEvent) d.getAreas().get(key);
                                            d.getAreas().put(key, new CellEvent.CellDeTouchEvent(eventSplit.getSource(), eventSplit.getTarget()));

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

            int maxTime = dynamicArea.getAreas().keySet().stream().mapToInt(i -> i).max().orElse(-1);
            int minTime = dynamicArea.getAreas().keySet().stream().mapToInt(i -> i).min().orElse(-1);
            dynamicArea.setBirth(0);
            dynamicArea.setDeath(0);
            if (maxTime != -1) {
                CellEvent event = dynamicArea.getAreas().get(maxTime);
                //maybe remove the cellendevent and let it stay as a cellaliveevent
                if (event instanceof CellEvent.CellAliveEvent) {
                    //change to CellEndEvent
                    dynamicArea.getAreas().put(maxTime, new CellEvent.CellEndEvent(((CellEvent.CellAliveEvent) event).getTarget()));
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

        setUniqueAreas(postProcessedAreas);
    }

}
