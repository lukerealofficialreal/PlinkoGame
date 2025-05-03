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
    void testGetLessThanFullString() {
        assertEquals("|1234", cycStr.getString(5));
    }

    @Test
    void testGetMoreThanFullString() {
        assertEquals("|123456||123456||12", cycStr.getString(19));
    }

}