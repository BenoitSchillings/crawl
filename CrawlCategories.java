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


public class CrawlCategories {
	static Map<String, Integer> currentDict = new HashMap<String, Integer>();
	static Map<String, Integer> mRefMap = null;
	static int	counter = 0;
	static int  known = 0;
	private static FileWriter tfstream;
	private static BufferedWriter links;

	static String get_url(String subject, int idx) throws Exception
	{

		String content;
		File file;


		try {
			char SubDir = subject.charAt(0);
			file = new File("/media/benoit/ssd/crawl/crawl/cache" + "/" + SubDir + "/" + subject + ".zip");
			ZipFile reader = new ZipFile(file);
			InputStream s = reader.getInputStream(reader.entries().nextElement());
			byte[] chars = new byte[s.available()];
			s.read(chars, 0, s.available());
			content = new String(chars);
			s.close();
			reader.close();
			return content;
		} catch (FileNotFoundException e) {
			System.out.println("fnf " + subject);
		}

		StringBuilder buf = new StringBuilder();
		try {
			URL url = new URL("http://en.wikipedia.org/wiki/" + subject);

			URLConnection con = url.openConnection();
			if (con == null)
				return null;


			Pattern p = Pattern.compile("text/html;\\s+charset=([^\\s]+)\\s*");
			Matcher m = p.matcher(con.getContentType());
			String charset = m.matches() ? m.group(1) : "ISO-8859-1";
			Reader r = new InputStreamReader(con.getInputStream(), charset);
			while (true) {
				int ch = r.read();
				if (ch < 0)
					break;
				buf.append((char) ch);
			}
		} catch (FileNotFoundException e) {
			System.out.println("no url");
		}

		String str = buf.toString();
		System.out.println("write");
		try {
		ZipOutputStream out = new ZipOutputStream(new FileOutputStream("/media/benoit/ssd/crawl/crawl/cache" + "/" + subject.charAt(0) + "/" + subject + ".zip"));

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

		load_reference();
		if (args[0].equals("cache")) {
			scanner_add_seed("Portal:Contents/Categories");
			loop_scanner(100);
		}
		dump_dict(args[0]);
	}

	@SuppressWarnings("unchecked")
	private static void load_reference() throws IOException, ClassNotFoundException {
		FileInputStream fis = new FileInputStream("reference.ser");
		ObjectInputStream ois = new ObjectInputStream(fis);

		mRefMap = (Map<String, Integer>)ois.readObject();	
		ois.close();
		fis.close();
	}
	
	private static void compare_dict() throws IOException, ClassNotFoundException {
		FileInputStream fis = new FileInputStream("reference.ser");
		ObjectInputStream ois = new ObjectInputStream(fis);
		@SuppressWarnings("unchecked")
		Map<String, Integer> referenceMap = (Map<String, Integer>) ois.readObject();

		Iterator<Map.Entry<String, Integer>> it = referenceMap.entrySet().iterator();

		//TreeMap<Float, String> output = new TreeMap<Float, String>();
		
		while (it.hasNext()) {
			Entry<String, Integer> entry = it.next();
			if (entry.getKey() != null) {
				String word = entry.getKey();
				int count = entry.getValue();
				//if (currentDict.containsKey(word)) {
					//int currentCount = 1 + currentDict.get(word);
					System.out.println(word + " " +  count);
					//output.put((float)count / (float)currentCount, word);
				//}
			}
		}
		/*
		Iterator<Map.Entry<Float, String>> topList = output.entrySet().iterator();

		while (topList.hasNext()) {
			Entry<Float, String> entry = topList.next();
			if (entry.getKey() != null) {
					System.out.println(entry.getKey() + " " +  entry.getValue());
			}
		}
		*/

	}

	private static void scanner_add_news_entries() {
		String[]	years = {"2011", "2012", "2013", "2014"};
		String[]	months = {"January", "February", "March", "April", "May", "June", "July", "August", "September", "October", "November", "December"};

		for (String year : years) {
			for (String month : months) {
				scanner_add_seed(month + "_" + year);
			}			
		}
	}

	private static void scanner_add_seed(String string) {
		System.out.println(string);
		currentDict.put(string, 0);		
	}

	private static void loop_scanner(int max_depth) throws Exception {
		int ccnt = max_depth;
		//links = new BufferedWriter(tfstream);
		boolean active = false;

		while(ccnt>0) {
			ccnt--;
			Object[] keys = currentDict.keySet().toArray();
			System.out.println("Count = " + keys.length + " " + counter);
			for (int idx = 0; idx < keys.length; idx++) {
				if (idx % 10000 == 0) {
					System.out.print(".");
				}
				String new_word = (String)keys[idx];
				int value = currentDict.get(new_word);
				if (value == 0) {
					scan(idx, new_word);
					currentDict.put(new_word, 1);	
					active = true;
				}
				else {
				}
			}
			if (active == false)
				break;
		}
	}

	@SuppressWarnings({ "rawtypes", "unchecked" })
	static Map<String, Integer> sortByValue(Map<String, Integer> map) {
		List list = new LinkedList(map.entrySet());
		Collections.sort(list, new Comparator() {
			public int compare(Object o1, Object o2) {
				return -((Comparable) ((Map.Entry) (o1)).getValue())
						.compareTo(((Map.Entry) (o2)).getValue());
			}
		});

		Map result = new LinkedHashMap();
		for (Iterator it = list.iterator(); it.hasNext();) {
			Map.Entry entry = (Map.Entry)it.next();
			result.put(entry.getKey(), entry.getValue());
		}
		return result;
	} 

	private static void dump_dict(String name) throws IOException {
		FileWriter fstream = new FileWriter("out.txt");
		BufferedWriter out = new BufferedWriter(fstream);

		Map<String, Integer> dict1 = sortByValue(currentDict);
		Iterator<Map.Entry<String, Integer>> it = dict1.entrySet().iterator();

		while (it.hasNext()) {
			Entry<String, Integer> entry = it.next();
			if (entry.getKey() != null) {
				String new_word = entry.getKey();
				int count = entry.getValue();
				if (count >= 0) {
					int cnt1 = 0;
					
					if (mRefMap.containsKey(new_word)) {
						cnt1 = mRefMap.get(new_word);
					}
					out.write(new_word + '\t' + Integer.toString(count) + " " + cnt1);
					out.newLine();
				}
			}
		}
		FileOutputStream fos = new FileOutputStream(name + ".ser");
		ObjectOutputStream oos = new ObjectOutputStream(fos);
		oos.writeObject(dict1);
		oos.close();

		//FileInputStream fis = new FileInputStream("map.ser");
		//ObjectInputStream ois = new ObjectInputStream(fis);
		//Map anotherMap = (Map) ois.readObject();
		oos.close();

		out.close();
	}

	private static void scan(int idx, String string) throws Exception {
		String markup;
		known = 0;
		try {
			markup = get_url(string, idx);
		} catch (UnsupportedEncodingException e) {
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
		//System.out.println("len is " + list_of_strings.size());
		int len = list_of_strings.size();
		for (int i = 0; i < len; i++) {
			System.out.println(string + " " + list_of_strings.get(i));
			String val = list_of_strings.get(i);
			if (!val.equals(string)) {
			}
			if (currentDict.get(val) != null) {
				int old_value;

				old_value = currentDict.get(val);
				currentDict.put(val, old_value + 1);
			}
			else {
				currentDict.put(val, 0);
				counter++;
			}
		}

	}



	private static List<String> parse_links(String markup) {
		List<String> list_of_links = new ArrayList<String>();
		String findStr = "<a href=\"/wiki/";


		int find_length = findStr.length();
		int lastIndex = 0;
		int length = markup.length();
		
		int max_length = markup.indexOf("mw-normal-catlinks");
		
		if (max_length > 0) {
			System.out.println("limit " + max_length);
			length = max_length;
		}
		
		max_length = markup.indexOf("id=\"References\"");
		if (max_length > 0) {
			if (max_length < length) {
				length = max_length;
				System.out.println("Trunc");
			}
		}
		
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
						if (cnt > 40)
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
		if (s.length() > 40)
			return false;


		for (int i=0; i<s.length(); i++) {
			char c = s.charAt(i);
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
		if (s.contains("Category:"))
			return false;
		if (s.startsWith("Lists_of_"))
			return false;
		if (s.startsWith("List_of_"))
			return false;
		return true;
	}
}
