package test;


/**
 *
 * @author Afonso Lage
 */
public class BitWiseTest {

    public static void main(String[] args) {
        int x = 10;
        int y = 3;
        int z = 12;
        //2620

        long begin, end;
        int d;
        int SIZE = 16;
        int SIZE_SHIFT = 4;
        int SIZE_SHIFT_Y = 4;
        int SIZE_SHIFT_X = 8;
        
        
        d = 0;
        System.gc();
        begin = System.currentTimeMillis();
        for (int i = 0; i < Integer.MAX_VALUE; i++) {
            d = z + SIZE * (y + SIZE * x);
        }
        end = System.currentTimeMillis();

        System.out.println("Method 1: " + (end - begin));

        d = 0;
        System.gc();
        begin = System.currentTimeMillis();
        for (int i = 0; i < Integer.MAX_VALUE; i++) {
            d = z + ((y + (x << SIZE_SHIFT) << SIZE_SHIFT));
        }
        end = System.currentTimeMillis();

        System.out.println("Method 2: " + (end - begin));

        d = 0;
        System.gc();
        begin = System.currentTimeMillis();
        for (int i = 0; i < Integer.MAX_VALUE; i++) {
            d = (x << SIZE_SHIFT_X) + (y << SIZE_SHIFT_Y) + z;
        }
        end = System.currentTimeMillis();

        System.out.println("Method 3: " + (end - begin));
    }

    //01000100
    //10 << 3 
    //00010000
    //01010100
}
