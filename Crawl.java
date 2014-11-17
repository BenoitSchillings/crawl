import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Reader;
import java.util.zip.GZIPOutputStream;
import java.util.zip.GZIPInputStream;
import java.util.zip.Inflater;
import java.util.zip.InflaterInputStream;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;
import java.util.zip.ZipOutputStream;
import java.io.UnsupportedEncodingException;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLConnection;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Dictionary;
import java.util.HashMap;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.SortedMap;
import java.util.TreeMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;


public class Crawl {
	static Map<String, Integer> currentDict = new HashMap<String, Integer>();
	static Map<String, Integer> mRefMap = null;
	static int	counter = 0;
	static int  known = 0;
	private static FileWriter tfstream;
	private static BufferedWriter links;
	static private FileWriter log;
	static ObjectOutputStream out;
	static String name_to_dir(String name)
	{
		if (name.startsWith("Cat"))
			return name.substring(0, 3) + name.hashCode() % 256;
		else
			return "cache" + name.hashCode() % 2048;
	}
	static String get_url(String subject, boolean reload) throws Exception
	{

		String content;
		File file;
		String path_to_cache = "/media/benoit/09d1f277-6968-4ef1-9018-453bdfde4ce2/cache/";

		if (reload == false)
		try {
			String SubDir =  path_to_cache + name_to_dir(subject);
			file = new File( SubDir  + "/" + subject + ".zip");
			ZipFile reader = new ZipFile(file);
			InputStream s = reader.getInputStream(reader.entries().nextElement());
			byte[] chars = new byte[s.available()];
			s.read(chars, 0, s.available());
			content = new String(chars);
			if (!content.startsWith("<!DOCTYPE html>")) {
				throw(new Exception());
			}
			//System.out.println("Found+" + subject);
			s.close();
			reader.close();
			return content;
		} catch (Exception e) {
			//System.out.println("fnf " + subject + " " + e.toString());
		}
		

		StringBuilder buf = new StringBuilder();
		try {
			URL url = new URL("http://en.wikipedia.org/wiki/" + subject);

			URLConnection con = url.openConnection();
			if (con == null)
				return null;
			//con.setRequestProperty("Accept-Encoding", "gzip, deflate");

			Pattern p = Pattern.compile("text/html;\\s+charset=([^\\s]+)\\s*");
			Matcher m = p.matcher(con.getContentType());
			String charset = m.matches() ? m.group(1) : "ISO-8859-1";
			
			
			String encoding = con.getContentEncoding();
			//System.out.println(encoding);
			InputStream inStr = null;

			// create the appropriate stream wrapper based on
			// the encoding type
			if (encoding != null && encoding.equalsIgnoreCase("gzip")) {
			    inStr = new GZIPInputStream(con.getInputStream());
			} else if (encoding != null && encoding.equalsIgnoreCase("deflate")) {
			    inStr = new InflaterInputStream(con.getInputStream(),
			      new Inflater(true));
			} else {
			    inStr = con.getInputStream();
			}
			
			
			Reader r = new InputStreamReader(con.getInputStream(), charset);
			while (true) {
				int ch = r.read();
				if (ch < 0)
					break;
				buf.append((char) ch);
			}
			r.close();
		
		} catch (FileNotFoundException e) {
			System.out.println("no url");
		}

		String str = buf.toString();
		//System.out.println("write");
		try {
			String SubDir = path_to_cache + name_to_dir(subject);
			System.out.println("new" + " " + SubDir  + "/" + subject + ".zip");
			new File(SubDir).mkdir();
			SubDir = path_to_cache + name_to_dir(subject);
			ZipOutputStream out = new ZipOutputStream(new FileOutputStream(SubDir  + "/" + subject + ".zip"));

			out.putNextEntry(new ZipEntry("subject"));
			out.write(str.getBytes(Charset.forName("UTF-8")));
			out.close();
		} catch (FileNotFoundException e) {
			System.out.println("bad filename");
		}

		return str;
	}


	public static void main(String[] args) throws Exception {
		int	max_depth;

		//java Crawl cache : Rescan the whole cache

		loop_scanner(111);
	}




	private static void loop_scanner(int max_depth) throws Exception {
			//scan(24, "Special:Statistics");
			scan(14, "Portal:Current_events");
	}



	private static void scan(int level, String string) throws Exception {
		String markup;
		known = 0;
		level--;


		counter++;
		if (counter % 10000 == 0) {
			System.out.println("Scan " + string + " " + level);
		}
		try {
			markup = get_url(string, false);
		} catch (UnsupportedEncodingException e) {
			System.out.println("failed get_url " + e);
			
			return;
		} catch (IOException e) {
			System.out.println("failed get_url " + e);
			return;
		}
		if (markup == null) {
			System.out.println("empty load for " + string);	
			return;
		}

		

		List<String> list_of_strings = parse_links(markup);
		int len = list_of_strings.size();
		if (len == 0) {
			//System.out.println("up");
			return;
		}
		

		
		if (level < 0)
			return;
		
		
		for (int i = 0; i < len; i++) {
			String val = list_of_strings.get(i);
			scan(level, val);
		}
		
	}



	private static List<String> parse_links(String markup) {
		List<String> list_of_links = new ArrayList<String>();
		String findStr = "<a href=\"/wiki/";


		int find_length = findStr.length();
		int lastIndex = 0;
		int length = markup.length();
		while(lastIndex != -1) {

			lastIndex = markup.indexOf(findStr, lastIndex);
			//System.out.println("index " + lastIndex);
			if( lastIndex != -1) {
				if (markup.charAt(lastIndex - 1) == ' ' || markup.charAt(lastIndex - 1) == '>') {

					char c;
					int cnt = 0;
					do {
						int	idx = lastIndex + find_length + cnt;

						if (idx >= length)
							break;
						if (cnt > 160)
							break;
						c = markup.charAt(idx);
						if (c != '\"') {
							cnt++;
						}
					}
					while (c != '\"');

					String s;

					s = markup.substring(lastIndex + find_length, lastIndex + find_length + cnt);
					//System.out.println(s);
					if (check_link(s)) {
						if (!list_of_links.contains(s))
							list_of_links.add(s);

					}
				}
				lastIndex += findStr.length();
			}
		}


		return list_of_links;
	}

	private static boolean check_link(String s) {
		if (noisyWord(s))
			return false;
		
		if (s.length() < 3)
			
			return false;
		if (s.length() > 150)
			return false;


		for (int i=0; i<s.length(); i++) {
			char c = s.charAt(i);
			if (c == ':')
				return false;
			if (c == '%')
				return false;
			if (c == '#')
				return false;
			if (c == '/')
				return false;
			if (c == ',')
				return false;
			if (c == '(')
				return false;
			if (c == ')')
				return false;
		}

		char c = s.charAt(0);
		if (c >= 'A' && c <= 'Z')
			return true;
		if (c >= '0' && c <= '9')
			return true;

		return false;

	}

	private static boolean noisyWord(String s) {
		if (mRefMap == null)
			return false;
		if (!mRefMap.containsKey(s))
			return false;
		int count = mRefMap.get(s);
		if (count > 2000 || count < 0) {
			System.out.println("noise " + s);
			return true;
		}
		System.out.println("good " + s);
		return false;
	}
}
