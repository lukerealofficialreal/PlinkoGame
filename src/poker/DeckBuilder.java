package poker;
import java.util.ArrayList;

public class DeckBuilder {
    public static ArrayList<String> getStandardDeck() {
        String[] ranks = {"A", "2", "3", "4", "5", "6", "7", "8", "9", "10", "J", "Q", "K"};
        ArrayList<String> deck = new ArrayList<>();
        for (int i = 0; i < 4; i++) {
            for (String rank : ranks) {
                deck.add(rank);
            }
        }
        return deck;
    }
}
