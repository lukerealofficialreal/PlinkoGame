package poker;
import java.util.ArrayList;

public class DeckBuilder {
    public static ArrayList<String> getStandardDeck() {
        String[] suits = {"S", "H", "D", "C"};
        String[] ranks = {"A", "2", "3", "4", "5", "6", "7", "8", "9", "10", "J", "Q", "K"};
        ArrayList<String> deck = new ArrayList<>();
        for (String suit : suits) {
            for (String rank : ranks) {
                deck.add(rank + suit);
            }
        }
        return deck;
    }
}
