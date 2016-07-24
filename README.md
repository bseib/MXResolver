How to use:


	private Resolver res;
	
	private Test() {
		this.res = new Resolver();
	}

	private void test1() throws ResolverException {
		String[] hosts = res.getMXHosts("gentomi.com");
		for ( String h : hosts ) {
			System.out.println(h);
		}
	}
	
	
You should create a singleton instance of a Resolver, so that it may benefit from caching.

You will need to include the javadns-2.x.x.jar file in your project.
