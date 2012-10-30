package mxresolver;

public class Test {
	
	static public void main(String[] args) throws Exception {
		Test t = new Test();
		t.test1();
		t.test2();
	}
	
	private MXResolver mxr;
	
	private Test() {
		this.mxr = new MXResolver();
	}

	private void test1() throws MXResolverException {
		String[] hosts = mxr.getMXHosts("gentomi.com");
		for ( String h : hosts ) {
			System.out.println(h);
		}
	}

	private void test2() throws MXResolverException {
		String[] hosts = mxr.getMXHosts("gmail.com");
		for ( String h : hosts ) {
			System.out.println(h);
		}
		hosts = mxr.getMXHosts("gentomi.com");
		for ( String h : hosts ) {
			System.out.println(h);
		}
	}

}
