//structure which stores a string of characters such that the character at the last index in the string is the
//character directly before the character at the first index of the string.
//Repeating strings of arbitrary length can be obtained using the built-in methods.
public class CyclicString {
    private final char[] charArr;
    private int currChar = 0;

    public CyclicString(String str)
    {
        charArr = str.toCharArray();
    }

    public char nextChar() {
        char c = charArr[currChar];
        currChar++;
        if (currChar >= charArr.length)
            currChar = 0;
        return c;
    }

    public void resetPos() {
        currChar = 0;
    }

    //Get the rest of the string
    public String getString() {
        return getString(charArr.length - currChar);
    }

    //Get amount characters from the string
    public String getString(int amount) {
        //Refactor to reuse code
        return getStringWithTransformation(amount, false, 0);

//        char[] result = new char[amount];
//
//        //Get the number of characters requested
//        //Check how many characters can be grabbed initially.
//        //  Grab them.
//        //if all in amount was grabbed, return
//        //  else, keep going until all grabbed
//        int currPos = 0;
//        int grabbable = Integer.min(amount, charArr.length - (currChar + 1));
//
//        System.arraycopy(charArr, currChar, result, currPos, grabbable);
//        amount -= grabbable;
//
//        while (amount != 0) {
//            currPos += grabbable;
//            currChar = 0;
//            grabbable = Integer.min(amount, charArr.length - 1);
//
//            System.arraycopy(charArr, 0, result, currPos, grabbable);
//            amount -= grabbable;
//        }
//
//        currChar += grabbable;
//
//        return new String(result);
    }

    //Get remaining characters from the string and apply transformation
    public String getStringWithTransformation(boolean reversed, int xOffset) {
        return getStringWithTransformation(charArr.length - currChar, reversed, xOffset);
    }

    //Get amount characters from the string and apply transformation
    public String getStringWithTransformation(int amount, boolean reversed, int xOffset) {
        char[] result = new char[amount];

        //skip forward by xOffset
        if(xOffset > 0) {
            getString(xOffset);
        }

        //Get the number of characters requested
        //Check how many characters can be grabbed initially.
        //  Grab them.
        //if all in amount was grabbed, return
        //  else, keep going until all grabbed
        int currPos = 0;
        int grabbable = Integer.min(amount, charArr.length - (currChar));

        //System.arraycopy(charArr, currChar, result, currPos, grabbable);
        for (int i = 0; i < grabbable; i++) {
            if(reversed) {
                result[result.length - 1 - i - currPos] = charArr[i+currChar];
            } else {
                result[i+currPos] = charArr[i+currChar];
            }
        }
        amount -= grabbable;
        while (amount != 0) {
            currPos += grabbable;
            currChar = 0;
            grabbable = Integer.min(amount, charArr.length);

            //System.arraycopy(charArr, 0, result, currPos, grabbable);
            for (int i = 0; i < grabbable; i++) {
                if(reversed) {
                    result[result.length - 1 - i - currPos] = charArr[i];
                } else {
                    result[i+currPos] = charArr[i];
                }
            }
            amount -= grabbable;
        }

        currChar += grabbable;

        return new String(result);
    }

    public byte[] getBytes(int amount) {
        //get charArray
        //Convert to string
        //get utf-8 bytes
        char[] returned = getString(amount).toCharArray();
        //System.out.println(returned.length);
        return new String(returned).getBytes(java.nio.charset.StandardCharsets.UTF_8);
        //return new String(getChars(amount)).getBytes(java.nio.charset.StandardCharsets.UTF_8);
    }


}
