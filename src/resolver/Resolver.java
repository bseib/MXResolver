package resolver;

import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.xbill.DNS.ARecord;
import org.xbill.DNS.Lookup;
import org.xbill.DNS.MXRecord;
import org.xbill.DNS.Name;
import org.xbill.DNS.PTRRecord;
import org.xbill.DNS.Record;
import org.xbill.DNS.ReverseMap;
import org.xbill.DNS.SimpleResolver;
import org.xbill.DNS.TextParseException;
import org.xbill.DNS.Type;

public class Resolver {

	final static private Logger logger = LoggerFactory.getLogger(Resolver.class);

	final private int MX_CACHE_MAX_ENTRIES = 128;
	final private int MX_CACHE_TTL_SECONDS = 30 * 60; // 30 minutes is the life of an entry

	final private String nameserver;
	private Map<String, MXLookupResult> mxCache;

	public Resolver() {
		this(null);
	}

	public Resolver(String nameserver) {
		this.nameserver = nameserver;
		// create a LRU cache using a linked hash map.
		Map<String, MXLookupResult> map = new LinkedHashMap<String, MXLookupResult>(MX_CACHE_MAX_ENTRIES + 1, .75F, true) {
			private static final long serialVersionUID = 1L;

			// This method is called just after a new entry has been added
			public boolean removeEldestEntry(Map.Entry<String, MXLookupResult> eldest) {
				boolean isFull = (this.size() > MX_CACHE_MAX_ENTRIES);
				if ( isFull ) {
					logger.debug("deleting oldest entry in cache; it has key: " + eldest.getKey());
				}
				return isFull;
			}
		};
		this.mxCache = Collections.synchronizedMap(map);
	}

	synchronized public String[] getHosts(String ipAddress) throws ResolverException {
		boolean doNewLookup = false;
		MXLookupResult answer = (MXLookupResult) mxCache.get(ipAddress);
		if ( answer == null ) {
			logger.debug("cache did NOT contain answer for address '" + ipAddress + "'");
			doNewLookup = true;
		}
		else if ( answer.isExpired(MX_CACHE_TTL_SECONDS) ) {
			logger.debug("cache DID contain answer for address '" + ipAddress + "', but was older than " + MX_CACHE_TTL_SECONDS + " seconds.");
			doNewLookup = true;
			// remove from cache? no it will subsequently be overwritten.
		}
		else {
			logger.debug("cache DID contain answer for address '" + ipAddress + "', and is not expired.");
		}
		if ( doNewLookup ) {
			try {
				Name reverseMap = ReverseMap.fromAddress(ipAddress);
				answer = getHostsDirectly(reverseMap);
				if ( !answer.isEmpty() ) {
					logger.debug("a new hostname lookup for address '" + ipAddress + "' yielded the hosts: " + answer.getHostListString());
					mxCache.put(ipAddress, answer);
				}
				else {
					logger.debug("No PTR records exist for address '" + ipAddress + "'.");
				}
			}
			catch ( UnknownHostException e ) {
				throw new ResolverException(e.getMessage());
			}
		}
		return answer.getHosts();
	}

	synchronized public String[] getMXHosts(String domain) throws ResolverException {
		boolean doNewLookup = false;
		MXLookupResult answer = (MXLookupResult) mxCache.get(domain);
		if ( answer == null ) {
			logger.debug("cache did NOT contain answer for domain '" + domain + "'");
			doNewLookup = true;
		}
		else if ( answer.isExpired(MX_CACHE_TTL_SECONDS) ) {
			logger.debug("cache DID contain answer for domain '" + domain + "', but was older than " + MX_CACHE_TTL_SECONDS + " seconds.");
			doNewLookup = true;
			// remove from cache? no it will subsequently be overwritten.
		}
		else {
			logger.debug("cache DID contain answer for domain '" + domain + "', and is not expired.");
		}
		if ( doNewLookup ) {
			answer = getMXHostsDirectly(domain);
			if ( !answer.isEmpty() ) {
				logger.debug("a new MX lookup for domain '" + domain + "' yielded the MX hosts: " + answer.getHostListString());
				mxCache.put(domain, answer);
			}
			else {
				logger.debug("No MX records or A records exist for domain '" + domain + "'.");
			}
		}
		return answer.getHosts();
	}

	private PTRLookupResult getHostsDirectly(Name ipAddress) throws ResolverException {
		try {
			org.xbill.DNS.Resolver resolver = (this.nameserver == null) ? new SimpleResolver() : new SimpleResolver(this.nameserver);

			// try to lookup the MX records
			Lookup lookup = new Lookup(ipAddress, Type.PTR);
			lookup.setResolver(resolver);
			lookup.run();
			Record[] records = lookup.getAnswers();
			if ( records == null ) {
				// there are no PTR records. we're done here
				// the caller will have to check for an empty result and decide not to put it in the cache.
				return new PTRLookupResult(null);
			}

			ArrayList<PTRRecord> list = new ArrayList<PTRRecord>(records.length);
			for ( int i = 0; i < records.length; i++ ) {
				if ( records[i] instanceof PTRRecord ) {
					list.add((PTRRecord) records[i]);
				}
			}
			// Collections.sort(list, new MXPriorityComparator());

			String answers[] = new String[list.size()];
			for ( int i = 0; i < list.size(); i++ ) {
				PTRRecord rec = list.get(i);
				answers[i] = rec.getTarget().toString();
				// System.out.println("host="+mx.getTarget()+" priority="+mx.getPriority());
			}
			return new PTRLookupResult(answers);
		}
//		catch ( TextParseException e ) {
//			throw new ResolverException("Can only get this if you used the wrong constants in your code.");
//		}
		catch ( UnknownHostException e ) {
			throw new ResolverException("Can only get here if " + this.nameserver + " seems to be 'unknown'.");
		}
	}

	private MXLookupResult getMXHostsDirectly(String atHost) throws ResolverException {

		try {
			org.xbill.DNS.Resolver resolver = (this.nameserver == null) ? new SimpleResolver() : new SimpleResolver(this.nameserver);

			// try to lookup the MX records
			Lookup lookup = new Lookup(atHost, Type.MX);
			lookup.setResolver(resolver);
			lookup.run();
			Record[] records = lookup.getAnswers();
			if ( records == null ) {
				// in the event there are no MX records, then punt and try to find A record
				Lookup lookup2 = new Lookup(atHost, Type.A);
				lookup2.setResolver(resolver);
				lookup2.run();
				records = lookup2.getAnswers();
				if ( records == null ) {
					// the caller will have to check for an empty result and decide not to put it in the cache.
					return new MXLookupResult(null);
				}
			}

			ArrayList<MXRecord> list = new ArrayList<MXRecord>(records.length);
			for ( int i = 0; i < records.length; i++ ) {
				if ( records[i] instanceof ARecord ) {
					ARecord rec = (ARecord) records[i];
					MXRecord mx = new MXRecord(rec.getName(), rec.getDClass(), rec.getTTL(), 100, rec.getName());
					list.add(mx);
				}
				else {
					list.add((MXRecord) records[i]);
				}
			}
			Collections.sort(list, new MXPriorityComparator());

			String answers[] = new String[list.size()];
			for ( int i = 0; i < list.size(); i++ ) {
				MXRecord mx = list.get(i);
				answers[i] = mx.getTarget().toString();
				// System.out.println("host="+mx.getTarget()+" priority="+mx.getPriority());
			}
			return new MXLookupResult(answers);
		}
		catch ( TextParseException e ) {
			throw new ResolverException("Can only get this if you used the wrong constants in your code.");
		}
		catch ( UnknownHostException e ) {
			throw new ResolverException("Can only get here if " + this.nameserver + " seems to be 'unknown'.");
		}
	}

	private class MXPriorityComparator implements Comparator<MXRecord> {

		public int compare(MXRecord mx1, MXRecord mx2) {
			if ( mx1.getPriority() > mx2.getPriority() ) {
				return 1;
			}
			else if ( mx1.getPriority() < mx2.getPriority() ) {
				return -1;
			}
			return 0;
		}

	}

	private class MXLookupResult {

		final private String[] EMPTY_HOSTS = new String[0];
		private String hosts[];
		private long timestamp;

		MXLookupResult(String hosts[]) {
			this.hosts = hosts;
			this.timestamp = Calendar.getInstance().getTimeInMillis();
		}

		public String getHostListString() {
			if ( hosts != null ) {
				StringBuilder buf = null;
				for ( String h : hosts ) {
					if ( buf == null ) {
						buf = new StringBuilder();
					}
					else {
						buf.append(", ");
					}
					buf.append(h);
				}
				return buf.toString();
			}
			return null;
		}

		public String[] getHosts() {
			if ( hosts == null ) {
				return EMPTY_HOSTS;
			}
			return hosts;
		}

		public boolean isEmpty() {
			if ( hosts == null ) {
				return true;
			}
			return false;
		}

		public boolean isExpired(int ttl_seconds) {
			long now = Calendar.getInstance().getTimeInMillis();
			if ( (now - this.timestamp) > (ttl_seconds * 1000) ) {
				return true;
			}
			return false;
		}
	}

	private class PTRLookupResult extends MXLookupResult {

		PTRLookupResult(String[] hosts) {
			super(hosts);
			// TODO Auto-generated constructor stub
		}

	}

}
