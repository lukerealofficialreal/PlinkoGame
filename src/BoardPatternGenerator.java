//BoardPatterns.json has all of the board patterns which can appear in the game.
//
//each board pattern has an id, list of tags, and lines which represent tiles
//
//
//there are 6 categories of board patterns which can be filtered for depending on the circumstances

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

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import org.json.*;

public class BoardPatternGenerator {

    private HashMap<String, List<BoardPattern>> patternMap; //stores all pattern templates which can be generated

    //Instantiates a new board pattern generator from a json file
    public BoardPatternGenerator(String path) throws IOException {
        patternMap = readPatternFile(path); //load all pattern templates
    }

    //Generate a pattern of the given tag
    //Applies a random transformation
    public BoardPattern genPatternWithTransformation(String tag) {

    }

    //Reads all patterns from a file and returns them in a structure
    //Path must be to a json file
    //file must contain a valid array of board patterns
    private HashMap<String,List<BoardPattern>> readPatternFile(String path) throws IOException {
        HashMap<String, List<BoardPattern>> patternMap = new HashMap<>();

        //read the json as one big blob of text
        String jsonString = new String(Files.readAllBytes(Paths.get(path)));
        JSONObject obj = new JSONObject(jsonString);


        //String pageName = obj.getJSONObject("pageInfo").getString("pageName");

        JSONArray jsonPatterns = obj.getJSONArray("board_patterns");
        //for each board pattern

        for (int i = 0; i < jsonPatterns.length(); i++)
        {
            //get id
            int id = jsonPatterns.getJSONObject(i).getInt("id");

            //get tags
            JSONArray jsonTags = jsonPatterns.getJSONObject(i).getJSONArray("tags");
            PatternTag[] tags = new String[jsonTags.length()];
            for(int j = 0; j < jsonTags.length(); j++) {
                String tagStr = jsonTags.getString(j);
                PatternTag tag = BoardPattern.strTagToPatternTag();
                tags[j] = jsonTags.getString(j);
            }

            //get all lines
            JSONArray jsonLines = jsonPatterns.getJSONObject(i).getJSONArray("lines");
            String[] lines = new String[jsonLines.length()];
            for(int j = 0; j < jsonLines.length(); j++) {
                tags[j] = jsonLines.getString(j);
            }

            //Make BoardPattern, add to hashmap in appropriate locations
            BoardPattern pattern = new BoardPattern(id, tags, lines);

            //map pattern to each of it's tags
            for(String tag : tags) {
                //Map the element to this key in the hashMap
                //If this is the first element mapping to this key in the hashmap, create a new arrayList to store them
                patternMap.computeIfAbsent(tag, k -> new ArrayList<>()).add(pattern);
            }
        }
        return patternMap;
    }
}
