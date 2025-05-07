import main.java.plinko.game.CyclicString;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class CyclicStringTest {

    private CyclicString cycStr;

    @BeforeEach
    void setUp() {
        cycStr = new CyclicString("|123456|");
    }

    @Test
    void testGetString() {
        assertEquals("|123456|", cycStr.getString());
    }
    @Test
    void testGetStringFlipped() {
        assertEquals("|654321|", cycStr.getStringWithTransformation(true,0));
    }
    @Test
    void testGetStringOffset() {
        assertEquals("3456||12", cycStr.getStringWithTransformation(false,3));
    }
    @Test
    void testGetStringOffsetAndFlipped() {
        assertEquals("21||6543", cycStr.getStringWithTransformation(true,3));
    }



    @Test
    void testGetLessThanFullString() {
        assertEquals("|1234", cycStr.getString(5));
    }

    @Test
    void testGetMoreThanFullString() {
        assertEquals("|123456||123456||12", cycStr.getString(19));
    }

    @Test
    void testGetStringMultiple() {
        assertEquals("|123456||1", cycStr.getStringWithTransformation(10, false,0));
        assertEquals("23456||1", cycStr.getStringWithTransformation(8, false,0));
    }

    @Test
    void testGetRestOfString() {
        assertEquals("|123456||1", cycStr.getString(10));
        assertEquals("23456|", cycStr.getString());
    }

    @Test
    void testGetStringForwardsBackwardsForwards() {
        assertEquals("|12345", cycStr.getString(6));
        assertEquals("1||6", cycStr.getStringWithTransformation(4, true, 0));
        assertEquals("2345", cycStr.getStringWithTransformation(4, false, 0));
    }




}