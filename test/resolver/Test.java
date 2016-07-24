package resolver;

public class Test {

	static public void main(String[] args) throws Exception {
		Test t = new Test();
		t.test1();
		t.test2();
		t.test3();
		t.test4();
	}

	private Resolver mxr;

	private Test() {
		this.mxr = new Resolver();
	}

	private void test1() throws ResolverException {
		String[] hosts = mxr.getMXHosts("gentomi.com");
		for ( String h : hosts ) {
			System.out.println(h);
		}
	}

	private void test2() throws ResolverException {
		String[] hosts = mxr.getMXHosts("gmail.com");
		for ( String h : hosts ) {
			System.out.println(h);
		}
		hosts = mxr.getMXHosts("gentomi.com");
		for ( String h : hosts ) {
			System.out.println(h);
		}

		hosts = mxr.getMXHosts("gentomi.blork");
		for ( String h : hosts ) {
			System.out.println(h);
		}
	}

	private void test3() throws ResolverException {
		String[] hosts = mxr.getHosts("157.55.39.169"); // 169.39.55.157.in-addr.arpa name =
														// msnbot-157-55-39-169.search.msn.com.
		for ( String h : hosts ) {
			System.out.println(h);
		}
	}

	private void test4() throws ResolverException {
		// this is an IP6 addr for google.com
		// Non-authoritative answer:
		// e.0.0.2.0.0.0.0.0.0.0.0.0.0.0.0.e.0.8.0.9.0.0.4.0.b.8.f.7.0.6.2.ip6.arpa name = ord30s25-in-x200e.1e100.net.
		// e.0.0.2.0.0.0.0.0.0.0.0.0.0.0.0.e.0.8.0.9.0.0.4.0.b.8.f.7.0.6.2.ip6.arpa name = ord30s25-in-x0e.1e100.net.

		String[] hosts = mxr.getHosts("2607:f8b0:4009:80e::200e"); // google.com
		for ( String h : hosts ) {
			System.out.println(h);
		}
	}

}
