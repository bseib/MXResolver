How to use:


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
	
	
You should create a singleton instance of a MXResolver, so that it may benefit from caching.

You will need to include the javadns-2.x.x.jar file in your project.
