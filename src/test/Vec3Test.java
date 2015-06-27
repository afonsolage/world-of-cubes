package test;

import com.lagecompany.storage.Vec3;

public class Vec3Test {
    
    public static void main(String[] args) {
	Vec3 v = new Vec3(10, 0, 0);
	v = v.addNew(new Vec3(-10, 0, 0));
	
	System.out.println(String.format("%s", v.equals(Vec3.ZERO)));
    }
}
