//BoardPatterns.json has all of the board patterns which can appear in the game.
//
//each board pattern has an id, list of tags, and lines which represent tiles
/*
"tags":["dropper","score_pit","below_dropper","above_pit","standard","sparse"],
	"board_patterns":[
		{
			"id":0,
			"tags":["dropper"],
			"line1":"|||-|",
			"line2":"|---|"
		}, {
			"id":1,
			"tags":["score_pit"],
			"line1":"|---|",
			"line2":"|||||"
		}, {
 */

package main.java.plinko.game;

import java.io.IOException;
import main.java.plinko.Exceptions.TagFormatException;

import java.io.Serializable;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;

import main.java.plinko.model.BoardPattern;
import main.java.plinko.model.PatternTag;
import org.json.*;

public class BoardPatternGenerator  implements Serializable {

    private final HashMap<PatternTag, List<BoardPattern>> patternMap; //stores all pattern templates which can be generated

    //Instantiates a new board pattern generator from a json file
    public BoardPatternGenerator(String path) throws IOException {
        patternMap = readPatternFile(path); //load all pattern templates
    }

    //Generates a new random board pattern which has one of the given tag(s)
    public BoardPattern genRandomPattern(RandomNumberGenerator random, PatternTag[] tag) {

        //Get a list of all patterns filtered to contain only patterns
        //with the given tags
        List<BoardPattern> filteredPatterns = new ArrayList<>();
        for(PatternTag t : tag) {
            //Get a list of all patterns with the current tag
            List<BoardPattern> patternListFromTag = patternMap.get(t);
            //If that item is not in the filtered list, add it
            for(BoardPattern item : patternListFromTag){
                if(item != null && !(filteredPatterns.contains(item))) {
                    filteredPatterns.add(item);
                }
            }
        }

        //If the given tag(s) map to no patterns, print an error and throw an invalid argument exception
        if(filteredPatterns.isEmpty()) {
            System.err.print("No elements for tag(s) ");
            for(PatternTag t : tag) {
                System.err.printf("'%s' ", t.name());
            }
            System.out.println();
            throw new IllegalArgumentException();
        }

        //Return a copy of a pattern from a random index in the filtered list
        return new BoardPattern(filteredPatterns.get((random.nextPositiveInt()%filteredPatterns.size())));
    }

    //Generates a new random board pattern which has the give tag and does not have one of the given excluded tags
    public BoardPattern genRandomPattern(RandomNumberGenerator random, PatternTag tag, PatternTag[] exclude) {

        //Get a list of all patterns filtered to contain only patterns
        //with the given tag
        List<BoardPattern> patterns = patternMap.get(tag);

        //Declare a list to store the patterns which have the given tag but not the excluded tags
        List<BoardPattern> filteredPatterns = new ArrayList<>();

        //For each element in patterns, check if it has any of the excluded tags.
        //If it does not, add it to filtered patterns
        outerLoop:
        for(BoardPattern p : patterns) {
            for(PatternTag e : exclude) {
                List<BoardPattern> patternListFromExclude = patternMap.get(e);

                if(patternListFromExclude.contains(p)) {
                    continue outerLoop; //Do not add this pattern to the filtered list, move on to the next pattern
                }
            }
            filteredPatterns.add(p);

        }

        //If the given tag(s) map to no patterns, print an error and throw an invalid argument exception
        if(filteredPatterns.isEmpty()) {
            System.err.printf("No elements with tag '%s' which excludes elements with tag(s) ", tag.name());
            for(PatternTag e : exclude) {
                System.err.printf("'%s' ", e.name());
            }
            System.out.println();
            throw new IllegalArgumentException();
        }

        //Return a copy of a pattern from a random index in the filtered list
        return new BoardPattern(filteredPatterns.get((random.nextPositiveInt()%filteredPatterns.size())));
    }


    //Generate a pattern which has one of the given tag(s)
    //Applies a random transformation
    public BoardPattern genRandomPatternWithRandomTransformation(RandomNumberGenerator random, int xLength, PatternTag[] tag) {
        //Get a copy of a random board pattern of the given tag
        BoardPattern randPattern = genRandomPattern(random, tag);

        //Apply transformations

        //50/50 chance to flip the pattern
        randPattern.setFlipped(random.nextBoolean());

        //randomly shift the board to the right by a number of tiles from 0 (inclusive) to xLength (exclusive)
        randPattern.setxOffset(random.nextPositiveInt()%xLength);

        return randPattern;
    }

    //Generate a pattern which has the given tag and does not have the excluded tags
    //Applies a random transformation
    public BoardPattern genRandomPatternWithRandomTransformation(RandomNumberGenerator random, int xLength, PatternTag tag, PatternTag[] exclude) {
        //Get a copy of a random board pattern of the given tag
        BoardPattern randPattern = genRandomPattern(random, tag, exclude);

        //Apply transformations

        //50/50 chance to flip the pattern
        randPattern.setFlipped(random.nextBoolean());

        //randomly shift the board to the right by a number of tiles from 0 (inclusive) to xLength (exclusive)
        randPattern.setxOffset(random.nextPositiveInt()%xLength);

        return randPattern;
    }

    //Generate a boardPattern of the given id with the given transformation
    public BoardPattern genPatternWithTransformation(int xOffset, boolean flipped, int patternId) {
        //Get a list of all boardPatterns
        List<BoardPattern> allPatterns = patternMap.get(PatternTag.any);

        //Find and copy the pattern which has the given patternId (throw NoSuchElementException if pattern is not found)
        BoardPattern pattern = new BoardPattern(findPatternById(patternId, allPatterns).orElseThrow());

        //Apply the specified transformations to the copied pattern
        pattern.setFlipped(flipped);
        pattern.setxOffset(xOffset);

        return pattern;
    }

    //Reads all patterns from a file and returns them in a structure
    //Path must be to a json file
    //file must contain a valid array of board patterns
    private HashMap<PatternTag,List<BoardPattern>> readPatternFile(String path) throws IOException {
        HashMap<PatternTag, List<BoardPattern>> patternMap = new HashMap<>();

        //read the json as one big blob of text
        String jsonString = new String(Files.readAllBytes(Paths.get(path)));
        JSONObject obj = new JSONObject(jsonString);

        JSONArray jsonPatterns = obj.getJSONArray("board_patterns");

        //Parse Json
        for (int i = 0; i < jsonPatterns.length(); i++)
        {
            //get id
            int id = jsonPatterns.getJSONObject(i).getInt("id");

            //get tags
            JSONArray jsonTags = jsonPatterns.getJSONObject(i).getJSONArray("tags");
            PatternTag[] tags = null;
            try {
                AtomicInteger counter = new AtomicInteger(0);
                tags = Arrays.stream(new String[jsonTags.length()])
                        .map(x -> jsonTags.getString(counter.getAndIncrement()))
                        .map(PatternTag::strTagToPatternTag)
                        .toList().toArray(new PatternTag[0]);
            } catch (TagFormatException e) {
                //If a tag in the json contains a typo, an exception will be thrown.
                System.err.println(e.getMessage());
                System.exit(1);
            }
            //get all lines
            JSONArray jsonLines = jsonPatterns.getJSONObject(i).getJSONArray("lines");
            String[] lines = new String[jsonLines.length()];
            for(int j = 0; j < jsonLines.length(); j++) {
                lines[j] = jsonLines.getString(j);
            }

            //Make BoardPattern, add to hashmap in appropriate locations
            BoardPattern pattern = new BoardPattern(id, tags, lines);

            //map pattern to each of it's tags
            for(PatternTag tag : tags) {
                //Map the element to this key in the hashMap
                //If this is the first element mapping to this key in the hashmap, create a new arrayList to store them
                patternMap.computeIfAbsent(tag, x -> new ArrayList<>()).add(pattern);
            }
            //Map this pattern to the "any" tag, which is
            patternMap.computeIfAbsent(PatternTag.any, x -> new ArrayList<>()).add(pattern);

        }
        return patternMap;
    }

    //Returns Optional BoardPattern, which has a boardPattern with the given id if it is present in the map.
    private Optional<BoardPattern> findPatternById(int patternId, List<BoardPattern> patternList) {
        return patternList.stream()
                .filter(item -> item.getId() == patternId)
                .findFirst();
    }
}
