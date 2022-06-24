package au.id.simo.tap2trip;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

/**
 *
 */
public class ChargeCalculatorTest {

    @Test
    public void testHappyPath() throws UnknownChargeException {
        ChargeCalculator calc = new ChargeCalculator();
        calc.addCharge("stop1", "stop2", 100);
        
        assertEquals(100,calc.getCharge("stop2", "stop1"));
        assertEquals(100,calc.getCharge("stop1", "stop2"));
        assertEquals(100,calc.getIncompleteCharge("stop1"));
        assertEquals(100,calc.getIncompleteCharge("stop2"));
    }

    @Test
    public void testUnknownFromStop()throws UnknownChargeException {
        ChargeCalculator calc = new ChargeCalculator();
        calc.addCharge("from", "to", 100);
        
        assertThrows(UnknownChargeException.class, () -> {
            calc.getCharge("unknown", "to");
        });
    }
    
    @Test
    public void testUnknownToStop()throws UnknownChargeException {
        ChargeCalculator calc = new ChargeCalculator();
        calc.addCharge("from", "to", 100);
        
        assertThrows(UnknownChargeException.class, () -> {
            calc.getCharge("from", "unknown");
        });
    }
    
    @Test
    public void testUnknownFromAndToStop()throws UnknownChargeException {
        ChargeCalculator calc = new ChargeCalculator();
        calc.addCharge("from", "to", 100);
        
        assertThrows(UnknownChargeException.class, () -> {
            calc.getCharge("unknown1", "unknown2");
        });
    }
    
    @Test
    public void testUnknownIncompleteStop()throws UnknownChargeException {
        ChargeCalculator calc = new ChargeCalculator();
        calc.addCharge("from", "to", 100);
        
        assertThrows(UnknownChargeException.class, () -> {
            calc.getIncompleteCharge("unknown");
        });
    }

    @Test
    public void testMaxCharge() throws UnknownChargeException {
        ChargeCalculator calc = new ChargeCalculator(); 
        calc.addCharge("hub", "spoke1", 10);
        
        assertEquals(10, calc.getIncompleteCharge("hub"));
    }
    
    @Test
    public void testMaxChargeWithMultipleCharges() throws UnknownChargeException {
        ChargeCalculator calc = new ChargeCalculator(); 
        calc.addCharge("hub", "spoke1", 10);
        calc.addCharge("hub", "spoke2", 20);
        calc.addCharge("hub", "spoke3", 30);
        
        assertEquals(30, calc.getIncompleteCharge("hub"));
    }
}
