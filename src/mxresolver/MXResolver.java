package mxresolver;

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
import org.xbill.DNS.Record;
import org.xbill.DNS.Resolver;
import org.xbill.DNS.SimpleResolver;
import org.xbill.DNS.TextParseException;
import org.xbill.DNS.Type;


public class MXResolver {
	
	final static private Logger logger = LoggerFactory.getLogger(MXResolver.class);

    final private int MX_CACHE_MAX_ENTRIES = 128;
    final private int MX_CACHE_TTL_SECONDS = 30 * 60; // 30 minutes is the life of an entry

    final private String nameserver;
	private Map<String, MXLookupResult> mxCache;

    
	public MXResolver() {
		this(null);
	}

	public MXResolver(String nameserver) {
		this.nameserver = nameserver;
		// create a LRU cache using a linked hash map.
	    Map<String, MXLookupResult> map = new LinkedHashMap<String, MXLookupResult>(MX_CACHE_MAX_ENTRIES + 1, .75F, true) {
			private static final long serialVersionUID = 1L;

			// This method is called just after a new entry has been added
	        public boolean removeEldestEntry(Map.Entry<String, MXLookupResult> eldest) {
	        	boolean isFull = ( this.size() > MX_CACHE_MAX_ENTRIES );
	        	if ( isFull ) {
	        		logger.debug("deleting oldest entry in cache; it has key: " + eldest.getKey());
	        	}
	            return isFull;
	        }
	    };
	    this.mxCache = Collections.synchronizedMap(map);
	}

	synchronized public String[] getMXHosts(String domain) throws MXResolverException {
		boolean doNewLookup = false;
		MXLookupResult answer = (MXLookupResult) mxCache.get(domain);
		if ( answer == null ) {
			logger.debug("cache did NOT contain answer for domain " + domain);
			doNewLookup = true;
		} else if ( answer.isExpired(MX_CACHE_TTL_SECONDS) ) {
			logger.debug("cache DID contain answer for domain " + domain + ", but was older than " + MX_CACHE_TTL_SECONDS + " seconds.");
			doNewLookup = true;
			// remove from cache? no it will subsequently be overwritten.
		} else {
			logger.debug("cache DID contain answer for domain " + domain + ", and is not expired.");
		}
		if ( doNewLookup ) {
			answer = getMXHostsDirectly(domain);
			logger.debug("a new MX lookup for domain " + domain + " yielded the MX hosts: " + answer.getHostListString());
			mxCache.put(domain,answer);
		}
		return answer.getHosts();
	}
	
	private MXLookupResult getMXHostsDirectly(String atHost) throws MXResolverException {

		try {
			Resolver resolver = (this.nameserver==null) ? new SimpleResolver() : new SimpleResolver(this.nameserver);

			// try to lookup the MX records
			Lookup lookup = new Lookup(atHost,Type.MX);
			lookup.setResolver(resolver);
			lookup.run();
			Record[] records = lookup.getAnswers();
			if ( records == null ) {
				//throw new MailSenderException("No MX records exist for this host.");
				// in the event there are no MX records, then punt and try to find A record
				Lookup lookup2 = new Lookup(atHost,Type.A);
				lookup2.setResolver(resolver);
				lookup2.run();
				records = lookup2.getAnswers();
				if ( records == null ) {
					throw new MXResolverException("No MX records or A records exist for '" + atHost + "'.");
				}
			}

			ArrayList<MXRecord> list = new ArrayList<MXRecord>(records.length);
			for (int i=0;i<records.length;i++) {
				if ( records[i] instanceof ARecord ) {
					ARecord rec = (ARecord) records[i];
					MXRecord mx = new MXRecord(
						rec.getName(),
						rec.getDClass(),
						rec.getTTL(),
						100,
						rec.getName()
					);
					list.add(mx);
				} else {
					list.add((MXRecord)records[i]);
				}
			}
			Collections.sort(list, new MXPriorityComparator());

			String answers[] = new String[list.size()];
			for (int i = 0; i < list.size(); i++) {
				MXRecord mx = list.get(i);
				answers[i] = mx.getTarget().toString();
// System.out.println("host="+mx.getTarget()+" priority="+mx.getPriority());
			}
			return new MXLookupResult(answers);
		} catch (TextParseException e) {
			throw new MXResolverException("Can only get this if you used the wrong constants in your code.");
		}
		catch (UnknownHostException e) {
			throw new MXResolverException("Can only get here if " + this.nameserver + " seems to be 'unknown'.");
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
					} else {
						buf.append(", ");
					}
					buf.append(h);
				}
				return buf.toString();
			}
			return null;
		}

		public String[] getHosts() {
			return hosts;
		}

		public boolean isExpired(int ttl_seconds) {
			long now = Calendar.getInstance().getTimeInMillis();
			if ( (now - this.timestamp) > (ttl_seconds * 1000) ) {
				return true;
			}
			return false;
		}
	}

}
