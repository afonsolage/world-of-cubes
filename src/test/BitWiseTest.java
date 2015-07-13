package test;

/**
 *
 * @author Afonso Lage
 */
public class BitWiseTest {

    public static void main(String[] args) {
	int i = 0xFF0000FF;
	System.out.println(Integer.toHexString(i));
	short s = 0x10;
	i |= (s << 8);
	System.out.println(Integer.toHexString(i));
    }
 
    
    //01000100
    //10 << 3 
    //00010000
    //01010100
}
