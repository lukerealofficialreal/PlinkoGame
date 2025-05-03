public class PlinkoGame {
    public static void main(String[] args) {
        //System.out.println("Plinko test");

        //test cyclic string transformations
        //String test = "|123456|";
        //CyclicString cycStr = new CyclicString(test);

        //System.out.println(test);
        //System.out.println(cycStr.getString());
        //cycStr.resetPos();
        //System.out.println(cycStr.getString(3));

        //Test board
        PlinkoBoard plinkoBoard = new PlinkoBoard(3);
        String[] boardText = plinkoBoard.getBoardAsText();
        for(String str : boardText) {
            System.out.println(str);
        }

    }
}
