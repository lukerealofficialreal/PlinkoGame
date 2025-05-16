package main.java.plinko.model;

import main.java.plinko.Exceptions.TagFormatException;

import java.io.Serializable;

//The tags which a board pattern can make use of
//"any" is a default tag which refers to all tags
public enum PatternTag implements Serializable {
    any, dropper, score_pit, below_dropper, above_pit, standard, sparse;

    //Converts a string into a PatternTag
    //Returns null if the given string does not represent a corresponding PatternTag
    public static PatternTag strTagToPatternTag(String strTag) throws TagFormatException {
        try {
            return PatternTag.valueOf(strTag);
        } catch (IllegalArgumentException e) {
            throw new TagFormatException(strTag);
        }
    }
}
